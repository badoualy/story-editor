package com.github.badoualy.storyeditor

import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import kotlinx.collections.immutable.toImmutableList

/**
 * Base element that can be added to a [StoryEditor]
 */
@Stable
interface StoryElement {

    /**
     * Called when the element gains focus and should update its state to an edit mode.
     * By default, calling startEdit will disable scale/rotation transformations.
     */
    suspend fun startEdit(editorSize: IntSize, bounds: Rect)

    /** @return true if the element should be kept, false otherwise */
    suspend fun stopEdit(editorSize: IntSize, bounds: Rect): Boolean

    data class ColorScheme(val primary: Color, val secondary: Color) {

        companion object {

            val White = ColorScheme(primary = Color.White, secondary = Color.Black)
            val Black = ColorScheme(primary = Color.Black, secondary = Color.White)
            val Magenta = ColorScheme(primary = Color.Magenta, secondary = Color.White)
            val Cyan = ColorScheme(primary = Color.Cyan, secondary = Color.White)
            val Blue = ColorScheme(primary = Color.Blue, secondary = Color.White)
            val Green = ColorScheme(primary = Color.Green, secondary = Color.White)
            val Yellow = ColorScheme(primary = Color.Yellow, secondary = Color.Black)
            val Red = ColorScheme(primary = Color.Red, secondary = Color.White)

            val DefaultList = listOf(
                White,
                Black,
                Magenta,
                Cyan,
                Blue,
                Green,
                Yellow,
                Red
            ).toImmutableList()
        }
    }
}

/**
 * A [StoryElement] that hols a [StoryElementTransformation].
 * Each element that can be dragged/scaled/rotated must implement this interface to be able
 * to apply predefined modifiers.
 */
@Stable
interface TransformableStoryElement : StoryElement {

    val transformation: StoryElementTransformation
}
