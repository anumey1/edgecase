package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.SystemClock
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * The Serpent's Eye — a service-state indicator for the main-menu lintel (Phase 7 #1).
 *
 * Stopped: a closed, angular lid (a dim horizontal slit). Running: the lid opens and an
 * emerald iris breathes, echoing the background gems. Lid open/close is eased; the iris pulse
 * runs only while the eye is open and the view is visible (battery-friendly, per Design Law L6).
 *
 * Two speeds (Docs/RAMIssuePDP.md Phase 3): the brief lid open/close runs at display rate so the
 * motion stays smooth, and the never-ending breathing rides the shared 12 fps [TempleClock]. Both
 * are driven by wall time, so neither speeds up or slows down with the frame rate.
 */
class ServiceEyeView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var running = false
    private var openFraction = 0f      // 0 = closed slit, 1 = fully open
    private var lastFrameMs = 0L       // 0 = no frame yet in the current animation
    private var subscribed = false

    private val lidPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.tarnished_silver)
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.MITER            // blocky (Law L1)
    }
    private val scleraPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = ContextCompat.getColor(context, R.color.obsidian_facet)
    }
    private val irisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    // Built once: the halo at full strength and unit radius, scaled per frame by its local matrix,
    // with the pulse applied as paint alpha (which multiplies the shader's).
    private val halo = RadialGradient(
        0f, 0f, 1f,
        intArrayOf(
            Color.argb(255, 0x50, 0xC8, 0x78),
            Color.argb(102, 0x2E, 0x8B, 0x57),   // 40 % of the core's alpha
            Color.TRANSPARENT
        ),
        floatArrayOf(0f, 0.5f, 1f), Shader.TileMode.CLAMP
    )
    private val haloMatrix = Matrix()
    private val emeraldDeep = ContextCompat.getColor(context, R.color.emerald_deep)
    private val emeraldGem = ContextCompat.getColor(context, R.color.emerald_gem)

    private val lensPath = Path()
    private val irisPath = Path()

    /** Update the service state; opens/closes the lid and starts/stops the pulse. */
    fun setRunning(r: Boolean) {
        if (r == running) return
        running = r
        updateAnimation()
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updateAnimation() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); updateAnimation() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updateAnimation()
    }

    /** Called when the Activity is stopped or started: a hidden window must not keep animating. */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updateAnimation()
    }

    private val target get() = if (running) 1f else 0f

    /**
     * Picks the animation mode:
     *  • not on screen → nothing
     *  • lid moving → a redraw every display frame until it settles (see [onDraw])
     *  • open and settled → breathing on the 12 fps clock
     *  • closed and settled → nothing at all, so a closed eye costs nothing
     */
    private fun updateAnimation() {
        val onScreen = isAttachedToWindow && isShown && windowVisibility == VISIBLE
        val transitioning = openFraction != target
        val breathe = onScreen && !transitioning && running
        if (breathe != subscribed) {
            subscribed = breathe
            if (breathe) TempleClock.subscribe(this) else TempleClock.unsubscribe(this)
        }
        if (!onScreen) lastFrameMs = 0L
        if (onScreen && transitioning) postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = SystemClock.uptimeMillis()

        // Ease the lid toward its target by elapsed time: 1 − 0.82^(dt / 16.67 ms) reproduces the
        // original 0.18-per-frame curve at 60 fps, and keeps the same timing at any other rate.
        if (openFraction != target) {
            val dt = if (lastFrameMs == 0L) 16L else (now - lastFrameMs).coerceIn(0L, 100L)
            openFraction += (target - openFraction) * (1f - 0.82f.pow(dt / 16.667f))
            if (abs(target - openFraction) < 0.01f) openFraction = target
            if (openFraction == target) {
                lastFrameMs = 0L
                updateAnimation()          // settled: hand over to the clock, or stop
            } else {
                lastFrameMs = now
                postInvalidateOnAnimation()
            }
        }

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
            val pulse = (0.5 + 0.5 * sin(now.toDouble() / 2600.0 * 2.0 * Math.PI)).toFloat()
            val irisR = (open * 0.72f)
            // Halo
            val glowAlpha = (pulse * 0.6f * 255).toInt()
            val haloR = irisR * 2.2f
            haloMatrix.setScale(haloR, haloR)
            haloMatrix.postTranslate(cx, cy)
            halo.setLocalMatrix(haloMatrix)
            glowPaint.shader = halo
            glowPaint.alpha = glowAlpha
            canvas.drawCircle(cx, cy, haloR, glowPaint)

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
            irisPaint.color = lerp(pulse, emeraldDeep, emeraldGem)
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
