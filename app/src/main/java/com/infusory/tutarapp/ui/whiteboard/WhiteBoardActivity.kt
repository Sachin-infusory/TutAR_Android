// WhiteboardActivity.kt - Updated with Camera Feed Integration
package com.infusory.tutarapp.ui.whiteboard

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import com.infusory.tutarapp.R
import com.infusory.tutarapp.ui.containers.Container3D
import com.infusory.tutarapp.ui.data.ModelData
import com.infusory.tutarapp.ui.models.ModelBrowserDrawer
import com.infusory.tutarapp.ui.annotation.AnnotationToolView
import com.infusory.tutarapp.ui.utils.containers.ContainerManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class WhiteboardActivity : AppCompatActivity() {

    private lateinit var surfaceView: android.view.SurfaceView
    private lateinit var mainLayout: android.widget.RelativeLayout
    private lateinit var containerManager: ContainerManager
    private var modelBrowserDrawer: ModelBrowserDrawer? = null

    // Annotation tool
    private var annotationTool: AnnotationToolView? = null

    // Camera components
    private var cameraPreviewView: PreviewView? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var camera: Camera? = null
    private lateinit var cameraExecutor: ExecutorService
    private var isCameraActive = false

    // Camera permission request code
    private val CAMERA_PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whiteboard)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        initViews()
        setupContainerManager()
        setupButtonListeners()
        setupModelBrowser()
        setupCameraPreview()

        Toast.makeText(this, "Welcome to TutAR Whiteboard with 3D!", Toast.LENGTH_LONG).show()
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.surface_view)
        mainLayout = findViewById(R.id.main)

        // Initialize annotation tool
        setupAnnotationTool()
    }

    private fun setupCameraPreview() {
        // Create PreviewView programmatically
        cameraPreviewView = PreviewView(this)
        val layoutParams = android.widget.RelativeLayout.LayoutParams(
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
        )
        cameraPreviewView?.layoutParams = layoutParams

        // Set elevation lower than surface view but higher than background
        cameraPreviewView?.elevation = 1f

        // Initially hide the camera preview
        cameraPreviewView?.visibility = android.view.View.GONE

        // Add to main layout
        mainLayout.addView(cameraPreviewView, 0) // Add at index 0 to be behind other views
    }

    private fun setupAnnotationTool() {
        annotationTool = AnnotationToolView(this)
        val layoutParams = android.widget.RelativeLayout.LayoutParams(
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
            android.widget.RelativeLayout.LayoutParams.MATCH_PARENT,
        )
        annotationTool?.layoutParams = layoutParams

        // Add annotation tool to main layout with high elevation to stay on top
        annotationTool?.elevation = 200f
        mainLayout.addView(annotationTool)

        // Set callback for annotation toggle
        annotationTool?.onAnnotationToggle = { isEnabled ->
            if (isEnabled) {
                Toast.makeText(this, "Annotation mode enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Annotation mode disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Connect drawing state changes to 3D rendering control
        annotationTool?.onDrawingStateChanged = { isDrawing ->
            if (isDrawing) {
                pauseAll3DRenderingForDrawing()
            } else {
                resumeAll3DRenderingAfterDrawing()
            }
        }
    }

    private fun setupContainerManager() {
        containerManager = ContainerManager(this, mainLayout, maxContainers = 8)

        // Set up callbacks
        containerManager.onContainerAdded = { container ->
            // Optional: Handle container added
        }

        containerManager.onContainerRemoved = { container ->
            // Handle 3D container cleanup if needed
            if (container is Container3D) {
                container.pauseRendering()
            }
        }

        containerManager.onContainerCountChanged = { count ->
            // Optional: Update UI based on container count
        }
    }

    private fun setupModelBrowser() {
        modelBrowserDrawer = ModelBrowserDrawer(this) { modelData, fullPath ->
            createCustom3DContainer(modelData, fullPath)
        }
    }

    private fun setupButtonListeners() {
        // FIRST LEFT BUTTON - Toggle annotation mode (btn_draw)
        findViewById<android.widget.ImageButton>(R.id.btn_draw).setOnClickListener {
            annotationTool?.toggleAnnotationMode()
        }

        // FIRST RIGHT BUTTON - Toggle annotation mode (btn_draw_rt)
        findViewById<android.widget.ImageButton>(R.id.btn_draw_rt).setOnClickListener {
            annotationTool?.toggleAnnotationMode()
        }

        // AR/Camera Button - Toggle camera feed (btn_ar)
        findViewById<android.widget.ImageButton>(R.id.btn_ar).setOnClickListener {
            toggleCameraFeed()
        }

        // AR/Camera Button - Toggle camera feed (btn_ar_rt)
        findViewById<android.widget.ImageButton>(R.id.btn_ar_rt).setOnClickListener {
            toggleCameraFeed()
        }


        // Left side button - Show add container menu (UPDATED to handle annotation mode)
        findViewById<android.widget.ImageButton>(R.id.btn_insert).setOnClickListener {
            if (annotationTool?.isInAnnotationMode() == true) {
                showAnnotationMenu()
            } else {
                showAddContainerMenu()
            }
        }

        // Right side button - Show model browser for 3D models
        findViewById<android.widget.ImageButton>(R.id.btn_insert_rt).setOnClickListener {
        showAddContainerMenu()
//            showModelBrowser()
        }

        // Menu buttons - Left menu shows model browser
        findViewById<android.widget.ImageButton>(R.id.btn_menu).setOnClickListener {
            showModelBrowser()
        }

        findViewById<android.widget.ImageButton>(R.id.btn_menu_rt).setOnClickListener {
//            showContainerManagementMenu()
            showModelBrowser()
        }
    }

    private fun toggleCameraFeed() {
        if (isCameraActive) {
            stopCamera()
        } else {
            if (checkCameraPermission()) {
                startCamera()
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                } else {
                    Toast.makeText(
                        this,
                        "Camera permission is required for AR features",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            try {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                cameraProvider = cameraProviderFuture.get()

                // Preview
                preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(cameraPreviewView?.surfaceProvider)
                }

                // Select back camera as a default
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    // Unbind use cases before rebinding
                    cameraProvider?.unbindAll()

                    // Bind use cases to camera
                    camera = cameraProvider?.bindToLifecycle(
                        this, cameraSelector, preview
                    )

                    // Show camera preview
                    cameraPreviewView?.visibility = android.view.View.VISIBLE
                    // Hide the surface view
                    surfaceView.visibility = android.view.View.GONE

                    isCameraActive = true
                    Toast.makeText(this, "Camera activated - AR mode enabled", Toast.LENGTH_SHORT).show()

                } catch (exc: Exception) {
                    Toast.makeText(this, "Failed to start camera: ${exc.message}", Toast.LENGTH_LONG).show()
                }

            } catch (exc: Exception) {
                Toast.makeText(this, "Camera initialization failed: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        try {
            // Unbind all use cases
            cameraProvider?.unbindAll()

            // Hide camera preview
            cameraPreviewView?.visibility = android.view.View.GONE
            // Show surface view again
            surfaceView.visibility = android.view.View.VISIBLE

            isCameraActive = false
            Toast.makeText(this, "Camera deactivated - Normal mode", Toast.LENGTH_SHORT).show()

        } catch (exc: Exception) {
            Toast.makeText(this, "Failed to stop camera: ${exc.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Show annotation controls menu
    private fun showAnnotationMenu() {
        val options = arrayOf(
            "Clear All Annotations",
            "Undo Last Annotation",
            "Exit Annotation Mode"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Annotation Controls")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> annotationTool?.clearAllAnnotations()
                    1 -> annotationTool?.undoLastAnnotation()
                    2 -> annotationTool?.toggleAnnotationMode(false)
                }
            }
            .show()
    }

    private fun showModelBrowser() {
        modelBrowserDrawer?.show()
    }

    private fun showAddContainerMenu() {
        val containerTypes = arrayOf(
            "Standard Container",
            "Text Container",
            "Image Container",
            "Minimal Container",
            "Read-Only Container",
            "3D Model (Browse Library)"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Add Container")
            .setItems(containerTypes) { _, which ->
                when (which) {
                    0 -> containerManager.addStandardContainer()
                    1 -> containerManager.addTextContainer()
                    2 -> containerManager.addImageContainer()
                    3 -> containerManager.addMinimalContainer()
                    4 -> containerManager.addReadOnlyContainer()
                    5 -> showModelBrowser()
                }
            }
            .show()
    }

    private fun createCustom3DContainer(modelData: ModelData, fullPath: String) {
        if (containerManager.getContainerCount() >= 8) {
            Toast.makeText(this, "Maximum containers reached", Toast.LENGTH_SHORT).show()
            return
        }

        // Create Container3D with default constructor, then set model data
        val container3D = Container3D(this)

        // Set the model data after creation
        container3D.setModelData(modelData, fullPath)

        // Set layout params
        val layoutParams = android.widget.RelativeLayout.LayoutParams(
            container3D.getDefaultWidth(),
            container3D.getDefaultHeight()
        )
        container3D.layoutParams = layoutParams

        // Position with offset based on existing container count
        val offsetX = containerManager.getContainerCount() * 60f
        val offsetY = containerManager.getContainerCount() * 60f + 100f
        container3D.moveContainerTo(offsetX, offsetY, animate = false)

        // Set removal callback
        container3D.onRemoveRequest = {
            containerManager.removeContainer(container3D)
        }

        // Add to layout and initialize
        mainLayout.addView(container3D)
        container3D.initializeContent()

        Toast.makeText(this, "3D Model loaded: ${modelData.name}", Toast.LENGTH_LONG).show()
    }

    fun pauseAll3DRenderingForDrawing() {
        // Get 3D containers from containerManager (managed containers)
        val managedContainer3Ds = containerManager.getAllContainers().filterIsInstance<Container3D>()

        // Also get 3D containers directly from mainLayout (direct containers)
        val directContainer3Ds = mutableListOf<Container3D>()
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is Container3D) {
                directContainer3Ds.add(child)
            }
        }

        // Combine both lists and remove duplicates
        val allContainer3Ds = (managedContainer3Ds + directContainer3Ds).distinct()

        android.util.Log.d("DEBUG", "Pausing ${allContainer3Ds.size} 3D containers")

        allContainer3Ds.forEach { container ->
            container.pauseRendering()
        }
    }

    fun resumeAll3DRenderingAfterDrawing() {
        // Same logic for resume
        val managedContainer3Ds = containerManager.getAllContainers().filterIsInstance<Container3D>()

        val directContainer3Ds = mutableListOf<Container3D>()
        for (i in 0 until mainLayout.childCount) {
            val child = mainLayout.getChildAt(i)
            if (child is Container3D) {
                directContainer3Ds.add(child)
            }
        }

        val allContainer3Ds = (managedContainer3Ds + directContainer3Ds).distinct()

        android.util.Log.d("DEBUG", "Resuming ${allContainer3Ds.size} 3D containers")

        allContainer3Ds.forEach { container ->
            container.resumeRendering()
        }
    }

    private fun showContainerManagementMenu() {
        val options = arrayOf(
            "Reset All Positions",
            "Zoom All to 1x",
            "Zoom All to 2x",
            "Arrange in Grid",
            "Toggle Dragging",
            "Toggle Resizing",
            "Pause All 3D Rendering",
            "Resume All 3D Rendering",
            "Container Statistics",
            "Clear All Containers",
            if (isCameraActive) "Stop Camera" else "Start Camera"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Container Management")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> containerManager.resetAllContainers()
                    1 -> containerManager.zoomAllContainers(1.0f)
                    2 -> containerManager.zoomAllContainers(2.0f)
                    3 -> containerManager.arrangeContainersInGrid()
                    4 -> containerManager.toggleDraggingForAllContainers()
                    5 -> containerManager.toggleResizingForAllContainers()
                    6 -> pauseAll3DRenderingForDrawing()
                    7 -> resumeAll3DRenderingAfterDrawing()
                    8 -> showContainerStatistics()
                    9 -> containerManager.clearAllContainers()
                    10 -> toggleCameraFeed()
                }
            }
            .show()
    }

    private fun showContainerStatistics() {
        val allContainers = containerManager.getAllContainers()
        val container3Ds = allContainers.filterIsInstance<Container3D>()

        val stats = """
            Total Containers: ${allContainers.size}
            Regular Containers: ${allContainers.size - container3Ds.size}
            3D Containers: ${container3Ds.size}
            Camera Status: ${if (isCameraActive) "Active" else "Inactive"}
            
            3D Models:
            ${
            container3Ds.mapIndexed { index, container ->
                "  ${index + 1}. ${container.getCurrentAnimationInfo()}"
            }.joinToString("\n")
        }
        """.trimIndent()

        android.app.AlertDialog.Builder(this)
            .setTitle("Container Statistics")
            .setMessage(stats)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun saveWhiteboardState() {
        val stateData = containerManager.saveState()

        // Save to SharedPreferences
        val sharedPrefs = getSharedPreferences("whiteboard_state", MODE_PRIVATE)
        val editor = sharedPrefs.edit()

        // Simple save - you might want to use JSON for complex data
        editor.putInt("container_count", stateData.containers.size)
        editor.putBoolean("camera_active", isCameraActive)

        stateData.containers.forEachIndexed { index, containerState ->
            editor.putString("container_${index}_type", containerState.type.name)
            editor.putFloat("container_${index}_x", containerState.position.first)
            editor.putFloat("container_${index}_y", containerState.position.second)
            editor.putFloat("container_${index}_scale", containerState.scale)
            editor.putInt("container_${index}_width", containerState.size.first)
            editor.putInt("container_${index}_height", containerState.size.second)

            // Save custom data as JSON string or individual preferences
            containerState.customData.forEach { (key, value) ->
                when (value) {
                    is String -> editor.putString("container_${index}_$key", value)
                    is Int -> editor.putInt("container_${index}_$key", value)
                    is Float -> editor.putFloat("container_${index}_$key", value)
                    is Boolean -> editor.putBoolean("container_${index}_$key", value)
                }
            }
        }

        editor.apply()
    }

    private fun loadWhiteboardState() {
        val sharedPrefs = getSharedPreferences("whiteboard_state", MODE_PRIVATE)
        val containerCount = sharedPrefs.getInt("container_count", 0)

        if (containerCount > 0) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Restore Previous Session?")
                .setMessage("Found $containerCount saved containers. Would you like to restore them?")
                .setPositiveButton("Restore") { _, _ ->
                    // You would need to implement proper state restoration
                    // This is a simplified version
                    Toast.makeText(
                        this,
                        "State restoration not fully implemented",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .setNegativeButton("Start Fresh") { _, _ ->
                    sharedPrefs.edit().clear().apply()
                }
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        // Pause all 3D rendering to save battery
        pauseAll3DRenderingForDrawing()
        // Optionally pause camera when app is not in foreground
        if (isCameraActive) {
            stopCamera()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume 3D rendering
        resumeAll3DRenderingAfterDrawing()
        // Note: Camera will need to be manually restarted by user
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure cleanup
        stopCamera()
        cameraExecutor.shutdown()
        pauseAll3DRenderingForDrawing()
        modelBrowserDrawer?.dismiss()
    }

    // Handle annotation mode and camera in back press
    override fun onBackPressed() {
        // First check if camera is active
        if (isCameraActive) {
            stopCamera()
            return
        }

        // Then check if annotation mode is active
        if (annotationTool?.isInAnnotationMode() == true) {
            annotationTool?.toggleAnnotationMode(false)
            return
        }

        // Then handle normal whiteboard exit logic
        if (containerManager.getContainerCount() > 0) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Save Whiteboard?")
                .setMessage("You have ${containerManager.getContainerCount()} container(s). Save before leaving?")
                .setPositiveButton("Save & Exit") { _, _ ->
                    saveWhiteboardState()
                    Toast.makeText(this, "Whiteboard saved", Toast.LENGTH_SHORT).show()
                    super.onBackPressed()
                }
                .setNegativeButton("Exit Without Saving") { _, _ ->
                    super.onBackPressed()
                }
                .setNeutralButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        } else {
            super.onBackPressed()
        }
    }
}