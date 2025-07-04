@file:Suppress("MemberVisibilityCanBePrivate")

package com.github.badoualy.storyeditor.element.text

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import com.github.badoualy.storyeditor.StoryEditorElementScope
import com.github.badoualy.storyeditor.StoryEditorState.ScreenshotMode
import com.github.badoualy.storyeditor.StoryElement
import com.github.badoualy.storyeditor.StoryElementTransformation
import com.github.badoualy.storyeditor.TransformableStoryElement
import com.github.badoualy.storyeditor.util.ClearFocusOnKeyboardCloseEffect
import com.github.badoualy.storyeditor.util.getLines
import com.github.badoualy.storyeditor.util.horizontalPadding
import com.github.badoualy.storyeditor.util.plus
import com.github.badoualy.storyeditor.util.toDpSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

object StoryTextElementDefaults {

    val HitboxPadding = PaddingValues(50.dp)
    val Padding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
    val BackgroundRadius = 4.dp
    val EditPositionFraction = Offset(0.15f, 0.15f)

    object FontStyle {

        val Classic = StoryTextElement.FontStyle(
            name = "Classic",
            textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 24.sp)
        )
        val Monospace = StoryTextElement.FontStyle(
            name = "Monospace",
            textStyle = Classic.textStyle.copy(fontFamily = FontFamily.Monospace)
        )
        val Serif = StoryTextElement.FontStyle(
            name = "Serif",
            textStyle = Classic.textStyle.copy(fontFamily = FontFamily.Serif)
        )
        val Cursive = StoryTextElement.FontStyle(
            name = "Cursive",
            textStyle = Classic.textStyle.copy(fontFamily = FontFamily.Cursive)
        )

        val DefaultList = listOf(Classic, Monospace, Serif, Cursive).toImmutableList()
    }
}

/**
 * @param enforceInitialTextLines if true, the text size will be automatically reduced if necessary
 * to keep the same line breaks than the initial text
 */
@Stable
class StoryTextElement(
    val initialText: String = "",
    alignType: AlignType = AlignType.START,
    fontStyle: FontStyle = StoryTextElementDefaults.FontStyle.Classic,
    colorSchemeType: ColorSchemeType = ColorSchemeType.BACKGROUND,
    colorScheme: StoryElement.ColorScheme = StoryElement.ColorScheme.White,

    initialSizeFraction: Size? = null,
    scale: Float = 1f,
    rotation: Float = 0f,
    positionFraction: Offset = StoryTextElementDefaults.EditPositionFraction,
    private val editPositionFraction: Offset = StoryTextElementDefaults.EditPositionFraction,

    minScale: Float = 0.5f,
    maxScale: Float = 3f,

    val enforceInitialTextLines: Boolean = true
) : StoryElement, TransformableStoryElement {

    var text by mutableStateOf(TextFieldValue(initialText))
    var alignType by mutableStateOf(alignType)
    var fontStyle by mutableStateOf(fontStyle)
    var colorSchemeType by mutableStateOf(colorSchemeType)
    var colorScheme by mutableStateOf(colorScheme)
    var textLines: String = initialText
        private set

    override val transformation = StoryElementTransformation(
        initialSizeFraction = initialSizeFraction,
        scale = scale,
        rotation = rotation,
        positionFraction = positionFraction,

        minScale = minScale,
        maxScale = maxScale
    )

    override suspend fun startEdit(editorSize: IntSize, bounds: Rect) {
        // Set cursor position at the end
        text = text.copy(selection = TextRange(text.text.length))

        // Override position
        transformation.startEdit(positionFraction = editPositionFraction)
    }

    override suspend fun stopEdit(editorSize: IntSize, bounds: Rect): Boolean {
        text = text.copy(text = text.text.trim())
        if (text.text.isBlank()) return false

        // Stop position override
        transformation.stopEdit(editorSize, bounds)
        return true
    }

    internal fun updateLayoutResult(textLayoutResult: TextLayoutResult) {
        textLines = textLayoutResult.getLines()
    }

    internal fun toggleAlignType() {
        val index = (alignType.ordinal + 1) % AlignType.entries.size
        alignType = AlignType.entries[index]
    }

    internal fun toggleColorSchemeType() {
        val index = (colorSchemeType.ordinal + 1) % ColorSchemeType.entries.size
        colorSchemeType = ColorSchemeType.entries[index]
    }

    fun textStyle(): TextStyle {
        return fontStyle.textStyle.copy(
            color = textColor(),
            textAlign = textAlign()
        )
    }

    fun backgroundColor(): Color {
        return when (colorSchemeType) {
            ColorSchemeType.BACKGROUND -> colorScheme.primary
            ColorSchemeType.INVERTED -> colorScheme.secondary
            ColorSchemeType.TEXT_ONLY -> Color.Transparent
        }
    }

    fun textColor(): Color {
        return when (colorSchemeType) {
            ColorSchemeType.BACKGROUND -> colorScheme.secondary
            ColorSchemeType.INVERTED -> colorScheme.primary
            ColorSchemeType.TEXT_ONLY -> colorScheme.primary
        }
    }

    private fun textAlign(): TextAlign {
        return when (alignType) {
            AlignType.START -> TextAlign.Start
            AlignType.CENTER -> TextAlign.Center
            AlignType.END -> TextAlign.End
        }
    }

    enum class AlignType { START, CENTER, END }

    enum class ColorSchemeType {
        /** primary is background */
        BACKGROUND,

        /** primary is text color */
        INVERTED,

        /** primary is text color, and no background */
        TEXT_ONLY
    }

    data class FontStyle(val name: String, val textStyle: TextStyle)
}

@Composable
fun StoryEditorElementScope.TextElement(
    element: StoryTextElement,
    modifier: Modifier = Modifier,
    hitboxPadding: PaddingValues = StoryTextElementDefaults.HitboxPadding,
    elementPadding: PaddingValues = StoryTextElementDefaults.Padding,
    backgroundRadius: Dp = StoryTextElementDefaults.BackgroundRadius,
    lineSpacingExtra: TextUnit = 5.sp,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    fontStyles: ImmutableList<StoryTextElement.FontStyle> = StoryTextElementDefaults.FontStyle.DefaultList,
    colorSchemes: ImmutableList<StoryElement.ColorScheme> = StoryElement.ColorScheme.DefaultList,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Editor overlay when focused
        val isFocused by remember(editorState, element) {
            derivedStateOf {
                editorState.focusedElement === element
            }
        }
        if (isFocused) {
            val focusManager = LocalFocusManager.current
            focusManager.ClearFocusOnKeyboardCloseEffect()

            TextElementEditorOverlay(
                element = element,
                isScreenshotModeEnabled = editorState.screenshotMode != ScreenshotMode.DISABLED,
                onClickOutside = {
                    focusManager.clearFocus()
                },
                fontStyles = fontStyles,
                colorSchemes = colorSchemes
            )
        }

        // Actual element
        val focusRequester = remember { FocusRequester() }
        Box(
            modifier = Modifier
                .elementTransformation(
                    element = element,
                    onClick = {
                        // request focus on TextField to edit text
                        if (!isFocused) {
                            focusRequester.requestFocus()
                        }
                    },
                    hitboxPadding = hitboxPadding
                )
        ) {
            val isEnabled by remember(editorState, element) {
                derivedStateOf {
                    editorState.isFocusable(element)
                }
            }

            // Instead of using BoxWithConstraints, we use editorState.elementBounds to compute maxWidth
            // But it means we might have a few frames with maxWidth to <= 0 until the size is reported
            val density = LocalDensity.current
            val hitboxPaddingPx = with(density) { hitboxPadding.horizontalPadding().toPx() }
            val elementPaddingPx = with(density) { elementPadding.horizontalPadding().toPx() }
            val maxWidth by remember(editorState, density) {
                derivedStateOf {
                    editorState.elementsBounds.width - hitboxPaddingPx - elementPaddingPx
                }
            }

            TextElementTextField(
                element = element,
                enabled = isEnabled,
                elementPadding = elementPadding,
                backgroundRadius = backgroundRadius,
                lineSpacingExtra = lineSpacingExtra,
                maxWidth = maxWidth,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                modifier = Modifier.focusableElement(element, focusRequester, skipFocusable = true),
            )
        }
    }
}

@Composable
fun TextElementTextField(
    element: StoryTextElement,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    elementPadding: PaddingValues = PaddingValues(),
    backgroundRadius: Dp = 0.dp,
    lineSpacingExtra: TextUnit = 0.sp,
    maxWidth: Float = Float.POSITIVE_INFINITY,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    onValueChange: (TextFieldValue) -> Unit = { element.text = it },
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    val isEmpty by remember(element) { derivedStateOf { element.text.text.isEmpty() } }
    var linesBounds by remember { mutableStateOf(listOf<Rect>()) }

    val textColor by animateColorAsState(element.textColor())
    val backgroundColor by animateColorAsState(element.backgroundColor())
    val textStyle = element.textStyle()

    // Resolve size to apply AutoResize effect
    val density = LocalDensity.current
    val context = LocalContext.current
    val resolvedTextSize = if (element.enforceInitialTextLines) {
        if (maxWidth <= 0) return
        remember(element, maxWidth) {
            resolveAutoResizeTextSize(
                text = element.initialText,
                textStyle = textStyle,
                maxWidth = maxWidth,
                density = density,
                context = context
            )
        }
    } else {
        textStyle.fontSize
    }

    val mergedTextStyle = textStyle.copy(
        color = textColor,
        fontSize = resolvedTextSize,
        lineHeight = if (textStyle.lineHeight.isSpecified) {
            (textStyle.lineHeight.value + lineSpacingExtra.value).sp
        } else {
            TextUnit.Unspecified
        },
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Top,
            trim = LineHeightStyle.Trim.LastLineBottom
        )
    )
    val textSelectionColors = remember(element.colorScheme.secondary) {
        TextSelectionColors(
            handleColor = element.colorScheme.secondary,
            backgroundColor = element.colorScheme.secondary.copy(alpha = 0.4f)
        )
    }

    CompositionLocalProvider(LocalTextSelectionColors provides textSelectionColors) {
        BasicTextField(
            value = element.text,
            onValueChange = onValueChange,
            modifier = Modifier
                .width(IntrinsicSize.Min)
                // Cursor thickness is 2.dp
                .widthIn(min = 2.dp)
                .drawWithContent {
                    if (linesBounds.isEmpty()) {
                        drawContent()
                        return@drawWithContent
                    }

                    // Draw background on each line
                    val paddingSize = elementPadding
                        .toDpSize()
                        .toSize()
                    val cornerRadius = CornerRadius(backgroundRadius.toPx())
                    val lastVisibleLine = linesBounds.lastOrNull { it.bottom <= size.height }

                    linesBounds.forEach { lineBounds ->
                        if (lineBounds.width == 0f || lineBounds.bottom > size.height) return@forEach
                        drawRoundRect(
                            color = backgroundColor,
                            topLeft = lineBounds.topLeft,
                            size = lineBounds.size + paddingSize,
                            cornerRadius = cornerRadius
                        )
                    }

                    // Clip to the last visible line to make sure we don't display vertically cropped text
                    // because of TextField internal scroll modifier
                    clipRect(bottom = lastVisibleLine?.bottom ?: size.height) {
                        this@drawWithContent.drawContent()
                    }
                }
                .then(if (isEmpty) Modifier else Modifier.padding(elementPadding))
                .then(modifier),
            textStyle = mergedTextStyle,
            cursorBrush = SolidColor(textColor),
            enabled = enabled,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            onTextLayout = { layout ->
                element.updateLayoutResult(layout)
                if (layout.layoutInput.text.isEmpty()) {
                    linesBounds = emptyList()
                    return@BasicTextField
                }

                // Because we add space to the bottom of the line and we trim lastLine bottom, last line is the real height
                val lineHeightPx = layout.multiParagraph.getLineHeight(layout.lineCount - 1)

                // Build bounding rect for each line
                linesBounds = List(layout.lineCount) { line ->
                    val lineContent = layout.layoutInput.text.text.substring(
                        layout.getLineStart(line),
                        layout.getLineEnd(line)
                    )

                    val top = layout.getLineTop(line)
                    val bottom = top + lineHeightPx
                    if (lineContent.isBlank()) return@List Rect(0f, top, 0f, bottom)
                    Rect(
                        left = layout.getLineLeft(line),
                        top = top,
                        right = layout.getLineRight(line),
                        bottom = bottom,
                    )
                }
            },
            decorationBox = decorationBox,
        )
    }
}

@Composable
fun TextElementEditorOverlay(
    element: StoryTextElement,
    isScreenshotModeEnabled: Boolean,
    onClickOutside: () -> Unit,
    modifier: Modifier = Modifier,
    fontStyles: ImmutableList<StoryTextElement.FontStyle>,
    colorSchemes: ImmutableList<StoryElement.ColorScheme>,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .imePadding()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClickOutside
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        TextElementEditorBar(
            colorSchemes = colorSchemes,
            fontStyles = fontStyles,
            currentColorScheme = element::colorScheme,
            currentAlignType = element::alignType,
            currentFontStyle = element::fontStyle,
            currentColorSchemeType = element::colorSchemeType,
            onAlignTypeClick = element::toggleAlignType,
            onColorSchemeTypeClick = element::toggleColorSchemeType,
            onSelectFontStyle = { element.fontStyle = it },
            onSelectColorScheme = { element.colorScheme = it }
        )
    }
}
