package io.igrant.data_wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.listeners.ConnectionFilterClickListener
import io.igrant.data_wallet.models.ConnectionFilter
import io.igrant.data_wallet.utils.DeviceUtils

class ConnectionFilterAdapter(
    var list: ArrayList<ConnectionFilter>,
    var mListener: ConnectionFilterClickListener,
    var selectedId: Int? = 0
) :
    RecyclerView.Adapter<ConnectionFilterAdapter.ViewHolder>() {
    var selectedFilter: Int = selectedId?:0

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvFilterName: TextView = itemView.findViewById<View>(R.id.tvFilterName) as TextView
        var ivFilter: ImageView = itemView.findViewById(R.id.ivFilter)
        var rbFilter: RadioButton = itemView.findViewById(R.id.cbFilter)
        var clItem: ConstraintLayout = itemView.findViewById(R.id.clItem)
        var vDisabled: View = itemView.findViewById(R.id.vDisabled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_connection_filter,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val connectionFilter = list[position]
        holder.tvFilterName.text = DeviceUtils.capitalize(connectionFilter.name ?: "")
        holder.ivFilter.visibility = View.GONE
        connectionFilter.logo?.let {
            holder.ivFilter.visibility = View.VISIBLE
            holder.ivFilter.setImageResource(it)
        }
        holder.rbFilter.isChecked = selectedFilter == position

        holder.vDisabled.visibility = if (connectionFilter.isEnabled) View.GONE else View.VISIBLE

        holder.clItem.setOnClickListener {
            selectedFilter = position
            mListener.onFilterClick(connectionFilter)
            notifyDataSetChanged()
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }
}