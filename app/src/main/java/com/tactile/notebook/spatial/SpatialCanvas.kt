package com.tactile.notebook.spatial

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.*

/**
 * Spatial Search Engine — Notes are found by dragging the background canvas
 * instead of typing in a search bar. The canvas represents an infinite 2D space
 * where notes are placed at spatial coordinates.
 */
class SpatialSearchEngine(
    private val viewportWidth: Float = 1080f,
    private val viewportHeight: Float = 2400f
) {
    // Current viewport position in world space
    var viewportOrigin by mutableStateOf(Offset(0f, 0f))
        private set

    // Zoom level
    var zoom by mutableStateOf(1f)
        private set

    // Velocity for inertial scrolling
    var velocity by mutableStateOf(Offset.Zero)
        private set

    // Viewport rect in world space
    val viewportRect: Rect
        get() = Rect(
            left = viewportOrigin.x,
            top = viewportOrigin.y,
            right = viewportOrigin.x + viewportWidth / zoom,
            bottom = viewportOrigin.y + viewportHeight / zoom
        )

    /**
     * Move the viewport by a delta (from drag gesture)
     */
    fun pan(delta: Offset) {
        viewportOrigin = Offset(
            viewportOrigin.x - delta.x / zoom,
            viewportOrigin.y - delta.y / zoom
        )
    }

    /**
     * Apply inertial scrolling with friction
     * Call this in an animation loop
     */
    fun applyInertia(friction: Float = 0.95f): Boolean {
        if (abs(velocity.x) < 0.1f && abs(velocity.y) < 0.1f) {
            velocity = Offset.Zero
            return false
        }
        viewportOrigin = Offset(
            viewportOrigin.x - velocity.x / zoom,
            viewportOrigin.y - velocity.y / zoom
        )
        velocity = Offset(velocity.x * friction, velocity.y * friction)
        return true
    }

    /**
     * Set velocity from fling gesture
     */
    fun fling(velocityX: Float, velocityY: Float) {
        velocity = Offset(velocityX, velocityY)
    }

    /**
     * Zoom towards a focus point
     */
    fun zoom(delta: Float, focus: Offset) {
        val newZoom = (zoom * (1f + delta)).coerceIn(0.3f, 3f)
        val zoomRatio = newZoom / zoom

        // Keep the focus point stable
        viewportOrigin = Offset(
            focus.x - (focus.x - viewportOrigin.x) * zoomRatio,
            focus.y - (focus.y - viewportOrigin.y) * zoomRatio
        )
        zoom = newZoom
    }

    /**
     * Convert screen coordinates to world coordinates
     */
    fun screenToWorld(screenOffset: Offset): Offset = Offset(
        screenOffset.x / zoom + viewportOrigin.x,
        screenOffset.y / zoom + viewportOrigin.y
    )

    /**
     * Convert world coordinates to screen coordinates
     */
    fun worldToScreen(worldOffset: Offset): Offset = Offset(
        (worldOffset.x - viewportOrigin.x) * zoom,
        (worldOffset.y - viewportOrigin.y) * zoom
    )

    /**
     * Check if a world-space rect is visible in the current viewport
     */
    fun isVisible(worldRect: Rect): Boolean = viewportRect.overlaps(worldRect)

    /**
     * Get the center point of the viewport in world space
     */
    fun getViewportCenter(): Offset = Offset(
        viewportOrigin.x + viewportWidth / (2 * zoom),
        viewportOrigin.y + viewportHeight / (2 * zoom)
    )

    /**
     * Animate viewport to center on a specific world position
     */
    fun centerOn(worldPosition: Offset) {
        viewportOrigin = Offset(
            worldPosition.x - viewportWidth / (2 * zoom),
            worldPosition.y - viewportHeight / (2 * zoom)
        )
    }

    /**
     * Reset viewport to origin
     */
    fun reset() {
        viewportOrigin = Offset(0f, 0f)
        zoom = 1f
        velocity = Offset.Zero
    }
}
