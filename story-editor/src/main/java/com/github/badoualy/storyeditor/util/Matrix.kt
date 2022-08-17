package com.github.badoualy.storyeditor.util

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Matrix

internal fun Matrix.rotateZ(degrees: Float, center: Offset) {
    translate(x = center.x, y = center.y)
    rotateZ(degrees)
    translate(x = -center.x, y = -center.y)
}
