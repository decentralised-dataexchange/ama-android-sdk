package io.igrant.data_wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.models.certificateOffer.Attributes

class RequestAttributeAdapter(var itemList: ArrayList<Attributes>) :
    RecyclerView.Adapter<RequestAttributeAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvAttributeName: TextView =
            itemView.findViewById<View>(R.id.tvAttributeName) as TextView
        var tvAttributeValue: TextView =
            itemView.findViewById<View>(R.id.tvAttributeValue) as TextView
        var vDivider: View = itemView.findViewById<View>(R.id.vDivider) as View
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_certificate_attribute, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attributes = itemList[position]
        holder.tvAttributeName.text = attributes.name ?: ""
        holder.tvAttributeValue.text = attributes.value ?: ""

        holder.vDivider.visibility = if (position == itemList.size - 1) View.GONE else View.VISIBLE
    }
}