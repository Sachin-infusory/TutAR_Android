// WhiteboardActivity.kt - Updated with AI Master Integration
package com.infusory.tutarapp.ui.whiteboard
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
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
import com.infusory.tutarapp.ui.ai.AiMasterDrawer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.widget.ImageButton
import android.view.View
import android.widget.Button
import android.widget.PopupWindow
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RelativeLayout
import com.infusory.tutarapp.ui.containers.ContainerImage

enum class ActionType {
    SAVE, INSERT
}
enum class ImageMode {
    BACKGROUND,
    INSERT
}


class WhiteboardActivity : AppCompatActivity() {

    private lateinit var surfaceView: android.view.SurfaceView
    private lateinit var mainLayout: android.widget.RelativeLayout
    private lateinit var containerManager: ContainerManager
    private var modelBrowserDrawer: ModelBrowserDrawer? = null
    private var aiMasterDrawer: AiMasterDrawer? = null

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
    private val BACKGROUND_PICK_CODE = 101
    private val CONTAINER_IMAGE_PICK_CODE = 102


    // Active buttons
    private lateinit var allButtons: List<ImageButton>
    private val pairedButtonsState = mutableMapOf<Int, Boolean>()

    private val IMAGE_PICK_CODE = 101

    // more button functionalities
    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK).apply {
            type = "image/*"
        }
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK) {
//            val imageUri = data?.data ?: return
//            try {
//                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
//                val drawable = BitmapDrawable(resources, bitmap)
//                surfaceView.background = drawable
//            } catch (e: Exception) {
//                Toast.makeText(this, "Failed to set background: ${e.message}", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode != RESULT_OK || data?.data == null) return
        val imageUri = data.data!!

        when (requestCode) {
            IMAGE_PICK_CODE -> {
                // For background
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    val drawable = BitmapDrawable(resources, bitmap)
                    surfaceView.background = drawable
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to set background: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            CONTAINER_IMAGE_PICK_CODE -> {
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)
                    // Get the inserted ContainerImage
                    val container = mainLayout.findViewWithTag<View>("lesson_image") as? ContainerImage
                    container?.setImage(bitmap, imageUri.toString())
                } catch (e: Exception) {
                    Toast.makeText(this, "Failed to load image into container: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    override
    fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_whiteboard)

        // Initialize camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        initViews()
        setupContainerManager()
        setupButtonListeners()
        setupModelBrowser()
        setupAiMaster()
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

    fun pickBackgroundImage() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, BACKGROUND_PICK_CODE)
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

    // bgColor selector popup container
    private fun showColorPopup(anchor: ImageButton) {
        Log.i("colorPopup", "colorPopupAnchor: ${anchor.tag}")
        val inflater = layoutInflater
        val colorView = inflater.inflate(R.layout.dialog_color_picker, null)

        val popup = PopupWindow(
            colorView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 20f
            isOutsideTouchable = true
        }

        // Bind buttons
        val white = colorView.findViewById<RadioButton>(R.id.colorWhite)
        val green = colorView.findViewById<RadioButton>(R.id.colorGreen)
        val black = colorView.findViewById<RadioButton>(R.id.colorBlack)
        val split = colorView.findViewById<RadioButton>(R.id.colorSplit)
        val pickImage = colorView.findViewById<ImageButton>(R.id.btnPickImage)

        // ✅ Set background color actions
        white.setOnClickListener {
            surfaceView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
            popup.dismiss()
        }
        green.setOnClickListener {
            surfaceView.setBackgroundColor(
                ContextCompat.getColor(
                    this,
                    android.R.color.holo_green_light
                )
            )
            popup.dismiss()
        }
        black.setOnClickListener {
            surfaceView.setBackgroundColor(ContextCompat.getColor(this, android.R.color.black))
            popup.dismiss()
        }
//        split.setOnClickListener {
//            surfaceView.background = ContextCompat.getDrawable(this, R.drawable.bg_color_split_drawable)
//            popup.dismiss()
//        }

        // ✅ Image picker
        pickImage.setOnClickListener {
            popup.dismiss()
            openImagePicker()
        }

        // Position popup near the anchor
        colorView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = colorView.measuredWidth
        val popupHeight = colorView.measuredHeight

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val anchorX = location[0]
        val anchorY = location[1]
        val anchorWidth = anchor.width
        val anchorHeight = anchor.height

        var popupX = anchorX + anchorWidth + 30
        val popupY = anchorY + (anchorHeight - popupHeight) / 2

        if (popupX + popupWidth > resources.displayMetrics.widthPixels) {
            popupX = anchorX - popupWidth - 30
        }

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY)

        popup.setOnDismissListener {
            anchor.tag = false
            anchor.background =
                ContextCompat.getDrawable(this, R.drawable.circular_button_background)
        }
    }

    // save options(lesson or pdf save) popup container
    private fun showActionOptionsPopup(anchor: View, type: ActionType) {
        Log.i("ActionPopup", "Anchor: ${anchor.tag}, Type: $type")

        val inflater = layoutInflater

        // Choose layout dynamically
        val popupView = when (type) {
            ActionType.SAVE -> inflater.inflate(R.layout.dialog_save_options, null)
            ActionType.INSERT -> inflater.inflate(R.layout.dialog_insert_options, null)
        }

        // Bind buttons dynamically
        when (type) {
            ActionType.SAVE -> {
                popupView.findViewById<Button>(R.id.btnSaveLesson).setOnClickListener {
                    Toast.makeText(this, "Save Lesson clicked", Toast.LENGTH_SHORT).show()
                }
                popupView.findViewById<Button>(R.id.btnSavePdf).setOnClickListener {
                    Toast.makeText(this, "Save PDF clicked", Toast.LENGTH_SHORT).show()
                }
            }
            ActionType.INSERT -> {
                popupView.findViewById<Button>(R.id.btnInsertImage).setOnClickListener {
                    val parent = mainLayout  // Use already initialized mainLayout

                    // Check if a ContainerImage is already added by tag
                    val existing = parent.findViewWithTag<View>("lesson_image") as? ContainerImage
                    existing?.let { parent.removeView(it) }

                    // Create a new ContainerImage
                    val imageContainer = ContainerImage(this@WhiteboardActivity).apply {
                        tag = "lesson_image"  // Tag for later reference
                        layoutParams = RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.WRAP_CONTENT,
                            RelativeLayout.LayoutParams.WRAP_CONTENT
                        )
                        // Optional: set default placeholder image
                        setImageResource(R.drawable.tutar_logo)
                    }

                    // Add container to layout
                    parent.addView(imageContainer)
                    imageContainer.initializeContent()

                    // Launch image picker
                    val intent = Intent(Intent.ACTION_PICK).apply { }
                    startActivityForResult(intent, CONTAINER_IMAGE_PICK_CODE)

//                    popup.dismiss()
                }

                popupView.findViewById<Button>(R.id.btnInsertPdf).setOnClickListener {
                    Toast.makeText(this, "Insert PDF clicked", Toast.LENGTH_SHORT).show()
                }
                popupView.findViewById<Button>(R.id.btnInsertYoutube).setOnClickListener {
                    Toast.makeText(this, "Insert YouTube clicked", Toast.LENGTH_SHORT).show()
                }
                popupView.findViewById<Button>(R.id.btnInsertWebsite).setOnClickListener {
                    Toast.makeText(this, "Insert Website clicked", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Create popup
        val popup = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            elevation = 20f
            isOutsideTouchable = true
        }

        // Measure & position
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
        val popupWidth = popupView.measuredWidth
        val popupHeight = popupView.measuredHeight

        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        var popupX = location[0] + anchor.width + 30
        var popupY = location[1]

        if (popupX + popupWidth > resources.displayMetrics.widthPixels) {
            popupX = location[0] - popupWidth - 30
        }
        if (popupY + popupHeight > resources.displayMetrics.heightPixels) {
            popupY = resources.displayMetrics.heightPixels - popupHeight - 10
        }
        if (popupY < 0) popupY = 10

        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, popupX, popupY)

        popup.setOnDismissListener {
            anchor.background = ContextCompat.getDrawable(this, R.drawable.circular_button_background)
        }
    }

    private fun setupModelBrowser() {
        modelBrowserDrawer = ModelBrowserDrawer(this) { modelData, fullPath ->
            createCustom3DContainer(modelData, fullPath)
        }
    }

    private fun setupAiMaster() {
        aiMasterDrawer = AiMasterDrawer(this) { response ->
            handleAiMasterResponse(response)
        }
    }

    private fun setClickListener(vararg ids: Int, onClick: (Boolean) -> Unit) {
        // Buttons handled individually
        val individualButtons = listOf(
            R.id.color_plate, R.id.color_plate_rt,
            R.id.btn_save, R.id.btn_save_rt,
            R.id.btn_insert, R.id.btn_insert_rt,
            R.id.btn_more, R.id.btn_more_rt
        )

        ids.forEach { id ->
            val btn = findViewById<ImageButton>(id) ?: return@forEach

            btn.setOnClickListener {
                if (individualButtons.contains(id)) {
                    // Toggle only this button
                    val isActive = btn.tag as? Boolean ?: false
                    val newState = !isActive
                    btn.tag = newState

                    if (newState) {
                        // Active state: set blue background
                        btn.background =
                            ContextCompat.getDrawable(this, R.drawable.circular_button_background)
                                ?.apply {
                                    setTint(
                                        ContextCompat.getColor(
                                            this@WhiteboardActivity,
                                            android.R.color.holo_blue_light
                                        )
                                    )
                                }
                    } else {
                        // Inactive: reset to default drawable
                        btn.background =
                            ContextCompat.getDrawable(this, R.drawable.circular_button_background)
                    }

                    onClick(newState)

                } else {
                    // Paired buttons logic
                    val pairedId = when (id) {
                        R.id.btn_draw -> R.id.btn_draw_rt
                        R.id.btn_draw_rt -> R.id.btn_draw
                        R.id.btn_ar -> R.id.btn_ar_rt
                        R.id.btn_ar_rt -> R.id.btn_ar
                        R.id.btn_menu -> R.id.btn_menu_rt
                        R.id.btn_menu_rt -> R.id.btn_menu
                        R.id.btn_settings -> R.id.btn_setting_rt
                        R.id.btn_settings_rt -> R.id.btn_setting
                        else -> null
                    }

                    val currentState = pairedButtonsState[pairedId ?: id] ?: false
                    val newState = !currentState

                    pairedButtonsState[id] = newState
                    pairedId?.let { pairedButtonsState[it] = newState }

                    val drawable = if (newState) {
                        ContextCompat.getDrawable(this, R.drawable.circular_button_background)
                            ?.apply {
                                setTint(
                                    ContextCompat.getColor(
                                        this@WhiteboardActivity,
                                        android.R.color.holo_blue_light
                                    )
                                )
                            }
                    } else {
                        ContextCompat.getDrawable(this, R.drawable.circular_button_background)
                    }

                    btn.background = drawable
                    pairedId?.let { findViewById<ImageButton>(it)?.background = drawable }

                    onClick(newState)
                }
            }
        }
    }

    // Setup all buttons with proper toggle behavior
    private fun setupButtonListeners() {
        allButtons = listOfNotNull(
            findViewById(R.id.btn_draw),
            findViewById(R.id.btn_draw_rt),
            findViewById(R.id.btn_ar),
            findViewById(R.id.btn_ar_rt),
            findViewById(R.id.color_plate),
            findViewById(R.id.color_plate_rt),
            findViewById(R.id.btn_load_lesson),
            findViewById(R.id.btn_load_lesson_rt),
            findViewById(R.id.btn_menu),
            findViewById(R.id.btn_menu_rt),
            findViewById(R.id.btn_save),
            findViewById(R.id.btn_save_rt),
            findViewById(R.id.btn_insert),
            findViewById(R.id.btn_insert_rt),
            findViewById(R.id.btn_setting),
            findViewById(R.id.btn_setting_rt),
            findViewById(R.id.btn_more),
            findViewById(R.id.btn_more_rt),
            findViewById(R.id.ai_master_btn),
        )

        // For Draw buttons (left + right)
        setClickListener(R.id.btn_draw, R.id.btn_draw_rt) { isActive ->
            if (isActive) annotationTool?.toggleAnnotationMode()
            else annotationTool?.toggleAnnotationMode(false)
        }

        // For AR buttons
        setClickListener(R.id.btn_ar, R.id.btn_ar_rt) { isActive ->
            if (isActive) toggleCameraFeed()
            else toggleCameraFeed()
        }

        // For color picker buttons
        setClickListener(R.id.color_plate) { isActive ->
            showColorPopup(findViewById(R.id.color_plate) as ImageButton)
        }
        setClickListener(R.id.color_plate_rt) { isActive ->
            showColorPopup(findViewById(R.id.color_plate_rt) as ImageButton)
        }

        // For Load(lesson or pdf) buttons
        setClickListener(R.id.btn_load_lesson) { isActive ->

        }

        setClickListener(R.id.btn_load_lesson_rt) { isActive ->

        }


        // Menu(3d models) buttons
        setClickListener(R.id.btn_menu, R.id.btn_menu_rt) { isActive ->
            if (isActive) {
                showModelBrowser()
                annotationTool?.toggleAnnotationMode(false)

                // 🔹 Force deactivate Draw buttons when Model Browser opens
                val drawButtons = listOf(
                    findViewById<ImageButton>(R.id.btn_draw),
                    findViewById<ImageButton>(R.id.btn_draw_rt)
                )

                drawButtons.forEach { btn ->
                    btn?.tag = false
                    btn?.background = ContextCompat.getDrawable(
                        this,
                        R.drawable.circular_button_background
                    )
                }

                // Update pairedButtonsState so it stays in sync
                pairedButtonsState[R.id.btn_draw] = false
                pairedButtonsState[R.id.btn_draw_rt] = false

            } else {
                showModelBrowser()
            }
        }

        // For Save Button
        setClickListener(R.id.btn_save) { isActive ->
            if (isActive) showActionOptionsPopup(findViewById(R.id.btn_save), ActionType.SAVE)
        }
        setClickListener(R.id.btn_save_rt) { isActive ->
            if (isActive) showActionOptionsPopup(findViewById(R.id.btn_save_rt), ActionType.SAVE)
        }

        // For Insert Button
        setClickListener(R.id.btn_insert) { isActive ->
            if (isActive) {
                showActionOptionsPopup(findViewById(R.id.btn_insert), ActionType.INSERT)
            }
        }
        setClickListener(R.id.btn_insert_rt) { isActive ->
            if (isActive) showActionOptionsPopup(findViewById(R.id.btn_insert_rt), ActionType.INSERT)
        }


        // For Settings Button
        setClickListener(R.id.btn_setting, R.id.btn_setting_rt) { isActive ->
        }

        // For More Button
        setClickListener(R.id.btn_more) { isActive ->
        }
        setClickListener(R.id.btn_more_rt) { isActive ->
        }

        // AI Master
        setClickListener(R.id.ai_master_btn) { isActive ->
            if (isActive) showAiMaster()
            else showAiMaster()
        }
    }

    private fun showAiMaster() {
        aiMasterDrawer?.show()
    }

    private fun handleAiMasterResponse(response: String) {
        // For now, just log the response and show a toast
        // We'll implement proper handling after seeing the API response format
        android.util.Log.d("AiMasterResponse", response)

        try {
            // Try to parse basic info from response
            if (response.isNotEmpty()) {
                // Show response in a simple dialog for now
                android.app.AlertDialog.Builder(this)
                    .setTitle("AI Master Response")
                    .setMessage("Response received! Check logs for details.\n\nResponse length: ${response.length} characters")
                    .setPositiveButton("OK") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setNeutralButton("View Raw") { dialog, _ ->
                        showRawResponseDialog(response)
                        dialog.dismiss()
                    }
                    .show()
            } else {
                Toast.makeText(this, "Empty response received", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("WhiteboardActivity", "Error handling AI response", e)
            Toast.makeText(this, "Error processing AI response: ${e.message}", Toast.LENGTH_LONG)
                .show()
        }
    }

    private fun showRawResponseDialog(response: String) {
        val scrollView = android.widget.ScrollView(this)
        val textView = android.widget.TextView(this).apply {
            text = response
            setPadding(16, 16, 16, 16)
            textSize = 12f
            setTextIsSelectable(true)
        }
        scrollView.addView(textView)

        android.app.AlertDialog.Builder(this)
            .setTitle("Raw API Response")
            .setView(scrollView)
            .setPositiveButton("OK", null)
            .show()
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
            BACKGROUND_PICK_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            BACKGROUND_PICK_CODE -> {
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
                    Toast.makeText(this, "Camera activated - AR mode enabled", Toast.LENGTH_SHORT)
                        .show()

                } catch (exc: Exception) {
                    Toast.makeText(
                        this,
                        "Failed to start camera: ${exc.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }

            } catch (exc: Exception) {
                Toast.makeText(
                    this,
                    "Camera initialization failed: ${exc.message}",
                    Toast.LENGTH_LONG
                ).show()
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

    private fun showModelBrowser() {
        modelBrowserDrawer?.show()
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
        val managedContainer3Ds =
            containerManager.getAllContainers().filterIsInstance<Container3D>()

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
        val managedContainer3Ds =
            containerManager.getAllContainers().filterIsInstance<Container3D>()

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
        aiMasterDrawer?.dismiss()
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