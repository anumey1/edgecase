package com.dicereligion.edgecase

import android.content.Context
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Persistence and colour contract for [SliverConfig].
 *
 * Instrumented because the class reads and writes real `SharedPreferences` and uses
 * `android.graphics.Color`.
 *
 * These tests write to the app's live prefs file, so the whole config is snapshotted in [snapshot]
 * and written back in [restore] — running them must not cost the user their customisations.
 */
@RunWith(AndroidJUnit4::class)
class SliverConfigTest {

    private lateinit var context: Context
    private lateinit var saved: SliverConfig

    private fun prefs() =
        context.getSharedPreferences(SliverConfig.PREFS, Context.MODE_PRIVATE)

    @Before
    fun snapshot() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        saved = SliverConfig.load(context)
    }

    @After
    fun restore() {
        saved.save(context)
    }

    @Test
    fun saveThenLoad_roundTripsEveryField() {
        val original = SliverConfig(
            opacity = 0.31f,
            colorMode = SliverConfig.ColorMode.CUSTOM,
            customHue = 123f,
            tooth1Thickness = 0.21f, tooth2Thickness = 0.22f,
            tooth1Length = 0.71f, tooth2Length = 0.72f,
            tooth1TipY = 0.11f, tooth2TipY = 0.91f,
            gumsDepth = 0.33f, gap = 0.55f,
            widthDp = 41f, heightDp = 57f,
            trayWidthDp = 111f, trayHeightDp = 222f
        )
        original.save(context)

        assertEquals(original, SliverConfig.load(context))
    }

    @Test
    fun defaultsReproduceTheOriginalAppearance() {
        val d = SliverConfig()
        assertEquals(0.5f, d.opacity, 0f)
        assertEquals(SliverConfig.ColorMode.DEFAULT, d.colorMode)
        assertEquals(27f, d.widthDp, 0f)
        assertEquals(38f, d.heightDp, 0f)
        assertEquals(80f, d.trayWidthDp, 0f)
        assertEquals(266f, d.trayHeightDp, 0f)
        // The documented invariant: the default drawer height is the legacy sliver-height x7.
        assertEquals(d.heightDp * 7f, d.trayHeightDp, 0f)
    }

    @Test
    fun missingDrawerHeight_isSeededFromTheLegacyTimesSevenFormula() {
        // Simulate an install from before the drawer was configurable: a custom sliver height,
        // and no tray_height_dp key at all.
        prefs().edit()
            .putFloat("sliver_height_dp", 50f)
            .remove("tray_height_dp")
            .apply()

        assertEquals(350f, SliverConfig.load(context).trayHeightDp, 0f)
    }

    @Test
    fun onceDrawerHeightExists_theLegacyCouplingIsSevered() {
        prefs().edit()
            .putFloat("sliver_height_dp", 50f)
            .putFloat("tray_height_dp", 180f)
            .apply()

        assertEquals(180f, SliverConfig.load(context).trayHeightDp, 0f)
    }

    @Test
    fun fillColorAppliesOpacityAsAlpha() {
        val c = SliverConfig(opacity = 1f)
        assertEquals(255, Color.alpha(c.fillColor()))
        assertEquals(SliverConfig.DEFAULT_GREY, c.baseColor())

        assertEquals(0, Color.alpha(SliverConfig(opacity = 0f).fillColor()))
        assertEquals(127, Color.alpha(SliverConfig(opacity = 0.5f).fillColor()))
    }

    @Test
    fun fillColorClampsOpacityOutsideZeroToOne() {
        assertEquals(255, Color.alpha(SliverConfig(opacity = 9f).fillColor()))
        assertEquals(0, Color.alpha(SliverConfig(opacity = -9f).fillColor()))
    }

    @Test
    fun customModeUsesTheHueAndKeepsTheRgbOfTheBaseColor() {
        val custom = SliverConfig(colorMode = SliverConfig.ColorMode.CUSTOM, customHue = 0f)
        assertEquals(Color.RED, custom.baseColor())

        // Out-of-range hues are coerced rather than throwing or wrapping unpredictably.
        val low = SliverConfig(colorMode = SliverConfig.ColorMode.CUSTOM, customHue = -50f)
        val high = SliverConfig(colorMode = SliverConfig.ColorMode.CUSTOM, customHue = 999f)
        assertEquals(Color.RED, low.baseColor())
        assertEquals(Color.HSVToColor(floatArrayOf(360f, 1f, 1f)), high.baseColor())
    }

    @Test
    fun unreadableColorModeFallsBackToDefaultInsteadOfThrowing() {
        prefs().edit().putString("sliver_color_mode", "NOT_A_MODE").apply()
        assertEquals(SliverConfig.ColorMode.DEFAULT, SliverConfig.load(context).colorMode)
    }
}
