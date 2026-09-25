package com.dicereligion.edgecase

import android.content.Context
import android.content.Intent
import android.os.Bundle

/**
 * Everything the overlay needs to draw itself: edge, vertical position, appearance, and the
 * ordered shortcut list.
 *
 * SidebarService runs in its own `:overlay` process, and each process keeps its own cached copy of
 * SharedPreferences, so the service cannot see what MainActivity has just saved. The Activity
 * therefore sends a complete snapshot with every Intent. The service reads prefs itself only on a
 * `START_STICKY` restart, which always happens in a fresh process whose cache is loaded from disk.
 * Docs/RAMIssuePDP.md §5.3.
 */
data class OverlaySnapshot(
    val side: ArcSliverView.Side,
    val yBias: Float,
    val config: SliverConfig,
    val shortcuts: List<String>
) {
    fun writeTo(intent: Intent): Intent = intent.putExtra(EXTRA_SNAPSHOT, Bundle().apply {
        putString(K_SIDE, if (side == ArcSliverView.Side.LEFT) "left" else "right")
        putFloat(K_Y_BIAS, yBias)
        putBundle(K_CONFIG, config.toBundle())
        putStringArrayList(K_SHORTCUTS, ArrayList(shortcuts))
    })

    companion object {
        private const val EXTRA_SNAPSHOT = "com.dicereligion.edgecase.extra.OVERLAY_SNAPSHOT"
        private const val K_SIDE = "side"
        private const val K_Y_BIAS = "y_bias"
        private const val K_CONFIG = "config"
        private const val K_SHORTCUTS = "shortcuts"

        /** The snapshot carried by [intent], or null if it carries none. */
        fun fromIntent(intent: Intent?): OverlaySnapshot? {
            val b = intent?.getBundleExtra(EXTRA_SNAPSHOT) ?: return null
            return OverlaySnapshot(
                side = if (b.getString(K_SIDE) == "left") ArcSliverView.Side.LEFT else ArcSliverView.Side.RIGHT,
                yBias = b.getFloat(K_Y_BIAS, 0.5f).coerceIn(0f, 1f),
                config = b.getBundle(K_CONFIG)?.let { SliverConfig.fromBundle(it) } ?: SliverConfig(),
                shortcuts = b.getStringArrayList(K_SHORTCUTS) ?: emptyList()
            )
        }

        /** Reads the persisted state. Only correct in the process that wrote it, or in a fresh one. */
        fun fromPrefs(context: Context): OverlaySnapshot {
            val prefs = context.getSharedPreferences(SliverConfig.PREFS, Context.MODE_PRIVATE)
            val orderStr = prefs.getString("saved_shortcuts_order", null)
            val shortcuts: List<String> = if (!orderStr.isNullOrEmpty()) {
                orderStr.split(",").filter { it.isNotEmpty() }
            } else {
                (prefs.getStringSet("saved_shortcuts", emptySet()) ?: emptySet()).toList()
            }
            return OverlaySnapshot(
                side = if (prefs.getString("sliver_side", "right") == "left")
                    ArcSliverView.Side.LEFT else ArcSliverView.Side.RIGHT,
                yBias = prefs.getFloat("sliver_y_bias", 0.5f).coerceIn(0f, 1f),
                config = SliverConfig.load(context),
                shortcuts = shortcuts
            )
        }
    }
}
