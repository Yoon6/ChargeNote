package com.daeyoon.chargenote

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class LeafItemDecoration : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0

        val margin = view.resources.getDimensionPixelSize(R.dimen.margin_s)
        if (position == 0) {
            outRect.top = margin
        }
        if (position == itemCount - 1) {
            outRect.bottom = margin
        }
    }
}