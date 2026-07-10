package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

/**
 * A static, scaled preview of the sliver fang for the Customize dialog. Draws [SliverShape] with the
 * supplied [config] centered and scaled to fit, mirroring the saved edge [side]. Call [setConfig] on
 * every edit for instant feedback.
 */
class SliverPreviewView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    private var config: SliverConfig = SliverConfig()
    var side: ArcSliverView.Side = ArcSliverView.Side.RIGHT

    private val path = Path()
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    fun setConfig(cfg: SliverConfig) {
        config = cfg
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        // Fit the fang within the view, preserving the config's H/W aspect, with a margin.
        val margin = 0.15f
        val availW = width * (1f - 2f * margin)
        val availH = height * (1f - 2f * margin)
        val aspect = config.heightDp / config.widthDp
        var shapeH = availH
        var shapeW = shapeH / aspect
        if (shapeW > availW) {
            shapeW = availW
            shapeH = shapeW * aspect
        }
        val left = (width - shapeW) / 2f
        val top = (height - shapeH) / 2f

        canvas.save()
        canvas.translate(left, top)
        SliverShape.buildPath(path, shapeW, shapeH, side, config)
        fillPaint.color = config.fillColor()
        canvas.drawPath(path, fillPaint)
        canvas.restore()
    }
}
