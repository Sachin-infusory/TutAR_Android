package com.infusory.tutarapp.managers

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.provider.MediaStore
import android.view.SurfaceView
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.infusory.tutarapp.ui.components.containers.ContainerImage
import com.infusory.tutarapp.ui.components.containers.ContainerPdf

class ImagePickerHandler(
    private val activity: AppCompatActivity,
    private val mainLayout: RelativeLayout,
    private val surfaceView: SurfaceView
) {
    companion object {
        const val BACKGROUND_PICK_CODE = 101
        const val CONTAINER_IMAGE_PICK_CODE = 102
        const val CONTAINER_PDF_PICK_CODE = 103

        // Maximum dimensions for the container (to prevent extremely large containers)
        private const val MAX_CONTAINER_WIDTH = 800
        private const val MAX_CONTAINER_HEIGHT = 800
    }

    fun pickBackgroundImage() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        activity.startActivityForResult(intent, BACKGROUND_PICK_CODE)
    }

    fun pickContainerImage() {
        // Directly launch the image picker without creating a container first
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        activity.startActivityForResult(intent, CONTAINER_IMAGE_PICK_CODE)
    }

    fun pickContainerPdf() {
        // Launch PDF picker
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
        }
        activity.startActivityForResult(intent, CONTAINER_PDF_PICK_CODE)
    }

    fun handleImagePickResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode != Activity.RESULT_OK || data?.data == null) return

        val uri = data.data!!

        when (requestCode) {
            BACKGROUND_PICK_CODE -> setBackgroundImage(uri)
            CONTAINER_IMAGE_PICK_CODE -> setContainerImage(uri)
            CONTAINER_PDF_PICK_CODE -> setContainerPdf(uri)
        }
    }

    private fun setBackgroundImage(imageUri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(
                activity.contentResolver,
                imageUri
            )
            val drawable = BitmapDrawable(activity.resources, bitmap)
            surfaceView.background = drawable
        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Failed to set background: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setContainerImage(imageUri: Uri) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(
                activity.contentResolver,
                imageUri
            )

            // Calculate appropriate container dimensions based on image size
            val (containerWidth, containerHeight) = calculateContainerDimensions(
                bitmap.width,
                bitmap.height
            )

            // Create a new ContainerImage with the selected image
            val imageContainer = ContainerImage(activity).apply {
                tag = "lesson_image_${System.currentTimeMillis()}" // Unique tag for each image
                layoutParams = RelativeLayout.LayoutParams(
                    containerWidth,
                    containerHeight
                )

                // Set up the removal callback
                onRemoveRequest = {
                    // Remove this container from the parent layout
                    mainLayout.removeView(this)
                    Toast.makeText(
                        activity,
                        "Image container removed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Add container to layout
            mainLayout.addView(imageContainer)
            imageContainer.initializeContent()

            // Set the selected image
            imageContainer.setImage(bitmap, imageUri.toString())

            Toast.makeText(
                activity,
                "Image loaded: ${bitmap.width}x${bitmap.height}",
                Toast.LENGTH_SHORT
            ).show()

        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Failed to load image into container: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun setContainerPdf(pdfUri: Uri) {
        try {
            // Create a new ContainerPdf with the selected PDF
            val pdfContainer = ContainerPdf(activity).apply {
                tag = "lesson_pdf_${System.currentTimeMillis()}" // Unique tag for each PDF
                layoutParams = RelativeLayout.LayoutParams(
                    getDefaultWidth(),
                    getDefaultHeight()
                )

                // Set up the removal callback
                onRemoveRequest = {
                    // Remove this container from the parent layout
                    mainLayout.removeView(this)
                    Toast.makeText(
                        activity,
                        "PDF container removed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            // Add container to layout
            mainLayout.addView(pdfContainer)
            pdfContainer.initializeContent()

            // Set the selected PDF
            pdfContainer.setPdfFromUri(pdfUri)

        } catch (e: Exception) {
            Toast.makeText(
                activity,
                "Failed to load PDF into container: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Calculate container dimensions that fit the image while respecting maximum size constraints
     * and maintaining aspect ratio
     */
    private fun calculateContainerDimensions(imageWidth: Int, imageHeight: Int): Pair<Int, Int> {
        // Convert max dimensions from dp to pixels
        val density = activity.resources.displayMetrics.density
        val maxWidth = (MAX_CONTAINER_WIDTH * density).toInt()
        val maxHeight = (MAX_CONTAINER_HEIGHT * density).toInt()

        var finalWidth = imageWidth
        var finalHeight = imageHeight

        // Scale down if image is too large, maintaining aspect ratio
        if (imageWidth > maxWidth || imageHeight > maxHeight) {
            val widthRatio = maxWidth.toFloat() / imageWidth
            val heightRatio = maxHeight.toFloat() / imageHeight
            val scaleFactor = minOf(widthRatio, heightRatio)

            finalWidth = (imageWidth * scaleFactor).toInt()
            finalHeight = (imageHeight * scaleFactor).toInt()
        }

        // Ensure minimum size for usability
        val minSize = (100 * density).toInt()
        finalWidth = maxOf(finalWidth, minSize)
        finalHeight = maxOf(finalHeight, minSize)

        return Pair(finalWidth, finalHeight)
    }
}