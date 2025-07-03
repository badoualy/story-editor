package com.github.badoualy.storyeditor

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
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
) {

    val destinationCanvas = Canvas(destination)
}

@Composable
internal fun Modifier.screenshotLayer(
    editorState: StoryEditorState,
    layer: ScreenshotLayer,
): Modifier = composed(
    "com.github.badoualy.storyeditor.screenshotLayer",
    editorState.screenshotMode,
    layer
) {
    if (layer in editorState.screenshotMode.layers) {
        val graphicsLayer = rememberGraphicsLayer()

        // Listen to request
        LaunchedEffect(editorState) {
            editorState.screenshotRequest
                .filter { it.layer == layer }
                .collect { request ->
                    try {
                        val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                        val bitmapToDraw = if (bitmap.isHardware) {
                            bitmap.copy(Bitmap.Config.ARGB_8888, false)
                        } else {
                            bitmap
                        }
                        request.destinationCanvas.drawBitmap(
                            bitmapToDraw,
                            0f,
                            0f,
                            null
                        )
                        bitmap.recycle()
                        if (bitmapToDraw !== bitmap) {
                            bitmapToDraw.recycle()
                        }
                        request.onSuccess()
                    } catch (e: Exception) {
                        request.onError(e)
                    }
                }
        }

        this
            .drawWithContent {
                // call record to capture the content in the graphics layer
                graphicsLayer.record {
                    // draw the contents of the composable into the graphics layer
                    this@drawWithContent.drawContent()
                }
                // draw the graphics layer on the visible canvas
                drawLayer(graphicsLayer)
            }
    } else {
        this
    }
}

private val Bitmap.isHardware
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && config == Bitmap.Config.HARDWARE
