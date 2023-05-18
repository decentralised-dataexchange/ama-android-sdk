package io.igrant.data_wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.utils.TextUtils

class CertificateAttributeAdapterV2(
    var itemList: ArrayList<Map<String,String>>,
    var isBlur: Boolean,
    var labelColor: Int?,
    var textColor: Int?,
    val width: Int
) : RecyclerView.Adapter<CertificateAttributeAdapterV2.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvAttributeName: TextView = itemView.findViewById<View>(R.id.tvAttributeName) as TextView
        var tvAttributeValue: TextView = itemView.findViewById<View>(R.id.tvAttributeValue) as TextView
        var tvAttributeValue1: TextView = itemView.findViewById<View>(R.id.tvAttributeValue1) as TextView
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
//        holder.tvAttributeName.text = getCapsSentences(attributes.name ?: "")
        holder.tvAttributeName.visibility = if ((attributes["name"]
                        ?: "") == "") View.GONE else View.VISIBLE
        TextUtils.findTextWidth(
            holder.tvAttributeValue,
            holder.tvAttributeValue1,
            holder.tvAttributeName,
            attributes["name"] ?: "",
            attributes["value"] ?: "",
            isBlur,
            width)
        holder.vDivider.visibility = if (position == itemList.size - 1) View.GONE else View.VISIBLE

        if (labelColor != null) {
            holder.tvAttributeName.setTextColor(labelColor ?: -1)
            holder.vDivider.setBackgroundColor(labelColor ?: -1)
        }

        if (textColor != null) {
            holder.tvAttributeValue.setTextColor(textColor ?: -1)
            holder.tvAttributeValue1.setTextColor(textColor ?: -1)
        }
    }

    fun setUpBlur(isBlur: Boolean){
        this.isBlur = isBlur
        itemList.mapIndexed { index, attributes ->
            notifyItemChanged(index)
        }
    }
}