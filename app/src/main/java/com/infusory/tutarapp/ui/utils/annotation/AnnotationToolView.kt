// AnnotationToolView.kt - Fixed to use callbacks instead of direct access
package com.infusory.tutarapp.ui.annotation

import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import com.infusory.tutarapp.R

// Enum for annotation tools
enum class AnnotationTool {
    FREE_DRAW,
    LINE,
    RECTANGLE,
    CIRCLE,
    ARROW,
    SELECTION
}

// AnnotationToolbar class
class AnnotationToolbar @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    // Tool buttons
    private lateinit var freeDrawButton: ImageButton
    private lateinit var lineButton: ImageButton
    private lateinit var rectangleButton: ImageButton
    private lateinit var circleButton: ImageButton
    private lateinit var arrowButton: ImageButton
    private lateinit var selectionButton: ImageButton
    private lateinit var undoButton: ImageButton
    private lateinit var clearButton: ImageButton
    private lateinit var closeButton: ImageButton

    // Currently selected tool
    private var selectedTool = AnnotationTool.FREE_DRAW

    // Callbacks
    var onToolSelected: ((AnnotationTool) -> Unit)? = null
    var onUndoPressed: (() -> Unit)? = null
    var onClearPressed: (() -> Unit)? = null
    var onCloseAnnotation: (() -> Unit)? = null

    init {
        setupToolbar()
    }

    private fun setupToolbar() {
        orientation = HORIZONTAL

        // Set toolbar background
        background = createToolbarBackground()

        // Add padding
        val padding = dpToPx(12)
        setPadding(padding, padding, padding, padding)

        // Create tool buttons
        createToolButtons()

        // Set initial selection
        selectTool(AnnotationTool.FREE_DRAW)
    }

    private fun createToolbarBackground(): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dpToPx(25).toFloat()
            setColor(ContextCompat.getColor(context, android.R.color.black))
            alpha = 200 // Semi-transparent
        }
    }

    private fun createToolButtons() {
        // Free Draw Button
        freeDrawButton = createToolButton(R.drawable.ic_flash, "Free Draw") {
            selectTool(AnnotationTool.FREE_DRAW)
        }
        addView(freeDrawButton)

        addSeparator()

        // Line Button
        lineButton = createToolButton(R.drawable.ic_flash, "Line") {
            selectTool(AnnotationTool.LINE)
        }
        addView(lineButton)

        // Rectangle Button
        rectangleButton = createToolButton(R.drawable.ic_flash, "Rectangle") {
            selectTool(AnnotationTool.RECTANGLE)
        }
        addView(rectangleButton)

        // Circle Button
        circleButton = createToolButton(R.drawable.ic_flash, "Circle") {
            selectTool(AnnotationTool.CIRCLE)
        }
        addView(circleButton)

        // Arrow Button
        arrowButton = createToolButton(R.drawable.ic_flash, "Arrow") {
            selectTool(AnnotationTool.ARROW)
        }
        addView(arrowButton)

        // Selection Button
        selectionButton = createToolButton(R.drawable.ic_flash, "Selection") {
            selectTool(AnnotationTool.SELECTION)
        }
        addView(selectionButton)

        addSeparator()

        // Undo Button
        undoButton = createActionButton(R.drawable.ic_flash, "Undo") {
            onUndoPressed?.invoke()
        }
        addView(undoButton)

        // Clear Button
        clearButton = createActionButton(R.drawable.ic_flash, "Clear") {
            onClearPressed?.invoke()
        }
        addView(clearButton)

        addSeparator()

        // Close Button
        closeButton = createActionButton(R.drawable.ic_flash, "Close") {
            onCloseAnnotation?.invoke()
        }
        addView(closeButton)
    }

    private fun createToolButton(iconRes: Int, contentDescription: String, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                marginStart = dpToPx(4)
                marginEnd = dpToPx(4)
            }

            background = createButtonBackground(false)
            setImageResource(iconRes)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            this.contentDescription = contentDescription

            setOnClickListener { onClick() }
        }
    }

    private fun createActionButton(iconRes: Int, contentDescription: String, onClick: () -> Unit): ImageButton {
        return ImageButton(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(40), dpToPx(40)).apply {
                marginStart = dpToPx(4)
                marginEnd = dpToPx(4)
            }

            background = createButtonBackground(false)
            setImageResource(iconRes)
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            this.contentDescription = contentDescription

            setOnClickListener { onClick() }
        }
    }

    private fun addSeparator() {
        val separator = View(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(1), dpToPx(30)).apply {
                marginStart = dpToPx(8)
                marginEnd = dpToPx(8)
            }
            setBackgroundColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }
        addView(separator)
    }

    private fun createButtonBackground(selected: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = dpToPx(8).toFloat()
            if (selected) {
                setColor(ContextCompat.getColor(context, android.R.color.holo_blue_light))
            } else {
                setColor(ContextCompat.getColor(context, android.R.color.transparent))
            }
        }
    }

    private fun selectTool(tool: AnnotationTool) {
        // Clear previous selection
        clearAllSelections()

        // Set new selection
        selectedTool = tool
        val selectedButton = when (tool) {
            AnnotationTool.FREE_DRAW -> freeDrawButton
            AnnotationTool.LINE -> lineButton
            AnnotationTool.RECTANGLE -> rectangleButton
            AnnotationTool.CIRCLE -> circleButton
            AnnotationTool.ARROW -> arrowButton
            AnnotationTool.SELECTION -> selectionButton
        }

        selectedButton.background = createButtonBackground(true)
        onToolSelected?.invoke(tool)
    }

    private fun clearAllSelections() {
        val buttons = listOf(
            freeDrawButton, lineButton, rectangleButton,
            circleButton, arrowButton, selectionButton
        )

        buttons.forEach { button ->
            button.background = createButtonBackground(false)
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    fun getSelectedTool(): AnnotationTool = selectedTool

    fun setToolbarEnabled(enabled: Boolean) {
        val buttons = listOf(
            freeDrawButton, lineButton, rectangleButton,
            circleButton, arrowButton, selectionButton,
            undoButton, clearButton, closeButton
        )

        buttons.forEach { button ->
            button.isEnabled = enabled
            button.alpha = if (enabled) 1.0f else 0.5f
        }
    }
}

// Main AnnotationToolView class
class AnnotationToolView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : RelativeLayout(context, attrs, defStyleAttr) {

    // Drawing surface - ALWAYS VISIBLE to preserve drawings
    private var drawingView: DrawingView? = null

    // Toolbar
    private var annotationToolbar: AnnotationToolbar? = null

    // State
    private var isAnnotationMode = false

    // Callbacks
    var onAnnotationToggle: ((Boolean) -> Unit)? = null
    // Callback for 3D rendering control
    var onDrawingStateChanged: ((isDrawing: Boolean) -> Unit)? = null

    init {
        setupAnnotationTool()
    }

    private fun setupAnnotationTool() {
        // Create drawing view - ALWAYS VISIBLE
        drawingView = DrawingView(context)
        val drawingParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        drawingView?.layoutParams = drawingParams
        // Drawing view is always visible but touch is disabled initially
        drawingView?.visibility = View.VISIBLE
        drawingView?.setTouchEnabled(false) // Disable touch initially
        addView(drawingView)

        // Create toolbar
        annotationToolbar = AnnotationToolbar(context)
        val toolbarParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        toolbarParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        toolbarParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        toolbarParams.bottomMargin = 120 // Space from bottom to avoid other UI elements
        annotationToolbar?.layoutParams = toolbarParams
        annotationToolbar?.visibility = View.GONE // Hidden initially
        addView(annotationToolbar)

        // Set up toolbar callbacks
        annotationToolbar?.onToolSelected = { tool: AnnotationTool ->
            drawingView?.setDrawingTool(tool)
        }

        annotationToolbar?.onUndoPressed = {
            drawingView?.undoLastDrawing()
        }

        annotationToolbar?.onClearPressed = {
            drawingView?.clearAllDrawings()
        }

        annotationToolbar?.onCloseAnnotation = {
            toggleAnnotationMode(false)
        }

        // Set up drawing state callback
        drawingView?.onDrawingStateChanged = { isDrawing ->
            onDrawingStateChanged?.invoke(isDrawing)
        }
    }

    fun toggleAnnotationMode(enable: Boolean? = null) {
        isAnnotationMode = enable ?: !isAnnotationMode

        if (isAnnotationMode) {
            // Show toolbar and enable touch
            annotationToolbar?.visibility = View.VISIBLE
            drawingView?.setTouchEnabled(true)
            drawingView?.clearSelection()
        } else {
            // Hide toolbar and disable touch (but keep drawings visible)
            annotationToolbar?.visibility = View.GONE
            drawingView?.setTouchEnabled(false)
        }

        onAnnotationToggle?.invoke(isAnnotationMode)
    }

    fun isInAnnotationMode(): Boolean = isAnnotationMode

    fun clearAllAnnotations() {
        drawingView?.clearAllDrawings()
    }

    fun undoLastAnnotation() {
        drawingView?.undoLastDrawing()
    }

    // Custom drawing view for handling annotations
    inner class DrawingView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : View(context, attrs, defStyleAttr) {

        private var currentTool = AnnotationTool.FREE_DRAW
        private var paint = Paint().apply {
            color = Color.RED
            strokeWidth = 5f
            style = Paint.Style.STROKE
            isAntiAlias = true
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        private var currentPath = Path()
        private var paths = mutableListOf<DrawingPath>()
        private var startX = 0f
        private var startY = 0f
        private var isDrawing = false
        private var touchEnabled = false // Control touch interaction

        // Callback for drawing state changes
        var onDrawingStateChanged: ((Boolean) -> Unit)? = null

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)

            // ALWAYS draw all saved paths (preserves drawings when tools are hidden)
            paths.forEach { drawingPath ->
                canvas.drawPath(drawingPath.path, drawingPath.paint)
            }

            // Draw current path if drawing
            if (isDrawing && touchEnabled) {
                canvas.drawPath(currentPath, paint)
            }
        }

        override fun onTouchEvent(event: MotionEvent): Boolean {
            // Only handle touch events if touch is enabled
            if (!touchEnabled) {
                return false
            }

            val x = event.x
            val y = event.y

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startDrawing(x, y)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    continueDrawing(x, y)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    finishDrawing(x, y)
                    return true
                }
            }
            return false
        }

        // Method to enable/disable touch interaction
        fun setTouchEnabled(enabled: Boolean) {
            touchEnabled = enabled
        }

        private fun startDrawing(x: Float, y: Float) {
            startX = x
            startY = y
            isDrawing = true
            currentPath.reset()

            // REMOVED: Direct calls to containerManager - use callback instead
            // Notify that drawing has started - pause 3D rendering
            onDrawingStateChanged?.invoke(true)

            when (currentTool) {
                AnnotationTool.FREE_DRAW -> {
                    currentPath.moveTo(x, y)
                }
                else -> {
                    // For shapes, we'll draw on ACTION_UP
                }
            }
            invalidate()
        }

        private fun continueDrawing(x: Float, y: Float) {
            when (currentTool) {
                AnnotationTool.FREE_DRAW -> {
                    currentPath.lineTo(x, y)
                    invalidate()
                }
                AnnotationTool.LINE -> {
                    currentPath.reset()
                    currentPath.moveTo(startX, startY)
                    currentPath.lineTo(x, y)
                    invalidate()
                }
                AnnotationTool.RECTANGLE -> {
                    currentPath.reset()
                    currentPath.addRect(startX, startY, x, y, Path.Direction.CW)
                    invalidate()
                }
                AnnotationTool.CIRCLE -> {
                    currentPath.reset()
                    val radius = kotlin.math.sqrt(
                        (x - startX) * (x - startX) + (y - startY) * (y - startY)
                    )
                    currentPath.addCircle(startX, startY, radius, Path.Direction.CW)
                    invalidate()
                }
                AnnotationTool.ARROW -> {
                    currentPath.reset()
                    drawArrow(currentPath, startX, startY, x, y)
                    invalidate()
                }
                AnnotationTool.SELECTION -> {
                    // Handle selection logic here
                }
            }
        }

        private fun finishDrawing(x: Float, y: Float) {
            if (isDrawing && currentTool != AnnotationTool.SELECTION) {
                // Save the current path
                val newPaint = Paint(paint)
                paths.add(DrawingPath(Path(currentPath), newPaint))
                currentPath.reset()
            }
            isDrawing = false

            // REMOVED: Direct calls to containerManager - use callback instead
            // Notify that drawing has ended - resume 3D rendering
            onDrawingStateChanged?.invoke(false)

            invalidate()
        }

        private fun drawArrow(path: Path, startX: Float, startY: Float, endX: Float, endY: Float) {
            path.moveTo(startX, startY)
            path.lineTo(endX, endY)

            // Calculate arrow head
            val arrowLength = 30f
            val arrowAngle = Math.PI / 6 // 30 degrees

            val angle = kotlin.math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())

            val arrowX1 = endX - arrowLength * kotlin.math.cos(angle - arrowAngle).toFloat()
            val arrowY1 = endY - arrowLength * kotlin.math.sin(angle - arrowAngle).toFloat()

            val arrowX2 = endX - arrowLength * kotlin.math.cos(angle + arrowAngle).toFloat()
            val arrowY2 = endY - arrowLength * kotlin.math.sin(angle + arrowAngle).toFloat()

            path.moveTo(endX, endY)
            path.lineTo(arrowX1, arrowY1)
            path.moveTo(endX, endY)
            path.lineTo(arrowX2, arrowY2)
        }

        fun setDrawingTool(tool: AnnotationTool) {
            currentTool = tool

            // Update paint style based on tool
            when (tool) {
                AnnotationTool.FREE_DRAW, AnnotationTool.LINE, AnnotationTool.ARROW -> {
                    paint.style = Paint.Style.STROKE
                }
                AnnotationTool.RECTANGLE, AnnotationTool.CIRCLE -> {
                    paint.style = Paint.Style.STROKE // Change to FILL for filled shapes
                }
                AnnotationTool.SELECTION -> {
                    // Selection tool doesn't draw
                }
            }
        }

        fun clearAllDrawings() {
            paths.clear()
            currentPath.reset()
            invalidate()
        }

        fun undoLastDrawing() {
            if (paths.isNotEmpty()) {
                paths.removeAt(paths.size - 1)
                invalidate()
            }
        }

        fun clearSelection() {
            // Clear any selection state
        }
    }

    // Data class to store drawing paths with their paint properties
    private data class DrawingPath(
        val path: Path,
        val paint: Paint
    )
}