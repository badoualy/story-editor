package com.github.badoualy.storyeditor.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.badoualy.storyeditor.StoryElement
import kotlinx.collections.immutable.ImmutableList

/**
 * A scrollable row to select one color scheme in a given list.
 * Each color scheme is represented by a filled circle.
 */
@Composable
fun ColorSchemeTypeToggleRow(
    colorSchemes: ImmutableList<StoryElement.ColorScheme>,
    currentColorScheme: () -> StoryElement.ColorScheme,
    onColorSchemeClick: (StoryElement.ColorScheme) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        colorSchemes.forEach { colorScheme ->
            key(colorScheme) {
                ColorSchemeButton(
                    colorScheme = colorScheme,
                    isSelected = currentColorScheme() == colorScheme,
                    onClick = { onColorSchemeClick(colorScheme) }
                )
            }
        }
    }
}

@Composable
private fun ColorSchemeButton(
    colorScheme: StoryElement.ColorScheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(32.dp)
            .then(
                if (isSelected) {
                    Modifier
                        .border(2.dp, Color.White, CircleShape)
                } else {
                    Modifier
                        .padding(1.dp)
                        .border(1.dp, Color.White, CircleShape)
                }
            )
            .clip(CircleShape)
            .background(colorScheme.primary)
            .clickable(onClick = onClick)
    )
}
