package com.dicereligion.edgecase

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.app.NotificationCompat
import java.util.Collections

class SidebarService : Service() {
    companion object {
        /** Start the overlay. Carries an [OverlaySnapshot]. */
        const val ACTION_START = "com.dicereligion.edgecase.START"
        /** Settings changed while running. Carries an [OverlaySnapshot]. */
        const val ACTION_SYNC_STATE = "com.dicereligion.edgecase.SYNC_STATE"
        private const val CHANNEL_ID = "EdgeCaseEngineChannel"
        private const val NOTIFICATION_ID = 9182

        /**
         * Whether the service is running, for the Serpent's Eyes when the settings screen resumes.
         *
         * This service lives in the `:overlay` process, so a static flag here would be invisible
         * to MainActivity. `getRunningServices` is deprecated for other apps' services but still
         * returns the caller's own. Measured on device (Docs/RAMIssuePDP.md Phase 1): exact once
         * the service is up, but blind for the ~70-100 ms after START while its process launches.
         * So it is used only for the eyes on resume; everything else goes through the binding.
         */
        @Suppress("DEPRECATION")
        fun isRunning(context: Context): Boolean =
            context.getSystemService(ActivityManager::class.java)
                .getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == SidebarService::class.java.name && it.started }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var sliverView: View
    private lateinit var trayView: View
    private lateinit var sliverParams: WindowManager.LayoutParams
    private lateinit var trayParams: WindowManager.LayoutParams
    private var densityDpi: Float = 1.0f
    private var currentSide: ArcSliverView.Side = ArcSliverView.Side.RIGHT
    private var currentYBias: Float = 0.5f
    private var config: SliverConfig = SliverConfig()
    private var screenHeight: Int = 0
    private var vibrator: Vibrator? = null

    // Window bookkeeping. Set at the moment of addView/removeView, never inferred from
    // isAttachedToWindow: a view only reports attached after its next frame, so a removal checked
    // that way straight after an add is skipped and the window leaks (seen on device when STOP's
    // onUnbind and onDestroy ran back to back — Docs/RAMIssuePDP.md Phase 2, V4).
    private var sliverAdded = false
    private var trayAdded = false
    private var destroyed = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var shortcuts: List<String> = emptyList()

    /** False until the first [OverlaySnapshot] has built the views. */
    private var stateApplied = false

    /**
     * True while MainActivity is resumed and bound to us: the settings screen is in front, so the
     * overlay stays off (Docs/Ads.md §4.3 — never draw over our own UI or the ad).
     *
     * The Activity binds WITHOUT BIND_AUTO_CREATE, so binding never starts us. Android delivers
     * onBind before onStartCommand (verified on device, Docs/RAMIssuePDP.md Phase 1 S7), so a START
     * or a sticky restart that happens while the app is open already knows to stay hidden.
     */
    private var bound = false
    private val binder = Binder()

    override fun onBind(intent: Intent?): IBinder {
        bound = true
        detachOverlayWindows()
        return binder
    }

    override fun onRebind(intent: Intent?) {
        bound = true
        detachOverlayWindows()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        bound = false
        // Posted, not immediate: on STOP Android unbinds and then destroys us back to back, and the
        // destroy is already queued, so it runs first and the sliver never flashes up. On an ordinary
        // unbind (the user left the settings screen) nothing is queued and the sliver returns at once.
        mainHandler.post { if (stateApplied) addSliverIfNeeded() }
        return true   // later binds arrive through onRebind
    }

    // ──────────────────────────────────────────────
    // 4.11  Lifecycle
    // ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        densityDpi = resources.displayMetrics.density

        // Real screen height for Y position calculation
        screenHeight = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            windowManager.currentWindowMetrics.bounds.height()
        } else {
            @Suppress("DEPRECATION")
            resources.displayMetrics.heightPixels
        }

        // Haptics engine
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // startForeground must happen here, within the foreground-service start deadline.
        buildSystemNotification()

        if (!Settings.canDrawOverlays(this)) stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            // A null intent is a START_STICKY restart. That always happens in a fresh process, whose
            // prefs cache is read from disk, so prefs are current there and only there.
            ACTION_START, ACTION_SYNC_STATE, null ->
                applyState(OverlaySnapshot.fromIntent(intent) ?: OverlaySnapshot.fromPrefs(this))
        }
        return START_STICKY
    }

    /** Takes on a new snapshot: builds the overlay the first time, updates it in place after that. */
    private fun applyState(snapshot: OverlaySnapshot) {
        if (!Settings.canDrawOverlays(this)) return
        currentSide = snapshot.side
        currentYBias = snapshot.yBias
        config = snapshot.config
        shortcuts = snapshot.shortcuts

        if (!stateApplied) {
            instantiateWindowParameters()
            assembleSliverView()
            assembleTrayView()
            stateApplied = true
            addSliverIfNeeded()
        } else {
            applySliverUpdate()
        }
    }

    override fun onDestroy() {
        destroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        removeSliver()
        removeTray()
        super.onDestroy()
    }

    // ──────────────────────────────────────────────
    // 4.2  Foreground notification
    // ──────────────────────────────────────────────

    private fun buildSystemNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "EdgeCase Engine Active",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("EdgeCase Active")
            .setContentText("Listening for gestures.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    // ──────────────────────────────────────────────
    // 4.3  Window parameter configuration (position-aware)
    // ──────────────────────────────────────────────

    private fun instantiateWindowParameters() {
        val sliverWidthPx = (config.widthDp * densityDpi).toInt()
        val sliverHeightPx = (config.heightDp * densityDpi).toInt()

        // Y position: map yBias [0,1] → vertical range [10%, 90%] of screen
        val restrictedTop = (screenHeight * 0.10f).toInt()
        val validRange = (screenHeight * 0.80f).toInt()
        val sliverYPx = restrictedTop + (validRange * currentYBias).toInt()

        sliverParams = WindowManager.LayoutParams(
            sliverWidthPx,
            sliverHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (currentSide == ArcSliverView.Side.RIGHT) {
                Gravity.END or Gravity.TOP
            } else {
                Gravity.START or Gravity.TOP
            }
            y = sliverYPx
        }

        val trayWidthPx = (config.trayWidthDp * densityDpi).toInt()
        val trayHeightPx = (config.trayHeightDp * densityDpi).toInt()
        // Bottom-anchored to the sliver's vertical center; clamp the top so a tall manual
        // drawer can't float off-screen (§12.5).
        val trayYPx = (sliverYPx + sliverHeightPx / 2 - trayHeightPx).coerceAtLeast(0)
        trayParams = WindowManager.LayoutParams(
            trayWidthPx,
            trayHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = if (currentSide == ArcSliverView.Side.RIGHT) {
                Gravity.END or Gravity.TOP
            } else {
                Gravity.START or Gravity.TOP
            }
            // Tray bottom aligns with sliver vertical center
            y = trayYPx
        }
    }

    // ──────────────────────────────────────────────
    // 4.4  Sliver view — gesture detection
    // ──────────────────────────────────────────────

    private fun assembleSliverView() {
        sliverView = ArcSliverView(this, currentSide, config) {
            transitionToExpandedTray()
        }.apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    val rect = Rect(0, 0, width, height)
                    systemGestureExclusionRects = Collections.singletonList(rect)
                }
            }
        }
    }

    // ──────────────────────────────────────────────
    // 4.4b  Position loading & hot-reload
    // ──────────────────────────────────────────────

    /**
     * Adds the sliver unless it is already up, the settings screen is in front ([bound]), or the
     * service is going away. Every attach goes through here, so those rules hold everywhere.
     */
    private fun addSliverIfNeeded() {
        if (destroyed || bound || sliverAdded || !::sliverView.isInitialized) return
        windowManager.addView(sliverView, sliverParams)
        sliverAdded = true
    }

    private fun removeSliver() {
        if (!sliverAdded) return
        try { windowManager.removeView(sliverView) } catch (_: Exception) {}
        sliverAdded = false
    }

    private fun removeTray() {
        if (!trayAdded) return
        try { windowManager.removeView(trayView) } catch (_: Exception) {}
        trayAdded = false
    }

    /**
     * Detaches the sliver, and any open tray, without stopping the service.
     *
     * Called while MainActivity is in the foreground (bound). Three independent reasons (Docs/Ads.md §4.3):
     *  • **Compliance** — an overlay window sitting above an ad is an obstruction; impressions
     *    beneath it are not viewable, and EdgeCase's sliver can be positioned at 90% of screen
     *    height, exactly where the Plinth's banner lives.
     *  • **UX** — an edge launcher floating over its own settings screen is noise.
     *  • **Correctness** — the fang currently overlaps our own UI, including the PositioningView
     *    drag canvas, where a stray fang beside the mock phone is actively confusing.
     *
     * Idempotent, and deliberately leaves the service running: only the windows go away.
     */
    private fun detachOverlayWindows() {
        removeSliver()
        removeTray()
    }

    /**
     * Update the single sliver overlay **in place** from the state [applyState] just set.
     *
     * We deliberately do NOT destroy/recreate the sliver window here: recreating and re-adding it left a
     * race where the previous window (still showing the old/default appearance) was not removed before the
     * new one was added, leaving a stale sliver underneath. Updating the existing view via
     * [ArcSliverView.applyConfig] + [WindowManager.updateViewLayout] keeps exactly one sliver on screen.
     */
    private fun applySliverUpdate() {
        instantiateWindowParameters()

        if (::sliverView.isInitialized) {
            (sliverView as? ArcSliverView)?.applyConfig(config, currentSide)
            if (sliverAdded) {
                try {
                    windowManager.updateViewLayout(sliverView, sliverParams)
                } catch (_: Exception) {
                }
            } else {
                addSliverIfNeeded()
            }
        } else {
            assembleSliverView()
            addSliverIfNeeded()
        }

        // Rebuild the tray so its size/side/position match the update (only shown on swipe).
        removeTray()
        assembleTrayView()
    }

    // ──────────────────────────────────────────────
    // 4.5  Tray view — shortcut panel
    // ──────────────────────────────────────────────

    private fun assembleTrayView() {
        // Root: horizontal LinearLayout with meander border + scroll area
        val trayRoot = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        // Greek meander border on the inward-facing edge
        val meanderBorder = ImageView(this).apply {
            val borderWidth = (12 * densityDpi).toInt()
            layoutParams = LinearLayout.LayoutParams(borderWidth, LinearLayout.LayoutParams.MATCH_PARENT)
            setImageResource(R.drawable.ic_meander_border)
            scaleType = ImageView.ScaleType.FIT_XY
            alpha = 0.7f
            // Native tarnished-silver (grey) meander
            contentDescription = "Meander border"
        }

        val scrollContainer = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            // The serpent's spine: obsidian with rows of emerald scales (Phase 7 #3)
            setBackgroundResource(R.drawable.bg_serpent_scales)
            layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.MATCH_PARENT, 1f
            )
        }
        val layoutList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setPadding(0, (8 * densityDpi).toInt(), 0, (8 * densityDpi).toInt())
        }
        scrollContainer.addView(layoutList)

        // Meander goes on the inward side: right-tray → meander on left; left-tray → meander on right
        if (currentSide == ArcSliverView.Side.RIGHT) {
            trayRoot.addView(meanderBorder)   // meander left (inward)
            trayRoot.addView(scrollContainer)  // icons right
        } else {
            trayRoot.addView(scrollContainer)  // icons left
            trayRoot.addView(meanderBorder)    // meander right (inward)
        }

        trayView = trayRoot
        populateShortcuts(layoutList)

        trayView.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                transitionToSliverState()
                true
            } else if (event.action == MotionEvent.ACTION_UP) {
                v.performClick()
                false
            } else false
        }
    }

    // ──────────────────────────────────────────────
    // 4.6  Populate shortcut icons
    // ──────────────────────────────────────────────

    private fun populateShortcuts(container: LinearLayout) {
        val orderedList = shortcuts
        val pm = packageManager
        container.removeAllViews()

        // Add in reverse order so #1 appears at the bottom, newest at the top
        for (packageName in orderedList.asReversed()) {
            try {
                val imgView = ImageView(this).apply {
                    val sideSize = (48 * densityDpi).toInt()
                    layoutParams = LinearLayout.LayoutParams(sideSize, sideSize).apply {
                        setMargins(0, (8 * densityDpi).toInt(), 0, (8 * densityDpi).toInt())
                    }
                    setImageDrawable(desaturateIcon(pm.getApplicationIcon(packageName)))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setOnClickListener {
                        // Restore full saturation on press
                        colorFilter = null
                        triggerHaptic(20, 150)
                        pm.getLaunchIntentForPackage(packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(this)
                            transitionToSliverState()
                        }
                    }
                }
                container.addView(imgView)
            } catch (_: Exception) {
                // App was uninstalled since selection — silently skip
            }
        }

        // Scroll to bottom
        (trayView as? LinearLayout)?.let { root ->
            val scroll = if (currentSide == ArcSliverView.Side.RIGHT) root.getChildAt(1) else root.getChildAt(0)
            (scroll as? ScrollView)?.post {
                scroll.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    // ──────────────────────────────────────────────
    // 4.7  Hot-reload tray contents
    // ──────────────────────────────────────────────

    private fun refreshTrayUiElements() {
        if (::trayView.isInitialized) {
            val root = trayView as? LinearLayout ?: return
            val scroll = if (currentSide == ArcSliverView.Side.RIGHT) root.getChildAt(1) else root.getChildAt(0)
            populateShortcuts((scroll as ScrollView).getChildAt(0) as LinearLayout)
        }
    }

    // ──────────────────────────────────────────────
    // 4.8 / 4.9  State transitions
    // ──────────────────────────────────────────────

    private fun transitionToExpandedTray() {
        if (destroyed || bound) return
        removeSliver()
        if (::trayView.isInitialized && !trayAdded) {
            refreshTrayUiElements()

            // Stone door unfurl: scale from 0 at edge → 1
            val trayWidthPx = (config.trayWidthDp * densityDpi).toInt()
            val pivotX = if (currentSide == ArcSliverView.Side.RIGHT) trayWidthPx.toFloat() else 0f
            trayView.scaleX = 0f
            trayView.pivotX = pivotX
            trayView.animate()
                .scaleX(1f)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()

            windowManager.addView(trayView, trayParams)
            trayAdded = true

            // Swipe haptic: escalating vibration
            triggerHaptic(40, 200)
        }
    }

    private fun transitionToSliverState() {
        removeTray()
        addSliverIfNeeded()
    }

    // ──────────────────────────────────────────────
    // 4.10  Icon desaturation
    // ──────────────────────────────────────────────

    /** Apply a 20% desaturation filter to the drawable for the ancient theme. */
    private fun desaturateIcon(drawable: android.graphics.drawable.Drawable): android.graphics.drawable.Drawable {
        val cm = ColorMatrix()
        cm.setSaturation(0.8f) // 80% saturation = 20% desaturation
        drawable.colorFilter = ColorMatrixColorFilter(cm)
        return drawable
    }

    // ──────────────────────────────────────────────
    // 4.11  Haptic feedback
    // ──────────────────────────────────────────────

    private fun triggerHaptic(durationMs: Long, amplitude: Int) {
        vibrator?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
            } else {
                @Suppress("DEPRECATION")
                it.vibrate(durationMs)
            }
        }
    }
}
