package com.github.badoualy.storyeditor.util

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumedWindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.ui.Modifier

// see https://issuetracker.google.com/issues/243778587
fun Modifier.imePadding(consume: WindowInsets?): Modifier {
    return then(if (consume != null) consumedWindowInsets(consume) else Modifier)
        .imePadding()
}
