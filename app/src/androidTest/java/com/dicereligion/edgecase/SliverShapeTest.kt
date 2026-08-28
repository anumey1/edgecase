package com.dicereligion.edgecase

import android.graphics.Path
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Geometry contract for [SliverShape].
 *
 * Instrumented rather than a plain JVM test because `android.graphics.Path` is a framework class —
 * it is not available to unqualified unit tests, and stubbing it away would leave nothing to assert.
 *
 * The property that matters: **no combination of knob values may push the shape outside its box or
 * invert it.** Every knob is user-editable through the Customize dialog, so this is what stops a
 * slider from producing a broken or invisible sliver.
 */
@RunWith(AndroidJUnit4::class)
class SliverShapeTest {

    private companion object {
        const val W = 100f
        const val H = 200f
        const val EPS = 0.01f
    }

    private fun boundsOf(cfg: SliverConfig, side: ArcSliverView.Side): RectF {
        val path = Path()
        SliverShape.buildPath(path, W, H, side, cfg)
        val r = RectF()
        @Suppress("DEPRECATION")
        path.computeBounds(r, true)
        return r
    }

    private fun assertInsideBox(r: RectF, label: String) {
        assertTrue("$label: left ${r.left} < 0", r.left >= -EPS)
        assertTrue("$label: top ${r.top} < 0", r.top >= -EPS)
        assertTrue("$label: right ${r.right} > $W", r.right <= W + EPS)
        assertTrue("$label: bottom ${r.bottom} > $H", r.bottom <= H + EPS)
    }

    @Test
    fun defaults_produceANonEmptyPathInsideTheBox() {
        for (side in ArcSliverView.Side.values()) {
            val r = boundsOf(SliverConfig(), side)
            assertFalse("$side: path is empty", r.isEmpty)
            assertInsideBox(r, side.name)
        }
    }

    @Test
    fun defaults_spanTheFullHeightAndReachTheConfiguredDepth() {
        // The spine runs the full height, and the deepest tip sits at length 0.60 of the width.
        val r = boundsOf(SliverConfig(), ArcSliverView.Side.RIGHT)
        assertEquals("top of spine", 0f, r.top, EPS)
        assertEquals("bottom of spine", H, r.bottom, EPS)
        assertEquals("spine on the right edge", W, r.right, EPS)
        assertEquals("tip reaches 0.60 inward", W * (1f - 0.60f), r.left, EPS)
    }

    @Test
    fun leftAndRightAreMirrorImages() {
        val cfg = SliverConfig()
        val right = boundsOf(cfg, ArcSliverView.Side.RIGHT)
        val left = boundsOf(cfg, ArcSliverView.Side.LEFT)
        assertEquals("mirrored left edge", W - right.right, left.left, EPS)
        assertEquals("mirrored right edge", W - right.left, left.right, EPS)
        assertEquals("same vertical extent", right.top, left.top, EPS)
        assertEquals("same vertical extent", right.bottom, left.bottom, EPS)
    }

    @Test
    fun outOfRangeKnobsAreCoercedRatherThanEscapingTheBox() {
        val extremes = listOf(-5f, -1f, 0f, 0.5f, 1f, 2f, 99f)
        for (side in ArcSliverView.Side.values()) {
            for (v in extremes) {
                val cfg = SliverConfig(
                    tooth1Thickness = v, tooth2Thickness = v,
                    tooth1Length = v, tooth2Length = v,
                    tooth1TipY = v, tooth2TipY = v,
                    gumsDepth = v, gap = v
                )
                assertInsideBox(boundsOf(cfg, side), "$side @ knob=$v")
            }
        }
    }

    @Test
    fun extremeGapDoesNotInvertTheGums() {
        // gap/2 is coerced into [0.01, 0.48], so the gums span never collapses or flips.
        for (gap in listOf(-1f, 0f, 0.001f, 0.9f, 1f, 5f)) {
            val r = boundsOf(SliverConfig(gap = gap), ArcSliverView.Side.RIGHT)
            assertFalse("gap=$gap produced an empty path", r.isEmpty)
            assertInsideBox(r, "gap=$gap")
        }
    }

    @Test
    fun zeroSizeIsHandledWithoutThrowing() {
        val path = Path()
        SliverShape.buildPath(path, 0f, 0f, ArcSliverView.Side.RIGHT, SliverConfig())
        val r = RectF()
        @Suppress("DEPRECATION")
        path.computeBounds(r, true)
        assertEquals(0f, r.width(), EPS)
        assertEquals(0f, r.height(), EPS)
    }
}
