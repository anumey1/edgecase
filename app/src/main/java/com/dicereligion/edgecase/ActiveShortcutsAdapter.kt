package com.dicereligion.edgecase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for the Altar (top 30%) — the current shortcuts
 * working set. Items can be toggled (selected/unselected), and the list
 * supports drag-and-drop reordering via ItemTouchHelper.
 */
class ActiveShortcutsAdapter(
    private val stateManager: ShortcutStateManager,
    private val onToggleSelection: (Int) -> Unit
) : RecyclerView.Adapter<ActiveShortcutsAdapter.AltarViewHolder>() {

    class AltarViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAltarIcon)
        val tvName: TextView = view.findViewById(R.id.tvAltarName)
        val tvOrder: TextView = view.findViewById(R.id.tvOrderNumber)
        val cbSelect: CheckBox = view.findViewById(R.id.cbAltarSelect)
        val dragHandle: ImageView = view.findViewById(R.id.ivDragHandle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AltarViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_item_shortcut_tile, parent, false)
        return AltarViewHolder(view)
    }

    override fun onBindViewHolder(holder: AltarViewHolder, position: Int) {
        val item = stateManager.altarItems[position]
        val appInfo = stateManager.getAppInfo(item.packageName)

        holder.tvOrder.text = "${position + 1}"

        holder.tvName.text = appInfo?.appName ?: item.packageName
        holder.ivIcon.setImageDrawable(appInfo?.icon)
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = item.isSelected
        holder.cbSelect.setOnCheckedChangeListener { _, _ ->
            onToggleSelection(holder.bindingAdapterPosition)
        }

        // Visual feedback: dim unselected items
        holder.itemView.alpha = if (item.isSelected) 1.0f else 0.5f
    }

    override fun getItemCount(): Int = stateManager.altarItems.size

    /** Called by ItemTouchHelper after a move to notify the adapter. */
    fun onItemMove(fromPos: Int, toPos: Int) {
        stateManager.moveAltarItem(fromPos, toPos)
        notifyItemMoved(fromPos, toPos)
    }
}
