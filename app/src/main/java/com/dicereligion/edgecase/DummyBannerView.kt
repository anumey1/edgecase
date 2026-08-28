package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View

/**
 * A stand-in for the real banner `AdView`, used to preview the Plinth's look, footprint, and
 * inertness before any ad SDK is wired up. It requests no network, needs no AdMob account, and
 * adds no dependencies.
 *
 * **It is deliberately drawn light, not in the EdgeCase palette.** Real ad creatives are supplied
 * by advertisers and are overwhelmingly bright; theming this placeholder to match the temple would
 * give a misleading impression of how the band actually reads against the obsidian frame.
 *
 * It reports its own measured size on screen, which is the quickest way to answer "how much room
 * does this actually cost?" — see [AdHost] for where that size comes from.
 *
 * Inert by construction: not clickable, not focusable, no touch handling, no animation. That
 * mirrors the real banner's surroundings (Docs/Ads.md §6.5) — though note the *real* ad will of
 * course be tappable; only the stone around it stays dead.
 *
 * Delete this class when the real SDK lands (Docs/Ads.md §7.3).
 */
class DummyBannerView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density

    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FAFAF7")
        style = Paint.Style.FILL
    }
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C9C4BA")
        style = Paint.Style.STROKE
        strokeWidth = 1f * density
    }
    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3A3A38")
        textAlign = Paint.Align.CENTER
        textSize = 15f * density
    }
    private val sizePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7A756C")
        textAlign = Paint.Align.CENTER
        textSize = 12f * density
    }
    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1A73E8")
        style = Paint.Style.FILL
    }
    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = 9f * density
    }

    private val badgePath = Path()

    init {
        isClickable = false
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return

        // Creative face
        canvas.drawRect(0f, 0f, w, h, facePaint)
        val inset = edgePaint.strokeWidth / 2f
        canvas.drawRect(inset, inset, w - inset, h - inset, edgePaint)

        // Centred label: what this is, and exactly how big it is
        val cx = w / 2f
        val cy = h / 2f
        canvas.drawText("DUMMY BANNER", cx, cy - 3f * density, titlePaint)
        val wDp = (w / density).toInt()
        val hDp = (h / density).toInt()
        canvas.drawText("$wDp × $hDp dp", cx, cy + 15f * density, sizePaint)

        // A stand-in for the AdChoices marker real banners carry in a top corner —
        // included so the preview accounts for the space it occupies.
        drawAdChoicesStandIn(canvas, w)
    }

    /** Small blue corner tag, roughly where Google places AdChoices on a served banner. */
    private fun drawAdChoicesStandIn(canvas: Canvas, w: Float) {
        val tagW = 26f * density
        val tagH = 13f * density
        badgePath.reset()
        badgePath.moveTo(w - tagW, 0f)
        badgePath.lineTo(w, 0f)
        badgePath.lineTo(w, tagH)
        badgePath.lineTo(w - tagW + tagH * 0.5f, tagH)
        badgePath.close()
        canvas.drawPath(badgePath, badgeBgPaint)
        canvas.drawText("Ad", w - tagW / 2f + 2f * density, tagH * 0.78f, badgeTextPaint)
    }
}
