package com.dicereligion.edgecase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for the Archives (bottom 60%) — all installed apps
 * with checkboxes. Checking/unchecking immediately adds/removes from the
 * Altar via [onToggle].
 */
class AvailableAppsAdapter(
    private val stateManager: ShortcutStateManager,
    private val onToggle: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AvailableAppsAdapter.ArchiveViewHolder>() {

    class ArchiveViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivArchiveIcon)
        val tvName: TextView = view.findViewById(R.id.tvArchiveName)
        val cbSelect: CheckBox = view.findViewById(R.id.cbArchiveSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_available_app, parent, false)
        return ArchiveViewHolder(view)
    }

    override fun onBindViewHolder(holder: ArchiveViewHolder, position: Int) {
        val app = stateManager.allApps[position]
        holder.tvName.text = app.appName
        holder.ivIcon.setImageDrawable(app.icon)

        // Determine checked state: app is active if it's in the Altar AND selected
        val isChecked = stateManager.isActiveShortcut(app.packageName)

        // Null out listener to avoid rebind loops
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = isChecked
        holder.cbSelect.setOnCheckedChangeListener { _, checked ->
            onToggle(app.packageName, checked)
        }
    }

    override fun getItemCount(): Int = stateManager.allApps.size
}
