package com.dicereligion.edgecase

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin

/**
 * The Serpent's Eye — a service-state indicator for the main-menu lintel (Phase 7 #1).
 *
 * Stopped: a closed, angular lid (a dim horizontal slit). Running: the lid opens and an
 * emerald iris breathes, echoing the background gems. Lid open/close is eased; the iris pulse
 * runs only while the eye is open and the view is visible (battery-friendly, per Design Law L6).
 */
class ServiceEyeView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var running = false
    private var openFraction = 0f      // 0 = closed slit, 1 = fully open
    private var nowMs = 0f
    private var animator: ValueAnimator? = null

    private val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#9AA0A6")     // tarnished_silver
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.MITER            // blocky (Law L1)
    }
    private val scleraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#0C1210")      // obsidian_facet
    }
    private val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    private val lensPath = Path()
    private val irisPath = Path()

    /** Update the service state; opens/closes the lid and starts/stops the pulse. */
    fun setRunning(r: Boolean) {
        if (r == running) return
        running = r
        startIfNeeded()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); startIfNeeded() }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (visibility == VISIBLE) startIfNeeded() else stop()
    }

    private fun startIfNeeded() {
        if (animator != null || !isAttachedToWindow || visibility != VISIBLE) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                nowMs += 16f
                val target = if (running) 1f else 0f
                openFraction += (target - openFraction) * 0.18f   // ease toward target
                // Settle & stop when fully closed and idle (saves battery)
                if (!running && openFraction < 0.01f) {
                    openFraction = 0f
                    invalidate()
                    stop()
                    return@addUpdateListener
                }
                invalidate()
            }
            start()
        }
    }

    private fun stop() { animator?.cancel(); animator = null }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val halfW = width * 0.42f
        val maxOpen = height * 0.30f
        val open = (maxOpen * openFraction).coerceAtLeast(1f)

        // Angular lens: left corner → top mid → right corner → bottom mid (a blocky almond)
        lensPath.reset()
        lensPath.moveTo(cx - halfW, cy)
        lensPath.lineTo(cx, cy - open)
        lensPath.lineTo(cx + halfW, cy)
        lensPath.lineTo(cx, cy + open)
        lensPath.close()

        // Sclera fill (only meaningful once open)
        if (openFraction > 0.05f) canvas.drawPath(lensPath, scleraPaint)

        // Iris + glow when open enough
        if (openFraction > 0.2f) {
            val pulse = 0.5f + 0.5f * sin(nowMs / 2600f * 2f * Math.PI.toFloat())
            val irisR = (open * 0.72f)
            // Halo
            val glowAlpha = (pulse * 0.6f * 255).toInt()
            glowPaint.shader = RadialGradient(
                cx, cy, irisR * 2.2f,
                intArrayOf(
                    Color.argb(glowAlpha, 0x50, 0xC8, 0x78),
                    Color.argb((glowAlpha * 0.4f).toInt(), 0x2E, 0x8B, 0x57),
                    Color.TRANSPARENT
                ),
                floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, irisR * 2.2f, glowPaint)

            // Emerald-cut octagon iris
            val r = irisR
            val c = 0.32f
            irisPath.reset()
            irisPath.moveTo(cx - r + r * c, cy - r)
            irisPath.lineTo(cx + r - r * c, cy - r)
            irisPath.lineTo(cx + r, cy - r + r * c)
            irisPath.lineTo(cx + r, cy + r - r * c)
            irisPath.lineTo(cx + r - r * c, cy + r)
            irisPath.lineTo(cx - r + r * c, cy + r)
            irisPath.lineTo(cx - r, cy + r - r * c)
            irisPath.lineTo(cx - r, cy - r + r * c)
            irisPath.close()
            irisPaint.color = lerp(pulse, Color.parseColor("#1D5C3F"), Color.parseColor("#2E8B57"))
            canvas.drawPath(irisPath, irisPaint)

            if (pulse > 0.7f) {
                corePaint.color = Color.argb(((pulse - 0.7f) / 0.3f * 255).toInt(), 0xA9, 0xF5, 0xC8)
                canvas.drawCircle(cx, cy, r * 0.3f, corePaint)
            }
        }

        // Lid outline: full lens when open, a single slit line when closed
        if (openFraction > 0.05f) {
            canvas.drawPath(lensPath, lidPaint)
        } else {
            canvas.drawLine(cx - halfW, cy, cx + halfW, cy, lidPaint)
        }
    }

    private fun lerp(t: Float, from: Int, to: Int): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
    )
}
