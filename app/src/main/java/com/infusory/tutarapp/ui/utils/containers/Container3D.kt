package com.infusory.tutarapp.ui.containers

import com.infusory.tutarapp.R
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.Choreographer
import android.view.SurfaceView
import android.widget.TextView
import android.widget.LinearLayout
import android.widget.ImageView
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import com.google.android.filament.View
import com.google.android.filament.android.UiHelper
import com.google.android.filament.utils.AutomationEngine
import com.google.android.filament.utils.KTX1Loader
import com.google.android.filament.utils.ModelViewer
import com.google.android.filament.utils.Utils
import java.nio.ByteBuffer
import java.nio.ByteOrder
import android.graphics.PixelFormat
import com.infusory.tutarapp.ui.data.ModelData


class Container3D @JvmOverloads constructor(
    context: Context,
    private val modelIndex: Int = 0,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ContainerBase(context, ContainerType.MODEL_3D, attrs, defStyleAttr) {

    companion object {
        init {
            Utils.init()
        }

        // Model file names - update these with your actual model files
        private val modelFiles = arrayOf(
            "Eagle.glb",
            "skeleton.glb",
            "skeleton.glb", // Replace with different models
            "Eagle.glb"     // Replace with different models
        )
    }

    // Filament components
    private var surfaceView: SurfaceView? = null
    private var uiHelper: UiHelper? = null
    private var modelViewer: ModelViewer? = null
    private val viewerContent = AutomationEngine.ViewerContent()
    private var frameCallback: FrameCallback? = null
    private var choreographer: Choreographer? = null

    private var touchEnabled = false
    private var passThroughTouches = true

    // Layout components
    private var controlsContainer: LinearLayout? = null
    private var contentContainer: android.widget.FrameLayout? = null
    private var animationToggleButton: ImageView? = null

    // Animation state
    private var currentAnimationIndex = 0
    private var animationStartTime = System.nanoTime()
    private var currentAnimationDuration = 0f
    private var isAnimationPaused = false
    private var pausedAnimationTime = 0f

    // Container state
    private var isInitialized = false
    private var isRenderingActive = false

    private var customModelData: ModelData? = null
    private var customModelPath: String? = null

    init {
        // Disable background for main container since content container will have it
        showBackground = false
        setup3DContainer()
    }


    fun setTouchEnabled(enabled: Boolean) {
        touchEnabled = enabled
        updateTouchHandling()
    }

    fun setModelData(modelData: ModelData, fullPath: String) {
        this.customModelData = modelData
        this.customModelPath = fullPath
    }

    /**
     * Set whether touches should pass through to underlying layers
     */
    fun setPassThroughTouches(enabled: Boolean) {
        passThroughTouches = enabled
        updateTouchHandling()
    }

    private fun updateTouchHandling() {
        surfaceView?.let { surface ->
            when {
                !touchEnabled || passThroughTouches -> {
                    // Disable 3D touch handling and pass through
                    surface.setOnTouchListener { _, _ -> false }
                    surface.isClickable = false
                    surface.isFocusable = false
                    surface.isFocusableInTouchMode = false
                }
                touchEnabled -> {
                    // Enable 3D model manipulation
                    surface.setOnTouchListener { _, event ->
                        modelViewer?.onTouchEvent(event) ?: false
                        true
                    }
                    surface.isClickable = true
                    surface.isFocusable = true
                    surface.isFocusableInTouchMode = true
                }
            }
        }
    }

    private fun setup3DContainer() {
        // Don't add control buttons to the base container anymore
        // We'll create our own layout structure
    }

    override fun initializeContent() {
        if (!isInitialized) {
            createSideControlsLayout()
            create3DView()
            isInitialized = true
        }
    }

    private fun createSideControlsLayout() {
        // Create main horizontal container
        val mainContainer = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Create controls container (left side)
        controlsContainer = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(48), // Fixed width for controls
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            gravity = Gravity.TOP
        }

        // Create content container (right side) with dotted border
        contentContainer = android.widget.FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                0, // Use weight
                LinearLayout.LayoutParams.MATCH_PARENT,
                1f // Take remaining space
            )
            // Apply the dotted border background to content container only
            setBackgroundResource(R.drawable.dotted_border_background)
            // Add padding so the dotted border is visible
            setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
        }

        // Add control buttons to controls container
        addControlButtonsToSide()

        // Assemble layout
        mainContainer.addView(controlsContainer)
        mainContainer.addView(contentContainer)

        // Set this as the main content
        setContent(mainContainer)
    }

    private fun addControlButtonsToSide() {
        controlsContainer?.let { container ->

            // Model info button
            container.addView(createSideControlButton(
                android.R.drawable.ic_menu_rotate,
                "interact"
            ) { showModelInfo() })

            // Add some spacing
            container.addView(createSpacer())

            // Animation toggle button (play/pause)
            animationToggleButton = createSideControlButton(
                android.R.drawable.ic_media_play,
                "Toggle Animation"
            ) { toggleAnimation() }
            container.addView(animationToggleButton!!)

            // Add some spacing
            container.addView(createSpacer())

//            // More options button
//            container.addView(createSideControlButton(
//                android.R.drawable.ic_menu_more,
//                "Options"
//            ) { show3DMenu() })

            // Close button
            container.addView(createSideControlButton(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close"
            ) { onRemoveRequest?.invoke() })
        }
    }

    private fun createSideControlButton(iconRes: Int, tooltip: String, onClick: () -> Unit): ImageView {
        return ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            }
            setImageResource(iconRes)
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E0FFFFFF"))
                setStroke(2, Color.parseColor("#CCCCCC"))
            }
            scaleType = ImageView.ScaleType.CENTER
            elevation = 4f
            alpha = 0.9f
            contentDescription = tooltip

            setOnClickListener { onClick() }

            // Add touch feedback
            setOnTouchListener { view, event ->
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        alpha = 0.6f
                        scaleX = 0.95f
                        scaleY = 0.95f
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        alpha = 0.9f
                        scaleX = 1.0f
                        scaleY = 1.0f
                    }
                }
                false
            }
        }
    }

    private fun createSpacer(): android.view.View {
        return android.view.View(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(2)
            )
        }
    }

    private fun create3DView() {
        try {
            // Create surface view for rendering
            surfaceView = SurfaceView(context).apply {
                layoutParams = android.widget.FrameLayout.LayoutParams(
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                    android.widget.FrameLayout.LayoutParams.MATCH_PARENT
                )

                // CRITICAL: Set up surface for transparency
                holder.setFormat(PixelFormat.TRANSLUCENT)
                setZOrderOnTop(false)  // Keep this
                setZOrderMediaOverlay(true)  // ADD THIS LINE - allows transparency
            }

            // Initialize UI helper with transparency
            uiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK).apply {
                isOpaque = false  // Keep this
            }

            modelViewer = ModelViewer(surfaceView = surfaceView!!, uiHelper = uiHelper!!)

            // Initial touch setup
            updateTouchHandling()

            // Configure viewer for transparency (MODIFIED)
            configureViewerForTransparency()
            loadModel()
            createIndirectLight()

            contentContainer?.addView(surfaceView!!)
            startRendering()

        } catch (e: Exception) {
            contentContainer?.addView(createFallbackView())
            android.util.Log.e("Container3D", "Failed to initialize 3D view", e)
        }
    }


    private fun configureViewerForTransparency() {
        modelViewer?.view?.apply {
            // CRITICAL: Use transparent blend mode instead of opaque
            blendMode = View.BlendMode.TRANSLUCENT

            // Set clear color to transparent through renderer
            // Clear color is set through the renderer, not the view directly

            // Optimize render quality for performance while maintaining transparency
            renderQuality = renderQuality.apply {
                hdrColorBuffer = View.QualityLevel.LOW
            }

            // Disable dynamic resolution for consistent performance
            dynamicResolutionOptions = dynamicResolutionOptions.apply {
                enabled = false
                quality = View.QualityLevel.LOW
            }

            // Keep MSAA disabled for better performance
            multiSampleAntiAliasingOptions = multiSampleAntiAliasingOptions.apply {
                enabled = false
            }

            // Use FXAA instead of no anti-aliasing for better quality with transparency
            antiAliasing = View.AntiAliasing.FXAA

            // Disable expensive effects for performance
            ambientOcclusionOptions = ambientOcclusionOptions.apply {
                enabled = false
            }

            bloomOptions = bloomOptions.apply {
                enabled = false
            }

            screenSpaceReflectionsOptions = screenSpaceReflectionsOptions.apply {
                enabled = false
            }

            temporalAntiAliasingOptions = temporalAntiAliasingOptions.apply {
                enabled = false
            }

            fogOptions = fogOptions.apply {
                enabled = false
            }

            depthOfFieldOptions = depthOfFieldOptions.apply {
                enabled = false
            }

            vignetteOptions = vignetteOptions.apply {
                enabled = false
            }
        }

        // Set transparent clear color through the renderer
        modelViewer?.renderer?.let { renderer ->
            renderer.clearOptions?.let { clearOptions ->
                clearOptions.clearColor = floatArrayOf(0.0f, 0.0f, 0.0f, 0.0f) // Transparent black
                clearOptions.clear = true
                renderer.setClearOptions(clearOptions)
            }
        }
    }



//    private fun addTouchModeToggleButton() {
//        controlsContainer?.let { container ->
//            val touchButton = createSideControlButton(
//                if (touchEnabled) android.R.drawable.ic_lock_idle_lock else android.R.drawable.ic_lock_lock,
//                "Toggle Touch Mode"
//            ) {
//                setTouchEnabled(!touchEnabled)
//                // Update button icon
//                (container.getChildAt(container.childCount - 1) as? ImageView)?.let { button ->
//                    button.setImageResource(
//                        if (touchEnabled) android.R.drawable.ic_lock_idle_lock else android.R.drawable.ic_lock_lock
//                    )
//                }
//            }
//            container.addView(touchButton)
//        }
//    }


//    private fun configureViewer() {
//        modelViewer?.view?.apply {
//            // Use opaque blend mode for better performance
//            blendMode = View.BlendMode.OPAQUE
//
//            // Optimize render quality for performance
//            renderQuality = renderQuality.apply {
//                hdrColorBuffer = View.QualityLevel.LOW
//            }
//
//            // Disable dynamic resolution for consistent performance
//            dynamicResolutionOptions = dynamicResolutionOptions.apply {
//                enabled = false
//                quality = View.QualityLevel.LOW
//            }
//
//            // Disable MSAA for better performance
//            multiSampleAntiAliasingOptions = multiSampleAntiAliasingOptions.apply {
//                enabled = false
//            }
//
//            // Disable anti-aliasing for maximum performance
//            antiAliasing = View.AntiAliasing.NONE
//
//            // Disable expensive effects for performance
//            ambientOcclusionOptions = ambientOcclusionOptions.apply {
//                enabled = false
//            }
//
//            bloomOptions = bloomOptions.apply {
//                enabled = false
//            }
//
//            screenSpaceReflectionsOptions = screenSpaceReflectionsOptions.apply {
//                enabled = false
//            }
//
//            temporalAntiAliasingOptions = temporalAntiAliasingOptions.apply {
//                enabled = false
//            }
//
//            fogOptions = fogOptions.apply {
//                enabled = false
//            }
//
//            depthOfFieldOptions = depthOfFieldOptions.apply {
//                enabled = false
//            }
//
//            vignetteOptions = vignetteOptions.apply {
//                enabled = false
//            }
//        }
//    }

    private fun loadModel() {
        try {
            val buffer = if (customModelData != null && customModelPath != null) {
                // Load custom model from model browser selection
                loadCustomModelBuffer()
            } else {
                // Load default model using modelIndex
                loadDefaultModelBuffer()
            }

            buffer?.let { modelBuffer ->
                modelViewer?.apply {
                    loadModelGlb(modelBuffer)
                    transformToUnitCube()

                    // Clear and setup scene
                    val scene = this.scene
                    val asset = this.asset

                    asset?.let {
                        // Remove all entities first
                        it.entities.forEach { entity -> scene.removeEntity(entity) }

                        // Add back the main entity
                        if (it.entities.isNotEmpty()) {
                            scene.addEntity(it.entities[0])
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val modelName = customModelData?.name ?: "Model ${modelIndex + 1}"
            android.util.Log.e("Container3D", "Failed to load model: $modelName", e)
        }
    }

    private fun loadCustomModelBuffer(): ByteBuffer? {
        return try {
            val modelFileName = customModelData?.filename ?: return null

            // Try different locations for the model file
            val buffer = try {
                // Try assets first
                context.assets.open("models/$modelFileName").use { input ->
                    createBufferFromStream(input)
                }
            } catch (e: Exception) {
                try {
                    // Try external storage
                    val externalFile = java.io.File(context.getExternalFilesDir(null), "models/$modelFileName")
                    if (externalFile.exists()) {
                        externalFile.inputStream().use { input ->
                            createBufferFromStream(input)
                        }
                    } else {
                        // Try internal storage
                        val internalFile = java.io.File(context.filesDir, "models/$modelFileName")
                        internalFile.inputStream().use { input ->
                            createBufferFromStream(input)
                        }
                    }
                } catch (e2: Exception) {
                    android.util.Log.e("Container3D", "Custom model not found in any location: $modelFileName", e2)
                    null
                }
            }

            buffer
        } catch (e: Exception) {
            android.util.Log.e("Container3D", "Error loading custom model: ${customModelData?.filename}", e)
            null
        }
    }

    private fun loadDefaultModelBuffer(): ByteBuffer? {
        return try {
            val modelFile = modelFiles[modelIndex % modelFiles.size]
            context.assets.open(modelFile).use { input ->
                createBufferFromStream(input)
            }
        } catch (e: Exception) {
            android.util.Log.e("Container3D", "Error loading default model", e)
            null
        }
    }

    private fun createBufferFromStream(inputStream: java.io.InputStream): ByteBuffer {
        val bytes = ByteArray(inputStream.available())
        inputStream.read(bytes)
        return ByteBuffer.allocateDirect(bytes.size).apply {
            order(ByteOrder.nativeOrder())
            put(bytes)
            rewind()
        }
    }

    private fun createIndirectLight() {
        try {
            val ibl = "default_env_ibl.ktx"
//            val skybox = "default_env_skybox.ktx"
            val buffer = readCompressedAsset(ibl)
//            val skyBuffer = readCompressedAsset(skybox)

            modelViewer?.let { viewer ->
                val indirectLight = KTX1Loader.createIndirectLight(viewer.engine, buffer)
                indirectLight.intensity = 30_000.0f

                viewer.scene.indirectLight = indirectLight
                viewerContent.indirectLight = indirectLight
            }

//            modelViewer?.let { viewer ->
//                val skybox = KTX1Loader.createSkybox(viewer.engine, skyBuffer)
//                viewer.scene.skybox = skybox
//            }
        } catch (e: Exception) {
            android.util.Log.e("Container3D", "Failed to create indirect light", e)
        }
    }

    private fun readCompressedAsset(assetName: String): ByteBuffer {
        val input = context.assets.open(assetName)
        val bytes = ByteArray(input.available())
        input.read(bytes)
        return ByteBuffer.wrap(bytes)
    }

    private fun startRendering() {
        if (!isRenderingActive) {
            choreographer = Choreographer.getInstance()
            frameCallback = FrameCallback()
            choreographer?.postFrameCallback(frameCallback)
            isRenderingActive = true
        }
    }

    private fun stopRendering() {
        frameCallback?.let { callback ->
            choreographer?.removeFrameCallback(callback)
        }
        frameCallback = null
        isRenderingActive = false
    }

    private inner class FrameCallback : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isInitialized && isRenderingActive) {
                choreographer?.postFrameCallback(this)
                modelViewer?.render(frameTimeNanos)
                if (!isAnimationPaused) {
                    handleAnimation(frameTimeNanos)
                }
            }
        }

        private fun handleAnimation(frameTimeNanos: Long) {
            modelViewer?.animator?.apply {
                if (animationCount > 0) {
                    val elapsedTimeSeconds = (frameTimeNanos - animationStartTime).toDouble() / 1000000000

                    if (currentAnimationDuration == 0f) {
                        currentAnimationDuration = getAnimationDuration(currentAnimationIndex)
                    }

                    if (elapsedTimeSeconds >= currentAnimationDuration) {
                        // Move to next animation
                        currentAnimationIndex = (currentAnimationIndex + 1) % animationCount
                        animationStartTime = frameTimeNanos
                        currentAnimationDuration = getAnimationDuration(currentAnimationIndex)
                        applyAnimation(currentAnimationIndex, 0f)
                    } else {
                        applyAnimation(currentAnimationIndex, elapsedTimeSeconds.toFloat())
                    }

                    updateBoneMatrices()
                }
            }
        }
    }

    private fun createFallbackView(): android.view.View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            setBackgroundColor(Color.parseColor("#263238"))

            addView(TextView(context).apply {
                text = "🎲"
                textSize = 48f
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    0,
                    1f
                )
            })

            addView(TextView(context).apply {
                text = "3D Model Container\n(Model ${modelIndex + 1})\nFilament not available"
                textSize = 12f
                gravity = android.view.Gravity.CENTER
                setTextColor(Color.LTGRAY)
                setPadding(0, dpToPx(8), 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            })
        }
    }

    private fun showModelInfo() {
        if(touchEnabled){
            this.setTouchEnabled(false)
            this.setPassThroughTouches(true)
        }else {
            this.setTouchEnabled(true)
            this.setPassThroughTouches(false)
        }


//        AlertDialog.Builder(context)
//            .setTitle("3D Model Information")
//            .setMessage(info)
//            .setPositiveButton("OK", null)
//            .show()
    }

    private fun toggleAnimation() {
        val animator = modelViewer?.animator
        if (animator == null || animator.animationCount == 0) {
            android.widget.Toast.makeText(context, "No animations available", android.widget.Toast.LENGTH_SHORT).show()
            return
        }

        isAnimationPaused = !isAnimationPaused

        if (isAnimationPaused) {
            // Animation is now paused
            pausedAnimationTime = ((System.nanoTime() - animationStartTime).toDouble() / 1000000000).toFloat()
            animationToggleButton?.setImageResource(android.R.drawable.ic_media_play)
            android.widget.Toast.makeText(context, "Animation paused", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            // Animation is now playing
            animationStartTime = System.nanoTime() - (pausedAnimationTime * 1000000000).toLong()
            animationToggleButton?.setImageResource(android.R.drawable.ic_media_pause)
            android.widget.Toast.makeText(context, "Animation playing", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun show3DMenu() {
        toggleRendering()
    }

    private fun resetView() {
        modelViewer?.transformToUnitCube()
        android.widget.Toast.makeText(context, "View reset", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showPerformanceStats() {
        val fps = if (isRenderingActive) "~60 FPS (optimized)" else "Paused"
        android.widget.Toast.makeText(context, "Performance: $fps", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showModelDetails() {
        val modelFile = modelFiles[modelIndex % modelFiles.size]
        val details = """
            Model File: $modelFile
            Current Animation: ${getCurrentAnimationInfo()}
            Rendering: ${if (isRenderingActive) "Active" else "Paused"}
            Animation: ${if (isAnimationPaused) "Paused" else "Playing"}
        """.trimIndent()

        AlertDialog.Builder(context)
            .setTitle("Model Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun toggleRendering() {
        if (isRenderingActive) {
            stopRendering()
            android.widget.Toast.makeText(context, "Rendering paused", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            startRendering()
            android.widget.Toast.makeText(context, "Rendering resumed", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Public methods for external control
    fun switchToAnimation(animationIndex: Int) {
        modelViewer?.animator?.apply {
            if (animationIndex >= 0 && animationIndex < animationCount) {
                currentAnimationIndex = animationIndex
                animationStartTime = System.nanoTime()
                currentAnimationDuration = getAnimationDuration(animationIndex)
                pausedAnimationTime = 0f
                isAnimationPaused = false
                animationToggleButton?.setImageResource(android.R.drawable.ic_media_pause)
            }
        }
    }

    fun getCurrentAnimationInfo(): String {
        return modelViewer?.animator?.let { animator ->
            if (animator.animationCount > 0) {
                "${animator.getAnimationName(currentAnimationIndex)} (${currentAnimationIndex + 1}/${animator.animationCount})"
            } else {
                "No animations"
            }
        } ?: "No animator"
    }

    fun pauseRendering() {
        stopRendering()
    }

    fun resumeRendering() {
        startRendering()
    }

    fun pauseAnimation() {
        if (!isAnimationPaused) {
            toggleAnimation()
        }
    }

    fun playAnimation() {
        if (isAnimationPaused) {
            toggleAnimation()
        }
    }

    // Lifecycle management
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isInitialized && !isRenderingActive) {
            startRendering()
        }
        // Update button icon based on animation state
        animationToggleButton?.setImageResource(
            if (isAnimationPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        )
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopRendering()
        uiHelper?.detach()
    }

    // Override ContainerBase methods
    override fun getDefaultWidth(): Int = dpToPx(400) // Slightly wider to accommodate side controls
    override fun getDefaultHeight(): Int = dpToPx(350)

    override fun getCustomSaveData(): Map<String, Any> {
        val baseData = super.getCustomSaveData().toMutableMap()
        baseData.putAll(mapOf(
            "modelIndex" to modelIndex,
            "currentAnimationIndex" to currentAnimationIndex,
            "isRenderingActive" to isRenderingActive,
            "isAnimationPaused" to isAnimationPaused,
            "pausedAnimationTime" to pausedAnimationTime
        ))
        return baseData
    }

    override fun loadCustomSaveData(data: Map<String, Any>) {
        super.loadCustomSaveData(data)

        data["currentAnimationIndex"]?.let {
            if (it is Int) currentAnimationIndex = it
        }
        data["isRenderingActive"]?.let {
            if (it is Boolean && it && !isRenderingActive) {
                startRendering()
            }
        }
        data["isAnimationPaused"]?.let {
            if (it is Boolean) isAnimationPaused = it
        }
        data["pausedAnimationTime"]?.let {
            if (it is Float) pausedAnimationTime = it
        }

        // Update button icon after loading state
        animationToggleButton?.setImageResource(
            if (isAnimationPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause
        )
    }
}