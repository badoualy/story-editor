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
 * @param debug In debug mode, the editor will draw the bounds of each element's hitbox
 */
@Stable
class StoryEditorState(
    private val elementsBoundsFraction: Rect = Rect(0.0f, 0.0f, 1f, 1f),
    editMode: Boolean = true,
    val screenshotEnabled: Boolean = false,
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

    var focusedElement by mutableStateOf<StoryElement?>(null)
    var draggedElement by mutableStateOf<StoryElement?>(null)
        internal set
    internal var pointerPosition: Offset by mutableStateOf(Offset.Unspecified)

    /**
     * Bounds of allowed position in px for each side.
     * eg:
     * - left 10 means elements' offset can't be smaller than 10
     * - right 90 means elements' right position in parent can't exceed 90
     */
    internal var elementsBounds by mutableStateOf(Rect.Zero)
        private set

    private val _screenshotRequest = MutableSharedFlow<ScreenshotRequest>(extraBufferCapacity = 1)
    internal val screenshotRequest get() = _screenshotRequest.asSharedFlow()

    suspend fun takeScreenshot(config: Bitmap.Config = Bitmap.Config.RGB_565): Bitmap {
        require(screenshotEnabled) { "screenshotMode not enabled" }

        return suspendCoroutine { cont ->
            _screenshotRequest.tryEmit(
                ScreenshotRequest(
                    config = config,
                    onError = { cont.resumeWithException(it) },
                    callback = { cont.resume(it) }
                )
            )
        }
    }

    suspend fun takeAndSaveScreenshot(
        directory: File,
        config: Bitmap.Config = Bitmap.Config.RGB_565
    ): Uri {
        val screenshot = takeScreenshot(config)
        return withContext(Dispatchers.IO) {
            val file = File(directory, "story_editor_screenshot.jpg").apply {
                if (exists()) delete()
            }
            file.outputStream().use {
                screenshot.compress(Bitmap.CompressFormat.JPEG, 100, it)
            }

            Uri.fromFile(file)
        }
    }

    fun isFocusable(element: StoryElement): Boolean {
        return editMode && (focusedElement == null || focusedElement == element) && draggedElement == null
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
}
