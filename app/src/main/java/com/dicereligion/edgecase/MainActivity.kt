package com.dicereligion.edgecase

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var appAdapter: AppSelectionAdapter
    private val selectedApps = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        loadSavedConfig()

        val rvApps = findViewById<RecyclerView>(R.id.rvAppList)
        rvApps.layoutManager = LinearLayoutManager(this)
        appAdapter = AppSelectionAdapter(getInstalledApps(), selectedApps) { pkg, isChecked ->
            if (isChecked) {
                selectedApps.add(pkg)
            } else {
                selectedApps.remove(pkg)
            }
            saveConfig()
            appAdapter.notifyDataSetChanged()

            // Hot-reload: notify the running service to refresh its shortcut tray
            val updateIntent = Intent(this, SidebarService::class.java).apply {
                action = SidebarService.ACTION_UPDATE_SHORTCUTS
            }
            startService(updateIntent)
        }
        rvApps.adapter = appAdapter

        findViewById<Button>(R.id.btnStartService).setOnClickListener {
            if (checkAndRequestPermissions()) startEdgeService()
        }
        findViewById<Button>(R.id.btnStopService).setOnClickListener {
            stopService(Intent(this, SidebarService::class.java))
        }
    }

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

    private fun saveConfig() {
        getSharedPreferences("EdgeCasePrefs", Context.MODE_PRIVATE)
            .edit()
            .putString("saved_shortcuts_order", selectedApps.joinToString(","))
            .putStringSet("saved_shortcuts", selectedApps.toHashSet())
            .apply()
    }

    private fun loadSavedConfig() {
        val prefs = getSharedPreferences("EdgeCasePrefs", Context.MODE_PRIVATE)
        val orderStr = prefs.getString("saved_shortcuts_order", null)
        if (!orderStr.isNullOrEmpty()) {
            selectedApps.addAll(orderStr.split(",").filter { it.isNotEmpty() })
        } else {
            val saved = prefs.getStringSet("saved_shortcuts", null)
            if (saved != null) selectedApps.addAll(saved)
        }
    }
}
