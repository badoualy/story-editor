package com.github.badoualy.storyeditor.util

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

fun PaddingValues.verticalPadding(): Dp {
    return calculateTopPadding() + calculateBottomPadding()
}

fun PaddingValues.horizontalPadding(): Dp {
    return calculateLeftPadding(LayoutDirection.Ltr) + calculateRightPadding(LayoutDirection.Ltr)
}

