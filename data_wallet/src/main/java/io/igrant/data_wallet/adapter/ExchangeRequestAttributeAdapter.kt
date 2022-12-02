package io.igrant.data_wallet.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.presentationExchange.ExchangeAttributes
import io.igrant.data_wallet.models.selfAttestedCredentials.AttributeTypes
import io.igrant.data_wallet.utils.TextUtils

class ExchangeRequestAttributeAdapter(
    var itemList: ArrayList<ExchangeData>,
    var isBlur: Boolean,
    val width: Int
) :
    RecyclerView.Adapter<ExchangeRequestAttributeAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvAttributeName: TextView =
            itemView.findViewById<View>(R.id.tvAttributeName) as TextView
        var tvAttributeValue: TextView =
            itemView.findViewById<View>(R.id.tvAttributeValue) as TextView
        var tvAttributeValue1: TextView =
            itemView.findViewById<View>(R.id.tvAttributeValue1) as TextView
        var ivAttributeValue: ImageView =
            itemView.findViewById<ImageView>(R.id.ivAttributeValue) as ImageView
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

        if (attributes.dataType == AttributeTypes.IMAGE) {
            holder.tvAttributeName.text = (attributes.name ?: "").replaceElements()?.trim()
            holder.tvAttributeValue.text = ""
            holder.tvAttributeValue.visibility = View.INVISIBLE
            holder.tvAttributeValue1.visibility = View.GONE
            holder.ivAttributeValue.visibility = View.VISIBLE

            try {
                val decodedString = Base64.decode(
                    attributes.data ?: "",
                    if (attributes.imageType == AttributeTypes.BASE_64_DEFAULT) Base64.DEFAULT else Base64.URL_SAFE
                )
                val decodedByte =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                holder.ivAttributeValue.setImageBitmap(decodedByte)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            TextUtils.findTextWidth(
                holder.tvAttributeValue,
                holder.tvAttributeValue1,
                holder.tvAttributeName,
                (attributes.name ?: "").replaceElements()?.trim().toString(),
                attributes.data ?: "",
                isBlur,
                width
            )
            holder.ivAttributeValue.visibility = View.GONE
        }
        holder.vDivider.visibility = if (position == itemList.size - 1) View.GONE else View.VISIBLE
    }

    fun setBlurValue(isBlur: Boolean) {
        this.isBlur = isBlur
        notifyDataSetChanged()
    }
}

private fun String.replaceElements(): CharSequence {
    return this.replace("PKPASS BoardingPass", "").replace("my-", "")
}
