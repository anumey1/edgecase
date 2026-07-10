package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * Side-aligned sliver anchored to the screen edge. A single continuous
 * shape with two fang-like protrusions on the inward-facing edge.
 *
 * The right edge is a flat vertical line flush with the screen edge.
 * The left edge has two sharp fangs (at 25% and 75% height) with a
 * V-shaped central recess between them, connected by smooth quadratic
 * curves at the top and bottom.
 */
class ArcSliverView(
    context: Context,
    var side: Side = Side.RIGHT,
    var config: SliverConfig = SliverConfig(),
    private val onSwipeListener: (() -> Unit)? = null
) : View(context) {

    enum class Side { LEFT, RIGHT }

    // ── Path ─────────────────────────────────────────────
    private val fangPath = Path()

    // ── Paints ───────────────────────────────────────────
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = config.fillColor()
        style = Paint.Style.FILL
    }

    // ── Touch / swipe tracking ──────────────────────────
    private var startRawX = 0f
    private var startRawY = 0f
    private var trackingSwipe = false

    companion object {
        const val SWIPE_THRESHOLD_X = 30f
        const val MAX_SWIPE_DEVIATION_Y = 150f
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    // ── Sizing ───────────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val density = resources.displayMetrics.density
        val w = resolveSize((config.widthDp * density).toInt(), widthMeasureSpec)
        val h = resolveSize((config.heightDp * density).toInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        buildPath(w.toFloat(), h.toFloat())
    }

    /** Rebuilds the fang path using the current view dimensions and [config]. */
    private fun buildPath(W: Float, H: Float) {
        SliverShape.buildPath(fangPath, W, H, side, config)
    }

    /** Update appearance/geometry in place (no view recreation), then re-measure and redraw. */
    fun applyConfig(newConfig: SliverConfig, newSide: Side) {
        config = newConfig
        side = newSide
        fillPaint.color = config.fillColor()
        if (width > 0 && height > 0) buildPath(width.toFloat(), height.toFloat())
        requestLayout()
        invalidate()
    }

    // ── Drawing ──────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(fangPath, fillPaint)
    }

    // ── Touch / swipe detection ──────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startRawX = event.rawX
                startRawY = event.rawY
                trackingSwipe = true
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!trackingSwipe) return false
                val rawX = event.rawX
                val rawY = event.rawY
                val swipeDeltaX = if (side == Side.RIGHT) {
                    startRawX - rawX
                } else {
                    rawX - startRawX
                }
                val deltaY = abs(rawY - startRawY)
                if (swipeDeltaX > SWIPE_THRESHOLD_X && deltaY < MAX_SWIPE_DEVIATION_Y) {
                    trackingSwipe = false
                    onSwipeListener?.invoke()
                    return true
                }
                return deltaY < MAX_SWIPE_DEVIATION_Y
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                trackingSwipe = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

}
