\# \*\*EdgeCase: High-Performance Android Gesture Sidebar\*\*

\#\# \*\*Technical Architecture & Complete Implementation Specification\*\*

This document details the architectural layout, system permission structures, resource-efficient background mechanics, and source code required to build a highly optimized, custom application-shortcut sidebar for Android devices.

\---

\#\# \*\*1. Project Goal & Design Requirements\*\*

The objective is to engineer a performance-critical Android system overlay that runs at a native 120Hz refresh rate with minimal CPU and battery impact.

\#\#\# \*\*Core Features\*\*

1\.  \*\*Interactive Configuration Activity:\*\* A clean setup screen to display all installed user and system apps, select target shortcuts, and store configurations.  
2\.  \*\*Persistent Foreground Edge Service:\*\* A service that runs continuously in the background, utilizing Android's low-level window composition tree (\`WindowManager\`) to capture gestures independently of the main app's lifecycle.  
3\.  \*\*Ergonomic Trigger Sliver:\*\* A discrete, vertical rectangle (12dp width) positioned on the right edge of the display. It is shifted upward away from the bottom navigation zone to prevent interaction conflicts.  
4\.  \*\*Intuitive Inward Swipe Gesture:\*\* Swiping horizontally leftward over the sliver replaces the sliver with a compact app launcher shortcut panel.  
5\.  \*\*Compact Fluid Expansion Tray:\*\* A high-speed UI layout presenting selected apps. The tray is height-limited, scrollable, and vertically centered relative to the trigger sliver for ergonomic thumb access.  
6\.  \*\*Instant Launch & Dismiss:\*\* Tapping an icon launches the respective application and immediately collapses the tray to return the system to the low-footprint sliver state.

\---

\#\# \*\*2. Technical Challenges & Optimization Philosophy\*\*

\#\#\# \*\*A. Bypassing System Navigation Conflicts\*\*  
Android's gesture-based navigation assigns "Swipe Inward from Left/Right" as the universal \*\*Back\*\* command.  
\*   \*\*Exclusion Rects:\*\* We utilize \`View.setSystemGestureExclusionRects()\` on the sliver view. This explicitly tells the Android OS to ignore the system "Back" gesture within the sliver's precise pixel boundaries.  
\*   \*\*Strategic Positioning:\*\* The sliver is placed in the bottom-right quadrant but offset significantly above the navigation bar dead-zone (approx. 110dp vertical offset).

\#\#\# \*\*B. Process Survival & Persistence\*\*  
\*   \*\*Foreground Service:\*\* The engine runs inside a dedicated \`SidebarService\` bound to a high-priority system channel with a persistent notification. This prevents the OS from killing the process even when the \`MainActivity\` is closed.  
\*   \*\*Battery Optimization Bypass:\*\* The app explicitly requests to ignore battery optimizations to ensure the background gesture engine remains responsive.

\#\#\# \*\*C. Rendering Efficiency\*\*  
\*   \*\*Flat UI Composition:\*\* Rendering utilizes GPU-friendly Hex color fills with fixed transparency (e.g., \`\#E6121212\`) to avoid the overhead of system-level blur filters.  
\*   \*\*WindowManager Priority:\*\* Views are registered with \`FLAG\_NOT\_FOCUSABLE\` and \`FLAG\_LAYOUT\_IN\_SCREEN\` to intercept touches with minimal latency.

\---

\#\# \*\*3. Project Configuration\*\*

\#\#\# \*\*AndroidManifest.xml\*\*  
Specifies necessary system permissions and package visibility queries for Android 11+.

\`\`\`xml  
\<?xml version="1.0" encoding="utf-8"?\>  
\<manifest xmlns:android="http://schemas.android.com/apk/res/android"\>

    \<uses-permission android:name="android.permission.SYSTEM\_ALERT\_WINDOW" /\>  
    \<uses-permission android:name="android.permission.FOREGROUND\_SERVICE" /\>  
    \<uses-permission android:name="android.permission.FOREGROUND\_SERVICE\_SPECIAL\_USE" /\>  
    \<uses-permission android:name="android.permission.REQUEST\_IGNORE\_BATTERY\_OPTIMIZATIONS" /\>

    \<queries\>  
        \<intent\>  
            \<action android:name="android.intent.action.MAIN" /\>  
            \<category android:name="android.intent.category.LAUNCHER" /\>  
        \</intent\>  
    \</queries\>

    \<application  
        android:label="EdgeCase"  
        android:theme="@style/Theme.AppCompat.DayNight.NoActionBar"\>  
          
        \<activity  
            android:name=".MainActivity"  
            android:exported="true"\>  
            \<intent-filter\>  
                \<action android:name="android.intent.action.MAIN" /\>  
                \<category android:name="android.intent.category.LAUNCHER" /\>  
            \</intent-filter\>  
        \</activity\>

        \<service  
            android:name=".SidebarService"  
            android:enabled="true"  
            android:exported="false"  
            android:foregroundServiceType="specialUse" /\>  
    \</application\>  
\</manifest\>  
\`\`\`

\---

\#\# \*\*4. Main Configuration Interface (MainActivity.kt)\*\*

Handles app listing, shortcut selection persistence via \`SharedPreferences\`, and service lifecycle control.

\`\`\`kotlin  
package com.dicereligion.edgecase

import android.content.Context  
import android.content.Intent  
import android.content.pm.PackageManager  
import android.net.Uri  
import android.os.Build  
import android.os.Bundle  
import android.os.PowerManager  
import android.provider.Settings  
import android.view.LayoutInflater  
import android.view.View  
import android.view.ViewGroup  
import android.widget.\*  
import androidx.appcompat.app.AppCompatActivity  
import androidx.recyclerview.widget.LinearLayoutManager  
import androidx.recyclerview.widget.RecyclerView

class MainActivity : AppCompatActivity() {  
    private lateinit var appAdapter: AppSelectionAdapter  
    private val selectedApps \= HashSet\<String\>()

    override fun onCreate(savedInstanceState: Bundle?) {  
        super.onCreate(savedInstanceState)  
        setContentView(R.layout.activity\_main)  
        loadSavedConfig()

        val rvApps \= findViewById\<RecyclerView\>(R.id.rvAppList)  
        rvApps.layoutManager \= LinearLayoutManager(this)  
        appAdapter \= AppSelectionAdapter(getInstalledApps(), selectedApps) { pkg, isChecked \-\>  
            if (isChecked) selectedApps.add(pkg) else selectedApps.remove(pkg)  
            saveConfig()  
              
            // Hot-reload service  
            val updateIntent \= Intent(this, SidebarService::class.java).apply {  
                action \= SidebarService.ACTION\_UPDATE\_SHORTCUTS  
            }  
            startService(updateIntent)  
        }  
        rvApps.adapter \= appAdapter

        findViewById\<Button\>(R.id.btnStartService).setOnClickListener {  
            if (checkAndRequestPermissions()) startEdgeService()  
        }  
        findViewById\<Button\>(R.id.btnStopService).setOnClickListener {  
            stopService(Intent(this, SidebarService::class.java))  
        }  
    }

    private fun checkAndRequestPermissions(): Boolean {  
        if (\!Settings.canDrawOverlays(this)) {  
            startActivity(Intent(Settings.ACTION\_MANAGE\_OVERLAY\_PERMISSION, Uri.parse("package:$packageName")))  
            return false  
        }  
        val pm \= getSystemService(Context.POWER\_SERVICE) as PowerManager  
        if (\!pm.isIgnoringBatteryOptimizations(packageName)) {  
            startActivity(Intent(Settings.ACTION\_REQUEST\_IGNORE\_BATTERY\_OPTIMIZATIONS, Uri.parse("package:$packageName")))  
        }  
        return true  
    }

    private fun startEdgeService() {  
        val intent \= Intent(this, SidebarService::class.java)  
        if (Build.VERSION.SDK\_INT \>= Build.VERSION\_CODES.O) startForegroundService(intent) else startService(intent)  
    }

    private fun getInstalledApps(): List\<AppInfoData\> {  
        val list \= ArrayList\<AppInfoData\>()  
        val pm \= packageManager  
        val intent \= Intent(Intent.ACTION\_MAIN, null).apply { addCategory(Intent.CATEGORY\_LAUNCHER) }  
        val resolveInfos \= pm.queryIntentActivities(intent, 0\)  
        for (ri in resolveInfos) {  
            list.add(AppInfoData(ri.loadLabel(pm).toString(), ri.activityInfo.packageName, ri.loadIcon(pm)))  
        }  
        return list.sortedBy { it.appName }  
    }

    private fun saveConfig() {  
        getSharedPreferences("EdgeCasePrefs", Context.MODE\_PRIVATE).edit().putStringSet("saved\_shortcuts", selectedApps).apply()  
    }

    private fun loadSavedConfig() {  
        val saved \= getSharedPreferences("EdgeCasePrefs", Context.MODE\_PRIVATE).getStringSet("saved\_shortcuts", null)  
        if (saved \!= null) selectedApps.addAll(saved)  
    }  
}

data class AppInfoData(val appName: String, val packageName: String, val icon: android.graphics.drawable.Drawable)

class AppSelectionAdapter(  
    private val appList: List\<AppInfoData\>,  
    private val selectedList: HashSet\<String\>,  
    private val onCheckChanged: (String, Boolean) \-\> Unit  
) : RecyclerView.Adapter\<AppSelectionAdapter.AppViewHolder\>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {  
        val imgIcon: ImageView \= view.findViewById(R.id.imgAppIcon)  
        val txtName: TextView \= view.findViewById(R.id.txtAppName)  
        val cbSelect: CheckBox \= view.findViewById(R.id.cbAppSelect)  
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {  
        val view \= LayoutInflater.from(parent.context).inflate(R.layout.item\_app\_row, parent, false)  
        return AppViewHolder(view)  
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {  
        val item \= appList\[position\]  
        holder.txtName.text \= item.appName  
        holder.imgIcon.setImageDrawable(item.icon)  
        holder.cbSelect.setOnCheckedChangeListener(null)  
        holder.cbSelect.isChecked \= selectedList.contains(item.packageName)  
        holder.cbSelect.setOnCheckedChangeListener { \_, isChecked \-\> onCheckChanged(item.packageName, isChecked) }  
    }

    override fun getItemCount(): Int \= appList.size  
}  
\`\`\`

\---

\#\# \*\*5. Persistent Foreground Edge Service (SidebarService.kt)\*\*

The core engine for gesture capture and overlay management.

\`\`\`kotlin  
package com.dicereligion.edgecase

import android.app.\*  
import android.content.\*  
import android.graphics.\*  
import android.os.\*  
import android.util.DisplayMetrics  
import android.view.\*  
import android.widget.\*  
import androidx.core.app.NotificationCompat  
import java.util.Collections  
import kotlin.math.abs

class SidebarService : Service() {  
    companion object {  
        const val ACTION\_UPDATE\_SHORTCUTS \= "com.dicereligion.edgecase.UPDATE\_SHORTCUTS"  
        private const val CHANNEL\_ID \= "EdgeCaseEngineChannel"  
        private const val NOTIFICATION\_ID \= 9182  
        private const val SWIPE\_THRESHOLD\_X \= 30   
        private const val MAX\_SWIPE\_DEVIATION\_Y \= 150  
    }

    private lateinit var windowManager: WindowManager  
    private lateinit var sliverView: View  
    private lateinit var trayView: View  
    private lateinit var sliverParams: WindowManager.LayoutParams  
    private lateinit var trayParams: WindowManager.LayoutParams  
    private var densityDpi: Float \= 1.0f

    override fun onBind(intent: Intent?): IBinder? \= null

    override fun onCreate() {  
        super.onCreate()  
        windowManager \= getSystemService(Context.WINDOW\_SERVICE) as WindowManager  
        densityDpi \= resources.displayMetrics.density  
          
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
        if (intent?.action \== ACTION\_UPDATE\_SHORTCUTS) refreshTrayUiElements()  
        return START\_STICKY  
    }

    private fun instantiateWindowParameters() {  
        val sliverWidthPx \= (12 \* densityDpi).toInt()   
        val sliverHeightPx \= (150 \* densityDpi).toInt()   
        val bottomOffsetPx \= (110 \* densityDpi).toInt() 

        sliverParams \= WindowManager.LayoutParams(  
            sliverWidthPx, sliverHeightPx,  
            WindowManager.LayoutParams.TYPE\_APPLICATION\_OVERLAY,  
            WindowManager.LayoutParams.FLAG\_NOT\_FOCUSABLE or WindowManager.LayoutParams.FLAG\_LAYOUT\_IN\_SCREEN or   
            WindowManager.LayoutParams.FLAG\_LAYOUT\_NO\_LIMITS or WindowManager.LayoutParams.FLAG\_WATCH\_OUTSIDE\_TOUCH,  
            PixelFormat.TRANSLUCENT  
        ).apply {  
            gravity \= Gravity.END or Gravity.BOTTOM  
            y \= bottomOffsetPx   
        }

        val trayWidthPx \= (80 \* densityDpi).toInt()   
        val trayHeightPx \= (sliverHeightPx \* 2\)   
        trayParams \= WindowManager.LayoutParams(  
            trayWidthPx, trayHeightPx,   
            WindowManager.LayoutParams.TYPE\_APPLICATION\_OVERLAY,  
            WindowManager.LayoutParams.FLAG\_NOT\_FOCUSABLE or WindowManager.LayoutParams.FLAG\_LAYOUT\_IN\_SCREEN or  
            WindowManager.LayoutParams.FLAG\_WATCH\_OUTSIDE\_TOUCH,  
            PixelFormat.TRANSLUCENT  
        ).apply {  
            gravity \= Gravity.END or Gravity.BOTTOM   
            // Center tray on sliver vertically  
            y \= bottomOffsetPx \- (trayHeightPx / 2\) \+ (sliverHeightPx / 2\)  
        }  
    }

    private fun assembleSliverView() {  
        sliverView \= View(this).apply {  
            setBackgroundColor(Color.argb(40, 255, 255, 255))  
            if (Build.VERSION.SDK\_INT \>= Build.VERSION\_CODES.Q) {  
                addOnLayoutChangeListener { \_, \_, \_, \_, \_, \_, \_, \_, \_ \-\>  
                    val rect \= Rect(0, 0, width, height)  
                    systemGestureExclusionRects \= Collections.singletonList(rect)  
                }  
            }  
        }

        var startX \= 0f  
        var startY \= 0f  
        sliverView.setOnTouchListener { v, event \-\>  
            when (event.action) {  
                MotionEvent.ACTION\_DOWN \-\> {  
                    startX \= event.rawX  
                    startY \= event.rawY  
                    true  
                }  
                MotionEvent.ACTION\_MOVE \-\> {  
                    val deltaX \= startX \- event.rawX   
                    val deltaY \= abs(event.rawY \- startY)  
                    if (deltaX \> SWIPE\_THRESHOLD\_X && deltaY \< MAX\_SWIPE\_DEVIATION\_Y) {  
                        transitionToExpandedTray()  
                        true  
                    } else false  
                }  
                MotionEvent.ACTION\_UP \-\> { v.performClick(); false }  
                else \-\> false  
            }  
        }  
    }

    private fun assembleTrayView() {  
        val scrollContainer \= ScrollView(this).apply {  
            isVerticalScrollBarEnabled \= false  
            setBackgroundColor(Color.parseColor("\#E6121212"))  
        }  
        val layoutList \= LinearLayout(this).apply {  
            orientation \= LinearLayout.VERTICAL  
            gravity \= Gravity.CENTER\_HORIZONTAL  
            setPadding(0, (16 \* densityDpi).toInt(), 0, (16 \* densityDpi).toInt())  
        }  
        scrollContainer.addView(layoutList)  
        trayView \= scrollContainer  
        populateShortcuts(layoutList)

        trayView.setOnTouchListener { v, event \-\>  
            if (event.action \== MotionEvent.ACTION\_OUTSIDE) {  
                transitionToSliverState()  
                true  
            } else if (event.action \== MotionEvent.ACTION\_UP) { v.performClick(); false } else false  
        }  
    }

    private fun populateShortcuts(container: LinearLayout) {  
        val savedSet \= getSharedPreferences("EdgeCasePrefs", Context.MODE\_PRIVATE).getStringSet("saved\_shortcuts", emptySet()) ?: emptySet()  
        val pm \= packageManager  
        container.removeAllViews()  
        for (packageName in savedSet) {  
            try {  
                val imgView \= ImageView(this).apply {  
                    val sideSize \= (48 \* densityDpi).toInt()  
                    layoutParams \= LinearLayout.LayoutParams(sideSize, sideSize).apply { setMargins(0, (16 \* densityDpi).toInt(), 0, (16 \* densityDpi).toInt()) }  
                    setImageDrawable(pm.getApplicationIcon(packageName))  
                    scaleType \= ImageView.ScaleType.FIT\_CENTER  
                    setOnClickListener {  
                        pm.getLaunchIntentForPackage(packageName)?.apply {  
                            addFlags(Intent.FLAG\_ACTIVITY\_NEW\_TASK)  
                            startActivity(this)  
                            transitionToSliverState()  
                        }  
                    }  
                }  
                container.addView(imgView)  
            } catch (e: Exception) {}  
        }  
    }

    private fun refreshTrayUiElements() {  
        if (::trayView.isInitialized) populateShortcuts((trayView as ScrollView).getChildAt(0) as LinearLayout)  
    }

    private fun transitionToExpandedTray() {  
        if (sliverView.isAttachedToWindow) windowManager.removeView(sliverView)  
        if (\!trayView.isAttachedToWindow) { refreshTrayUiElements(); windowManager.addView(trayView, trayParams) }  
    }

    private fun transitionToSliverState() {  
        if (trayView.isAttachedToWindow) windowManager.removeView(trayView)  
        if (\!sliverView.isAttachedToWindow) windowManager.addView(sliverView, sliverParams)  
    }

    private fun buildSystemNotification() {  
        if (Build.VERSION.SDK\_INT \>= Build.VERSION\_CODES.O) {  
            val channel \= NotificationChannel(CHANNEL\_ID, "EdgeCase Engine Active", NotificationManager.IMPORTANCE\_LOW)  
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)  
        }  
        val notification \= NotificationCompat.Builder(this, CHANNEL\_ID)  
            .setContentTitle("EdgeCase Active").setContentText("Listening for gestures.")  
            .setSmallIcon(android.R.drawable.ic\_dialog\_info).setPriority(NotificationCompat.PRIORITY\_LOW).build()  
        startForeground(NOTIFICATION\_ID, notification)  
    }

    override fun onDestroy() {  
        super.onDestroy()  
        if (::sliverView.isInitialized && sliverView.isAttachedToWindow) windowManager.removeView(sliverView)  
        if (::trayView.isInitialized && trayView.isAttachedToWindow) windowManager.removeView(trayView)  
    }  
}  
\`\`\`  
