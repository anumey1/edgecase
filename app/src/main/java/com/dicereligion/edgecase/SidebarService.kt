package com.dicereligion.edgecase

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
import android.os.Build
import android.os.IBinder
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
        const val ACTION_UPDATE_SHORTCUTS = "com.dicereligion.edgecase.UPDATE_SHORTCUTS"
        const val ACTION_UPDATE_POSITION = "com.dicereligion.edgecase.UPDATE_POSITION"
        const val ACTION_UPDATE_STYLE = "com.dicereligion.edgecase.UPDATE_STYLE"
        private const val CHANNEL_ID = "EdgeCaseEngineChannel"
        private const val NOTIFICATION_ID = 9182
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
    private var sliverAdded = false

    override fun onBind(intent: Intent?): IBinder? = null

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

        // Load saved position + style before building views
        loadPositionFromPrefs()
        config = SliverConfig.load(this)

        // Haptics engine
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        buildSystemNotification()
        instantiateWindowParameters()
        assembleSliverView()
        assembleTrayView()

        if (Settings.canDrawOverlays(this)) {
            addSliverIfNeeded()
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UPDATE_SHORTCUTS -> refreshTrayUiElements()
            ACTION_UPDATE_POSITION -> applySliverUpdate()
            ACTION_UPDATE_STYLE -> applySliverUpdate()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sliverView.isInitialized && sliverView.isAttachedToWindow) {
            windowManager.removeView(sliverView)
            sliverAdded = false
        }
        if (::trayView.isInitialized && trayView.isAttachedToWindow) {
            windowManager.removeView(trayView)
        }
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

        val trayWidthPx = (80 * densityDpi).toInt()
        val trayHeightPx = (sliverHeightPx * 7)
        val trayYPx = sliverYPx + sliverHeightPx / 2 - trayHeightPx
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

    /** Adds the sliver to the window if not already showing. Idempotent guard. */
    private fun addSliverIfNeeded() {
        if (!sliverAdded && ::sliverView.isInitialized && !sliverView.isAttachedToWindow) {
            windowManager.addView(sliverView, sliverParams)
            sliverAdded = true
        }
    }

    private fun loadPositionFromPrefs() {
        val prefs = getSharedPreferences("EdgeCasePrefs", Context.MODE_PRIVATE)
        val sideStr = prefs.getString("sliver_side", "right") ?: "right"
        currentSide = if (sideStr == "left") ArcSliverView.Side.LEFT else ArcSliverView.Side.RIGHT
        currentYBias = prefs.getFloat("sliver_y_bias", 0.5f).coerceIn(0f, 1f)
    }

    /**
     * Re-read position + style prefs and update the single sliver overlay **in place**.
     *
     * We deliberately do NOT destroy/recreate the sliver window here: recreating and re-adding it left a
     * race where the previous window (still showing the old/default appearance) was not removed before the
     * new one was added, leaving a stale sliver underneath. Updating the existing view via
     * [ArcSliverView.applyConfig] + [WindowManager.updateViewLayout] keeps exactly one sliver on screen.
     */
    private fun applySliverUpdate() {
        loadPositionFromPrefs()
        config = SliverConfig.load(this)
        instantiateWindowParameters()

        if (::sliverView.isInitialized) {
            (sliverView as? ArcSliverView)?.applyConfig(config, currentSide)
            if (sliverView.isAttachedToWindow) {
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
        if (::trayView.isInitialized && trayView.isAttachedToWindow) {
            try {
                windowManager.removeView(trayView)
            } catch (_: Exception) {
            }
        }
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
            contentDescription = "Meander border"
        }

        val scrollContainer = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#E6121212"))
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
        val prefs = getSharedPreferences("EdgeCasePrefs", Context.MODE_PRIVATE)
        val orderStr = prefs.getString("saved_shortcuts_order", null)
        val orderedList: List<String> = if (!orderStr.isNullOrEmpty()) {
            orderStr.split(",").filter { it.isNotEmpty() }
        } else {
            (prefs.getStringSet("saved_shortcuts", emptySet()) ?: emptySet()).toList()
        }
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
        if (::sliverView.isInitialized && sliverView.isAttachedToWindow) {
            windowManager.removeView(sliverView)
            sliverAdded = false
        }
        if (::trayView.isInitialized && !trayView.isAttachedToWindow) {
            refreshTrayUiElements()

            // Stone door unfurl: scale from 0 at edge → 1
            val trayWidthPx = (80 * densityDpi).toInt()
            val pivotX = if (currentSide == ArcSliverView.Side.RIGHT) trayWidthPx.toFloat() else 0f
            trayView.scaleX = 0f
            trayView.pivotX = pivotX
            trayView.animate()
                .scaleX(1f)
                .setDuration(250)
                .setInterpolator(DecelerateInterpolator())
                .start()

            windowManager.addView(trayView, trayParams)

            // Swipe haptic: escalating vibration
            triggerHaptic(40, 200)
        }
    }

    private fun transitionToSliverState() {
        if (::trayView.isInitialized && trayView.isAttachedToWindow) {
            windowManager.removeView(trayView)
        }
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
