package com.dicereligion.edgecase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.app.NotificationCompat
import java.util.Collections
import kotlin.math.abs

class SidebarService : Service() {
    companion object {
        const val ACTION_UPDATE_SHORTCUTS = "com.dicereligion.edgecase.UPDATE_SHORTCUTS"
        private const val CHANNEL_ID = "EdgeCaseEngineChannel"
        private const val NOTIFICATION_ID = 9182
        private const val SWIPE_THRESHOLD_X = 30
        private const val MAX_SWIPE_DEVIATION_Y = 150
    }

    private lateinit var windowManager: WindowManager
    private lateinit var sliverView: View
    private lateinit var trayView: View
    private lateinit var sliverParams: WindowManager.LayoutParams
    private lateinit var trayParams: WindowManager.LayoutParams
    private var densityDpi: Float = 1.0f

    override fun onBind(intent: Intent?): IBinder? = null

    // ──────────────────────────────────────────────
    // 4.11  Lifecycle
    // ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        densityDpi = resources.displayMetrics.density

        buildSystemNotification()
        instantiateWindowParameters()
        assembleSliverView()
        assembleTrayView()

        if (Settings.canDrawOverlays(this)) {
            windowManager.addView(sliverView, sliverParams)
        } else {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_UPDATE_SHORTCUTS) refreshTrayUiElements()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::sliverView.isInitialized && sliverView.isAttachedToWindow) {
            windowManager.removeView(sliverView)
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
    // 4.3  Window parameter configuration
    // ──────────────────────────────────────────────

    private fun instantiateWindowParameters() {
        val sliverWidthPx = (15 * densityDpi).toInt()
        val sliverHeightPx = (75 * densityDpi).toInt()
        val bottomOffsetPx = (110 * densityDpi).toInt()

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
            gravity = Gravity.END or Gravity.BOTTOM
            y = bottomOffsetPx
        }

        val trayWidthPx = (80 * densityDpi).toInt()
        val trayHeightPx = sliverHeightPx * 4
        trayParams = WindowManager.LayoutParams(
            trayWidthPx,
            trayHeightPx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.BOTTOM
            // Bottom edge of tray aligns with bottom edge of sliver
            y = bottomOffsetPx
        }
    }

    // ──────────────────────────────────────────────
    // 4.4  Sliver view — gesture detection
    // ──────────────────────────────────────────────

    private fun assembleSliverView() {
        val cornerRadiusPx = (8 * densityDpi).toFloat()
        sliverView = View(this).apply {
            background = GradientDrawable().apply {
                setColor(Color.argb(8, 255, 255, 255))
                cornerRadii = floatArrayOf(
                    cornerRadiusPx, cornerRadiusPx,  // top-left
                    0f, 0f,                            // top-right
                    0f, 0f,                            // bottom-right
                    cornerRadiusPx, cornerRadiusPx     // bottom-left
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                    val rect = Rect(0, 0, width, height)
                    systemGestureExclusionRects = Collections.singletonList(rect)
                }
            }
        }

        var startX = 0f
        var startY = 0f
        sliverView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = event.rawX
                    startY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = startX - event.rawX
                    val deltaY = abs(event.rawY - startY)
                    if (deltaX > SWIPE_THRESHOLD_X && deltaY < MAX_SWIPE_DEVIATION_Y) {
                        transitionToExpandedTray()
                        true
                    } else false
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick()
                    false
                }
                else -> false
            }
        }
    }

    // ──────────────────────────────────────────────
    // 4.5  Tray view — shortcut panel
    // ──────────────────────────────────────────────

    private fun assembleTrayView() {
        val scrollContainer = ScrollView(this).apply {
            isVerticalScrollBarEnabled = false
            setBackgroundColor(Color.parseColor("#E6121212"))
        }
        val layoutList = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            setPadding(0, (8 * densityDpi).toInt(), 0, (8 * densityDpi).toInt())
        }
        scrollContainer.addView(layoutList)
        trayView = scrollContainer
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
                    setImageDrawable(pm.getApplicationIcon(packageName))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setOnClickListener {
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

        // Scroll to bottom so newest shortcuts are visible first
        (trayView as ScrollView).post {
            (trayView as ScrollView).fullScroll(View.FOCUS_DOWN)
        }
    }

    // ──────────────────────────────────────────────
    // 4.7  Hot-reload tray contents
    // ──────────────────────────────────────────────

    private fun refreshTrayUiElements() {
        if (::trayView.isInitialized) {
            populateShortcuts((trayView as ScrollView).getChildAt(0) as LinearLayout)
        }
    }

    // ──────────────────────────────────────────────
    // 4.8 / 4.9  State transitions
    // ──────────────────────────────────────────────

    private fun transitionToExpandedTray() {
        if (::sliverView.isInitialized && sliverView.isAttachedToWindow) {
            windowManager.removeView(sliverView)
        }
        if (::trayView.isInitialized && !trayView.isAttachedToWindow) {
            refreshTrayUiElements()
            windowManager.addView(trayView, trayParams)
        }
    }

    private fun transitionToSliverState() {
        if (::trayView.isInitialized && trayView.isAttachedToWindow) {
            windowManager.removeView(trayView)
        }
        if (::sliverView.isInitialized && !sliverView.isAttachedToWindow) {
            windowManager.addView(sliverView, sliverParams)
        }
    }
}
