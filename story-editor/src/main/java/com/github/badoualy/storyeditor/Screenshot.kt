package com.github.badoualy.storyeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.viewinterop.AndroidView
import com.github.badoualy.storyeditor.StoryEditorState.ScreenshotMode
import kotlinx.coroutines.flow.filter

internal enum class ScreenshotLayer {
    EDITOR,
    BACKGROUND,
    ELEMENTS
}

@Stable
internal data class ScreenshotRequest(
    val layer: ScreenshotLayer,
    val destination: Bitmap,
    val onSuccess: () -> Unit,
    val onError: (Throwable) -> Unit
)

@Composable
internal fun View.ListenToScreenshotRequest(
    editorState: StoryEditorState,
    layer: ScreenshotLayer
) {
    LaunchedEffect(editorState) {
        require(editorState.screenshotMode != ScreenshotMode.DISABLED) {
            "Screenshot support disabled"
        }
        editorState.screenshotRequest
            .filter { it.layer == layer }
            .collect { request ->
                try {
                    drawToBitmap(destination = request.destination)
                    request.onSuccess()
                } catch (e: Exception) {
                    request.onError(e)
                }
            }
    }
}

@Composable
internal fun ScreenshotContent(
    state: StoryEditorState,
    layer: ScreenshotLayer,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (layer in state.screenshotMode.layers) {
        AndroidView(
            factory = { context ->
                ComposeView(context).apply {
                    setContent {
                        content()
                        ListenToScreenshotRequest(editorState = state, layer = layer)
                    }
                }
            },
            modifier = modifier
        )
    } else {
        Box(modifier = modifier) {
            content()
        }
    }
}

private fun View.drawToBitmap(destination: Bitmap) {
    check(isLaidOut) { "View needs to be laid out before calling drawToBitmap()" }
    draw(Canvas(destination))
}
