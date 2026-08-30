package com.dicereligion.edgecase

import android.app.AlertDialog
import android.content.ActivityNotFoundException
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
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {

    companion object {
        /**
         * Whether this Activity is currently in the foreground. Read by [SidebarService.onCreate]
         * so a service started *from* the settings screen does not attach its overlay on top of
         * our own UI. Mirrors [SidebarService.isRunning] in the opposite direction.
         */
        @Volatile
        var isForeground = false
    }

    // ── Screen views ───────────────────────────────────
    private lateinit var screenMainMenu: View
    private lateinit var screenShortcuts: View
    private lateinit var screenPositioning: View
    private lateinit var screenCredits: View

    // ── Shortcuts screen state (Phase 3 bipartite) ─────
    private var stateManager: ShortcutStateManager? = null
    private var altarAdapter: ActiveShortcutsAdapter? = null
    private var archiveAdapter: AvailableAppsAdapter? = null
    private var shortcutsInitialized = false
    private var cachedApps: List<AppInfoData>? = null
    private var appsLoading = false  // true while a background load is in flight

    // ── Positioning screen state (Phase 4) ─────────────
    private var positioningView: PositioningView? = null
    private var positioningInitialized = false

    // ── Dust particles (Phase 6) ───────────────────────
    private var dustView: DustParticleView? = null
    private var crackView: CrackFlashView? = null

    // ── Serpent's Eyes service indicator (Phase 7 #1) — one on each flank ────
    private val serviceEyes = mutableListOf<ServiceEyeView>()

    // ── Haptics ─────────────────────────────────────────
    private var vibrator: Vibrator? = null

    // ── Ad plinth (Docs/Ads.md §5, §7.5) ────────────────
    private var adHost: AdHost? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // The Plinth: one persistent banner slot below all four screens. It lives outside
        // screenContainer, so showScreen() never touches it. See Docs/Ads.md §5.
        adHost = AdHost(this, findViewById(R.id.adFrame)).also {
            // UMP resolves after onCreate, so the consent button's visibility cannot be decided
            // once here and left alone (Docs/Ads.md §7.7).
            it.onConsentResolved = { syncAdConsentButton() }
            it.start()
        }

        // Resolve screen views from the container
        screenMainMenu = findViewById(R.id.screenMainMenu)
        screenShortcuts = findViewById(R.id.screenShortcuts)
        screenPositioning = findViewById(R.id.screenPositioning)
        screenCredits = findViewById(R.id.screenCredits)

        // Per-screen temple-lintel titles (§5.5). The same tvTempleTitle id exists in each
        // included header, so it MUST be resolved scoped to each screen, never on the Activity.
        // The main-menu copy keeps its default "ΞDGΞCΛSΞ" @ header_title_size — no code needed.
        screenShortcuts.findViewById<TextView>(R.id.tvTempleTitle)?.apply {
            text = "SHORTCUTS"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.header_title_size_sub))
        }
        screenPositioning.findViewById<TextView>(R.id.tvTempleTitle)?.apply {
            text = "SLIVER POSITION"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.header_title_size_sub))
        }
        screenCredits.findViewById<TextView>(R.id.tvTempleTitle)?.apply {
            text = "CREDITS"
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.header_title_size_sub))
        }

        // Serpent's Eyes live only in the main-menu lintel — one on each flank (Phase 7 #1)
        serviceEyes.clear()
        listOf(R.id.serviceEyeLeft, R.id.serviceEyeRight).forEach { id ->
            screenMainMenu.findViewById<ServiceEyeView>(id)?.also {
                it.visibility = View.VISIBLE
                serviceEyes.add(it)
            }
        }

        // Haptics engine
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        // Predictive-back navigation (§12.4)
        onBackPressedDispatcher.addCallback(this, backCallback)

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

        // Crack-flash overlay, on top of the dust (Phase 7 #2)
        crackView = CrackFlashView(this).apply {
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        dustContainer?.addView(crackView)

        // Pre-load the app list in background so the Shortcuts screen opens instantly.
        preloadApps()

        // Show main menu
        showScreen(Screen.MAIN_MENU)
    }

    override fun onResume() {
        super.onResume()
        isForeground = true
        // Sync the Serpent's Eyes with the actual service state (Phase 7 #1)
        serviceEyes.forEach { it.setRunning(SidebarService.isRunning) }
        // Take the edge back while our own UI is on screen (Docs/Ads.md §4.3)
        setOverlaySuspended(true)
    }

    override fun onPause() {
        super.onPause()
        isForeground = false
        // Hand the edge back to the user
        setOverlaySuspended(false)
    }

    /**
     * Signals the running service to detach or re-attach its overlay windows.
     *
     * Never *starts* the service: the guard means a stopped service stays stopped, so opening the
     * settings screen can't resurrect an overlay the user turned off.
     */
    private fun setOverlaySuspended(suspended: Boolean) {
        if (!SidebarService.isRunning) return
        startService(Intent(this, SidebarService::class.java).apply {
            action = if (suspended) SidebarService.ACTION_SUSPEND_OVERLAY
                     else SidebarService.ACTION_RESUME_OVERLAY
        })
    }

    override fun onDestroy() {
        isForeground = false
        adHost?.destroy()
        adHost = null
        super.onDestroy()
    }

    // ──────────────────────────────────────────────────
    // Screen routing
    // ──────────────────────────────────────────────────

    private enum class Screen { MAIN_MENU, SHORTCUTS, POSITIONING, CREDITS }

    private var currentScreen: Screen = Screen.MAIN_MENU

    private fun showScreen(screen: Screen) {
        screenMainMenu.visibility = if (screen == Screen.MAIN_MENU) View.VISIBLE else View.GONE
        screenShortcuts.visibility = if (screen == Screen.SHORTCUTS) View.VISIBLE else View.GONE
        screenPositioning.visibility = if (screen == Screen.POSITIONING) View.VISIBLE else View.GONE
        screenCredits.visibility = if (screen == Screen.CREDITS) View.VISIBLE else View.GONE

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

        // Enable back interception only on sub-screens (§12.4)
        backCallback.isEnabled = (screen != Screen.MAIN_MENU)
    }

    // Predictive-back-compatible navigation (§12.4). Disabled on the main menu so the system
    // handles back natively there (OS predictive back-to-home); enabled on sub-screens so both
    // the gesture and 3-button back route through the same dirty-check logic.
    private val backCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when (currentScreen) {
                Screen.SHORTCUTS ->
                    if (stateManager?.isDirty() == true) showDiscardDialog()
                    else showScreen(Screen.MAIN_MENU)
                Screen.POSITIONING -> showScreen(Screen.MAIN_MENU)
                Screen.CREDITS -> showScreen(Screen.MAIN_MENU)
                Screen.MAIN_MENU -> Unit   // unreachable: callback is disabled on the menu
            }
        }
    }

    // ──────────────────────────────────────────────────
    // Discard confirmation dialog
    // ──────────────────────────────────────────────────

    private fun showDiscardDialog() {
        // Same rule as the Customize popup: any modal over the app hides the banner.
        adHost?.setAdVisible(false)
        val dialog = AlertDialog.Builder(this)
            .setTitle("ABANDON THE UNCARVED?")
            .setMessage("Your offerings are not yet carved in stone. Abandon them?")
            .setPositiveButton("ABANDON") { _, _ ->
                stateManager?.discard()
                refreshAdapters()
                showScreen(Screen.MAIN_MENU)
            }
            .setNegativeButton("KEEP CARVING", null)
            .create()
        dialog.setOnDismissListener { adHost?.setAdVisible(true) }
        // Square temple-panel window background — no rounded system dialog frame (§9)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_temple_panel)
        dialog.show()
    }

    // ──────────────────────────────────────────────────
    // Main menu button wiring
    // ──────────────────────────────────────────────────

    private fun wireMainMenuButtons() {
        // Wire version from build config
        findViewById<TextView>(R.id.tvVersion).text = "ΕΚΔ. ${BuildConfig.VERSION_NAME}"

        applyStoneButtonBehavior(findViewById<Button>(R.id.btnShortcuts)).setOnClickListener {
            showScreen(Screen.SHORTCUTS)
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnPosition)).setOnClickListener {
            showScreen(Screen.POSITIONING)
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnCredits)).setOnClickListener {
            showScreen(Screen.CREDITS)
        }

        applyStoneButtonBehavior(findViewById<Button>(R.id.btnStartService)).setOnClickListener {
            if (checkAndRequestPermissions()) {
                startEdgeService()
                serviceEyes.forEach { it.setRunning(true) }   // the eyes open (Phase 7 #1)
            }
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnStopService)).setOnClickListener {
            stopService(Intent(this, SidebarService::class.java))
            serviceEyes.forEach { it.setRunning(false) }      // the eyes close
        }
    }

    private fun wireSubScreenButtons() {
        // Shortcuts screen
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnBackToMenu)).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnSaveShortcuts)).setOnClickListener {
            saveShortcuts()
        }
        // Positioning screen
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnCustomizeSliver)).setOnClickListener {
            openCustomizeSliverDialog()
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnBackToMenuFromPosition)).setOnClickListener {
            showScreen(Screen.MAIN_MENU)
        }
        // Credits screen
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnBackToMenuFromCredits)).setOnClickListener {
            showScreen(Screen.MAIN_MENU)
        }
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnPrivacyPolicy)).setOnClickListener {
            openUrl(getString(R.string.url_privacy_policy))
        }
        // Not a link: this reopens Google's consent form in-app so the choice can be changed or
        // withdrawn. Hidden unless UMP says a consent regime applies to this user.
        applyStoneButtonBehavior(findViewById<Button>(R.id.btnAdConsent)).setOnClickListener {
            adHost?.showPrivacyOptionsForm()
        }
        syncAdConsentButton()
        // The Seal is a FrameLayout, not a Button — it gets the same slab press behaviour.
        applyStoneButtonBehavior(findViewById<View>(R.id.btnDeveloperSeal)).setOnClickListener {
            openUrl(getString(R.string.url_developer_page))
        }
    }

    // ──────────────────────────────────────────────────
    // Ad consent entry point (Credits screen)
    // ──────────────────────────────────────────────────

    /**
     * Shows or hides the AD CONSENT slab.
     *
     * Called once at wiring time and again whenever UMP resolves. Returns the button to GONE
     * rather than INVISIBLE so the action bar reclaims the row's 68dp for everyone outside a
     * consent regime — which, on a utility app, is most people.
     *
     * Not an outbound link: [AdHost.showPrivacyOptionsForm] renders Google's own form in-process.
     */
    private fun syncAdConsentButton() {
        findViewById<Button>(R.id.btnAdConsent)?.visibility =
            if (adHost?.isPrivacyOptionsRequired() == true) View.VISIBLE else View.GONE
    }

    // ──────────────────────────────────────────────────
    // Outbound links (Credits screen)
    // ──────────────────────────────────────────────────

    /**
     * Hands a URL to whatever the user has set as their browser / Play client.
     *
     * NEW_TASK keeps the external page out of EdgeCase's own task, so returning here lands on
     * the Credits screen rather than on a foreign Activity stacked inside our history. Leaving
     * the app fires [onPause], which correctly hands the edge back to the overlay.
     *
     * `url_privacy_policy` is **live** — verified 200 on 2026-08-30 — and must never move, since a
     * privacy-policy URL that 404s is itself a Play policy violation. It is not yet registered in
     * Play Console, because no listing exists yet. `url_developer_page` is still a placeholder,
     * pending the numeric `dev?id=` form from the Play Console. See strings.xml.
     */
    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (_: ActivityNotFoundException) {
            // No browser at all: say so rather than dying silently.
            Toast.makeText(this, "NO PATH TO THE OUTER WORLD", Toast.LENGTH_SHORT).show()
        }
    }

    // ──────────────────────────────────────────────────
    // Customize sliver dialog
    // ──────────────────────────────────────────────────

    private fun openCustomizeSliverDialog() {
        val current = SliverConfig.load(this)
        // The dialog dims the banner and puts its action row right above it — hide the ad for
        // the dialog's lifetime (Docs/Ads.md §3.5, §3.2). Restored however the dialog closes.
        adHost?.setAdVisible(false)
        SliverCustomizeDialog.show(
            context = this,
            initial = current,
            onApplied = { applied ->
                // Reflect on the positioning preview and hot-reload the running overlay.
                positioningView?.setSliverConfig(applied)
                val intent = Intent(this, SidebarService::class.java).apply {
                    action = SidebarService.ACTION_UPDATE_STYLE
                }
                startService(intent)
                Toast.makeText(this, "THE FANG IS FORGED", Toast.LENGTH_SHORT).show()
            },
            onDismissed = { adHost?.setAdVisible(true) }
        )
    }

    // ──────────────────────────────────────────────────
    // Stone button press animation + haptics
    // ──────────────────────────────────────────────────

    /**
     * Press animation + haptic + dust/crack burst, shared by every tappable slab.
     *
     * Generic over [View] rather than typed to [Button]: the Credits screen's Seal is a
     * FrameLayout wrapping an ImageView, and it must feel identical to the stone buttons.
     */
    private fun <T : View> applyStoneButtonBehavior(view: T): T {
        view.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.animate()
                        .translationY(resources.getDimension(R.dimen.stone_button_pressed_translation))
                        .setDuration(80)
                        .start()
                    triggerHaptic(30, 255)
                    dustView?.burst(6)
                    // Fracture the slab at the touch point (Phase 7 #2)
                    crackView?.let { cv ->
                        val loc = IntArray(2)
                        cv.getLocationOnScreen(loc)
                        cv.crackAt(event.rawX - loc[0], event.rawY - loc[1])
                    }
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
        return view
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
    // App list preloading (done once in onCreate) ─────
    // ──────────────────────────────────────────────────

    /** Kick off a background load of the installed-app list so the Shortcuts
     *  screen opens instantly when the user navigates to it. */
    private fun preloadApps() {
        appsLoading = true
        Thread {
            val apps = getInstalledApps()
            synchronized(this) {
                cachedApps = apps
                appsLoading = false
            }
        }.start()
    }

    // ──────────────────────────────────────────────────
    // Shortcuts screen — bipartite initialization
    // ──────────────────────────────────────────────────

    private fun initShortcutsScreen() {
        val ready = synchronized(this) { cachedApps }

        if (ready != null) {
            // Apps already loaded — populate immediately.  Fast path.
            buildShortcutsLists(ready)
        } else {
            // Still loading — set up empty shells and populate asynchronously.
            appsLoading = true
            Thread {
                val apps = getInstalledApps()
                synchronized(this) { cachedApps = apps; appsLoading = false }
                runOnUiThread { buildShortcutsLists(apps) }
            }.start()
        }
    }

    /** Wire up the two RecyclerViews using [allApps] as the data source. */
    private fun buildShortcutsLists(allApps: List<AppInfoData>) {
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

        // Apply saved sliver appearance/geometry to the preview
        positioningView?.setSliverConfig(SliverConfig.load(this))

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

        Toast.makeText(this, "CARVED IN STONE", Toast.LENGTH_SHORT).show()
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
