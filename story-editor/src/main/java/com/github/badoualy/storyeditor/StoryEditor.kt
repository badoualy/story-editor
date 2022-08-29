@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.badoualy.storyeditor

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import com.github.badoualy.storyeditor.component.EditorDeleteButton
import com.github.badoualy.storyeditor.util.horizontalPadding
import com.github.badoualy.storyeditor.util.verticalPadding
import kotlinx.collections.immutable.ImmutableList

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
        ScreenshotLayer(
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
        ScreenshotLayer(
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
                            if (state.draggedElement != null) return@detectTapGestures
                            if (state.focusedElement != null) return@detectTapGestures
                            onClick()
                        }
                    }
            ) {
                background()
            }
        }

        // Elements
        ScreenshotLayer(
            state = state,
            layer = ScreenshotLayer.ELEMENTS,
            modifier = Modifier.fillMaxSize()
        ) {
            val scope = remember(state) { StoryEditorScopeImpl(state) }
            with(scope) {
                elements.forEach { element ->
                    key(element) {
                        val isFocusedElement by remember(state) {
                            derivedStateOf {
                                state.focusedElement === element
                            }
                        }

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

    fun Modifier.focusableElement(
        element: StoryElement,
        focusRequester: FocusRequester,
        skipFocusable: Boolean = false
    ): Modifier

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

    override fun Modifier.focusableElement(
        element: StoryElement,
        focusRequester: FocusRequester,
        skipFocusable: Boolean
    ): Modifier {
        return composed(
            "StoryEditorScopeImpl.focusableElement",
            element,
            focusRequester,
            skipFocusable,
            editorState,
            editorState.editMode
        ) {
            if (!editorState.editMode) return@composed Modifier

            val focusManager = LocalFocusManager.current
            val keyboardController = LocalSoftwareKeyboardController.current

            val isFocused by remember { derivedStateOf { editorState.focusedElement === element } }
            var localFocusState by remember { mutableStateOf(false) }
            var waitingForFocus by remember { mutableStateOf(isFocused) }
            LaunchedEffect(Unit) {
                snapshotFlow { isFocused }
                    .collect { isFocused ->
                        if (localFocusState != isFocused) {
                            if (isFocused) {
                                waitingForFocus = true
                                focusRequester.requestFocus()
                            } else {
                                waitingForFocus = false
                                focusManager.clearFocus()
                            }
                        }

                        if (isFocused) {
                            element.startEdit()
                        } else {
                            element.stopEdit()
                        }
                    }
            }

            Modifier
                .focusRequester(focusRequester)
                .onFocusChanged {
                    if (waitingForFocus && !it.hasFocus) {
                        // Ignore event if we're waiting for focus
                        // This can happen when the element enters composition,
                        // onFocusedChanged will be called once with hasFocus=false
                        return@onFocusChanged
                    }

                    localFocusState = it.hasFocus
                    if (it.hasFocus) {
                        // The view can be focused from a click, make sure that the property is set
                        editorState.focusedElement = element
                        waitingForFocus = false
                    } else if (editorState.focusedElement === element) {
                        editorState.focusedElement = null
                        keyboardController?.hide()
                    }
                }
                .then(if (!skipFocusable) Modifier.focusable() else Modifier)
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
            .dragTapListener(element = element, onTap = onClick)
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
                    if (editorState.draggedElement !== element) return@detectTransformGestures
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

    private fun Modifier.dragTapListener(
        element: TransformableStoryElement,
        onTap: () -> Unit,
        dragThreshold: Int = 50
    ): Modifier {
        val dragThresholdSquare = dragThreshold * dragThreshold
        return this.pointerInput(
            editorState,
            editorState.editMode,
            element
        ) {
            if (!editorState.editMode) return@pointerInput
            val transformation = element.transformation

            forEachGesture {
                awaitPointerEventScope {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    if (!transformation.gesturesEnabled) return@awaitPointerEventScope

                    do {
                        val event = awaitPointerEvent()

                        // Only allow 1 element to be dragged at the same time
                        if (editorState.draggedElement.let { it != null && it !== element }) {
                            return@awaitPointerEventScope
                        }
                        // No drag while an element is focused
                        if (editorState.focusedElement != null) {
                            return@awaitPointerEventScope
                        }

                        // Detect taps
                        val isTapEvent = editorState.draggedElement == null &&
                                event.changes.fastAll { it.changedToUp() } &&
                                event.changes[0].uptimeMillis - down.uptimeMillis < 500
                        if (isTapEvent) {
                            onTap()
                            return@awaitPointerEventScope
                        }

                        // Detect drags beyond a threshold
                        val movedBeyondThreshold = event.changes
                            .fastMap { (down.position - it.position).getDistanceSquared() }
                            .fastAny { it > dragThresholdSquare }
                        if (movedBeyondThreshold) {
                            editorState.draggedElement = element
                        }

                        // Update pointer position
                        if (editorState.draggedElement != null) {
                            val pointerPosition = event.changes.lastOrNull()?.position
                            editorState.pointerPosition = if (pointerPosition != null) {
                                pointerPosition + transformation.scaledHitboxPosition(editorState.editorSize)
                            } else {
                                Offset.Unspecified
                            }
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
}
