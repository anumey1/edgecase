package com.dicereligion.edgecase

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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
    // Fill color is set per-frame from sliverConfig.fillColor() in drawSliverPreview().
    private val sliverCorePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
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
    /** Fraction of view width the mockup may occupy (§12.1 — side gaps come from layout margins). */
    private var mockupWidthFraction = 0.98f
    /** Fraction of view height the mockup may occupy (§12.1). */
    private var mockupHeightFraction = 0.98f
    /** Aspect ratio of the mockup (height/width). Typical phone ~2.1. */
    private var mockupAspectRatio = 2.1f
    /** Corner radius of the mockup. */
    private var mockupCornerRadius = 0f

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
    private var sliverPreviewWidth = 28f  // scaled width of preview
    private var sliverPreviewHeight = 38f // scaled height of preview

    /** Reusable path for the preview fang (built via [SliverShape]). */
    private val previewPath = Path()
    /** Current sliver appearance/geometry — drives the preview's shape, color, and aspect. */
    var sliverConfig: SliverConfig = SliverConfig()
        private set
    /** Cached mockup width so preview size can be recomputed when the config changes. */
    private var mockupWpx = 0f

    // ── Tracking arrow (The Herald, §12.2) ─────────────
    private val density = resources.displayMetrics.density
    private val arrowFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#50C878")     // emerald_bright
        style = Paint.Style.FILL
    }
    private val arrowOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#071A15")
        style = Paint.Style.STROKE
        strokeWidth = 1.5f
        strokeJoin = Paint.Join.MITER
    }
    private val arrowPath = Path()

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
        // Fit-inside sizing: on tall phones the height cap binds; on short/wide screens the width
        // cap binds. The 2.1 aspect is preserved either way (§12.1).
        val mockupW = minOf(w * mockupWidthFraction, (h * mockupHeightFraction) / mockupAspectRatio)
        val mockupH = mockupW * mockupAspectRatio
        mockupLeft = (w - mockupW) / 2f
        mockupTop = (h - mockupH) / 2f
        mockupRight = mockupLeft + mockupW
        mockupBottom = mockupTop + mockupH

        val restrictedHeight = mockupH * RESTRICTED_FRACTION
        validTopY = mockupTop + restrictedHeight
        validBottomY = mockupBottom - restrictedHeight

        // Scale sliver preview size based on mockup width + the current config aspect
        mockupWpx = mockupW
        recalcPreviewSize()
        mockupCornerRadius = 0f

        recalcSliverPosition()
    }

    /** Recompute the on-screen preview size so width/height edits change its aspect. */
    private fun recalcPreviewSize() {
        val previewScale = (mockupWpx * 0.04f) / SliverConfig.DEF_WIDTH_DP
        sliverPreviewWidth = previewScale * sliverConfig.widthDp
        sliverPreviewHeight = previewScale * sliverConfig.heightDp
    }

    /** Apply a new sliver appearance/geometry; refreshes the preview immediately. */
    fun setSliverConfig(cfg: SliverConfig) {
        sliverConfig = cfg
        recalcPreviewSize()
        recalcSliverPosition()
        invalidate()
    }

    private fun recalcSliverPosition() {
        val validHeight = validBottomY - validTopY
        sliverPixelY = validTopY + validHeight * sliverYBias
        sliverPixelX = if (sliverSide == ArcSliverView.Side.RIGHT) mockupRight else mockupLeft
    }

    // ── Drawing ────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // ── Marble stele slab (square, §11 #4) ──────────
        val mockupRect = RectF(mockupLeft, mockupTop, mockupRight, mockupBottom)
        canvas.drawRoundRect(mockupRect, mockupCornerRadius, mockupCornerRadius, marblePaint)
        canvas.drawRoundRect(mockupRect, mockupCornerRadius, mockupCornerRadius, marbleBorderPaint)

        // ── Chiseled 2-step pediment top (temple-gable hint) ──
        drawSteleCornice(canvas)

        // ── Restricted zones (Greek-key hatching) ───────
        drawGreekKeyZone(canvas, mockupTop, mockupTop + (mockupBottom - mockupTop) * RESTRICTED_FRACTION)
        drawGreekKeyZone(canvas, mockupBottom - (mockupBottom - mockupTop) * RESTRICTED_FRACTION, mockupBottom)

        // ── Sliver preview ──────────────────────────────
        drawSliverPreview(canvas)

        // ── Tracking arrow (visible even at 0% sliver opacity, §12.2) ──
        drawTrackingArrow(canvas)

        // ── Particle trail ──────────────────────────────
        for (p in trailParticles) {
            particlePaint.alpha = (p.alpha * 255).toInt()
            canvas.drawCircle(p.x, p.y, p.radius, particlePaint)
        }
        particlePaint.alpha = 120 // reset

        // ── Instruction ─────────────────────────────────
        // Drawn inside the bottom restricted (crosshatch) zone — the expanded canvas leaves no
        // room below the mockup, and text over hatching reads as carved signage (§12.1).
        if (!isDragging && trailParticles.isEmpty()) {
            val restrictedH = (mockupBottom - mockupTop) * RESTRICTED_FRACTION
            canvas.drawText(
                "Drag the sliver to reposition",
                width / 2f,
                mockupBottom - restrictedH / 2f + instructionPaint.textSize / 3f,  // optically centered
                instructionPaint
            )
        }
    }

    /** A chiseled 2-step pediment (gable + cornice ledge) across the stele's top (§11 #4). */
    private fun drawSteleCornice(canvas: Canvas) {
        val cx = (mockupLeft + mockupRight) / 2f
        val inset = (mockupRight - mockupLeft) * 0.14f
        val stepY = 9f * density

        // Shallow gable: left ledge → apex → right ledge
        val gable = Path()
        gable.moveTo(mockupLeft + inset, mockupTop + stepY)
        gable.lineTo(cx, mockupTop + 3f)
        gable.lineTo(mockupRight - inset, mockupTop + stepY)
        canvas.drawPath(gable, marbleBorderPaint)

        // Cornice ledge below the gable
        canvas.drawLine(
            mockupLeft + inset * 0.5f, mockupTop + stepY * 1.9f,
            mockupRight - inset * 0.5f, mockupTop + stepY * 1.9f,
            marbleBorderPaint
        )
    }

    /** Restricted zone rendered as a Greek-key (meander) hatch band — carved signage (§11 #4). */
    private fun drawGreekKeyZone(canvas: Canvas, top: Float, bottom: Float) {
        val zoneH = bottom - top
        val s = zoneH * 0.5f                       // key unit size
        if (s <= 0f) return
        val yTop = top + (zoneH - s) / 2f
        val left = mockupLeft + 4f
        val right = mockupRight - 4f

        val key = Path()
        var x = left
        while (x + s <= right) {
            // A square-spiral hook — the Greek-key unit (all straight lines, Law L1)
            key.moveTo(x, yTop + s)
            key.lineTo(x, yTop)
            key.lineTo(x + s, yTop)
            key.lineTo(x + s, yTop + s * 0.66f)
            key.lineTo(x + s * 0.34f, yTop + s * 0.66f)
            key.lineTo(x + s * 0.34f, yTop + s * 0.34f)
            x += s * 1.3f
        }
        canvas.drawPath(key, crosshatchPaint)
    }

    private fun drawSliverPreview(canvas: Canvas) {
        val W = sliverPreviewWidth
        val H = sliverPreviewHeight
        val cx = sliverPixelX
        val cy = sliverPixelY

        // Translate so the sliver is centred at (cx, cy)
        canvas.save()
        canvas.translate(cx - (if (sliverSide == ArcSliverView.Side.RIGHT) W else 0f), cy - H / 2f)

        SliverShape.buildPath(previewPath, W, H, sliverSide, sliverConfig)
        sliverCorePaint.color = sliverConfig.fillColor()
        canvas.drawPath(previewPath, sliverCorePaint)

        canvas.restore()
    }

    /** X of the sliver's inward-most point (deepest fang tip) at the current preview scale. */
    private fun sliverInwardX(): Float {
        val reach = sliverPreviewWidth *
            maxOf(sliverConfig.tooth1Length, sliverConfig.tooth2Length).coerceIn(0f, 1f)
        return if (sliverSide == ArcSliverView.Side.RIGHT) sliverPixelX - reach else sliverPixelX + reach
    }

    private fun drawTrackingArrow(canvas: Canvas) {
        val gap = 20f * density
        val headLen = 10f * density; val headHalf = 8f * density
        val tailLen = 12f * density; val tailHalf = 3f * density
        val dir = if (sliverSide == ArcSliverView.Side.RIGHT) 1f else -1f   // +x = rightward
        val tipX = sliverInwardX() - dir * gap                              // 20dp inward of the fangs
        val y = sliverPixelY

        arrowPath.reset()
        arrowPath.moveTo(tipX, y)                                           // tip aims at the sliver
        arrowPath.lineTo(tipX - dir * headLen, y - headHalf)
        arrowPath.lineTo(tipX - dir * headLen, y - tailHalf)
        arrowPath.lineTo(tipX - dir * (headLen + tailLen), y - tailHalf)
        arrowPath.lineTo(tipX - dir * (headLen + tailLen), y + tailHalf)
        arrowPath.lineTo(tipX - dir * headLen, y + tailHalf)
        arrowPath.lineTo(tipX - dir * headLen, y + headHalf)
        arrowPath.close()
        canvas.drawPath(arrowPath, arrowFillPaint)
        canvas.drawPath(arrowPath, arrowOutlinePaint)
    }

    /** Generous hit-test around the arrow so it works as a drag handle. */
    private fun isTouchOnArrow(tx: Float, ty: Float): Boolean {
        val gap = 20f * density
        val len = 22f * density          // head + tail
        val slop = 20f * density         // finger-friendly
        val dir = if (sliverSide == ArcSliverView.Side.RIGHT) 1f else -1f
        val tipX = sliverInwardX() - dir * gap
        val nearX = minOf(tipX, tipX - dir * len) - slop
        val farX = maxOf(tipX, tipX - dir * len) + slop
        val halfH = 8f * density + slop
        return tx in nearX..farX && ty >= sliverPixelY - halfH && ty <= sliverPixelY + halfH
    }

    // ── Touch handling ─────────────────────────────────

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Cancel any running snap animation
                snapAnimator?.cancel()

                // Check if touch is near the sliver preview or its tracking arrow (§12.2)
                if (isTouchOnSliver(x, y) || isTouchOnArrow(x, y)) {
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
