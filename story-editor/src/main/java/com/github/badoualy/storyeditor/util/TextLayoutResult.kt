package com.github.badoualy.storyeditor.util

import androidx.compose.ui.text.TextLayoutResult

fun TextLayoutResult.getLines(): String {
    val input = layoutInput.text.toString()

    return buildString {
        var currentIndex = 0

        repeat(lineCount) { line ->
            val lineEnd = getLineEnd(line)
            appendLine(input.substring(currentIndex, lineEnd))

            currentIndex = lineEnd
        }
    }
}
