package io.igrant.data_wallet.adapter

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R

class ReceiptItemListAdapter(
    var itemList: ArrayList<Map<String, String>>
) : RecyclerView.Adapter<ReceiptItemListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvAttributeName: TextView =
            itemView.findViewById<View>(R.id.tvAttributeName) as TextView
        var tvAttributeQuantity: TextView =
            itemView.findViewById<View>(R.id.tvAttributeQuantity) as TextView
        var tvAttributePrice: TextView =
            itemView.findViewById<View>(R.id.tvAttributePrice) as TextView
        var vDivider: View = itemView.findViewById<View>(R.id.vDivider) as View
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_receipt_attribute, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val attributes = itemList[position]

        holder.tvAttributeName.text = attributes["name"]
        holder.tvAttributeQuantity.text = attributes["quantity"]
        holder.tvAttributePrice.text = attributes["value"]
        holder.vDivider.visibility = if (position == itemList.size - 1) View.GONE else View.VISIBLE


        holder.tvAttributeName.setTextColor(
            ContextCompat.getColor(
                holder.tvAttributeName.context,
                R.color.black
            )
        )
        holder.tvAttributeQuantity.setTextColor(
            ContextCompat.getColor(
                holder.tvAttributeName.context,
                R.color.black
            )
        )

        if (position == 0) {
            holder.tvAttributePrice.setTextColor(
                ContextCompat.getColor(
                    holder.tvAttributeName.context,
                    R.color.black
                )
            )
        } else {
            holder.tvAttributePrice.setTextColor(
                ContextCompat.getColor(
                    holder.tvAttributeName.context,
                    R.color.textColorLight
                )
            )
        }

        if (position == 0 || position == itemList.size - 1) {
            holder.tvAttributeName.setTypeface(holder.tvAttributeName.typeface, Typeface.BOLD);
            holder.tvAttributeQuantity.setTypeface(
                holder.tvAttributeQuantity.typeface,
                Typeface.BOLD
            );
            holder.tvAttributePrice.setTypeface(holder.tvAttributePrice.typeface, Typeface.BOLD);
        } else {
            holder.tvAttributeName.setTypeface(holder.tvAttributeName.typeface, Typeface.NORMAL);
            holder.tvAttributeQuantity.setTypeface(
                holder.tvAttributeQuantity.typeface,
                Typeface.NORMAL
            );
            holder.tvAttributePrice.setTypeface(holder.tvAttributePrice.typeface, Typeface.NORMAL);
        }
    }

}