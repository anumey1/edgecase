package com.dicereligion.edgecase

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

/**
 * A custom edge-sliver View that renders a dual-layered semicircular arc:
 * - Inner arc: Serpent Emerald (#2E8B57), ~60% of original sliver width
 * - Outer arc: Ethereal Pink (#4DFFC0CB), 3× wider, pulses alpha 20%–30%
 *
 * The semicircle is slightly off-center (160° sweep instead of 180°)
 * giving it an asymmetric, organic arc profile. The flat edge of the
 * arc aligns with the screen edge. The arc bows inward toward the
 * center of the screen.
 *
 * @param context        Android context
 * @param side           Which screen edge this sliver sits on
 * @param onSwipeListener Callback invoked when a valid swipe gesture is detected
 */
class ArcSliverView(
    context: Context,
    var side: Side = Side.RIGHT,
    private val onSwipeListener: (() -> Unit)? = null
) : View(context) {

    enum class Side { LEFT, RIGHT }

    // ── dp → px conversion ──────────────────────────────
    private val density: Float = context.resources.displayMetrics.density

    /** Inner arc width from screen edge (60% of original 15dp) */
    private val innerArcRadiusPx: Float = (9f * density)

    /** Outer arc width from screen edge (3× inner arc) */
    private val outerArcRadiusPx: Float = (27f * density)

    /** Total view height (half previous: 38dp) */
    private val sliverHeightPx: Float = (38f * density)

    // ── Arc geometry (configurable for off-center effect) ──
    /** Start angle in degrees for the arc sweep (100° = slightly below top) */
    private val arcStartAngle: Float = 100f

    /** Sweep angle in degrees (160° instead of 180° for off-center look) */
    private val arcSweepAngle: Float = 160f

    // ── Paints ───────────────────────────────────────────
    private val innerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60C0C0C0") // Light transparent grey
        style = Paint.Style.FILL
    }

    private val outerArcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    /** Current alpha fraction for the outer arc (pulses 0.20–0.30) */
    private var outerArcAlphaFraction: Float = 0.30f

    // ── Pulse animation ──────────────────────────────────
    private val pulseAnimator: ValueAnimator = ValueAnimator.ofFloat(0.20f, 0.30f).apply {
        duration = 4000L
        repeatCount = ValueAnimator.INFINITE
        repeatMode = ValueAnimator.REVERSE
        addUpdateListener { anim ->
            outerArcAlphaFraction = anim.animatedValue as Float
            outerArcPaint.color = Color.argb(
                (outerArcAlphaFraction * 255f).toInt(),
                0xFF, 0xC0, 0xCB
            )
            invalidate()
        }
    }

    // ── Touch / swipe tracking ──────────────────────────
    private var startRawX = 0f
    private var startRawY = 0f
    private var trackingSwipe = false

    companion object {
        /** Minimum horizontal drag (px) to qualify as a swipe */
        const val SWIPE_THRESHOLD_X = 30f

        /** Maximum vertical deviation (px) during a horizontal swipe */
        const val MAX_SWIPE_DEVIATION_Y = 150f
    }

    init {
        // Ethereal Pink base color with dynamic alpha
        outerArcPaint.color = Color.argb(
            (outerArcAlphaFraction * 255f).toInt(),
            0xFF, 0xC0, 0xCB
        )
        setBackgroundColor(Color.TRANSPARENT)
    }

    // ── Measurement ──────────────────────────────────────

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = resolveSize(outerArcRadiusPx.toInt(), widthMeasureSpec)
        val h = resolveSize(sliverHeightPx.toInt(), heightMeasureSpec)
        setMeasuredDimension(w, h)
    }

    // ── Drawing ──────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewW = width.toFloat()
        val viewH = height.toFloat()

        // Build the oval rect used for both arcs.
        // The arc center is positioned at the screen-edge side of the view.
        // For RIGHT side: oval's right edge = view right; arc bows left.
        // For LEFT  side: oval's left edge  = view left;  arc bows right.
        val innerOval: RectF
        val outerOval: RectF

        if (side == Side.RIGHT) {
            // Arc center at (viewW, viewH/2) — right edge of view
            innerOval = RectF(
                viewW - 2f * innerArcRadiusPx, 0f,
                viewW, viewH
            )
            outerOval = RectF(
                viewW - 2f * outerArcRadiusPx, 0f,
                viewW, viewH
            )
            // Draw outer first (behind), then inner (on top)
            canvas.drawArc(outerOval, arcStartAngle, arcSweepAngle, true, outerArcPaint)
            canvas.drawArc(innerOval, arcStartAngle, arcSweepAngle, true, innerArcPaint)
        } else {
            // Arc center at (0f, viewH/2) — left edge of view
            innerOval = RectF(
                0f, 0f,
                2f * innerArcRadiusPx, viewH
            )
            outerOval = RectF(
                0f, 0f,
                2f * outerArcRadiusPx, viewH
            )
            // For left side the arc bows rightward: sweep from 280° through 0° to 80°
            val leftStartAngle = arcStartAngle + 180f // mirror: 100° → 280°
            canvas.drawArc(outerOval, leftStartAngle, arcSweepAngle, true, outerArcPaint)
            canvas.drawArc(innerOval, leftStartAngle, arcSweepAngle, true, innerArcPaint)
        }
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

                // Determine swipe direction based on which side the sliver is on
                val swipeDeltaX = if (side == Side.RIGHT) {
                    startRawX - rawX  // positive when swiping LEFT (inward from right edge)
                } else {
                    rawX - startRawX  // positive when swiping RIGHT (inward from left edge)
                }
                val deltaY = abs(rawY - startRawY)

                if (swipeDeltaX > SWIPE_THRESHOLD_X && deltaY < MAX_SWIPE_DEVIATION_Y) {
                    trackingSwipe = false
                    onSwipeListener?.invoke()
                    return true
                }
                return deltaY < MAX_SWIPE_DEVIATION_Y // keep tracking if Y is within bounds
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                trackingSwipe = false
                // Consume the event to prevent it falling through
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    // ── Lifecycle ────────────────────────────────────────

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        pulseAnimator.start()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator.cancel()
    }
}
