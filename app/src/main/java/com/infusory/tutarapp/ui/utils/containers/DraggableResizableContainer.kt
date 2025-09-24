// UnifiedDraggableZoomableContainer.kt - Pure Zoom/Pan Container
package com.infusory.tutarapp.ui.utils.containers

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import com.infusory.tutarapp.R
import kotlin.math.*

data class ControlButton(
    val iconRes: Int,
    val onClick: () -> Unit,
    val position: ButtonPosition = ButtonPosition.TOP_START
)

enum class ButtonPosition {
    TOP_START, TOP_CENTER, TOP_END,
    BOTTOM_START, BOTTOM_CENTER, BOTTOM_END,
    CENTER_START, CENTER_END
}

open class UnifiedDraggableZoomableContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // Touch state
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isDragging = false
    private var isResizing = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // Container properties
    private var containerTranslationX = 0f
    private var containerTranslationY = 0f
    private var baseWidth = 300
    private var baseHeight = 300
    private var currentWidth = 300
    private var currentHeight = 300
    private var minSize = 150
    private var maxSize = 1200

    // Multi-touch support
    private val scaleGestureDetector: ScaleGestureDetector
    private var lastResizeTime = 0L

    // Dynamic control buttons
    private val controlButtons = mutableListOf<ImageView>()
    private var buttonExclusionAreas = mutableListOf<RectF>()

    // Callbacks
    var onContainerMoved: ((x: Float, y: Float) -> Unit)? = null
    var onContainerResized: ((width: Int, height: Int) -> Unit)? = null

    // Configuration - made open for inheritance
    open var isDraggingEnabled = true
    open var isResizingEnabled = true
    open var showBackground = true
        set(value) {
            field = value
            updateBackground()
        }

    init {
        // Set initial size
        baseWidth = dpToPx(300)
        baseHeight = dpToPx(300)
        currentWidth = baseWidth
        currentHeight = baseHeight

        // Set dynamic max size based on screen dimensions
        setDynamicSizeLimits()

        clipChildren = false
        clipToPadding = false

        // Set default background
        updateBackground()

        // Initialize scale gesture detector for resizing
        scaleGestureDetector = ScaleGestureDetector(context, ResizeListener())

        // Enable hardware acceleration for smooth transformations
        setLayerType(LAYER_TYPE_HARDWARE, null)

        // Set initial position
        containerTranslationX = 100f
        containerTranslationY = 100f
        applyPosition()
    }

    private fun updateBackground() {
        if (showBackground) {
            setBackgroundResource(R.drawable.dotted_border_background)
        } else {
            background = null
        }
    }

    // Method to add control buttons dynamically - made open for inheritance
    open fun addControlButtons(buttons: List<ControlButton>) {
        // Clear existing buttons
//        clearControlButtons()

        buttons.forEach { buttonConfig ->
            val button = createControlButton(buttonConfig.iconRes, buttonConfig.onClick)
            controlButtons.add(button)
            addView(button)
            positionButton(button, buttonConfig.position)
        }

        updateButtonExclusionAreas()
    }

    open fun addControlButton(button: ControlButton) {
        val imageView = createControlButton(button.iconRes, button.onClick)
        controlButtons.add(imageView)
        addView(imageView)
        positionButton(imageView, button.position)
        updateButtonExclusionAreas()
    }

//    fun clearControlButtons() {
//        controlButtons.forEach { removeView(it) }
//        controlButtons.clear()
//        buttonExclusionAreas.clear()
//    }

    private fun createControlButton(iconRes: Int, onClick: () -> Unit): ImageView {
        return ImageView(context).apply {
            layoutParams = LayoutParams(dpToPx(24), dpToPx(24))
            setImageResource(iconRes)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E0FFFFFF"))
                setStroke(2, Color.parseColor("#CCCCCC"))
            }
            scaleType = ImageView.ScaleType.CENTER
            elevation = 6f
            alpha = 0.9f
            setOnClickListener { onClick() }
        }
    }


    private fun bringButtonsToFront() {
        controlButtons.forEach { button ->
            button.bringToFront()
            button.elevation = 100f
        }
    }


    private fun positionButton(button: ImageView, position: ButtonPosition) {
        val margin = dpToPx(8)
        val buttonSize = dpToPx(24)
        val layoutParams = button.layoutParams as LayoutParams

        // Count existing buttons at the same position for stacking
        val existingButtonsAtPosition = controlButtons.count { existingButton ->
            if (existingButton == button) return@count false
            val existingLayoutParams = existingButton.layoutParams as LayoutParams

            when (position) {
                ButtonPosition.TOP_START ->
                    existingLayoutParams.gravity == (Gravity.TOP or Gravity.START)
                ButtonPosition.TOP_CENTER ->
                    existingLayoutParams.gravity == (Gravity.TOP or Gravity.CENTER_HORIZONTAL)
                ButtonPosition.TOP_END ->
                    existingLayoutParams.gravity == (Gravity.TOP or Gravity.END)
                ButtonPosition.BOTTOM_START ->
                    existingLayoutParams.gravity == (Gravity.BOTTOM or Gravity.START)
                ButtonPosition.BOTTOM_CENTER ->
                    existingLayoutParams.gravity == (Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL)
                ButtonPosition.BOTTOM_END ->
                    existingLayoutParams.gravity == (Gravity.BOTTOM or Gravity.END)
                ButtonPosition.CENTER_START ->
                    existingLayoutParams.gravity == (Gravity.CENTER_VERTICAL or Gravity.START)
                ButtonPosition.CENTER_END ->
                    existingLayoutParams.gravity == (Gravity.CENTER_VERTICAL or Gravity.END)
            }
        }

        // Calculate vertical offset for stacking
        val verticalOffset = existingButtonsAtPosition * (buttonSize + dpToPx(4))

        when (position) {
            ButtonPosition.TOP_START -> {
                // Position buttons at the very edge of the container
                val horizontalOffset = dpToPx(2) // Small positive margin from edge
                val verticalStartOffset = dpToPx(2) // Small positive margin from edge

                layoutParams.setMargins(
                    horizontalOffset,
                    verticalStartOffset + verticalOffset,
                    0,
                    0
                )
                layoutParams.gravity = Gravity.TOP or Gravity.START
            }
            ButtonPosition.TOP_CENTER -> {
                layoutParams.setMargins(0, -(buttonSize + dpToPx(8)) + verticalOffset, 0, 0)
                layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            }
            ButtonPosition.TOP_END -> {
                layoutParams.setMargins(0, -(buttonSize + dpToPx(8)) + verticalOffset, -(buttonSize + dpToPx(8)), 0)
                layoutParams.gravity = Gravity.TOP or Gravity.END
            }
            ButtonPosition.BOTTOM_START -> {
                layoutParams.setMargins(-(buttonSize + dpToPx(8)), 0, 0, -(buttonSize + dpToPx(8)) + verticalOffset)
                layoutParams.gravity = Gravity.BOTTOM or Gravity.START
            }
            ButtonPosition.BOTTOM_CENTER -> {
                layoutParams.setMargins(0, 0, 0, -(buttonSize + dpToPx(8)) + verticalOffset)
                layoutParams.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
            ButtonPosition.BOTTOM_END -> {
                layoutParams.setMargins(0, 0, -(buttonSize + dpToPx(8)), -(buttonSize + dpToPx(8)) + verticalOffset)
                layoutParams.gravity = Gravity.BOTTOM or Gravity.END
            }
            ButtonPosition.CENTER_START -> {
                layoutParams.setMargins(-(buttonSize + dpToPx(8)), verticalOffset, 0, 0)
                layoutParams.gravity = Gravity.CENTER_VERTICAL or Gravity.START
            }
            ButtonPosition.CENTER_END -> {
                layoutParams.setMargins(0, verticalOffset, -(buttonSize + dpToPx(8)), 0)
                layoutParams.gravity = Gravity.CENTER_VERTICAL or Gravity.END
            }
        }

        button.layoutParams = layoutParams
    }

    private fun updateButtonExclusionAreas() {
        buttonExclusionAreas.clear()

        controlButtons.forEach { button ->
            // Calculate the area around each button where touches should be ignored for dragging
            val layoutParams = button.layoutParams as LayoutParams
            val buttonSize = dpToPx(24)
            val touchPadding = dpToPx(16) // Extra touch area around button

            val rect = when {
                layoutParams.gravity and Gravity.TOP != 0 && layoutParams.gravity and Gravity.START != 0 -> {
                    RectF(0f, 0f, (buttonSize + touchPadding).toFloat(), (buttonSize + touchPadding).toFloat())
                }
                layoutParams.gravity and Gravity.TOP != 0 && layoutParams.gravity and Gravity.END != 0 -> {
                    RectF((currentWidth - buttonSize - touchPadding).toFloat(), 0f, currentWidth.toFloat(), (buttonSize + touchPadding).toFloat())
                }
                layoutParams.gravity and Gravity.BOTTOM != 0 && layoutParams.gravity and Gravity.START != 0 -> {
                    RectF(0f, (currentHeight - buttonSize - touchPadding).toFloat(), (buttonSize + touchPadding).toFloat(), currentHeight.toFloat())
                }
                layoutParams.gravity and Gravity.BOTTOM != 0 && layoutParams.gravity and Gravity.END != 0 -> {
                    RectF((currentWidth - buttonSize - touchPadding).toFloat(), (currentHeight - buttonSize - touchPadding).toFloat(), currentWidth.toFloat(), currentHeight.toFloat())
                }
                // Add more cases for other positions as needed
                else -> RectF(0f, 0f, (buttonSize + touchPadding).toFloat(), (buttonSize + touchPadding).toFloat())
            }

            buttonExclusionAreas.add(rect)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDraggingEnabled && !isResizingEnabled) {
            return super.onTouchEvent(event)
        }

        // Handle resize gestures first
        val handled = if (isResizingEnabled) scaleGestureDetector.onTouchEvent(event) else false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(0)
                lastTouchX = event.rawX
                lastTouchY = event.rawY
                isDragging = false

                // Don't start dragging if we're in button area
                if (isDraggingEnabled && !isTouchInButtonArea(event.x, event.y)) {
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // Multi-touch started - prepare for resizing
                if (isResizingEnabled && event.pointerCount == 2) {
                    isResizing = true
                    isDragging = false
                    lastResizeTime = System.currentTimeMillis()
                    parent?.requestDisallowInterceptTouchEvent(true)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isResizing && isResizingEnabled && event.pointerCount >= 2) {
                    // Let ScaleGestureDetector handle resizing
                    return handled
                } else if (!isResizing && isDraggingEnabled && event.pointerCount == 1) {
                    // Handle dragging
                    val pointerIndex = event.findPointerIndex(activePointerId)
                    if (pointerIndex < 0) return true

                    if (!isDragging) {
                        // Check if we've moved enough to start dragging
                        val deltaX = abs(event.rawX - lastTouchX)
                        val deltaY = abs(event.rawY - lastTouchY)
                        if (deltaX > 10 || deltaY > 10) { // Touch slop threshold
                            isDragging = true
                        }
                    }

                    if (isDragging && !isTouchInButtonArea(event.x, event.y)) {
                        val currentX = event.rawX
                        val currentY = event.rawY

                        val dx = currentX - lastTouchX
                        val dy = currentY - lastTouchY

                        containerTranslationX += dx
                        containerTranslationY += dy

                        // Apply bounds to keep container partially on screen
                        applyScreenBounds()
                        applyPosition()

                        onContainerMoved?.invoke(containerTranslationX, containerTranslationY)

                        lastTouchX = currentX
                        lastTouchY = currentY
                    }
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount <= 2) {
                    isResizing = false
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                isDragging = false
                isResizing = false
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }

        return handled || super.onTouchEvent(event)
    }

    private fun isTouchInButtonArea(x: Float, y: Float): Boolean {
        return buttonExclusionAreas.any { rect -> rect.contains(x, y) }
    }

    private fun applyScreenBounds() {
        val parent = parent as? ViewGroup ?: return

        val parentWidth = parent.width
        val parentHeight = parent.height

        // Allow 80% of container to go off-screen but keep 20% visible
        val minX = -currentWidth * 0.8f
        val maxX = parentWidth - currentWidth * 0.2f
        val minY = -currentHeight * 0.8f
        val maxY = parentHeight - currentHeight * 0.2f

        containerTranslationX = containerTranslationX.coerceIn(minX, maxX)
        containerTranslationY = containerTranslationY.coerceIn(minY, maxY)
    }

    private fun applyPosition() {
        translationX = containerTranslationX
        translationY = containerTranslationY
    }

    private fun resizeContainer(newWidth: Int, newHeight: Int) {
        val constrainedWidth = newWidth.coerceIn(minSize, maxSize)
        val constrainedHeight = newHeight.coerceIn(minSize, maxSize)

        if (constrainedWidth != currentWidth || constrainedHeight != currentHeight) {
            currentWidth = constrainedWidth
            currentHeight = constrainedHeight

            // Post layout update to avoid crash during touch events
            post {
                try {
                    val currentLayoutParams = layoutParams
                    if (currentLayoutParams != null) {
                        currentLayoutParams.width = currentWidth
                        currentLayoutParams.height = currentHeight
                        layoutParams = currentLayoutParams
                    }
                    requestLayout()

                    // Update button exclusion areas when size changes
                    updateButtonExclusionAreas()

                    // Adjust position to keep container partially on screen
                    applyScreenBounds()
                    applyPosition()

                    onContainerResized?.invoke(currentWidth, currentHeight)
                } catch (e: Exception) {
                    Log.e("Container", "Error resizing container", e)
                }
            }
        }
    }

    private inner class ResizeListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            isResizing = true
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
            if (!isResizing || !isResizingEnabled) return false

            try {
                val scaleFactor = detector.scaleFactor
                val newSize = (currentWidth * scaleFactor).toInt()
                val constrainedSize = newSize.coerceIn(minSize, maxSize)

                if (abs(constrainedSize - currentWidth) > 5) {
                    resizeContainer(constrainedSize, constrainedSize)
                }
            } catch (e: Exception) {
                Log.e("Container", "Error during scaling", e)
                return false
            }

            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            isResizing = false
        }
    }

    // Public methods for content management - made open for inheritance
    open fun setContent(view: View) {
        // Remove existing content (except buttons)
        val viewsToRemove = mutableListOf<View>()

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (!controlButtons.contains(child)) {
                viewsToRemove.add(child)
            }
        }

        viewsToRemove.forEach { removeView(it) }

        // Add new content with proper margins to avoid button overlap
        val contentLayoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        ).apply {
            val margin = dpToPx(8)
            val topMargin = if (hasButtonsInTopArea()) dpToPx(40) else margin
            val bottomMargin = if (hasButtonsInBottomArea()) dpToPx(40) else margin
            setMargins(margin, topMargin, margin, bottomMargin)
        }

        view.layoutParams = contentLayoutParams
        addView(view, 0) // Add at index 0 so buttons stay on top

        bringButtonsToFront()
    }

    open fun removeContent() {
        val viewsToRemove = mutableListOf<View>()

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (!controlButtons.contains(child)) {
                viewsToRemove.add(child)
            }
        }

        viewsToRemove.forEach { removeView(it) }
    }

    private fun hasButtonsInTopArea(): Boolean {
        return controlButtons.any { button ->
            val layoutParams = button.layoutParams as LayoutParams
            layoutParams.gravity and Gravity.TOP != 0
        }
    }

    private fun hasButtonsInBottomArea(): Boolean {
        return controlButtons.any { button ->
            val layoutParams = button.layoutParams as LayoutParams
            layoutParams.gravity and Gravity.BOTTOM != 0
        }
    }

    // Public methods for container manipulation - made open for inheritance
    open fun setContainerSize(width: Int, height: Int, animate: Boolean = false) {
        if (animate) {
            val startWidth = currentWidth
            val startHeight = currentHeight
            val targetWidth = width.coerceIn(minSize, maxSize)
            val targetHeight = height.coerceIn(minSize, maxSize)

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 300
                addUpdateListener { animation ->
                    val progress = animation.animatedValue as Float
                    val newWidth = (startWidth + (targetWidth - startWidth) * progress).toInt()
                    val newHeight = (startHeight + (targetHeight - startHeight) * progress).toInt()
                    resizeContainer(newWidth, newHeight)
                }
            }
            animator.start()
        } else {
            resizeContainer(width, height)
        }
    }

    open fun resetTransform() {
        currentWidth = baseWidth
        currentHeight = baseHeight
        containerTranslationX = 100f
        containerTranslationY = 100f

        val currentLayoutParams = layoutParams
        if (currentLayoutParams != null) {
            currentLayoutParams.width = currentWidth
            currentLayoutParams.height = currentHeight
            layoutParams = currentLayoutParams
        }
        requestLayout()
        updateButtonExclusionAreas()
        applyPosition()
    }

    open fun getCurrentSize(): Pair<Int, Int> = Pair(currentWidth, currentHeight)

    open fun getCurrentPosition(): Pair<Float, Float> = Pair(containerTranslationX, containerTranslationY)

    open fun moveContainerTo(x: Float, y: Float, animate: Boolean = false) {
        if (animate) {
            animate()
                .translationX(x)
                .translationY(y)
                .setDuration(300)
                .withEndAction {
                    containerTranslationX = x
                    containerTranslationY = y
                }
                .start()
        } else {
            containerTranslationX = x
            containerTranslationY = y
            applyPosition()
        }
    }

    open fun setSizeLimits(minSize: Int, maxSize: Int) {
        this.minSize = minSize.coerceAtLeast(100)
        this.maxSize = maxSize.coerceAtMost(getScreenHeight())

        if (currentWidth < this.minSize || currentWidth > this.maxSize) {
            val newWidth = currentWidth.coerceIn(this.minSize, this.maxSize)
            val newHeight = currentHeight.coerceIn(this.minSize, this.maxSize)
            resizeContainer(newWidth, newHeight)
        }
    }

    private fun setDynamicSizeLimits() {
        val screenWidth = getScreenWidth()
        val screenHeight = getScreenHeight()

        minSize = dpToPx(150)
        maxSize = (min(screenWidth, screenHeight) * 0.9f).toInt()
    }

    private fun getScreenWidth(): Int = context.resources.displayMetrics.widthPixels
    private fun getScreenHeight(): Int = context.resources.displayMetrics.heightPixels

    open fun resizeTo(size: Int, animate: Boolean = false) {
        setContainerSize(size, size, animate)
    }

    open fun zoomTo(scale: Float, animate: Boolean = false) {
        val newSize = (baseWidth * scale).toInt()
        setContainerSize(newSize, newSize, animate)
    }

    open fun getCurrentScale(): Float = currentWidth.toFloat() / baseWidth.toFloat()

    protected fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}