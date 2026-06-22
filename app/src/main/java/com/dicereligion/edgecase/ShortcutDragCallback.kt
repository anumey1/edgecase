package com.dicereligion.edgecase

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView

/**
 * ItemTouchHelper.Callback that enables long-press drag-to-reorder
 * within the Altar RecyclerView. Swipe-to-dismiss is disabled —
 * removal is handled via the checkbox toggle.
 */
class ShortcutDragCallback(
    private val adapter: ActiveShortcutsAdapter
) : ItemTouchHelper.Callback() {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val dragFlags = ItemTouchHelper.UP or ItemTouchHelper.DOWN
        return makeMovementFlags(dragFlags, 0) // no swipe flags
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPos = viewHolder.bindingAdapterPosition
        val toPos = target.bindingAdapterPosition
        adapter.onItemMove(fromPos, toPos)
        return true
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // No-op — swipe-to-dismiss is disabled
    }

    override fun isLongPressDragEnabled(): Boolean = true

    override fun isItemViewSwipeEnabled(): Boolean = false

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && viewHolder != null) {
            // Lift effect: scale up slightly during drag
            viewHolder.itemView.animate()
                .scaleX(1.05f)
                .scaleY(1.05f)
                .setDuration(150)
                .start()
        } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE && viewHolder != null) {
            viewHolder.itemView.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(150)
                .start()
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .setDuration(150)
            .start()
    }
}
