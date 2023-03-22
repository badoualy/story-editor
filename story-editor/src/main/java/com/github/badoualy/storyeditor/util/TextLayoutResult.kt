package com.github.badoualy.storyeditor.util

import androidx.compose.ui.text.TextLayoutResult

internal fun TextLayoutResult.getLines(): String {
    val input = layoutInput.text.toString()
    if (input.isEmpty()) return ""

    return (0 until lineCount).joinToString("\n") { line ->
        val lineContent = input.substring(
            getLineStart(line),
            getLineEnd(line, visibleEnd = true)
        )
        lineContent
    }
}
