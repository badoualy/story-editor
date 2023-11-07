@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.github.badoualy.storyeditor

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import com.github.badoualy.storyeditor.StoryEditorState.ScreenshotMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * @param elementsBoundsFraction Bounds of allowed position in fraction for each side
 *
 *   eg:
 *       - left 0.1 means elements' offset can't be smaller than 10% of the width
 *       - right 0.9 means elements' right position in parent can't exceed 90% of the width
 * @param screenshotMode screenshot support mode.
 * When using this, parts of the editor will be wrapped in an ComposeView inside an AndroidView.
 * see [ScreenshotMode]
 * @param debug In debug mode, the editor will draw the bounds of each element's hitbox
 */
@Stable
class StoryEditorState(
    private val elementsBoundsFraction: Rect = Rect(0.0f, 0.0f, 1f, 1f),
    editMode: Boolean = true,
    val screenshotMode: ScreenshotMode = ScreenshotMode.DISABLED,
    val debug: Boolean = false
) {

    /** allow editing (adding/updating/moving elements) */
    var editMode by mutableStateOf(editMode)

    /**
     * Size of the editor's content (without any padding).
     * eg: size of the *picture* composable passed to [StoryEditor]
     */
    var editorSize by mutableStateOf(IntSize.Zero)
        private set

    /**
     * Bounds of allowed position in px for each side.
     * eg:
     * - left 10 means elements' offset can't be smaller than 10
     * - right 90 means elements' right position in parent can't exceed 90
     */
    var elementsBounds by mutableStateOf(Rect.Zero)
        private set

    /**
     * Element currently focused for edition.
     * When an element is focused, other elements can't be interacted with.
     */
    var focusedElement by mutableStateOf<StoryElement?>(null)

    /**
     * Element currently dragged. Only one element can be drag/interacted with at the same time.
     */
    var draggedElement by mutableStateOf<StoryElement?>(null)
        internal set

    /**
     * Pointer position while [draggedElement] is not null.
     * Reset to [Offset.Unspecified] when drag is finished.
     */
    internal var pointerPosition: Offset by mutableStateOf(Offset.Unspecified)

    private val _screenshotRequest = MutableSharedFlow<ScreenshotRequest>(extraBufferCapacity = 1)
    internal val screenshotRequest get() = _screenshotRequest.asSharedFlow()

    suspend fun takeScreenshot(destination: Bitmap) {
        require(screenshotMode != ScreenshotMode.DISABLED) { "screenshotMode set to DISABLED" }

        screenshotMode.layers.forEach { layer ->
            suspendCoroutine { cont ->
                _screenshotRequest.tryEmit(
                    ScreenshotRequest(
                        layer = layer,
                        destination = destination,
                        onError = cont::resumeWithException,
                        onSuccess = { cont.resume(Unit) }
                    )
                )
            }
        }
    }

    suspend fun takeScreenshot(config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap {
        require(screenshotMode != ScreenshotMode.DISABLED) { "screenshotMode set to DISABLED" }
        val destination = withContext(Dispatchers.Default) {
            Bitmap.createBitmap(editorSize.width, editorSize.height, config)
        }

        try {
            takeScreenshot(destination)
            return destination
        } catch (e: Exception) {
            destination.recycle()
            throw e
        }
    }

    suspend fun takeAndSaveScreenshot(
        directory: File,
        config: Bitmap.Config = Bitmap.Config.RGB_565
    ): Uri {
        val screenshot = takeScreenshot(config)

        return withContext(Dispatchers.IO) {
            val file = File(directory, "story_editor_screenshot.png").apply {
                if (exists()) delete()
            }
            file.outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            Uri.fromFile(file)
        }
    }

    fun isFocusable(element: StoryElement): Boolean {
        return editMode && (focusedElement == null || focusedElement === element) && draggedElement == null
    }

    internal fun updateBackgroundSize(size: IntSize) {
        editorSize = size
        elementsBounds = Rect(
            left = size.width * elementsBoundsFraction.left,
            top = size.height * elementsBoundsFraction.top,
            right = size.width * elementsBoundsFraction.right,
            bottom = size.height * elementsBoundsFraction.bottom
        )
    }

    enum class ScreenshotMode(internal val layers: Array<ScreenshotLayer>) {
        /** Screenshot support is disabled, no AndroidView used */
        DISABLED(layers = emptyArray()),

        /** Screenshot support is enabled, and the screenshot will contain background + content */
        FULL(layers = arrayOf(ScreenshotLayer.EDITOR)),

        /**
         * Same as [FULL], but the screenshot won't be clipped to the [StoryEditor]'s shape.
         * This is useful when you specify a shape for the background, and you don't want the screenshot to be clipped.
         */
        FULL_NOT_CLIPPED(layers = arrayOf(ScreenshotLayer.BACKGROUND, ScreenshotLayer.ELEMENTS)),

        /** Screenshot support is enabled, and the screenshot will contain only the content without the background */
        CONTENT(layers = arrayOf(ScreenshotLayer.ELEMENTS));

        companion object {

            val ScreenshotMode.isBackgroundDrawn get() = this == FULL || this == FULL_NOT_CLIPPED
        }
    }
}
