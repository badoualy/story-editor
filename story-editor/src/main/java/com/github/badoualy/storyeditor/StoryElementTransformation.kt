@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package com.github.badoualy.storyeditor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize
import com.github.badoualy.storyeditor.util.coerceInOrCenter
import com.github.badoualy.storyeditor.util.rotateBy
import com.github.badoualy.storyeditor.util.rotateZ
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/**
 * Holds current transformation state for the element (position, scale, rotation, ...).
 *
 * @param initialSizeFraction the requested initial size in fraction of the editor's size.
 * If set, when the element is first laid out, it'll compute the [scale] value to honor the requested size.
 * @param scale element scale. If [initialSizeFraction] is not null, this value will be overridden once the view is laid out.
 * @param rotation angle in degrees of the element with the origin being the center.
 * @param positionFraction position in fraction of the editor's size.
 * @param minScale minimum scale value allowed with pinching
 * @param maxScale max scale value allowed with pinching
 */
@Stable
open class StoryElementTransformation(
    private var initialSizeFraction: Size? = null,
    scale: Float = 1f,
    rotation: Float = 0f,
    positionFraction: Offset = Offset(0.15f, 0.30f),
    val minScale: Float = 0.5f,
    val maxScale: Float = 2f
) {

    var gesturesEnabled by mutableStateOf(true)

    // Real values
    var scale by mutableStateOf(scale)
        private set
    var rotation by mutableStateOf(rotation)
        private set
    var positionFraction by mutableStateOf(positionFraction)
        private set

    // Currently displayed state's values
    private val _displayScale = Animatable(scale)
    private val _displayRotation = Animatable(rotation)
    private val _displayPositionFraction = Animatable(positionFraction, Offset.VectorConverter)

    // Values currently displayed (can be different from real values if an animation is in progress
    val displayScale get() = _displayScale.value
    val displayRotation get() = _displayRotation.value

    // Unscaled sizes

    /** Unscaled element size in px */
    var size: IntSize by mutableStateOf(IntSize.Zero)
        private set

    /** Unscaled element size in fraction (percent) of editor's size */
    var sizeFraction: Size by mutableStateOf(initialSizeFraction ?: Size.Zero)
        private set

    /** Unscaled element size in dp */
    var sizeDp: DpSize by mutableStateOf(DpSize.Zero)
        private set

    /** Unscaled element hitbox (with padding for gestures) size in px */
    var hitboxSize: IntSize by mutableStateOf(IntSize.Zero)
        private set

    // Scaled size (unscaled sizes multiplied by displayScale

    /** Scaled element size in px */
    val scaledSize: Size
        get() = size.toSize() * displayScale

    /** Scaled element size in fraction (percent) of editor's size */
    val scaledSizeFraction: Size
        get() = sizeFraction * displayScale

    /** Scaled element size in dp */
    val scaledSizeDp: DpSize
        get() = sizeDp * displayScale

    /** Scaled element hitbox (with padding) size in px */
    val scaledHitboxSize: Size
        get() = hitboxSize.toSize() * displayScale

    private val isAnimating: Boolean
        get() = _displayScale.isRunning || _displayPositionFraction.isRunning || _displayRotation.isRunning
    internal var isOverridingDisplayState = false
        private set

    /**
     * Override currently displayed transformation state to the given values
     *
     * Note: you can still access real values, and reset the display state to those values via [resetDisplayState]
     */
    suspend fun setDisplayState(
        scale: Float,
        rotation: Float,
        positionFraction: Offset,
        animate: Boolean
    ) {
        // If size is zero, it means no draw phase passed, no need to animate
        if (animate && size != IntSize.Zero) {
            // Might get cancelled if animateTo is called elsewhere while running
            try {
                coroutineScope {
                    launch { _displayScale.animateTo(scale) }
                    launch { _displayRotation.animateTo(rotation) }
                    launch { _displayPositionFraction.animateTo(positionFraction) }
                }
            } catch (e: Throwable) {
            }
        } else {
            _displayScale.snapTo(scale)
            _displayRotation.snapTo(rotation)
            _displayPositionFraction.snapTo(positionFraction)
        }
    }

    /**
     * Reset currently displayed transformation state to the real values
     */
    suspend fun resetDisplayState(animate: Boolean) {
        setDisplayState(
            scale = scale,
            rotation = rotation,
            positionFraction = positionFraction,
            animate = animate
        )
    }

    /**
     * Called when a transformable element starts being edited. This will disable gestures,
     * and override display state to the given values (by default disable scales/rotation).
     */
    suspend fun startEdit(
        scale: Float = 1f,
        rotation: Float = 0f,
        positionFraction: Offset
    ) {
        // Disable scale/rotation and override position to given edit position
        isOverridingDisplayState = true
        gesturesEnabled = false
        setDisplayState(
            scale = scale,
            rotation = rotation,
            positionFraction = positionFraction,
            animate = true
        )
    }

    /**
     * Called when a transformable element stops being edited. Gestures are re-enabled,
     * and display state is animated back to real values.
     */
    suspend fun stopEdit() {
        // Stop position override
        resetDisplayState(animate = true)
        gesturesEnabled = true
        isOverridingDisplayState = false
    }

    internal fun updateSize(
        size: IntSize,
        sizeDp: DpSize,
        hitboxSize: IntSize,
        editorSize: IntSize
    ) {
        this.size = size
        sizeFraction = Size(
            size.width / editorSize.width.toFloat(),
            size.height / editorSize.height.toFloat()
        )
        this.sizeDp = sizeDp
        this.hitboxSize = hitboxSize

        // Compute scale to get the requested size
        initialSizeFraction?.let { requestedRatioSize ->
            this.initialSizeFraction = null
            val ratioWidth = this.size.width / editorSize.width.toFloat()
            val newValue = requestedRatioSize.width / ratioWidth

            scale = newValue
            snapDisplayValue { _displayScale.snapTo(newValue) }
        }
    }

    internal fun updateScale(scale: Float, bounds: Rect) {
        val maxFactor = minOf(
            bounds.width / size.width,
            bounds.height / size.height,
            maxScale
        )
        val newValue = when {
            maxFactor > minScale -> scale.coerceIn(minScale, maxFactor)
            else -> scale
        }

        this.scale = newValue
        snapDisplayValue { _displayScale.snapTo(newValue) }
    }

    internal fun updateRotation(rotation: Float) {
        this.rotation = rotation
        snapDisplayValue { _displayRotation.snapTo(rotation) }
    }

    internal fun updatePosition(
        pan: Offset,
        editorSize: IntSize,
        bounds: Rect
    ) {
        val newValue = positionFraction
            .fractionToPx(editorSize)
            // Apply changes from gesture
            .let { it + pan.rotateBy(rotation) * scale }
            .coerceOffsetInBounds(bounds = bounds)
            .pxToFraction(editorSize)

        positionFraction = newValue
        snapDisplayValue { _displayPositionFraction.snapTo(newValue) }
    }

    /**
     * @return position for [graphicsLayer]'s translationX/translationY
     */
    internal fun scaledHitboxPosition(editorSize: IntSize): Offset {
        // Offset for scale
        // The scale from graphicsLayer is centered on the coordinates
        // If left = 150 with width = 400, centerX is 350
        // When scaled to 3*, width = 1200, centerX will stay at 350, and left = -250
        val scaleOffset = Offset(
            (scaledHitboxSize.width - hitboxSize.width) / 2f,
            (scaledHitboxSize.height - hitboxSize.height) / 2f
        )

        // Offset for hitbox padding
        val hitboxOffset = Offset(
            (scaledHitboxSize.width - scaledSize.width) / 2f,
            (scaledHitboxSize.height - scaledSize.height) / 2f,
        )

        return _displayPositionFraction.value.fractionToPx(editorSize) + scaleOffset - hitboxOffset
    }

    private fun Offset.coerceOffsetInBounds(bounds: Rect): Offset {
        val scaledElementSize = scaledSize
        val elementRect = Rect(this, scaledElementSize)

        // Apply rotation transformation onto rect
        val matrix = Matrix().apply { rotateZ(displayRotation, elementRect.center) }
        val transformedRect = matrix.map(elementRect)

        // Coerce in bounds
        val coercedRect = transformedRect.coerceInOrCenter(bounds)

        // Difference between coerced position and original position
        val offset = (coercedRect.topLeft - transformedRect.topLeft) / scale

        return this + offset
    }

    private fun Offset.fractionToPx(editorSize: IntSize): Offset {
        return Offset(x = x * editorSize.width, y = y * editorSize.height)
    }

    private fun Offset.pxToFraction(editorSize: IntSize): Offset {
        return Offset(x = x / editorSize.width, y = y / editorSize.height)
    }

    private inline fun snapDisplayValue(crossinline block: suspend () -> Unit) {
        if (isAnimating) return
        if (isOverridingDisplayState) return

        runCatching {
            runBlocking {
                withTimeout(100) {
                    block()
                }
            }
        }
    }
}
