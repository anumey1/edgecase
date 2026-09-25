package com.dicereligion.edgecase

import android.content.Context
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The hand-off contract between MainActivity and the `:overlay` process (Docs/RAMIssuePDP.md §5.3).
 *
 * These tests write to the app's live prefs file, so every key they touch is snapshotted in
 * [snapshot] and written back in [restore] — running them must not cost the user their setup.
 */
@RunWith(AndroidJUnit4::class)
class OverlaySnapshotTest {

    private lateinit var context: Context
    private lateinit var saved: Map<String, *>

    private fun prefs() =
        context.getSharedPreferences(SliverConfig.PREFS, Context.MODE_PRIVATE)

    @Before
    fun snapshot() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        saved = HashMap(prefs().all)
    }

    @After
    fun restore() {
        val e = prefs().edit().clear()
        for ((k, v) in saved) {
            @Suppress("UNCHECKED_CAST")
            when (v) {
                is String -> e.putString(k, v)
                is Float -> e.putFloat(k, v)
                is Int -> e.putInt(k, v)
                is Long -> e.putLong(k, v)
                is Boolean -> e.putBoolean(k, v)
                is Set<*> -> e.putStringSet(k, v as Set<String>)
            }
        }
        e.commit()
    }

    @Test
    fun intentRoundTripsEveryField() {
        val original = OverlaySnapshot(
            side = ArcSliverView.Side.LEFT,
            yBias = 0.27f,
            config = SliverConfig(opacity = 0.8f, colorMode = SliverConfig.ColorMode.CUSTOM, customHue = 40f,
                                  widthDp = 33f, trayHeightDp = 300f),
            shortcuts = listOf("com.a", "com.b", "com.c")
        )
        assertEquals(original, OverlaySnapshot.fromIntent(original.writeTo(Intent())))
    }

    @Test
    fun anIntentWithoutASnapshotGivesNull() {
        assertNull(OverlaySnapshot.fromIntent(Intent()))
        assertNull(OverlaySnapshot.fromIntent(null))
    }

    @Test
    fun fromPrefsReadsTheOrderedListSideAndPosition() {
        prefs().edit().clear()
            .putString("saved_shortcuts_order", "com.x,com.y,,com.z")
            .putString("sliver_side", "left")
            .putFloat("sliver_y_bias", 0.79f)
            .commit()

        val s = OverlaySnapshot.fromPrefs(context)
        assertEquals(listOf("com.x", "com.y", "com.z"), s.shortcuts)   // empty entries dropped
        assertEquals(ArcSliverView.Side.LEFT, s.side)
        assertEquals(0.79f, s.yBias, 0f)
        assertEquals(SliverConfig(), s.config)
    }

    @Test
    fun fromPrefsFallsBackToTheLegacySetWhenThereIsNoOrder() {
        prefs().edit().clear().putStringSet("saved_shortcuts", setOf("com.legacy")).commit()
        assertEquals(listOf("com.legacy"), OverlaySnapshot.fromPrefs(context).shortcuts)
    }

    @Test
    fun fromPrefsWithNothingSavedGivesAnEmptyListOnTheRightAtMidHeight() {
        prefs().edit().clear().commit()
        val s = OverlaySnapshot.fromPrefs(context)
        assertEquals(emptyList<String>(), s.shortcuts)
        assertEquals(ArcSliverView.Side.RIGHT, s.side)
        assertEquals(0.5f, s.yBias, 0f)
    }

    @Test
    fun outOfRangeVerticalPositionIsClampedAsTheServiceAlwaysDid() {
        prefs().edit().clear().putFloat("sliver_y_bias", 1.7f).commit()
        assertEquals(1f, OverlaySnapshot.fromPrefs(context).yBias, 0f)
    }
}
