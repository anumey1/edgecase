package com.dicereligion.edgecase

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A subtle dust particle effect view that spawns small particles on trigger.
 * Particles float outward with random velocities and fade out.
 *
 * Usage: call [burst] to spawn a cluster of particles at the center.
 */
class DustParticleView(context: Context) : View(context) {

    private data class Particle(
        var x: Float, var y: Float, var radius: Float,
        var alpha: Float, var vx: Float, var vy: Float, var life: Float
    )

    private val particles = mutableListOf<Particle>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 245, 239, 230) // Aged Marble
        style = Paint.Style.FILL
    }

    private var animator: ValueAnimator? = null
    private var isRunning = false

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    /** Trigger a burst of particles from the center of this view. */
    fun burst(count: Int = 6) {
        val cx = width / 2f
        val cy = height / 2f
        for (i in 0 until count) {
            val angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 1.5f + Random.nextFloat() * 3f
            particles.add(
                Particle(
                    x = cx + (Random.nextFloat() - 0.5f) * 20f,
                    y = cy + (Random.nextFloat() - 0.5f) * 10f,
                    radius = 1.5f + Random.nextFloat() * 2.5f,
                    alpha = 0.7f + Random.nextFloat() * 0.3f,
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed - 2f, // slight upward bias
                    life = 0.4f + Random.nextFloat() * 0.3f
                )
            )
        }
        startIfNeeded()
    }

    private fun startIfNeeded() {
        if (isRunning) return
        isRunning = true
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 600
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val dt = 0.016f
                val iter = this@DustParticleView.particles.iterator()
                while (iter.hasNext()) {
                    val p = iter.next()
                    p.x += p.vx
                    p.y += p.vy
                    p.vy += 0.5f
                    p.life -= dt
                    p.alpha = (p.life / 0.7f).coerceIn(0f, 1f) * 0.8f
                    p.radius *= 0.98f
                    if (p.life <= 0f) iter.remove()
                }
                if (this@DustParticleView.particles.isEmpty()) {
                    this@DustParticleView.isRunning = false
                    this@DustParticleView.animator?.cancel()
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (p in particles) {
            paint.alpha = (p.alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.radius, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        particles.clear()
        isRunning = false
    }
}
