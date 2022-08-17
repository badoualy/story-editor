package com.github.badoualy.storyeditor.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.github.badoualy.storyeditor.StoryEditorState
import com.github.badoualy.storyeditor.StoryElement
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.take

/**
 * Delete button with a scaling effect and haptic feedback when an element enters the deletion range
 */
@Composable
internal fun EditorDeleteButton(
    editorState: StoryEditorState,
    onDelete: (StoryElement) -> Unit,
    modifier: Modifier = Modifier
) {
    var deleteBounds by remember { mutableStateOf(Rect.Zero) }
    val isInDeleteRange by remember(editorState) {
        derivedStateOf {
            if (editorState.pointerPosition.isSpecified) {
                deleteBounds.contains(editorState.pointerPosition)
            } else {
                false
            }
        }
    }

    if (isInDeleteRange) {
        val hapticFeedback = LocalHapticFeedback.current
        val rememberedOnDelete by rememberUpdatedState(onDelete)
        LaunchedEffect(Unit) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

            val element = editorState.draggedElement ?: return@LaunchedEffect
            snapshotFlow { editorState.draggedElement }
                .dropWhile { it != null }
                .take(1)
                .collect { rememberedOnDelete(element) }
        }
    }

    AnimatedVisibility(
        visible = editorState.draggedElement != null,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .onPlaced { deleteBounds = it.boundsInParent() }
            .padding(24.dp)
    ) {
        val color = if (isInDeleteRange) Color.Red else Color.Black
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.5f),
            contentColor = Color.White,
            modifier = Modifier.scale(if (isInDeleteRange) 1.5f else 1f)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = null,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
