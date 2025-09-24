// WhiteboardActivity.kt - Updated with Model Browser Integration
package com.infusory.tutarapp.ui.whiteboard

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.infusory.tutarapp.R
import com.infusory.tutarapp.ui.containers.Container3D
import com.infusory.tutarapp.ui.data.ModelData
import com.infusory.tutarapp.ui.models.ModelBrowserDrawer
import com.infusory.tutarapp.ui.utils.containers.ContainerManager

class WhiteboardActivity : AppCompatActivity() {

    private lateinit var surfaceView: android.view.SurfaceView
    private lateinit var mainLayout: android.widget.RelativeLayout
    private lateinit var containerManager: ContainerManager
    private var modelBrowserDrawer: ModelBrowserDrawer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whiteboard)

        initViews()
        setupContainerManager()
        setupButtonListeners()
        setupModelBrowser()

        Toast.makeText(this, "Welcome to TutAR Whiteboard with 3D!", Toast.LENGTH_LONG).show()
    }

    private fun initViews() {
        surfaceView = findViewById(R.id.surface_view)
        mainLayout = findViewById(R.id.main)
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
        // Left side button - Show add container menu
        findViewById<android.widget.ImageButton>(R.id.btn_add).setOnClickListener {
            showAddContainerMenu()
        }

        // Right side button - Add 3D container directly
        findViewById<android.widget.ImageButton>(R.id.btn_add_rt).setOnClickListener {
            show3DModelSelectionDialog()
        }

        // Menu buttons - UPDATED: Left menu now shows model browser
        findViewById<android.widget.ImageButton>(R.id.btn_menu).setOnClickListener {
            showModelBrowser()
        }

        findViewById<android.widget.ImageButton>(R.id.btn_menu_rt).setOnClickListener {
            showContainerManagementMenu()
        }
    }

    private fun showModelBrowser() {
        modelBrowserDrawer?.show()
    }

    private fun showAddContainerMenu() {
        val containerTypes = arrayOf(
            "Standard Container",
            "Text Container",
            "3D Model Container",
            "3D Model from Library",
            "Image Container",
            "Minimal Container",
            "Read-Only Container"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Add Container")
            .setItems(containerTypes) { _, which ->
                when (which) {
                    0 -> containerManager.addStandardContainer()
                    1 -> containerManager.addTextContainer()
                    2 -> show3DModelSelectionDialog()
                    3 -> showModelBrowser()
                    4 -> containerManager.addImageContainer()
                    5 -> containerManager.addMinimalContainer()
                    6 -> containerManager.addReadOnlyContainer()
                }
            }
            .show()
    }

    private fun show3DModelSelectionDialog() {
        val modelNames = arrayOf(
            "Model 1: Eagle",
            "Model 2: Skeleton",
            "Model 3: Alternative",
            "Model 4: Custom"
        )

        android.app.AlertDialog.Builder(this)
            .setTitle("Select 3D Model")
            .setItems(modelNames) { _, which ->
                create3DContainer(which)
            }
            .show()
    }

    private fun create3DContainer(modelIndex: Int) {
        if (containerManager.getContainerCount() >= 8) {
            Toast.makeText(this, "Maximum containers reached", Toast.LENGTH_SHORT).show()
            return
        }

        // Create Container3D with specific model index
        val container3D = Container3D(this, modelIndex)

        // Set layout params
        val layoutParams = android.widget.RelativeLayout.LayoutParams(
            container3D.getDefaultWidth(),
            container3D.getDefaultHeight()
        )
        container3D.layoutParams = layoutParams

        // Position with offset
        val offsetX = containerManager.getContainerCount() * 60f
        val offsetY = containerManager.getContainerCount() * 60f + 100f
        container3D.moveContainerTo(offsetX, offsetY, animate = false)

        // Set removal callback
        container3D.onRemoveRequest = {
            containerManager.removeContainer(container3D)
        }

        // Add to layout manually (since ContainerManager doesn't handle custom 3D creation)
        mainLayout.addView(container3D)

        // Initialize content (this calls initializeContent() internally)
        container3D.initializeContent()

        Toast.makeText(this, "3D Container added (Model ${modelIndex + 1})", Toast.LENGTH_SHORT)
            .show()
    }

    private fun createCustom3DContainer(modelData: ModelData, fullPath: String) {
        if (containerManager.getContainerCount() >= 8) {
            Toast.makeText(this, "Maximum containers reached", Toast.LENGTH_SHORT).show()
            return
        }

        // For now, create a basic 3D container with model index 0
        // In a full implementation, you'd modify Container3D to accept custom model files
        val container3D =
            Container3D(this, 0) // You'll need to modify Container3D to handle custom models

        // Set layout params
        val layoutParams = android.widget.RelativeLayout.LayoutParams(
            container3D.getDefaultWidth(),
            container3D.getDefaultHeight()
        )
        container3D.layoutParams = layoutParams

        // Position with offset
        val offsetX = containerManager.getContainerCount() * 60f
        val offsetY = containerManager.getContainerCount() * 60f + 100f
        container3D.moveContainerTo(offsetX, offsetY, animate = false)

        // Set removal callback
        container3D.onRemoveRequest = {
            containerManager.removeContainer(container3D)
        }

        // Add to layout
        mainLayout.addView(container3D)
        container3D.initializeContent()

        Toast.makeText(this, "3D Model loaded: ${modelData.name}", Toast.LENGTH_LONG).show()
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
            "Clear All Containers"
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
                    6 -> pauseAll3DRendering()
                    7 -> resumeAll3DRendering()
                    8 -> showContainerStatistics()
                    9 -> containerManager.clearAllContainers()
                }
            }
            .show()
    }

    private fun pauseAll3DRendering() {
        containerManager.getAllContainers().forEach { container ->
            if (container is Container3D) {
                container.pauseRendering()
            }
        }
        Toast.makeText(this, "All 3D rendering paused", Toast.LENGTH_SHORT).show()
    }

    private fun resumeAll3DRendering() {
        containerManager.getAllContainers().forEach { container ->
            if (container is Container3D) {
                container.resumeRendering()
            }
        }
        Toast.makeText(this, "All 3D rendering resumed", Toast.LENGTH_SHORT).show()
    }

    private fun showContainerStatistics() {
        val allContainers = containerManager.getAllContainers()
        val container3Ds = allContainers.filterIsInstance<Container3D>()

        val stats = """
            Total Containers: ${allContainers.size}
            Regular Containers: ${allContainers.size - container3Ds.size}
            3D Containers: ${container3Ds.size}
            
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
        pauseAll3DRendering()
    }

    override fun onResume() {
        super.onResume()
        // Resume 3D rendering
        resumeAll3DRendering()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Ensure cleanup
        pauseAll3DRendering()
        modelBrowserDrawer?.dismiss()
    }

    override fun onBackPressed() {
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