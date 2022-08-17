package com.github.badoualy.storyeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.IntSize

@Stable
internal data class ScreenshotRequest(
    val config: Bitmap.Config,
    val callback: (Bitmap) -> Unit,
    val onError: (Throwable) -> Unit
)

@Composable
internal fun View.ListenToScreenshotRequest(editorState: StoryEditorState) {
    LaunchedEffect(editorState) {
        editorState.screenshotRequest
            .collect { request ->
                try {
                    val bitmap = drawToBitmap(
                        size = editorState.editorSize,
                        config = request.config
                    )
                    request.callback(bitmap)
                } catch (e: Exception) {
                    request.onError(e)
                }
            }
    }
}

private fun View.drawToBitmap(
    size: IntSize,
    config: Bitmap.Config = Bitmap.Config.ARGB_8888
): Bitmap {
    check(isLaidOut) { "View needs to be laid out before calling drawToBitmap()" }
    return Bitmap.createBitmap(size.width, size.height, config).applyCanvas {
        translate(-scrollX.toFloat(), -scrollY.toFloat())
        draw(this)
    }
}

private inline fun Bitmap.applyCanvas(block: Canvas.() -> Unit): Bitmap {
    val c = Canvas(this)
    c.block()
    return this
}
