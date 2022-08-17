package com.github.badoualy.storyeditor.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Coerce the rect into the given bounds.
 * If the rect doesn't fit the bounds on one axis, it'll be centered instead.
 */
internal fun Rect.coerceInOrCenter(bounds: Rect): Rect {
    val x = try {
        left.coerceIn(
            bounds.left,
            bounds.right - width
        )
    } catch (e: IllegalArgumentException) {
        bounds.center.x - width / 2f
    }

    val y = try {
        top.coerceIn(
            bounds.top,
            bounds.bottom - height
        )
    } catch (e: IllegalArgumentException) {
        bounds.center.y - height / 2f
    }

    return Rect(Offset(x, y), size)
}
