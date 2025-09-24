// ContainerManager.kt
package com.infusory.tutarapp.ui.utils.containers

import android.content.Context
import android.view.ViewGroup
import android.widget.Toast
import com.infusory.tutarapp.ui.containers.Container3D
import com.infusory.tutarapp.ui.containers.ContainerImage
import com.infusory.tutarapp.ui.containers.ContainerText
import com.infusory.tutarapp.ui.containers.ContainerBase

class ContainerManager(
    private val context: Context,
    private val parentLayout: ViewGroup,
    private val maxContainers: Int = 6
) {
    private val containers = mutableListOf<ContainerBase>()

    // Callbacks
    var onContainerAdded: ((ContainerBase) -> Unit)? = null
    var onContainerRemoved: ((ContainerBase) -> Unit)? = null
    var onContainerCountChanged: ((Int) -> Unit)? = null

    fun addTextContainer(): ContainerBase? {
        if (containers.size >= maxContainers) {
            showMaxContainersMessage()
            return null
        }

        val container = ContainerText(context)
        return addContainer(container, "Text container added")
    }

    fun add3DContainer(): ContainerBase? {
        if (containers.size >= maxContainers) {
            showMaxContainersMessage()
            return null
        }

        val container = Container3D(context)
        return addContainer(container, "3D container added")
    }

    fun addImageContainer(): ContainerBase? {
        if (containers.size >= maxContainers) {
            showMaxContainersMessage()
            return null
        }

        val container = ContainerImage(context)
        return addContainer(container, "Image container added")
    }

    fun addStandardContainer(): ContainerBase? {
        if (containers.size >= maxContainers) {
            showMaxContainersMessage()
            return null
        }

        val container = ContainerBase(context, ContainerBase.ContainerType.STANDARD)
        return addContainer(container, "Standard container added")
    }

    fun addMinimalContainer(): ContainerBase? {
        if (containers.size >= maxContainers) {
            showMaxContainersMessage()
            return null
        }

        val container = ContainerBase(context, ContainerBase.ContainerType.MINIMAL)
        return addContainer(container, "Minimal container added")
    }

    fun addReadOnlyContainer(): ContainerBase? {
        if (containers.size >= maxContainers) {
            showMaxContainersMessage()
            return null
        }

        val container = ContainerBase(context, ContainerBase.ContainerType.READ_ONLY)
        return addContainer(container, "Read-only container added")
    }

    private fun addContainer(container: ContainerBase, message: String): ContainerBase {
        // Set layout params
        val layoutParams = when (parentLayout) {
            is android.widget.RelativeLayout -> android.widget.RelativeLayout.LayoutParams(
                container.getDefaultWidth(),
                container.getDefaultHeight()
            )
            is android.widget.FrameLayout -> android.widget.FrameLayout.LayoutParams(
                container.getDefaultWidth(),
                container.getDefaultHeight()
            )
            else -> android.view.ViewGroup.LayoutParams(
                container.getDefaultWidth(),
                container.getDefaultHeight()
            )
        }
        container.layoutParams = layoutParams

        // Position container with offset to avoid overlap
        val offsetX = containers.size * 50f
        val offsetY = containers.size * 50f + 100f
        container.moveContainerTo(offsetX, offsetY, animate = false)

        // Set up remove callback
        container.onRemoveRequest = { removeContainer(container) }

        // Add to parent layout and track
        parentLayout.addView(container)
        containers.add(container)

        // Initialize container content
        container.initializeContent()

        // Notify callbacks
        onContainerAdded?.invoke(container)
        onContainerCountChanged?.invoke(containers.size)

        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        return container
    }

    fun removeContainer(container: ContainerBase) {
        parentLayout.removeView(container)
        containers.remove(container)

        onContainerRemoved?.invoke(container)
        onContainerCountChanged?.invoke(containers.size)

        Toast.makeText(context, "Container removed. ${containers.size} remaining", Toast.LENGTH_SHORT).show()
    }

    fun clearAllContainers() {
        containers.forEach { container ->
            parentLayout.removeView(container)
        }
        val removedCount = containers.size
        containers.clear()

        onContainerCountChanged?.invoke(0)
        Toast.makeText(context, "$removedCount containers cleared", Toast.LENGTH_SHORT).show()
    }

    fun resetAllContainers() {
        containers.forEachIndexed { index, container ->
            val offsetX = index * 50f
            val offsetY = index * 50f + 100f
            container.resetTransform()
            container.moveContainerTo(offsetX, offsetY, animate = true)
        }
        Toast.makeText(context, "All containers reset", Toast.LENGTH_SHORT).show()
    }

    fun zoomAllContainers(scale: Float) {
        containers.forEach { container ->
            container.zoomTo(scale, animate = true)
        }
        Toast.makeText(context, "All containers zoomed to ${scale}x", Toast.LENGTH_SHORT).show()
    }

    fun arrangeContainersInGrid() {
        val cols = 2
        val spacing = dpToPx(320) // Container width + margin
        val startX = dpToPx(50)
        val startY = dpToPx(100)

        containers.forEachIndexed { index, container ->
            val row = index / cols
            val col = index % cols
            val x = startX + (col * spacing)
            val y = startY + (row * spacing)

            container.moveContainerTo(x.toFloat(), y.toFloat(), animate = true)
            container.zoomTo(1.0f, animate = true)
        }

        Toast.makeText(context, "Containers arranged in grid", Toast.LENGTH_SHORT).show()
    }

    fun toggleDraggingForAllContainers() {
        var allDraggingEnabled = true
        containers.forEach { container ->
            if (!container.isDraggingEnabled) {
                allDraggingEnabled = false
            }
        }

        containers.forEach { container ->
            container.isDraggingEnabled = !allDraggingEnabled
        }

        Toast.makeText(
            context,
            "Dragging ${if (!allDraggingEnabled) "enabled" else "disabled"} for all containers",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun toggleResizingForAllContainers() {
        var allResizingEnabled = true
        containers.forEach { container ->
            if (!container.isResizingEnabled) {
                allResizingEnabled = false
            }
        }

        containers.forEach { container ->
            container.isResizingEnabled = !allResizingEnabled
        }

        Toast.makeText(
            context,
            "Resizing ${if (!allResizingEnabled) "enabled" else "disabled"} for all containers",
            Toast.LENGTH_SHORT
        )
    }

    fun getContainerCount(): Int = containers.size

    fun getAllContainers(): List<ContainerBase> = containers.toList()

    fun getContainerByType(type: ContainerBase.ContainerType): List<ContainerBase> {
        return containers.filter { it.containerType == type }
    }

    fun saveState(): ContainerStateData {
        val containerStates = containers.map { container ->
            ContainerState(
                type = container.containerType,
                position = container.getCurrentPosition(),
                scale = container.getCurrentScale(),
                size = container.getCurrentSize(),
                customData = container.getCustomSaveData()
            )
        }
        return ContainerStateData(containerStates)
    }

    fun loadState(stateData: ContainerStateData) {
        // Clear existing containers
        clearAllContainers()

        // Recreate containers from saved state
        stateData.containers.forEach { state ->
            val container = when (state.type) {
                ContainerBase.ContainerType.TEXT -> ContainerText(context)
                ContainerBase.ContainerType.MODEL_3D -> Container3D(context)
                ContainerBase.ContainerType.IMAGE -> ContainerImage(context)
                ContainerBase.ContainerType.STANDARD -> ContainerBase(context, ContainerBase.ContainerType.STANDARD)
                ContainerBase.ContainerType.MINIMAL -> ContainerBase(context, ContainerBase.ContainerType.MINIMAL)
                ContainerBase.ContainerType.READ_ONLY -> ContainerBase(context, ContainerBase.ContainerType.READ_ONLY)
            }

            // Set layout params
            val layoutParams = when (parentLayout) {
                is android.widget.RelativeLayout -> android.widget.RelativeLayout.LayoutParams(
                    state.size.first,
                    state.size.second
                )
                else -> android.view.ViewGroup.LayoutParams(
                    state.size.first,
                    state.size.second
                )
            }
            container.layoutParams = layoutParams

            // Restore position and scale
            container.moveContainerTo(state.position.first, state.position.second, animate = false)
            container.zoomTo(state.scale, animate = false)

            // Set up remove callback
            container.onRemoveRequest = { removeContainer(container) }

            // Load custom data
            container.loadCustomSaveData(state.customData)

            // Add to layout and track
            parentLayout.addView(container)
            containers.add(container)

            // Initialize content
            container.initializeContent()
        }

        onContainerCountChanged?.invoke(containers.size)
        if (containers.isNotEmpty()) {
            Toast.makeText(context, "Restored ${containers.size} containers", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showMaxContainersMessage() {
        Toast.makeText(context, "Maximum $maxContainers containers allowed", Toast.LENGTH_SHORT).show()
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }
}

// Data classes for save/load functionality
data class ContainerStateData(
    val containers: List<ContainerState>
)

data class ContainerState(
    val type: ContainerBase.ContainerType,
    val position: Pair<Float, Float>,
    val scale: Float,
    val size: Pair<Int, Int>,
    val customData: Map<String, Any> = emptyMap()
)