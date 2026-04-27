package com.tactile.notebook.ui.animation

import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Velocity
import kotlin.math.*

/**
 * Physics engine for card fling and spring animations.
 * Provides realistic physics-based motion for the Overlap Canvas.
 */
object PhysicsEngine {

    // Physics constants
    const val GRAVITY = 980f          // pixels/s²
    const val FRICTION = 0.92f        // velocity damping per frame
    const val FLING_THRESHOLD = 300f  // min velocity to trigger fling
    const val SPRING_STIFFNESS = 200f
    const val SPRING_DAMPING = 0.7f

    /**
     * Calculate fling destination based on initial velocity
     */
    fun calculateFlingDestination(
        startPosition: Offset,
        velocity: Velocity,
        bounds: androidx.compose.ui.geometry.Rect
    ): Offset {
        val speed = sqrt(velocity.x.pow(2) + velocity.y.pow(2))
        if (speed < FLING_THRESHOLD) return startPosition

        // Distance = v² / (2 * friction * g)
        val distance = speed.pow(2) / (2 * FRICTION * GRAVITY)
        val angle = atan2(velocity.y, velocity.x)

        val destX = startPosition.x + distance * cos(angle)
        val destY = startPosition.y + distance * sin(angle)

        // Clamp to bounds
        return Offset(
            destX.coerceIn(bounds.left, bounds.right),
            destY.coerceIn(bounds.top, bounds.bottom)
        )
    }

    /**
     * Check if a fling gesture qualifies as an "archive" action.
     * Archive = fling towards top-right corner
     */
    fun isArchiveFling(velocity: Velocity, dragOffset: Offset): Boolean {
        val flingAngle = atan2(velocity.y, velocity.x).toDegrees()
        val isUpRight = flingAngle in -90f..0f // Top-right quadrant
        val hasSufficientVelocity = sqrt(velocity.x.pow(2) + velocity.y.pow(2)) > FLING_THRESHOLD
        val hasDragOffset = dragOffset.y < -100f && dragOffset.x > 50f
        return isUpRight && hasSufficientVelocity && hasDragOffset
    }

    /**
     * Spring animation spec for card return
     */
    fun cardSpringSpec(): SpringSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Decay spec for fling animation
     */
    fun flingDecaySpec(): DecayAnimationSpec<Float> = exponentialDecay(
        frictionMultiplier = 1.2f
    )

    private fun Float.toDegrees(): Float = Math.toDegrees(this.toDouble()).toFloat()
}

/**
 * Represents the animated state of a card being flung
 */
data class FlingState(
    val cardId: Long,
    val startPosition: Offset,
    val endPosition: Offset,
    val rotation: Float = 0f,
    val alpha: Float = 1f,
    val progress: Float = 0f
) {
    val currentOffset: Offset
        get() = Offset(
            startPosition.x + (endPosition.x - startPosition.x) * progress,
            startPosition.y + (endPosition.y - startPosition.y) * progress
        )

    val currentAlpha: Float
        get() = alpha * (1f - progress)
}
