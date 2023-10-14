@file:OptIn(ExperimentalLayoutApi::class)

package com.github.badoualy.storyeditor.util

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.focus.FocusManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.filter

@SuppressLint("ComposableNaming")
@Composable
fun FocusManager.clearFocusOnKeyboardClose() {
    val isImeVisible by rememberUpdatedState(WindowInsets.isImeVisible)
    LaunchedEffect(Unit) {
        // Weird bug where keyboard is closing when we focus an element rapidly after unselecting it
        // It might be because of re-composition
        // To make sure we don't un-focus the element because of this bug, add a slight delay...
        delay(300)
        snapshotFlow { isImeVisible }
            .dropWhile { !it } // Wait for a first keyboard open event
            .filter { !it }
            .collect { clearFocus() }
    }
}
