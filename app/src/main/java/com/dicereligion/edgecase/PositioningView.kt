package com.dicereligion.edgecase

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * A custom View that renders a scaled-down phone mockup with a draggable
 * edge-sliver preview. The sliver snaps to the left or right edge on release.
 * The top 10% and bottom 10% of the mockup are crosshatched to indicate
 * restricted placement zones.
 *
 * Position is persisted to SharedPreferences as two keys:
 * - `sliver_side` → "left" or "right"
 * - `sliver_y_bias` → float 0.0 (top of valid zone) to 1.0 (bottom)
 */
class PositioningView(context: Context, attrs: android.util.AttributeSet? = null) : View(context, attrs) {

    companion object {
        /** Fraction of mockup height reserved at top/bottom (restricted). */
        private const val RESTRICTED_FRACTION = 0.10f
        /** Snap animation duration in ms. */
        private const val SNAP_DURATION_MS = 200L
    }

    // ── Paints ─────────────────────────────────────────
    private val marblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A2822")
        style = Paint.Style.FILL
    }
    private val marbleBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3B5249")
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val crosshatchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4D3B5249") // Faded Olive Teal at ~30%
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
    }
    private val sliverGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#4DFFC0CB") // Ethereal Pink
        style = Paint.Style.FILL
    }
    private val sliverCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#60C0C0C0") // Light transparent grey
        style = Paint.Style.FILL
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(120, 154, 160, 166) // Tarnished Silver, semi-transparent
        style = Paint.Style.FILL
    }
    private val instructionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5EFE6")
        textSize = 32f
        textAlign = Paint.Align.CENTER
    }

    // ── Mockup geometry ────────────────────────────────
    /** Fraction of view width the mockup occupies. */
    private var mockupWidthFraction = 0.55f
    /** Aspect ratio of the mockup (height/width). Typical phone ~2.1. */
    private var mockupAspectRatio = 2.1f
    /** Corner radius of the mockup. */
    private var mockupCornerRadius = 40f

    // ── Sliver state ───────────────────────────────────
    /** Current side: LEFT or RIGHT edge of the mockup. */
    var sliverSide: ArcSliverView.Side = ArcSliverView.Side.RIGHT
        private set
    /** Normalised Y bias [0.0, 1.0] within the valid (non-restricted) zone. */
    var sliverYBias: Float = 0.5f
        private set
    /** Actual pixel Y position of the sliver center within the mockup. */
    private var sliverPixelY: Float = 0f
    /** Actual pixel X position of the sliver edge within the mockup. */
    private var sliverPixelX: Float = 0f

    // ── Sliver drawing dimensions ──────────────────────
    private var sliverPreviewWidth = 28f  // scaled width of preview arc
    private var sliverPreviewHeight = 70f // scaled height of preview arc

    // ── Drag state ─────────────────────────────────────
    private var isDragging = false
    private var dragOffsetY = 0f

    // ── Particle trail ─────────────────────────────────
    private val trailParticles = mutableListOf<TrailParticle>()
    private var lastDragFrameTime = 0L

    // ── Snap animation ─────────────────────────────────
    private var snapAnimator: ValueAnimator? = null
    private var snapStartX: Float = 0f
    private var snapEndX: Float = 0f

    // ── Computed mockup rect (updated in onSizeChanged) ─
    private var mockupLeft: Float = 0f
    private var mockupTop: Float = 0f
    private var mockupRight: Float = 0f
    private var mockupBottom: Float = 0f

    /** The Y range (top, bottom) where the sliver is allowed within the mockup. */
    private var validTopY: Float = 0f
    private var validBottomY: Float = 0f

    // ── Callback ───────────────────────────────────────
    var onPositionChanged: ((ArcSliverView.Side, Float) -> Unit)? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    // ── Public setters for initialising from saved prefs ──

    fun setSliverPosition(side: ArcSliverView.Side, yBias: Float) {
        sliverSide = side
        sliverYBias = yBias.coerceIn(0f, 1f)
        recalcSliverPosition()
        invalidate()
    }

    // ── Layout ─────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val mockupW = w * mockupWidthFraction
        val mockupH = mockupW * mockupAspectRatio
        mockupLeft = (w - mockupW) / 2f
        mockupTop = (h - mockupH) / 2f
        mockupRight = mockupLeft + mockupW
        mockupBottom = mockupTop + mockupH

        val restrictedHeight = mockupH * RESTRICTED_FRACTION
        validTopY = mockupTop + restrictedHeight
        validBottomY = mockupBottom - restrictedHeight

        // Scale sliver preview size based on mockup width (half height)
        sliverPreviewWidth = mockupW * 0.04f
        sliverPreviewHeight = mockupH * 0.045f
        mockupCornerRadius = mockupW * 0.04f

        recalcSliverPosition()
    }

    private fun recalcSliverPosition() {
        val validHeight = validBottomY - validTopY
        sliverPixelY = validTopY + validHeight * sliverYBias
        sliverPixelX = if (sliverSide == ArcSliverView.Side.RIGHT) mockupRight else mockupLeft
    }

    // ── Drawing ────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ── Phone mockup (dark marble slab) ────────────
        val mockupRect = RectF(mockupLeft, mockupTop, mockupRight, mockupBottom)
        canvas.drawRoundRect(mockupRect, mockupCornerRadius, mockupCornerRadius, marblePaint)
        canvas.drawRoundRect(mockupRect, mockupCornerRadius, mockupCornerRadius, marbleBorderPaint)

        // ── Restricted zones (crosshatch) ───────────────
        drawCrosshatchZone(canvas, mockupTop, mockupTop + (mockupBottom - mockupTop) * RESTRICTED_FRACTION)
        drawCrosshatchZone(canvas, mockupBottom - (mockupBottom - mockupTop) * RESTRICTED_FRACTION, mockupBottom)

        // ── Sliver preview ──────────────────────────────
        drawSliverPreview(canvas)

        // ── Particle trail ──────────────────────────────
        for (p in trailParticles) {
            particlePaint.alpha = (p.alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
        }
        particlePaint.alpha = 120 // reset

        // ── Instruction ─────────────────────────────────
        if (!isDragging && trailParticles.isEmpty()) {
            canvas.drawText(
                "Drag the sliver to reposition",
                width / 2f,
                mockupBottom + 60f,
                instructionPaint
            )
        }
    }

    private fun drawCrosshatchZone(canvas: Canvas, top: Float, bottom: Float) {
        val spacing = 12f
        val left = mockupLeft + mockupCornerRadius
        val right = mockupRight - mockupCornerRadius

        var x = left
        while (x < right) {
            canvas.drawLine(x, top, x + (bottom - top), bottom, crosshatchPaint)
            canvas.drawLine(x, bottom, x + (bottom - top), top, crosshatchPaint)
            x += spacing
        }
    }

    private fun drawSliverPreview(canvas: Canvas) {
        val halfH = sliverPreviewHeight / 2f
        val arcCenterX = sliverPixelX
        val arcCenterY = sliverPixelY

        // Outer glow arc
        val outerRect: RectF
        val innerRect: RectF
        val startAngle: Float
        val sweepAngle: Float = 160f

        if (sliverSide == ArcSliverView.Side.RIGHT) {
            outerRect = RectF(arcCenterX - 2 * sliverPreviewWidth, arcCenterY - halfH,
                arcCenterX, arcCenterY + halfH)
            innerRect = RectF(arcCenterX - 2 * (sliverPreviewWidth * 0.33f), arcCenterY - halfH,
                arcCenterX, arcCenterY + halfH)
            startAngle = 100f
        } else {
            outerRect = RectF(arcCenterX, arcCenterY - halfH,
                arcCenterX + 2 * sliverPreviewWidth, arcCenterY + halfH)
            innerRect = RectF(arcCenterX, arcCenterY - halfH,
                arcCenterX + 2 * (sliverPreviewWidth * 0.33f), arcCenterY + halfH)
            startAngle = 280f
        }

        // Draw outer glow
        canvas.drawArc(outerRect, startAngle, sweepAngle, true, sliverGlowPaint)
        // Draw inner core
        canvas.drawArc(innerRect, startAngle, sweepAngle, true, sliverCorePaint)
    }

    // ── Touch handling ─────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Cancel any running snap animation
                snapAnimator?.cancel()

                // Check if touch is near the sliver preview
                if (isTouchOnSliver(x, y)) {
                    isDragging = true
                    dragOffsetY = sliverPixelY - y
                    trailParticles.clear()
                    lastDragFrameTime = System.currentTimeMillis()
                    return true
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (!isDragging) return false

                // Update sliver Y position (clamped to valid zone)
                var newY = y + dragOffsetY
                newY = newY.coerceIn(validTopY, validBottomY)
                sliverPixelY = newY

                // Spawn trail particles
                val now = System.currentTimeMillis()
                if (now - lastDragFrameTime > 30) {
                    spawnTrailParticle(sliverPixelX, sliverPixelY)
                    lastDragFrameTime = now
                }

                // Update trail particles
                updateTrailParticles()
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!isDragging) return false
                isDragging = false

                // Snap to nearest edge
                val midX = (mockupLeft + mockupRight) / 2f
                val newSide: ArcSliverView.Side
                val targetX: Float

                if (x < midX) {
                    newSide = ArcSliverView.Side.LEFT
                    targetX = mockupLeft
                } else {
                    newSide = ArcSliverView.Side.RIGHT
                    targetX = mockupRight
                }

                // Animate snap
                snapSliverTo(sliverPixelX, targetX, newSide)
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun isTouchOnSliver(tx: Float, ty: Float): Boolean {
        val halfH = sliverPreviewHeight / 2f
        val touchSlop = 40f // generous touch target

        val inY = ty >= (sliverPixelY - halfH - touchSlop) &&
                  ty <= (sliverPixelY + halfH + touchSlop)

        val inX = if (sliverSide == ArcSliverView.Side.RIGHT) {
            tx >= (sliverPixelX - sliverPreviewWidth * 2 - touchSlop) && tx <= sliverPixelX
        } else {
            tx >= sliverPixelX && tx <= (sliverPixelX + sliverPreviewWidth * 2 + touchSlop)
        }
        return inX && inY
    }

    // ── Snap animation ─────────────────────────────────

    private fun snapSliverTo(fromX: Float, toX: Float, newSide: ArcSliverView.Side) {
        snapStartX = fromX
        snapEndX = toX

        snapAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = SNAP_DURATION_MS
            interpolator = DecelerateInterpolator()
            addUpdateListener { anim ->
                val fraction = anim.animatedValue as Float
                sliverPixelX = snapStartX + (snapEndX - snapStartX) * fraction
                spawnTrailParticle(sliverPixelX, sliverPixelY)
                updateTrailParticles()
                invalidate()
            }
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(a: android.animation.Animator) {}
                override fun onAnimationEnd(a: android.animation.Animator) {
                    sliverSide = newSide
                    sliverPixelX = snapEndX
                    // Update yBias from pixel position
                    val validHeight = validBottomY - validTopY
                    sliverYBias = ((sliverPixelY - validTopY) / validHeight).coerceIn(0f, 1f)
                    trailParticles.clear()
                    invalidate()
                    // Notify listener
                    onPositionChanged?.invoke(sliverSide, sliverYBias)
                }
                override fun onAnimationCancel(a: android.animation.Animator) {}
                override fun onAnimationRepeat(a: android.animation.Animator) {}
            })
            start()
        }
    }

    // ── Particle trail ─────────────────────────────────

    private data class TrailParticle(
        var x: Float, var y: Float, var radius: Float, var alpha: Float,
        var vx: Float, var vy: Float
    )

    private fun spawnTrailParticle(x: Float, y: Float) {
        val vx = (Math.random().toFloat() - 0.5f) * 4f
        val vy = (Math.random().toFloat() - 0.5f) * 4f
        trailParticles.add(TrailParticle(x, y, 2f + Math.random().toFloat() * 3f, 0.6f, vx, vy))
        // Limit trail length
        while (trailParticles.size > 20) {
            trailParticles.removeAt(0)
        }
    }

    private fun updateTrailParticles() {
        val iter = trailParticles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx
            p.y += p.vy
            p.alpha -= 0.03f
            p.radius *= 0.96f
            if (p.alpha <= 0f) {
                iter.remove()
            }
        }
    }

    // ── Lifecycle cleanup ──────────────────────────────

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        snapAnimator?.cancel()
        trailParticles.clear()
    }
}
