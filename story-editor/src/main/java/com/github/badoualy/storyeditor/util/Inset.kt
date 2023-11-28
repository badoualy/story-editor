@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "unused")

package com.github.badoualy.storyeditor.util

import androidx.compose.foundation.layout.WindowInsetsHolder
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

// Fix for insets not working inside AndroidView...
// see https://issuetracker.google.com/issues/243778587
@Composable
internal fun currentWindowInsetsHolderForAndroidView(): WindowInsetsHolder {
    return LocalRootWindowInsetsHolder.current
}

@Composable
internal fun ProvidesWindowInsets(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalRootWindowInsetsHolder provides WindowInsetsHolder.current(),
        content = content
    )
}

private val LocalRootWindowInsetsHolder = staticCompositionLocalOf<WindowInsetsHolder> {
    error("CompositionLocal LocalRootWindowInsetsHolder not present")
}

internal fun Modifier.imePadding(ignoreNavigationBar: Boolean): Modifier = composed {
    val inset = currentWindowInsetsHolderForAndroidView().ime
        .let {
            if (ignoreNavigationBar) {
                it.exclude(currentWindowInsetsHolderForAndroidView().navigationBars)
            } else {
                it
            }
        }

    padding(inset.asPaddingValues())
}
