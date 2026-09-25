package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Bitmap
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
import kotlin.math.sin
import kotlin.random.Random

/**
 * The living temple floor: fractured obsidian with emerald gems pulsing inside the cracks.
 *
 * Static layers (obsidian facets, vignette, speckles, crack lines) are rasterised ONCE into
 * [staticLayer] on size change. Only the gem glow pulses per-frame — a handful of
 * hardware-accelerated radial-gradient circles + tiny paths. See Docs/NewTheme.md §6.
 *
 * The pulse is redrawn by the shared 12 fps [TempleClock], only while this view is actually on
 * screen, and every gem's path, shader and colours are built once per layout rather than per frame
 * (Docs/RAMIssuePDP.md Phase 3).
 */
class ObsidianCrackView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // ── Tunables (Docs/NewTheme.md §6.5) ────────────────────────────
    var crackCount = 7
    var gemCount = 11
    var seed = 20260711L          // fixed seed → identical temple on every launch
    var maxGlowAlpha = 0.55f      // keep ambient so foreground text stays readable

    // ── Static layer ────────────────────────────────────────────────
    private var staticLayer: Bitmap? = null
    private val crackPaths = mutableListOf<Path>()

    // ── Gems ────────────────────────────────────────────────────────
    private class Gem(
        val x: Float, val y: Float, val size: Float,
        val phase: Float, val periodMs: Float, val angleDeg: Float
    ) {
        /** The emerald-cut outline, already rotated and placed: static, so built once. */
        val path = Path()

        /**
         * The halo at full strength and unit radius. Each frame only scales it (local matrix) and
         * sets the paint's alpha, which multiplies the shader's — the same pixels as building a new
         * gradient with the alpha baked in, without the allocation.
         */
        val halo = RadialGradient(
            0f, 0f, 1f,
            intArrayOf(
                Color.argb(255, 0x50, 0xC8, 0x78),                // emerald_bright core
                Color.argb(102, 0x2E, 0x8B, 0x57),                // 40 % of the core's alpha
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f), Shader.TileMode.CLAMP
        )
    }
    private val gems = mutableListOf<Gem>()
    private val gemMatrix = Matrix()
    private val haloMatrix = Matrix()

    private val emeraldDeep = ContextCompat.getColor(context, R.color.emerald_deep)
    private val emeraldGem = ContextCompat.getColor(context, R.color.emerald_gem)

    // ── Paints ──────────────────────────────────────────────────────
    private val basePaint = Paint()
    private val facetPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val crackGlowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.emerald_glow_faint)
        strokeWidth = 7f
        strokeJoin = Paint.Join.MITER            // sharp — never round (Design Law L1)
    }
    private val crackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = ContextCompat.getColor(context, R.color.crack_void)
        strokeWidth = 2.5f
        strokeJoin = Paint.Join.MITER
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gemBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val gemFacetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.parseColor("#66A9F5C8")    // emerald_core @ 40%
    }

    private var subscribed = false

    // ── Lifecycle ───────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) regenerate(w, h)
    }

    override fun onAttachedToWindow() { super.onAttachedToWindow(); updatePulse() }
    override fun onDetachedFromWindow() { super.onDetachedFromWindow(); updatePulse() }
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        updatePulse()
    }

    /** Called when the Activity is stopped or started: a hidden window must not keep ticking. */
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        updatePulse()
    }

    /** Pulse only while really on screen: attached, shown (self and ancestors) and window visible. */
    private fun updatePulse() {
        val shouldPulse = isAttachedToWindow && isShown && windowVisibility == VISIBLE
        if (shouldPulse == subscribed) return
        subscribed = shouldPulse
        if (shouldPulse) TempleClock.subscribe(this) else TempleClock.unsubscribe(this)
    }

    // ── Generation (runs once per size) ─────────────────────────────

    private fun regenerate(w: Int, h: Int) {
        val rnd = Random(seed)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)

        drawBase(c, w, h, rnd)
        crackPaths.clear()
        repeat(crackCount) { crackPaths.add(generateCrack(w, h, rnd)) }
        for (p in crackPaths) { c.drawPath(p, crackGlowPaint); c.drawPath(p, crackPaint) }

        placeGems(w, h, rnd)
        staticLayer?.recycle()
        staticLayer = bmp
    }

    private fun drawBase(c: Canvas, w: Int, h: Int, rnd: Random) {
        // Obsidian body
        basePaint.color = ContextCompat.getColor(context, R.color.obsidian_black)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), basePaint)

        // 4 giant conchoidal facets — huge dim triangles
        val facetColors = intArrayOf(
            ContextCompat.getColor(context, R.color.obsidian_facet),  // obsidian_facet
            ContextCompat.getColor(context, R.color.obsidian_sheen),  // obsidian_sheen
            Color.parseColor("#0A0E0C"),
            Color.parseColor("#0D1412")
        )
        repeat(4) { i ->
            facetPaint.color = facetColors[i]
            val p = Path()
            p.moveTo(rnd.nextFloat() * w, rnd.nextFloat() * h)
            p.lineTo(rnd.nextFloat() * w, rnd.nextFloat() * h)
            p.lineTo(rnd.nextFloat() * w, rnd.nextFloat() * h)
            p.close()
            c.drawPath(p, facetPaint)
        }

        // Mineral speckle: 90 tiny flecks
        facetPaint.color = Color.parseColor("#14FFFFFF")
        repeat(90) {
            val sx = rnd.nextFloat() * w; val sy = rnd.nextFloat() * h
            c.drawRect(sx, sy, sx + 1.5f, sy + 1.5f, facetPaint)
        }

        // Vignette — darkness pooling at the edges
        val vignette = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                w / 2f, h / 2f, (w.coerceAtLeast(h)) * 0.75f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#66020403")),
                floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP
            )
        }
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), vignette)
    }

    /** Jagged random-walk from a random edge point across the screen, with one optional branch. */
    private fun generateCrack(w: Int, h: Int, rnd: Random): Path {
        val path = Path()
        // Seed on a random edge
        var x: Float; var y: Float; var dirX: Float; var dirY: Float
        when (rnd.nextInt(4)) {
            0 -> { x = rnd.nextFloat() * w; y = 0f;            dirX = rnd.nextFloat() - 0.5f; dirY = 1f }
            1 -> { x = rnd.nextFloat() * w; y = h.toFloat();   dirX = rnd.nextFloat() - 0.5f; dirY = -1f }
            2 -> { x = 0f; y = rnd.nextFloat() * h;            dirX = 1f; dirY = rnd.nextFloat() - 0.5f }
            else -> { x = w.toFloat(); y = rnd.nextFloat() * h; dirX = -1f; dirY = rnd.nextFloat() - 0.5f }
        }
        path.moveTo(x, y)
        val steps = 9 + rnd.nextInt(8)
        val stepLen = (w + h) / 2f / steps
        repeat(steps) {
            // advance with jitter — sharp kinks, never smooth curves
            x += dirX * stepLen * (0.6f + rnd.nextFloat() * 0.8f) + (rnd.nextFloat() - 0.5f) * stepLen * 0.9f
            y += dirY * stepLen * (0.6f + rnd.nextFloat() * 0.8f) + (rnd.nextFloat() - 0.5f) * stepLen * 0.9f
            path.lineTo(x, y)
            // 25% chance: a short branch splinter
            if (rnd.nextFloat() < 0.25f) {
                val bx = x + (rnd.nextFloat() - 0.5f) * stepLen * 1.6f
                val by = y + (rnd.nextFloat() - 0.5f) * stepLen * 1.6f
                path.moveTo(x, y); path.lineTo(bx, by); path.moveTo(x, y)
            }
        }
        return path
    }

    private fun placeGems(w: Int, h: Int, rnd: Random) {
        gems.clear()
        val density = resources.displayMetrics.density
        repeat(gemCount) {
            // Sample a point along a random crack via PathMeasure
            val path = crackPaths[rnd.nextInt(crackPaths.size)]
            val pm = android.graphics.PathMeasure(path, false)
            val pos = FloatArray(2)
            pm.getPosTan(pm.length * rnd.nextFloat(), pos, null)
            // Keep gems on-screen with margin
            val gx = pos[0].coerceIn(w * 0.06f, w * 0.94f)
            val gy = pos[1].coerceIn(h * 0.06f, h * 0.94f)
            val gem = Gem(
                x = gx, y = gy,
                size = (4f + rnd.nextFloat() * 5f) * density,
                phase = rnd.nextFloat(),
                periodMs = 2400f + rnd.nextFloat() * 2400f,
                angleDeg = rnd.nextFloat() * 180f
            )
            buildGemPath(gem)
            gems.add(gem)
        }
    }

    // ── Per-frame drawing ───────────────────────────────────────────

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        staticLayer?.let { canvas.drawBitmap(it, 0f, 0f, null) }

        // Wall time, so the pulse keeps its period at any redraw rate. Double: uptime in ms has
        // outgrown a Float's precision.
        val now = SystemClock.uptimeMillis().toDouble()
        for (g in gems) {
            // pulse ∈ [0,1], sinusoidal, per-gem phase & period
            val pulse = (0.5 + 0.5 * sin((now / g.periodMs + g.phase) * 2.0 * Math.PI)).toFloat()
            val glowAlpha = (pulse * maxGlowAlpha * 255).toInt()
            val glowRadius = g.size * (2.6f + 1.8f * pulse)

            // Halo (RadialGradient — fully hardware accelerated; never BlurMaskFilter)
            haloMatrix.setScale(glowRadius, glowRadius)
            haloMatrix.postTranslate(g.x, g.y)
            g.halo.setLocalMatrix(haloMatrix)
            glowPaint.shader = g.halo
            glowPaint.alpha = glowAlpha
            canvas.drawCircle(g.x, g.y, glowRadius, glowPaint)

            // Emerald-cut gem body: elongated octagon, trough emerald_deep → peak emerald_gem
            gemBodyPaint.color = lerpColor(pulse, emeraldDeep, emeraldGem)
            canvas.drawPath(g.path, gemBodyPaint)
            canvas.drawPath(g.path, gemFacetPaint)

            // Hot core pixel at peak
            if (pulse > 0.75f) {
                gemBodyPaint.color = Color.argb(
                    ((pulse - 0.75f) / 0.25f * 255).toInt(), 0xA9, 0xF5, 0xC8)  // emerald_core
                canvas.drawCircle(g.x, g.y, g.size * 0.22f, gemBodyPaint)
            }
        }
    }

    /** Elongated octagon = classic emerald cut, rotated by the gem's resting angle. */
    private fun buildGemPath(g: Gem) {
        val w = g.size; val h = g.size * 1.5f; val c = 0.30f  // corner cut fraction
        val gemPath = g.path
        gemPath.reset()
        gemPath.moveTo(-w / 2 + w * c, -h / 2)
        gemPath.lineTo(w / 2 - w * c, -h / 2)
        gemPath.lineTo(w / 2, -h / 2 + h * c)
        gemPath.lineTo(w / 2, h / 2 - h * c)
        gemPath.lineTo(w / 2 - w * c, h / 2)
        gemPath.lineTo(-w / 2 + w * c, h / 2)
        gemPath.lineTo(-w / 2, h / 2 - h * c)
        gemPath.lineTo(-w / 2, -h / 2 + h * c)
        gemPath.close()
        gemMatrix.reset()
        gemMatrix.postRotate(g.angleDeg)
        gemMatrix.postTranslate(g.x, g.y)
        gemPath.transform(gemMatrix)
    }

    private fun lerpColor(t: Float, from: Int, to: Int): Int = Color.rgb(
        (Color.red(from) + (Color.red(to) - Color.red(from)) * t).toInt(),
        (Color.green(from) + (Color.green(to) - Color.green(from)) * t).toInt(),
        (Color.blue(from) + (Color.blue(to) - Color.blue(from)) * t).toInt()
    )
}
