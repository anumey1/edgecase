package com.dicereligion.edgecase

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    // ── Screen views ───────────────────────────────────
    private lateinit var screenMainMenu: View
    private lateinit var screenShortcuts: View
    private lateinit var screenPositioning: View

    // ── Shortcuts screen state (Phase 3 bipartite) ─────
    private var stateManager: ShortcutStateManager? = null
    private var altarAdapter: ActiveShortcutsAdapter? = null
    private var archiveAdapter: AvailableAppsAdapter? = null
    private var shortcutsInitialized = false

    // ── Positioning screen state (Phase 4) ─────────────
    private var positioningView: PositioningView? = null
    private var positioningInitialized = false

    // ── Dust particles (Phase 6) ───────────────────────
    private var dustView: DustParticleView? = null

    // ── Haptics ─────────────────────────────────────────
    private var vibrator: Vibrator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Resolve screen views from the container
        screenMainMenu = findViewById(R.id.screenMainMenu)
        screenShortcuts = findViewById(R.id.screenShortcuts)
        screenPositioning = findViewById(R.id.screenPositioning)

        // Haptics engine
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Wire main menu buttons
        wireMainMenuButtons()
        // Wire sub-screen buttons (Back + Save)
        wireSubScreenButtons()

        // Dust particle overlay (on main menu)
        dustView = DustParticleView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val dustContainer = findViewById<android.widget.FrameLayout>(R.id.dustContainer)
        dustContainer?.addView(dustView)

        // Show main menu
        showScreen(Screen.MAIN_MENU)
    }

    // ──────────────────────────────────────────────────
    // Screen routing
    // ──────────────────────────────────────────────────

    private enum class Screen { MAIN_MENU, SHORTCUTS, POSITIONING }

    private var currentScreen: Screen = Screen.MAIN_MENU

    private fun showScreen(screen: Screen) {
        screenMainMenu.visibility = if (screen == Screen.MAIN_MENU) View.VISIBLE else View.GONE
        screenShortcuts.visibility = if (screen == Screen.SHORTCUTS) View.VISIBLE else View.GONE
        screenPositioning.visibility = if (screen == Screen.POSITIONING) View.VISIBLE else View.GONE

        currentScreen = screen

        if (screen == Screen.SHORTCUTS) {
            if (!shortcutsInitialized) {
                initShortcutsScreen()
                shortcutsInitialized = true
            } else {
                // Re-initialize state from prefs each time we enter the screen
                refreshShortcutsState()
            }
        }
        if (screen == Screen.POSITIONING && !positioningInitialized) {
            initPositioningScreen()
            positioningInitialized = true
        }
    }

    override fun onBackPressed() {
        when (currentScreen) {
            Screen.MAIN_MENU -> super.onBackPressed()
            Screen.SHORTCUTS -> {
                if (stateManager?.isDirty() == true) {
                    showDiscardDialog()
                } else {
                    showScreen(Screen.MAIN_MENU)
                }
            }
            Screen.POSITIONING -> showScreen(Screen.MAIN_MENU)
        }
    }

    // ──────────────────────────────────────────────────
    // Discard confirmation dialog
    // ──────────────────────────────────────────────────

    private fun showDiscardDialog() {
        AlertDialog.Builder(this)
            .setTitle("Discard Changes?")
            .setMessage("You have unsaved changes to your shortcuts. Discard them?")
            .setPositiveButton("Discard") { _, _ ->
                stateManager?.discard()
                refreshAdapters()
                showScreen(Screen.MAIN_MENU)
            }
            .setNegativeButton("Keep Editing", null)
            .show()
    }

    // ──────────────────────────────────────────────────
    // Main menu button wiring
    // ──────────────────────────────────────────────────

    private fun wireMainMenuButtons() {
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnShortcuts)).setOnClickListener {
            showScreen(Screen.SHORTCUTS)
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnPosition)).setOnClickListener {
            showScreen(Screen.POSITIONING)
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnDummy)).setOnClickListener {
            Toast.makeText(this, "Dummy — nothing here yet", Toast.LENGTH_SHORT).show()
        }

        applyStoneButtonBehavior(findViewById<Button>(R.id.btnStartService)).setOnClickListener {
            if (checkAndRequestPermissions()) startEdgeService()
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnStopService)).setOnClickListener {
            stopService(Intent(this, SidebarService::class.java))
        }
    }

    private fun wireSubScreenButtons() {
        // Shortcuts screen
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnBackToMenu)).setOnClickListener {
            onBackPressed()
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnSaveShortcuts)).setOnClickListener {
            saveShortcuts()
        }
        // Positioning screen
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnBackToMenuFromPosition)).setOnClickListener {
            showScreen(Screen.MAIN_MENU)
        }
    }

    // ──────────────────────────────────────────────────
    // Stone button press animation + haptics
    // ──────────────────────────────────────────────────

    private fun applyStoneButtonBehavior(button: Button): Button {
        button.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .translationY(resources.getDimension(R.dimen.stone_button_pressed_translation))
                        .setDuration(80)
                        .start()
                    triggerHaptic(30, 255)
                    dustView?.burst(6)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    v.animate()
                        .translationY(0f)
                        .setDuration(120)
                        .start()
                }
            }
            false
        }
        return button
    }

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

    // ──────────────────────────────────────────────────
    // Shortcuts screen — bipartite initialization
    // ──────────────────────────────────────────────────

    private fun initShortcutsScreen() {
        val allApps = getInstalledApps()
        stateManager = ShortcutStateManager(this, allApps)

        // ── Altar (top 30%) ────────────────────────
        val rvAltar = findViewById<RecyclerView>(R.id.rvAltarShortcuts)
        rvAltar.layoutManager = LinearLayoutManager(this)
        rvAltar.setHasFixedSize(true)
        altarAdapter = ActiveShortcutsAdapter(stateManager!!) { position ->
            // Toggle selection in Altar
            stateManager!!.toggleAltarSelection(position)
            altarAdapter?.notifyItemChanged(position)
            // Also refresh the Archives checkbox states
            archiveAdapter?.notifyDataSetChanged()
        }
        rvAltar.adapter = altarAdapter

        // Attach drag-to-reorder
        val dragCallback = ShortcutDragCallback(altarAdapter!!)
        ItemTouchHelper(dragCallback).attachToRecyclerView(rvAltar)

        // ── Archives (bottom 60%) ───────────────────
        val rvArchive = findViewById<RecyclerView>(R.id.rvArchiveApps)
        rvArchive.layoutManager = LinearLayoutManager(this)
        rvArchive.setHasFixedSize(true)
        archiveAdapter = AvailableAppsAdapter(stateManager!!) { pkg, checked ->
            stateManager!!.setFromArchives(pkg, checked)
            // Refresh both lists
            altarAdapter?.notifyDataSetChanged()
            archiveAdapter?.notifyDataSetChanged()
            updateAltarEmptyState()
        }
        rvArchive.adapter = archiveAdapter

        updateAltarEmptyState()
    }

    /** Re-load state from prefs and refresh adapters when re-entering the screen. */
    private fun refreshShortcutsState() {
        stateManager?.discard() // re-reads from prefs
        refreshAdapters()
    }

    private fun refreshAdapters() {
        altarAdapter?.notifyDataSetChanged()
        archiveAdapter?.notifyDataSetChanged()
        updateAltarEmptyState()
    }

    private fun updateAltarEmptyState() {
        val emptyView = findViewById<TextView>(R.id.tvAltarEmpty)
        val isEmpty = stateManager?.altarItems?.isEmpty() == true
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    // ──────────────────────────────────────────────────
    // Positioning screen — initialization
    // ──────────────────────────────────────────────────

    private fun initPositioningScreen() {
        positioningView = findViewById(R.id.positioningView)

        // Load saved position from SharedPreferences
        val prefs = getSharedPreferences("EdgeCasePrefs", Context.MODE_PRIVATE)
        val savedSide = prefs.getString("sliver_side", "right") ?: "right"
        val savedYBias = prefs.getFloat("sliver_y_bias", 0.5f)

        val side = if (savedSide == "left") ArcSliverView.Side.LEFT else ArcSliverView.Side.RIGHT
        positioningView?.setSliverPosition(side, savedYBias)
        updatePositionInfoText(side, savedYBias)

        // Listen for position changes and persist immediately
        positioningView?.onPositionChanged = { newSide, newYBias ->
            val sideStr = if (newSide == ArcSliverView.Side.LEFT) "left" else "right"
            prefs.edit()
                .putString("sliver_side", sideStr)
                .putFloat("sliver_y_bias", newYBias)
                .apply()
            updatePositionInfoText(newSide, newYBias)

            // Hot-reload: notify running service of new position
            val posIntent = Intent(this, SidebarService::class.java).apply {
                action = SidebarService.ACTION_UPDATE_POSITION
            }
            startService(posIntent)
        }
    }

    private fun updatePositionInfoText(side: ArcSliverView.Side, yBias: Float) {
        val infoView = findViewById<TextView>(R.id.tvPositionInfo)
        val sideLabel = if (side == ArcSliverView.Side.LEFT) "Left" else "Right"
        val yPercent = (yBias * 100).toInt()
        infoView.text = "Side: $sideLabel  •  Position: ${yPercent}% from top"
    }

    // ──────────────────────────────────────────────────
    // Save shortcuts
    // ──────────────────────────────────────────────────

    private fun saveShortcuts() {
        stateManager?.commit()
        // Refresh both adapters to reflect the committed state
        altarAdapter?.notifyDataSetChanged()
        archiveAdapter?.notifyDataSetChanged()
        updateAltarEmptyState()

        // Notify the running service
        val updateIntent = Intent(this, SidebarService::class.java).apply {
            action = SidebarService.ACTION_UPDATE_SHORTCUTS
        }
        startService(updateIntent)

        Toast.makeText(this, "Shortcuts saved", Toast.LENGTH_SHORT).show()
    }

    // ──────────────────────────────────────────────────
    // Permissions & service control
    // ──────────────────────────────────────────────────

    private fun checkAndRequestPermissions(): Boolean {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            return false
        }
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:$packageName")
                )
            )
        }
        return true
    }

    private fun startEdgeService() {
        val intent = Intent(this, SidebarService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    // ──────────────────────────────────────────────────
    // App listing
    // ──────────────────────────────────────────────────

    private fun getInstalledApps(): List<AppInfoData> {
        val list = ArrayList<AppInfoData>()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        for (ri in resolveInfos) {
            list.add(
                AppInfoData(
                    appName = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    icon = ri.loadIcon(pm)
                )
            )
        }
        return list.sortedBy { it.appName.lowercase() }
    }
}
