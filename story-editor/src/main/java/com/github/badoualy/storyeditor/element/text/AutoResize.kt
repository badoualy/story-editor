package com.github.badoualy.storyeditor.element.text

import android.content.Context
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

fun resolveAutoResizeTextSize(
    text: String,
    textStyle: TextStyle,
    maxWidth: Float,
    density: Density,
    context: Context,
    interval: Float = 1f
): TextUnit {
    require(maxWidth > 0) { "maxWidth should be > 0" }
    val originalTextSize = textStyle.fontSize
    // Only SP unit is supported for AutoResize
    require(originalTextSize.isSp) { "Only SP unit is supported for AutoResize" }

    val fontFamilyResolver = createFontFamilyResolver(context)
    fun measure(text: String, size: TextUnit): ParagraphIntrinsics {
        return ParagraphIntrinsics(
            text = text,
            style = textStyle.copy(fontSize = size),
            density = density,
            fontFamilyResolver = fontFamilyResolver
        )
    }

    // We need to first measure each line, and use it for auto resize operations
    val largestLineInfo = text.lines()
        .map { it to measure(it, originalTextSize).maxIntrinsicWidth }
        .maxBy { it.second }
    val largestLine = largestLineInfo.first
    var largestLineWidth = largestLineInfo.second

    // Loop until the line fits
    var currentTextSizePx = originalTextSize.value
    while (largestLineWidth > maxWidth && currentTextSizePx > 0) {
        // TODO: instead of using a fixed interval, we might be able to use the textSize and compare with (largestLineWidth - maxWidth)
        currentTextSizePx -= interval
        largestLineWidth = measure(largestLine, currentTextSizePx.sp).maxIntrinsicWidth
    }
    return currentTextSizePx.sp
}
