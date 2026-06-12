package com.dicereligion.edgecase

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AppSelectionAdapter(
    private val appList: List<AppInfoData>,
    private val selectedList: MutableList<String>,
    private val onCheckChanged: (String, Boolean) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgIcon: ImageView = view.findViewById(R.id.imgAppIcon)
        val txtName: TextView = view.findViewById(R.id.txtAppName)
        val cbSelect: CheckBox = view.findViewById(R.id.cbAppSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_row, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = appList[position]
        holder.txtName.text = item.appName
        holder.imgIcon.setImageDrawable(item.icon)

        val isSelected = selectedList.contains(item.packageName)
        val orderIndex = if (isSelected) selectedList.indexOf(item.packageName) else -1

        // Null out listener before setting checked state to avoid
        // infinite callback loops during RecyclerView rebinding
        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = isSelected
        holder.cbSelect.text = if (isSelected) "${orderIndex + 1}" else ""
        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            onCheckChanged(item.packageName, isChecked)
        }
    }

    override fun getItemCount(): Int = appList.size
}
