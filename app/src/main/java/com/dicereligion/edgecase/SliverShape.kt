package com.dicereligion.edgecase

import android.graphics.Path

/**
 * Single source of truth for the sliver's fang path.
 *
 * Builds the 8-point profile from a [SliverConfig] so the live overlay ([ArcSliverView]) and every
 * preview ([PositioningView], [SliverPreviewView]) render identical geometry from the same knobs.
 *
 * Coordinate model (see `Docs/SliverAnatomy.md`):
 * - `u` = inward depth: 0 = the flat spine at the screen edge, 1 = deepest inward reach.
 * - `v` = vertical: 0 = top, 1 = bottom.
 *
 * Per-side conversion:  RIGHT → `x = w*(1-u)`,  LEFT → `x = w*u`,  both → `y = h*v`.
 */
object SliverShape {

    /** Rebuilds [path] as the fang profile for [side] at size [w]×[h] using [cfg]. */
    fun buildPath(path: Path, w: Float, h: Float, side: ArcSliverView.Side, cfg: SliverConfig) {
        path.reset()

        // Inner roots / gums span, symmetric about the vertical center.
        val half = (cfg.gap / 2f).coerceIn(0.01f, 0.48f)
        val v3 = 0.5f - half
        val v4 = 0.5f + half

        // Outer roots on the spine; coerced so the shape stays inside [0,1] and never inverts.
        val v1 = (v3 - cfg.tooth1Thickness).coerceIn(0f, v3 - 0.001f)
        val v6 = (v4 + cfg.tooth2Thickness).coerceIn(v4 + 0.001f, 1f)

        val tipY1 = cfg.tooth1TipY.coerceIn(0f, 1f)
        val tipY2 = cfg.tooth2TipY.coerceIn(0f, 1f)
        val len1 = cfg.tooth1Length.coerceIn(0f, 1f)
        val len2 = cfg.tooth2Length.coerceIn(0f, 1f)
        val gums = cfg.gumsDepth.coerceIn(0f, 1f)

        fun x(u: Float) = if (side == ArcSliverView.Side.RIGHT) w * (1f - u) else w * u

        path.moveTo(x(0f), 0f)           // C0  top spine corner
        path.lineTo(x(0f), h * v1)       // V1  top fang root (on spine)
        path.lineTo(x(len1), h * tipY1)  // V2  top fang tip
        path.lineTo(x(gums), h * v3)     // V3  gums top / top inner root
        path.lineTo(x(gums), h * v4)     // V4  gums bottom / bottom inner root
        path.lineTo(x(len2), h * tipY2)  // V5  bottom fang tip
        path.lineTo(x(0f), h * v6)       // V6  bottom fang root (on spine)
        path.lineTo(x(0f), h)            // C7  bottom spine corner
        path.close()
    }
}
