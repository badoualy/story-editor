package com.github.badoualy.storyeditor.element.text

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.badoualy.storyeditor.R
import com.github.badoualy.storyeditor.StoryElement
import com.github.badoualy.storyeditor.component.ColorSchemeTypeToggleRow
import com.github.badoualy.storyeditor.element.text.StoryTextElement.AlignType
import com.github.badoualy.storyeditor.element.text.StoryTextElement.ColorSchemeType
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TextElementEditorBar(
    colorSchemes: ImmutableList<StoryElement.ColorScheme>,
    fontStyles: ImmutableList<StoryTextElement.FontStyle>,
    currentColorScheme: () -> StoryElement.ColorScheme,
    currentAlignType: () -> AlignType,
    currentFontStyle: () -> StoryTextElement.FontStyle,
    currentColorSchemeType: () -> ColorSchemeType,
    onSelectColorScheme: (StoryElement.ColorScheme) -> Unit,
    onAlignTypeClick: () -> Unit,
    onSelectFontStyle: (StoryTextElement.FontStyle) -> Unit,
    onColorSchemeTypeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Align / Colors type / Font style
        Row(
            modifier = Modifier.padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAlignTypeClick) {
                val icon = when (currentAlignType()) {
                    AlignType.START -> R.drawable.ic_round_format_align_left_24
                    AlignType.CENTER -> R.drawable.ic_round_format_align_center_24
                    AlignType.END -> R.drawable.ic_round_format_align_right_24
                }

                Icon(
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }

            IconButton(onClick = onColorSchemeTypeClick) {
                val icon = when (currentColorSchemeType()) {
                    ColorSchemeType.BACKGROUND -> R.drawable.ic_round_font_download_24
                    ColorSchemeType.INVERTED -> R.drawable.ic_round_font_download_24
                    ColorSchemeType.TEXT_ONLY -> R.drawable.ic_outline_font_download_24
                }
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null
                )
            }

            Box(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(width = 2.dp, height = 24.dp)
                    .background(Color.White.copy(alpha = 0.5f))
            )

            FontStyleToggleRow(
                fontStyles = fontStyles,
                currentFontStyle = currentFontStyle,
                onSelectFontStyle = onSelectFontStyle
            )
        }

        // Color toggle
        ColorSchemeTypeToggleRow(
            colorSchemes = colorSchemes,
            currentColorScheme = currentColorScheme,
            onColorSchemeClick = onSelectColorScheme
        )
    }
}

@Composable
private fun FontStyleToggleRow(
    fontStyles: ImmutableList<StoryTextElement.FontStyle>,
    currentFontStyle: () -> StoryTextElement.FontStyle,
    onSelectFontStyle: (StoryTextElement.FontStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        fontStyles.forEach { fontStyle ->
            key(fontStyle) {
                FontStyleToggle(
                    fontStyle = fontStyle,
                    isSelected = currentFontStyle() == fontStyle,
                    onClick = { onSelectFontStyle(fontStyle) }
                )
            }
        }
    }
}

@Composable
private fun FontStyleToggle(
    fontStyle: StoryTextElement.FontStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(min = 32.dp),
        shape = RoundedCornerShape(4.dp),
        color = Color.Transparent,
        contentColor = Color.White,
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = Color.White.copy(alpha = if (isSelected) 1f else 0.5f)
        ),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = fontStyle.name,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                style = fontStyle.textStyle,
                fontSize = 12.sp
            )
        }
    }
}
