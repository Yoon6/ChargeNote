package com.daeyoon.chargenote

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.daeyoon.chargenote.data.DrivingRecord
import com.daeyoon.chargenote.data.RecordUiData
import java.text.SimpleDateFormat
import java.util.Locale

class RecordListAdapter(
    private val itemClickListener: OnItemClickListener
) : ListAdapter<RecordUiData, RecordListAdapter.ViewHolder>(DIFF_CALLBACK) {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder)
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEfficiency: TextView
        val tvDate: TextView
        val tvTripMileage: TextView
        val tvTotalAmount: TextView

        init {
            // Define click listener for the ViewHolder's View
            tvEfficiency = view.findViewById(R.id.tv_efficiency)
            tvDate = view.findViewById(R.id.tv_date)
            tvTripMileage = view.findViewById(R.id.tv_trip_mileage)
            tvTotalAmount = view.findViewById(R.id.tv_totalAmount)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_record, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val item = getItem(position)
        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.tvEfficiency.text = item.efficiency
        viewHolder.tvTotalAmount.text = item.totalAmount
        viewHolder.tvTripMileage.text = item.tripMileage

        viewHolder.tvDate.text = item.date
        viewHolder.itemView.setOnClickListener {
            itemClickListener.onItemClick(item)
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<RecordUiData>() {
            override fun areItemsTheSame(oldItem: RecordUiData, newItem: RecordUiData) =
                oldItem.uid == newItem.uid

            override fun areContentsTheSame(oldItem: RecordUiData, newItem: RecordUiData) =
                oldItem == newItem
        }
    }

}