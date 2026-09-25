package com.dicereligion.edgecase

import android.os.Handler
import android.os.Looper
import android.view.View

/**
 * One shared, slow clock for the settings screens' never-ending ambient animations: the gem pulse
 * in [ObsidianCrackView] and the Serpent's Eyes breathing in [ServiceEyeView].
 *
 * Why it exists (Docs/RAMIssuePDP.md Phase 3): every invalidate of those views re-renders the whole
 * window, because the gem background sits behind everything. Driven by their own animators they
 * redrew ~60 times a second and cost 29-35 % of a CPU core while the user did nothing. The pulses
 * are slow (2.4-4.8 s periods), so 12 redraws a second look the same. And because every subscriber
 * is invalidated in the same tick, they share one redraw rather than each adding their own.
 *
 * Views read wall time ([android.os.SystemClock.uptimeMillis]), not a tick count, so their motion
 * keeps its speed whatever the rate. Main thread only. The loop runs only while something is
 * subscribed, and views unsubscribe whenever they are not actually on screen.
 */
object TempleClock {

    /** 12 frames a second (maintainer's decision, 2026-09-25). */
    const val FRAME_INTERVAL_MS = 1000L / 12

    private val handler = Handler(Looper.getMainLooper())
    private val subscribers = LinkedHashSet<View>()
    private val snapshot = ArrayList<View>()   // reused, so a tick allocates nothing
    private var ticking = false

    private val tick = object : Runnable {
        override fun run() {
            if (subscribers.isEmpty()) {
                ticking = false
                return
            }
            snapshot.clear()
            snapshot.addAll(subscribers)
            for (v in snapshot) v.invalidate()
            handler.postDelayed(this, FRAME_INTERVAL_MS)
        }
    }

    fun subscribe(view: View) {
        if (subscribers.add(view) && !ticking) {
            ticking = true
            handler.postDelayed(tick, FRAME_INTERVAL_MS)
        }
    }

    fun unsubscribe(view: View) {
        if (subscribers.remove(view) && subscribers.isEmpty()) {
            handler.removeCallbacks(tick)
            ticking = false
        }
    }
}
