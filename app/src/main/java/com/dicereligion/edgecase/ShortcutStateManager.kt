package com.dicereligion.edgecase

import android.content.Context
import java.util.Collections

/**
 * Manages the dual-list state for the Shortcuts configuration screen.
 *
 * Architecture:
 * - [altarItems]: What's currently shown in the Altar (top 30%). Each item has
 *   a package name and an isSelected flag. Selected items survive Save;
 *   unselected items are removed from the Altar on Save.
 * - [allApps]: All installed launchable apps (the Archives data source).
 * - [committedList]: Snapshot of what was saved in SharedPreferences when the
 *   screen opened — used for dirty-checking and discard.
 *
 * Rules:
 * - Toggling in the Archives (bottom list): immediately adds/removes from altarItems.
 * - Toggling in the Altar (top list): only flips isSelected; item stays visible
 *   until Save, at which point unselected items are evicted.
 * - Reordering in the Altar: directly mutates altarItems order.
 * - Save: writes the ordered list of selected altarItems to SharedPreferences.
 * - Discard: resets altarItems to the committedList.
 */
class ShortcutStateManager(context: Context, allInstalledApps: List<AppInfoData>) {

    /** An item in the Altar (top shortcuts list). */
    data class AltarItem(
        val packageName: String,
        var isSelected: Boolean = true
    )

    // ── Data sources ──────────────────────────────────
    private val prefs = context.getSharedPreferences("EdgeCasePrefs", Context.MODE_PRIVATE)

    /** Every installed launchable app (immutable after construction). */
    val allApps: List<AppInfoData> = allInstalledApps

    /** The working set shown in the Altar. This is the single source of truth
     *  during editing. Items with isSelected=false are shown but will be
     *  removed on commit. */
    val altarItems: MutableList<AltarItem> = mutableListOf()

    /** Snapshot of the persisted shortcut list when the screen opened. */
    private var committedList: List<String> = emptyList()

    // ── Initialisation ────────────────────────────────

    init {
        loadFromPreferences()
    }

    private fun loadFromPreferences() {
        val orderStr = prefs.getString("saved_shortcuts_order", null)
        committedList = if (!orderStr.isNullOrEmpty()) {
            orderStr.split(",").filter { it.isNotEmpty() }
        } else {
            (prefs.getStringSet("saved_shortcuts", emptySet()) ?: emptySet()).toList()
        }
        // Populate altarItems from committed list — all start as selected
        altarItems.clear()
        for (pkg in committedList) {
            // Only add if the app is still installed
            if (allApps.any { it.packageName == pkg }) {
                altarItems.add(AltarItem(pkg, isSelected = true))
            }
        }
    }

    // ── Queries ───────────────────────────────────────

    /** Whether [pkg] is currently in the Altar AND selected (will survive Save). */
    fun isActiveShortcut(pkg: String): Boolean {
        return altarItems.any { it.packageName == pkg && it.isSelected }
    }

    /** Whether [pkg] exists in the Altar at all (selected or not). */
    fun isInAltar(pkg: String): Boolean {
        return altarItems.any { it.packageName == pkg }
    }

    /** True if the current selected state differs from what was committed. */
    fun isDirty(): Boolean {
        val currentSelection = altarItems.filter { it.isSelected }.map { it.packageName }
        return currentSelection != committedList
    }

    // ── Archives (bottom list) operations ─────────────

    /**
     * Set an app's presence in the Altar from the Archives checkbox.
     * - If [add] is true: ensure the app is in the Altar and selected
     *   (re-selects if previously unselected in-Altar, or adds fresh).
     * - If [add] is false: remove the app from the Altar entirely.

     */
    fun setFromArchives(pkg: String, add: Boolean) {
        if (add) {
            val existing = altarItems.indexOfFirst { it.packageName == pkg }
            if (existing >= 0) {
                // Re-select an item that was unselected in the Altar
                altarItems[existing].isSelected = true
            } else {
                altarItems.add(AltarItem(pkg, isSelected = true))
            }
        } else {
            altarItems.removeAll { it.packageName == pkg }
        }
    }

    // ── Altar (top list) operations ───────────────────

    /** Toggle the selected state of the Altar item at [position].
     *  Returns the new isSelected value. */
    fun toggleAltarSelection(position: Int): Boolean {
        val item = altarItems[position]
        item.isSelected = !item.isSelected
        return item.isSelected
    }

    /** Move an Altar item from [fromPos] to [toPos]. */
    fun moveAltarItem(fromPos: Int, toPos: Int) {
        if (fromPos < toPos) {
            for (i in fromPos until toPos) {
                Collections.swap(altarItems, i, i + 1)
            }
        } else {
            for (i in fromPos downTo toPos + 1) {
                Collections.swap(altarItems, i, i - 1)
            }
        }
    }

    /** Get the AppInfoData for a given package name (for icon/name lookup). */
    fun getAppInfo(pkg: String): AppInfoData? {
        return allApps.firstOrNull { it.packageName == pkg }
    }

    // ── Commit / Discard ──────────────────────────────

    /** Persist the current selected Altar items (in order) to SharedPreferences.
     *  Removes unselected items from the Altar. */
    fun commit() {
        val selected = altarItems.filter { it.isSelected }.map { it.packageName }
        prefs.edit()
            .putString("saved_shortcuts_order", selected.joinToString(","))
            .putStringSet("saved_shortcuts", selected.toHashSet())
            .apply()
        // Remove unselected items from the Altar visual list
        altarItems.removeAll { !it.isSelected }
        committedList = selected
    }

    /** Discard all in-memory changes, reset to the last committed state. */
    fun discard() {
        altarItems.clear()
        for (pkg in committedList) {
            if (allApps.any { it.packageName == pkg }) {
                altarItems.add(AltarItem(pkg, isSelected = true))
            }
        }
    }
}
