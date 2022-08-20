@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.badoualy.storyeditor

import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import com.github.badoualy.storyeditor.component.EditorDeleteButton
import com.github.badoualy.storyeditor.util.horizontalPadding
import com.github.badoualy.storyeditor.util.verticalPadding
import com.github.badoualy.storyeditor.util.withPrevious
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

/*
    TODO:
        * when dragging, exclude dynamically from gesture navigation the pointer region
 */
@Composable
fun StoryEditor(
    elements: ImmutableList<StoryElement>,
    onClick: () -> Unit,
    onDeleteElement: (StoryElement) -> Unit,
    background: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    state: StoryEditorState = remember { StoryEditorState() },
    shape: Shape = RectangleShape,
    content: @Composable StoryEditorScope.(StoryElement) -> Unit
) {
    // When used in a Pager, without wrapping in a key, the size is never reported, investigate
    key(state) {
        ScreenshotContent(
            state = state,
            layer = ScreenshotLayer.EDITOR,
            modifier = modifier
        ) {
            StoryEditorContent(
                state = state,
                elements = elements,
                onClick = onClick,
                onDeleteElement = onDeleteElement,
                background = background,
                shape = shape,
                content = content
            )
        }
    }
}

@Composable
private fun StoryEditorContent(
    state: StoryEditorState,
    elements: ImmutableList<StoryElement>,
    onClick: () -> Unit,
    onDeleteElement: (StoryElement) -> Unit,
    modifier: Modifier = Modifier,
    background: @Composable () -> Unit,
    shape: Shape = RectangleShape,
    content: @Composable StoryEditorScope.(StoryElement) -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Background
        ScreenshotContent(
            state = state,
            layer = ScreenshotLayer.BACKGROUND,
            modifier = Modifier.clip(shape)
        ) {
            Box(
                modifier = Modifier
                    .onSizeChanged { state.updateBackgroundSize(it) }
                    .pointerInput(state, state.editMode) {
                        if (!state.editMode) return@pointerInput

                        detectTapGestures {
                            if (state.focusedElement != null) return@detectTapGestures
                            if (state.draggedElement != null) return@detectTapGestures
                            onClick()
                        }
                    }
            ) {
                background()
            }
        }

        // Edit mode actions
        if (state.editMode) {
            // Stop edit on keyboard manual close
            val focusManager = LocalFocusManager.current
            val isImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)
            if (isImeVisible) {
                LaunchedEffect(Unit) {
                    snapshotFlow { isImeVisible }
                        .filter { !it && state.focusedElement != null }
                        .collect { focusManager.clearFocus() }
                }
            }

            // Animate to/out of edit state
            val coroutineScope = rememberCoroutineScope()
            LaunchedEffect(Unit) {
                snapshotFlow { state.focusedElement }
                    .withPrevious()
                    .collect { (previous, value) ->
                        if (previous != null && !previous.stopEdit()) {
                            coroutineScope.launch {
                                onDeleteElement(previous)
                            }
                        }

                        coroutineScope.launch {
                            value?.startEdit()
                        }
                    }
            }
        }

        // Elements
        ScreenshotContent(
            state = state,
            layer = ScreenshotLayer.ELEMENTS,
            modifier = Modifier.fillMaxSize()
        ) {
            val scope = remember(state) { StoryEditorScopeImpl(state) }
            with(scope) {
                elements.forEach { element ->
                    key(element) {
                        val isFocusedElement by remember(state) { derivedStateOf { state.focusedElement == element } }

                        Box(
                            // Make sure the focus element is on top of others
                            modifier = Modifier.zIndex(if (isFocusedElement) 1f else 0f)
                        ) {
                            content(element)
                        }
                    }
                }
            }
        }

        // Controls overlay
        if (state.editMode) {
            val editorSizeDp = with(LocalDensity.current) { state.editorSize.toSize().toDpSize() }
            Box(modifier = Modifier.size(editorSizeDp)) {
                EditorDeleteButton(
                    editorState = state,
                    onDelete = onDeleteElement,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

interface StoryEditorScope {

    val editorState: StoryEditorState

    fun Modifier.elementFocus(element: StoryElement, focusRequester: FocusRequester): Modifier

    fun Modifier.elementTransformation(
        element: TransformableStoryElement,
        clickEnabled: Boolean,
        onClick: () -> Unit,
        hitboxPadding: PaddingValues = PaddingValues(0.dp)
    ): Modifier
}

private class StoryEditorScopeImpl(
    override val editorState: StoryEditorState
) : StoryEditorScope {

    override fun Modifier.elementFocus(
        element: StoryElement,
        focusRequester: FocusRequester
    ): Modifier {
        return composed(
            "StoryEditorScopeImpl.elementFocus",
            element,
            focusRequester,
            editorState.editMode
        ) {
            if (!editorState.editMode) return@composed Modifier

            // Check for initial focus when entering composition
            // Setting hasFocus = true before entering composition doesn't work, because onFocusChanged is called before :/
            val requestInitialFocus =
                remember(editorState) { editorState.focusedElement == element }
            var waitingForInitialFocus by remember { mutableStateOf(requestInitialFocus) }
            if (requestInitialFocus) {
                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }

            Modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (waitingForInitialFocus && !it.hasFocus) {
                        // Ignore event if we didn't have initial focus yet
                        return@onFocusChanged
                    }
                    if (it.hasFocus) {
                        editorState.focusedElement = element
                    } else if (editorState.focusedElement == element) {
                        editorState.focusedElement = null
                    }
                    waitingForInitialFocus = false
                }
        }
    }

    override fun Modifier.elementTransformation(
        element: TransformableStoryElement,
        clickEnabled: Boolean,
        onClick: () -> Unit,
        hitboxPadding: PaddingValues
    ): Modifier {
        val transformation = element.transformation
        return this
            .detectAndApplyTransformation(element)
            .dragListener(element)
            .pointerInput(clickEnabled) {
                if (!clickEnabled) return@pointerInput

                detectTapGestures {
                    if (editorState.draggedElement != null) return@detectTapGestures
                    if (editorState.focusedElement != null) return@detectTapGestures
                    if (!transformation.gesturesEnabled) return@detectTapGestures

                    onClick()
                }
            }
            .hitbox(transformation = transformation, paddingValues = hitboxPadding)
    }

    private fun Modifier.detectAndApplyTransformation(
        element: TransformableStoryElement
    ): Modifier {
        val transformation = element.transformation
        return this
            .graphicsLayer {
                scaleX = transformation.displayScale
                scaleY = transformation.displayScale
                rotationZ = transformation.displayRotation
                val position = transformation.scaledHitboxPosition(
                    editorSize = editorState.editorSize
                )
                translationX = position.x
                translationY = position.y
            }
            .then(if (editorState.debug) Modifier.border(1.dp, Color.Red) else Modifier)
            .pointerInput(editorState, editorState.editMode, element) {
                if (!editorState.editMode) return@pointerInput

                detectTransformGestures { _, pan, zoom, rotation ->
                    if (editorState.draggedElement != element) return@detectTransformGestures
                    if (editorState.focusedElement != null) return@detectTransformGestures
                    if (!transformation.gesturesEnabled) return@detectTransformGestures

                    transformation.updateScale(
                        scale = (transformation.scale * zoom),
                        bounds = editorState.elementsBounds
                    )
                    transformation.updateRotation(
                        rotation = transformation.rotation + rotation
                    )
                    transformation.updatePosition(
                        pan = pan,
                        editorSize = editorState.editorSize,
                        bounds = editorState.elementsBounds
                    )
                }
            }
    }

    private fun Modifier.hitbox(
        transformation: StoryElementTransformation,
        paddingValues: PaddingValues
    ): Modifier {
        return composed(
            "StoryEditorScopeImpl.hitbox",
            editorState,
            transformation,
            paddingValues
        ) {
            val density = LocalDensity.current
            val hitboxPaddingSize = with(density) {
                IntSize(
                    width = paddingValues.horizontalPadding().roundToPx(),
                    height = paddingValues.verticalPadding().roundToPx()
                )
            }
            Modifier
                .padding(paddingValues)
                .onSizeChanged {
                    transformation.updateSize(
                        size = it,
                        sizeDp = with(density) {
                            DpSize(
                                width = it.width.toDp(),
                                height = it.height.toDp()
                            )
                        },
                        hitboxSize = IntSize(
                            width = it.width + hitboxPaddingSize.width,
                            height = it.height + hitboxPaddingSize.height
                        ),
                        editorSize = editorState.editorSize
                    )
                }
        }
    }

    private fun Modifier.dragListener(element: TransformableStoryElement): Modifier {
        return this.pointerInput(
            editorState,
            editorState.editMode,
            element
        ) {
            if (!editorState.editMode) return@pointerInput
            val transformation = element.transformation

            // see https://stackoverflow.com/a/68689713/1014223
            // Notify gesture end events
            forEachGesture {
                awaitPointerEventScope {
                    awaitFirstDown(requireUnconsumed = false)
                    if (!transformation.gesturesEnabled) return@awaitPointerEventScope

                    do {
                        val event = awaitPointerEvent()

                        // Only allow 1 element to be dragged at the same time
                        if (editorState.draggedElement.let { it != null && it != element }) {
                            return@awaitPointerEventScope
                        }
                        if (editorState.focusedElement != null) {
                            return@awaitPointerEventScope
                        }
                        if (event.changes.any { it.previousPosition != it.position }) {
                            editorState.draggedElement = element
                        }

                        val pointerPosition = event.changes.lastOrNull()?.position
                        editorState.pointerPosition = if (pointerPosition != null) {
                            pointerPosition + transformation.scaledHitboxPosition(editorState.editorSize)
                        } else {
                            Offset.Unspecified
                        }

                        val canceled = event.changes.fastAny { it.isConsumed }
                    } while (!canceled && event.changes.fastAny { it.pressed })

                    editorState.draggedElement = null
                    // Important, reset position AFTER setting dragged element to null
                    editorState.pointerPosition = Offset.Unspecified
                }
            }
        }
    }
}
