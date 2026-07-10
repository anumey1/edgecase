package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Color

/**
 * All user-customizable properties of the sliver: color, opacity, fang geometry, and size.
 *
 * Persisted in the shared `"EdgeCasePrefs"` SharedPreferences. Every default reproduces the original
 * hardcoded appearance, so an install that never opens the Customize dialog looks exactly as before.
 *
 * Geometry fields use the normalized fang model documented in `Docs/SliverAnatomy.md`
 * (u = inward depth 0..1, v = vertical 0..1). See [SliverShape] for how they become a path.
 */
data class SliverConfig(
    var opacity: Float = DEF_OPACITY,
    var colorMode: ColorMode = ColorMode.DEFAULT,
    var customHue: Float = DEF_HUE,
    var tooth1Thickness: Float = DEF_T1_THICKNESS,
    var tooth2Thickness: Float = DEF_T2_THICKNESS,
    var tooth1Length: Float = DEF_T1_LENGTH,
    var tooth2Length: Float = DEF_T2_LENGTH,
    var tooth1TipY: Float = DEF_T1_TIPY,
    var tooth2TipY: Float = DEF_T2_TIPY,
    var gumsDepth: Float = DEF_GUMS_DEPTH,
    var gap: Float = DEF_GAP,
    var widthDp: Float = DEF_WIDTH_DP,
    var heightDp: Float = DEF_HEIGHT_DP
) {
    enum class ColorMode { DEFAULT, CUSTOM }

    /** Opaque base RGB (no alpha): the default grey, or a fully-saturated hue. */
    fun baseColor(): Int = when (colorMode) {
        ColorMode.DEFAULT -> DEFAULT_GREY
        ColorMode.CUSTOM -> Color.HSVToColor(floatArrayOf(customHue.coerceIn(0f, 360f), 1f, 1f))
    }

    /** The base color with [opacity] applied as its alpha channel — what the sliver is actually filled with. */
    fun fillColor(): Int {
        val base = baseColor()
        val a = (opacity.coerceIn(0f, 1f) * 255f).toInt()
        return Color.argb(a, Color.red(base), Color.green(base), Color.blue(base))
    }

    fun save(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat(K_OPACITY, opacity)
            .putString(K_COLOR_MODE, colorMode.name)
            .putFloat(K_HUE, customHue)
            .putFloat(K_T1_THICK, tooth1Thickness)
            .putFloat(K_T2_THICK, tooth2Thickness)
            .putFloat(K_T1_LEN, tooth1Length)
            .putFloat(K_T2_LEN, tooth2Length)
            .putFloat(K_T1_TIPY, tooth1TipY)
            .putFloat(K_T2_TIPY, tooth2TipY)
            .putFloat(K_GUMS, gumsDepth)
            .putFloat(K_GAP, gap)
            .putFloat(K_WIDTH, widthDp)
            .putFloat(K_HEIGHT, heightDp)
            .apply()
    }

    companion object {
        const val PREFS = "EdgeCasePrefs"
        val DEFAULT_GREY: Int = Color.parseColor("#808080")

        // ── Defaults (reproduce the original shape/appearance) ──
        const val DEF_OPACITY = 0.5f
        const val DEF_HUE = 210f
        const val DEF_T1_THICKNESS = 0.114f
        const val DEF_T2_THICKNESS = 0.113f
        const val DEF_T1_LENGTH = 0.60f
        const val DEF_T2_LENGTH = 0.60f
        const val DEF_T1_TIPY = 0.20f
        const val DEF_T2_TIPY = 0.80f
        const val DEF_GUMS_DEPTH = 0.07f
        const val DEF_GAP = 0.44f
        const val DEF_WIDTH_DP = 27f
        const val DEF_HEIGHT_DP = 38f

        private const val K_OPACITY = "sliver_opacity"
        private const val K_COLOR_MODE = "sliver_color_mode"
        private const val K_HUE = "sliver_color_hue"
        private const val K_T1_THICK = "sliver_t1_thickness"
        private const val K_T2_THICK = "sliver_t2_thickness"
        private const val K_T1_LEN = "sliver_t1_length"
        private const val K_T2_LEN = "sliver_t2_length"
        private const val K_T1_TIPY = "sliver_t1_tipy"
        private const val K_T2_TIPY = "sliver_t2_tipy"
        private const val K_GUMS = "sliver_gums_depth"
        private const val K_GAP = "sliver_gap"
        private const val K_WIDTH = "sliver_width_dp"
        private const val K_HEIGHT = "sliver_height_dp"

        fun load(context: Context): SliverConfig {
            val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val mode = try {
                ColorMode.valueOf(p.getString(K_COLOR_MODE, ColorMode.DEFAULT.name) ?: ColorMode.DEFAULT.name)
            } catch (_: Exception) {
                ColorMode.DEFAULT
            }
            return SliverConfig(
                opacity = p.getFloat(K_OPACITY, DEF_OPACITY),
                colorMode = mode,
                customHue = p.getFloat(K_HUE, DEF_HUE),
                tooth1Thickness = p.getFloat(K_T1_THICK, DEF_T1_THICKNESS),
                tooth2Thickness = p.getFloat(K_T2_THICK, DEF_T2_THICKNESS),
                tooth1Length = p.getFloat(K_T1_LEN, DEF_T1_LENGTH),
                tooth2Length = p.getFloat(K_T2_LEN, DEF_T2_LENGTH),
                tooth1TipY = p.getFloat(K_T1_TIPY, DEF_T1_TIPY),
                tooth2TipY = p.getFloat(K_T2_TIPY, DEF_T2_TIPY),
                gumsDepth = p.getFloat(K_GUMS, DEF_GUMS_DEPTH),
                gap = p.getFloat(K_GAP, DEF_GAP),
                widthDp = p.getFloat(K_WIDTH, DEF_WIDTH_DP),
                heightDp = p.getFloat(K_HEIGHT, DEF_HEIGHT_DP)
            )
        }
    }
}
