// ContainerImage.kt
package com.infusory.tutarapp.ui.containers

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.infusory.tutarapp.R
import com.infusory.tutarapp.ui.utils.containers.ControlButton
import com.infusory.tutarapp.ui.utils.containers.ButtonPosition

class ContainerImage @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ContainerBase(context, ContainerType.IMAGE, attrs, defStyleAttr) {

    private var currentImageResource: Int? = null
    private var currentImagePath: String? = null
    private var imageRotation = 0f
    private var imageAlpha = 1.0f
    private var imageScaleType = ImageView.ScaleType.CENTER_INSIDE
    private var applyFilter = FilterType.NONE
    private var imageTint: Int? = null
    private var currentImageView: ImageView? = null

    enum class FilterType {
        NONE, GRAYSCALE, SEPIA, BLUR, BRIGHTNESS, CONTRAST, VINTAGE
    }

    init {
        setupImageContainer()
    }

    private fun setupImageContainer() {
//        clearControlButtons()

        val buttons = listOf(
            ControlButton(
                iconRes = android.R.drawable.ic_menu_close_clear_cancel,
                onClick = { onRemoveRequest?.invoke() },
                position = ButtonPosition.TOP_START
            ),
            ControlButton(
                iconRes = android.R.drawable.ic_menu_camera,
                onClick = { showImageSelectionDialog() },
                position = ButtonPosition.TOP_CENTER
            ),
            ControlButton(
                iconRes = android.R.drawable.ic_menu_rotate,
                onClick = { rotateImage() },
                position = ButtonPosition.TOP_END
            ),
            ControlButton(
                iconRes = android.R.drawable.ic_menu_edit,
                onClick = { showImageEditDialog() },
                position = ButtonPosition.BOTTOM_START
            ),
            ControlButton(
                iconRes = android.R.drawable.ic_menu_more,
                onClick = { showImageMenu() },
                position = ButtonPosition.BOTTOM_END
            )
        )

        addControlButtons(buttons)
    }

    override fun initializeContent() {
        createImageView()
    }

    private fun createImageView() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
            setBackgroundColor(Color.parseColor("#FAFAFA"))
        }

        currentImageView = ImageView(context).apply {
            // Set default image if no image is loaded
            if (currentImageResource == null && currentImagePath == null) {
                setImageResource(R.drawable.tutar_logo) // Default placeholder
                currentImageResource = R.drawable.tutar_logo
            } else {
                currentImageResource?.let { setImageResource(it) }
                // TODO: Handle image path loading
            }

            scaleType = imageScaleType
            rotation = imageRotation
            alpha = imageAlpha.toFloat()

            // Apply tint if set
            imageTint?.let { setColorFilter(it, PorterDuff.Mode.SRC_ATOP) }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        container.addView(currentImageView)

        // Add image info text
        container.addView(TextView(context).apply {
            text = getImageInfo()
            textSize = 10f
            gravity = android.view.Gravity.CENTER
            setTextColor(Color.GRAY)
            setPadding(0, dpToPx(4), 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        setContent(container)
        applyCurrentFilter()
    }

    private fun getImageInfo(): String {
        val rotationText = if (imageRotation != 0f) " • ${imageRotation.toInt()}°" else ""
        val filterText = if (applyFilter != FilterType.NONE) " • ${applyFilter.name}" else ""
        val alphaText = if (imageAlpha != 1.0f) " • ${(imageAlpha * 100).toInt()}%" else ""

        return "Image${rotationText}${filterText}${alphaText}"
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf(
            "Sample Images",
            "Camera (Placeholder)",
            "Gallery (Placeholder)",
            "Remove Image"
        )

        AlertDialog.Builder(context)
            .setTitle("Select Image Source")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSampleImagesDialog()
                    1 -> {
                        android.widget.Toast.makeText(context, "Camera integration coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        android.widget.Toast.makeText(context, "Gallery integration coming soon", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    3 -> removeImage()
                }
            }
            .show()
    }

    private fun showSampleImagesDialog() {
        val images = arrayOf("App Logo", "Android Robot", "Star Icon", "Info Icon")
        val imageResources = arrayOf(
            R.drawable.tutar_logo,
            android.R.drawable.sym_def_app_icon,
            android.R.drawable.btn_star_big_on,
            android.R.drawable.ic_dialog_info
        )

        AlertDialog.Builder(context)
            .setTitle("Select Sample Image")
            .setItems(images) { _, which ->
                currentImageResource = imageResources[which]
                currentImagePath = null
                updateImageView()
                android.widget.Toast.makeText(context, "Image: ${images[which]}", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun removeImage() {
        currentImageResource = null
        currentImagePath = null
        currentImageView?.setImageDrawable(null)
        currentImageView?.setBackgroundColor(Color.LTGRAY)
        android.widget.Toast.makeText(context, "Image removed", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun rotateImage() {
        imageRotation = (imageRotation + 90f) % 360f
        updateImageView()
        android.widget.Toast.makeText(context, "Rotated to ${imageRotation.toInt()}°", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showImageEditDialog() {
        val editView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dpToPx(20), dpToPx(16), dpToPx(20), dpToPx(16))
        }

        // Rotation control
        editView.addView(TextView(context).apply {
            text = "Rotation: ${imageRotation.toInt()}°"
            textSize = 16f
        })

        val rotationSeekBar = android.widget.SeekBar(context).apply {
            max = 360
            progress = imageRotation.toInt()
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        imageRotation = progress.toFloat()
                        (editView.getChildAt(0) as TextView).text = "Rotation: ${progress}°"
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
        editView.addView(rotationSeekBar)

        // Alpha control
        editView.addView(TextView(context).apply {
            text = "Opacity: ${(imageAlpha * 100).toInt()}%"
            textSize = 16f
            setPadding(0, dpToPx(16), 0, 0)
        })

        val alphaSeekBar = android.widget.SeekBar(context).apply {
            max = 100
            progress = (imageAlpha * 100).toInt()
            setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        imageAlpha = progress / 100f
                        (editView.getChildAt(2) as TextView).text = "Opacity: ${progress}%"
                    }
                }
                override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
            })
        }
        editView.addView(alphaSeekBar)

        AlertDialog.Builder(context)
            .setTitle("Edit Image")
            .setView(editView)
            .setPositiveButton("Apply") { _, _ ->
                updateImageView()
                android.widget.Toast.makeText(context, "Changes applied", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Reset") { _, _ ->
                imageRotation = 0f
                imageAlpha = 1.0f
                updateImageView()
                android.widget.Toast.makeText(context, "Image reset", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showImageMenu() {
        val options = arrayOf(
            "Scale Type",
            "Apply Filter",
            "Tint Color",
            "Reset All Effects",
            "Image Info"
        )

        AlertDialog.Builder(context)
            .setTitle("Image Options")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showScaleTypeDialog()
                    1 -> showFilterDialog()
                    2 -> showTintDialog()
                    3 -> resetAllEffects()
                    4 -> showImageInfoDialog()
                }
            }
            .show()
    }

    private fun showScaleTypeDialog() {
        val scaleTypes = arrayOf(
            "Center Inside", "Center Crop", "Fit XY", "Fit Center", "Fit Start", "Fit End", "Center", "Matrix"
        )
        val scaleTypeValues = arrayOf(
            ImageView.ScaleType.CENTER_INSIDE,
            ImageView.ScaleType.CENTER_CROP,
            ImageView.ScaleType.FIT_XY,
            ImageView.ScaleType.FIT_CENTER,
            ImageView.ScaleType.FIT_START,
            ImageView.ScaleType.FIT_END,
            ImageView.ScaleType.CENTER,
            ImageView.ScaleType.MATRIX
        )

        val currentIndex = scaleTypeValues.indexOf(imageScaleType)

        AlertDialog.Builder(context)
            .setTitle("Select Scale Type")
            .setSingleChoiceItems(scaleTypes, currentIndex) { dialog, which ->
                imageScaleType = scaleTypeValues[which]
                updateImageView()
                android.widget.Toast.makeText(context, "Scale: ${scaleTypes[which]}", android.widget.Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFilterDialog() {
        val filters = FilterType.values().map { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }.toTypedArray()
        val currentIndex = FilterType.values().indexOf(applyFilter)

        AlertDialog.Builder(context)
            .setTitle("Apply Filter")
            .setSingleChoiceItems(filters, currentIndex) { dialog, which ->
                applyFilter = FilterType.values()[which]
                updateImageView()
                android.widget.Toast.makeText(context, "Filter: ${filters[which]}", android.widget.Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showTintDialog() {
        val colors = arrayOf("None", "Red", "Green", "Blue", "Yellow", "Purple", "Orange", "Gray")
        val colorValues = arrayOf(
            null, Color.RED, Color.GREEN, Color.BLUE,
            Color.YELLOW, Color.parseColor("#800080"), Color.parseColor("#FFA500"), Color.GRAY
        )

        AlertDialog.Builder(context)
            .setTitle("Select Tint Color")
            .setItems(colors) { _, which ->
                imageTint = colorValues[which]
                updateImageView()
                android.widget.Toast.makeText(context, "Tint: ${colors[which]}", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun resetAllEffects() {
        imageRotation = 0f
        imageAlpha = 1.0f
        imageScaleType = ImageView.ScaleType.CENTER_INSIDE
        applyFilter = FilterType.NONE
        imageTint = null
        updateImageView()
        android.widget.Toast.makeText(context, "All effects reset", android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun showImageInfoDialog() {
        val info = buildString {
            append("Image Information:\n\n")
            append("Source: ${if (currentImageResource != null) "Resource" else if (currentImagePath != null) "File" else "None"}\n")
            append("Rotation: ${imageRotation.toInt()}°\n")
            append("Opacity: ${(imageAlpha * 100).toInt()}%\n")
            append("Scale Type: ${imageScaleType.name}\n")
            append("Filter: ${applyFilter.name}\n")
            append("Tint: ${if (imageTint != null) "Applied" else "None"}")
        }

        AlertDialog.Builder(context)
            .setTitle("Image Information")
            .setMessage(info)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun updateImageView() {
        currentImageView?.apply {
            rotation = imageRotation
            alpha = imageAlpha.toFloat()
            scaleType = imageScaleType

            // Clear previous tint
            clearColorFilter()

            // Apply tint if set
            imageTint?.let { setColorFilter(it, PorterDuff.Mode.SRC_ATOP) }

            // Apply filter
            applyCurrentFilter()
        }

        // Update info text
        val container = currentImageView?.parent as? LinearLayout
        val infoText = container?.getChildAt(1) as? TextView
        infoText?.text = getImageInfo()
    }

    private fun applyCurrentFilter() {
        currentImageView?.let { imageView ->
            when (applyFilter) {
                FilterType.NONE -> {
                    imageView.colorFilter = null
                }
                FilterType.GRAYSCALE -> {
                    val matrix = ColorMatrix().apply { setSaturation(0f) }
                    imageView.colorFilter = ColorMatrixColorFilter(matrix)
                }
                FilterType.SEPIA -> {
                    val matrix = ColorMatrix().apply {
                        setSaturation(0f)
                        // Apply sepia effect
                        val sepiaMatrix = ColorMatrix(floatArrayOf(
                            0.393f, 0.769f, 0.189f, 0f, 0f,
                            0.349f, 0.686f, 0.168f, 0f, 0f,
                            0.272f, 0.534f, 0.131f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                        postConcat(sepiaMatrix)
                    }
                    imageView.colorFilter = ColorMatrixColorFilter(matrix)
                }
                FilterType.BRIGHTNESS -> {
                    val matrix = ColorMatrix().apply {
                        set(floatArrayOf(
                            1.2f, 0f, 0f, 0f, 50f,
                            0f, 1.2f, 0f, 0f, 50f,
                            0f, 0f, 1.2f, 0f, 50f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                    imageView.colorFilter = ColorMatrixColorFilter(matrix)
                }
                FilterType.CONTRAST -> {
                    val matrix = ColorMatrix().apply {
                        set(floatArrayOf(
                            1.5f, 0f, 0f, 0f, -64f,
                            0f, 1.5f, 0f, 0f, -64f,
                            0f, 0f, 1.5f, 0f, -64f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                    imageView.colorFilter = ColorMatrixColorFilter(matrix)
                }
                FilterType.VINTAGE -> {
                    val matrix = ColorMatrix().apply {
                        set(floatArrayOf(
                            0.9f, 0.5f, 0.1f, 0f, 0f,
                            0.3f, 0.8f, 0.1f, 0f, 0f,
                            0.2f, 0.3f, 0.5f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                    imageView.colorFilter = ColorMatrixColorFilter(matrix)
                }
                FilterType.BLUR -> {
                    // Note: Blur filter would require more complex implementation
                    // This is a placeholder that darkens the image slightly
                    val matrix = ColorMatrix().apply {
                        set(floatArrayOf(
                            0.8f, 0f, 0f, 0f, 0f,
                            0f, 0.8f, 0f, 0f, 0f,
                            0f, 0f, 0.8f, 0f, 0f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    }
                    imageView.colorFilter = ColorMatrixColorFilter(matrix)
                }
            }
        }
    }

    // Image-specific methods
    fun setImageResource(resourceId: Int) {
        currentImageResource = resourceId
        currentImagePath = null
        currentImageView?.setImageResource(resourceId)
        updateImageView()
    }

    fun setImagePath(path: String) {
        currentImagePath = path
        currentImageResource = null
        // TODO: Implement actual image loading from path
        android.widget.Toast.makeText(context, "Image path loading not implemented yet", android.widget.Toast.LENGTH_SHORT).show()
    }

    fun setImageRotation(rotation: Float) {
        imageRotation = rotation % 360f
        updateImageView()
    }

    fun setImageAlpha(alpha: Float) {
        imageAlpha = alpha.coerceIn(0f, 1f)
        updateImageView()
    }

    fun setImageScaleType(scaleType: ImageView.ScaleType) {
        imageScaleType = scaleType
        updateImageView()
    }

    fun setImageFilter(filter: FilterType) {
        applyFilter = filter
        updateImageView()
    }

    fun setImageTint(color: Int?) {
        imageTint = color
        updateImageView()
    }

    fun getCurrentImageResource(): Int? = currentImageResource
    fun getCurrentImagePath(): String? = currentImagePath
    fun getImageRotation(): Float = imageRotation
    fun getImageAlpha(): Float = imageAlpha
    fun getImageScaleType(): ImageView.ScaleType = imageScaleType
    fun getImageFilter(): FilterType = applyFilter
    fun getImageTint(): Int? = imageTint

    override fun getDefaultWidth(): Int = dpToPx(320)
    override fun getDefaultHeight(): Int = dpToPx(320)

    override fun getCustomSaveData(): Map<String, Any> {
        val baseData = super.getCustomSaveData().toMutableMap()
        baseData.putAll(mapOf(
            "imageResource" to (currentImageResource ?: -1),
            "imagePath" to (currentImagePath ?: ""),
            "imageRotation" to imageRotation,
            "imageAlpha" to imageAlpha,
            "imageScaleType" to imageScaleType.name,
            "imageFilter" to applyFilter.name,
            "imageTint" to (imageTint ?: -1)
        ))
        return baseData
    }

    override fun loadCustomSaveData(data: Map<String, Any>) {
        super.loadCustomSaveData(data)

        data["imageResource"]?.let {
            if (it is Int && it != -1) currentImageResource = it
        }
        data["imagePath"]?.let {
            if (it is String && it.isNotEmpty()) currentImagePath = it
        }
        data["imageRotation"]?.let {
            if (it is Float) imageRotation = it
            else if (it is Double) imageRotation = it.toFloat()
        }
        data["imageAlpha"]?.let {
            if (it is Float) imageAlpha = it
            else if (it is Double) imageAlpha = it.toFloat()
        }
        data["imageScaleType"]?.let {
            if (it is String) {
                try {
                    imageScaleType = ImageView.ScaleType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    imageScaleType = ImageView.ScaleType.CENTER_INSIDE
                }
            }
        }
        data["imageFilter"]?.let {
            if (it is String) {
                try {
                    applyFilter = FilterType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    applyFilter = FilterType.NONE
                }
            }
        }
        data["imageTint"]?.let {
            if (it is Int && it != -1) imageTint = it
        }

        // Update the image view if it exists
        if (currentImageView != null) {
            updateImageView()
        }
    }
}