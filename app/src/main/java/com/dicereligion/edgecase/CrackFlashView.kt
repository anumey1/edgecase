package com.dicereligion.edgecase

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * A one-shot crack flash: when a stone slab is pressed, 2–3 jagged fractures spider outward
 * from the touch point and fade over ~300ms — the slab cracks under your thumb (Phase 7 #2).
 * Pairs with [DustParticleView]'s dust burst. Overlaid full-screen in the main-menu dust container.
 */
class CrackFlashView(context: Context) : View(context) {

    private class Crack(val path: Path, var age: Float = 0f, val life: Float = 0.30f)

    private val cracks = mutableListOf<Crack>()
    private val density = resources.displayMetrics.density

    private val voidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#020403")      // crack_void
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.MITER            // sharp only (Design Law L1)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#502E8B57")    // faint emerald fracture-light
        strokeWidth = 5f
        strokeJoin = Paint.Join.MITER
    }

    private var animator: ValueAnimator? = null
    private var isRunning = false

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    /** Fracture the stone at (x, y) in this view's coordinates. */
    fun crackAt(x: Float, y: Float) {
        val count = 2 + Random.nextInt(2)   // 2–3 fractures
        repeat(count) {
            val path = Path()
            path.moveTo(x, y)
            var cx = x
            var cy = y
            var angle = Random.nextFloat() * Math.PI.toFloat() * 2f
            val segments = 3 + Random.nextInt(2)
            repeat(segments) {
                val len = (12f + Random.nextFloat() * 18f) * density
                angle += (Random.nextFloat() - 0.5f) * 1.1f   // sharp kinks
                cx += cos(angle) * len
                cy += sin(angle) * len
                path.lineTo(cx, cy)
            }
            cracks.add(Crack(path))
        }
        startIfNeeded()
    }

    private fun startIfNeeded() {
        if (isRunning) return
        isRunning = true
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                val dt = 0.016f
                val iter = this@CrackFlashView.cracks.iterator()
                while (iter.hasNext()) {
                    val c = iter.next()
                    c.age += dt
                    if (c.age >= c.life) iter.remove()
                }
                if (this@CrackFlashView.cracks.isEmpty()) {
                    this@CrackFlashView.isRunning = false
                    this@CrackFlashView.animator?.cancel()
                }
                this@CrackFlashView.invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (c in cracks) {
            val a = (1f - c.age / c.life).coerceIn(0f, 1f)
            glowPaint.alpha = (a * 0x50).toInt()
            voidPaint.alpha = (a * 0xC0).toInt()
            canvas.drawPath(c.path, glowPaint)
            canvas.drawPath(c.path, voidPaint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
        cracks.clear()
        isRunning = false
    }
}
