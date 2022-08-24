package com.github.badoualy.storyeditor.util

import androidx.compose.ui.geometry.Size

operator fun Size.plus(size: Size): Size {
    return Size(
        width = width + size.width,
        height = height + size.height
    )
}
