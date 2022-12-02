package io.igrant.data_wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.data_wallet.R
import io.igrant.data_wallet.listeners.HistoryClickListener
import io.igrant.data_wallet.models.history.History
import io.igrant.data_wallet.utils.DateUtils
import io.igrant.data_wallet.utils.HistoryType

class HistoryAdapter(val list: ArrayList<History>, val mListener: HistoryClickListener) :
    RecyclerView.Adapter<HistoryAdapter.BaseViewHolder>() {
    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    class VerifyViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo) as ImageView
        var tvName: TextView = itemView.findViewById<View>(R.id.tvName) as TextView
        var tvType: TextView = itemView.findViewById<View>(R.id.tvType) as TextView
        var tvDate: TextView = itemView.findViewById(R.id.tvDate) as TextView
        var cvOffer: ConstraintLayout =
            itemView.findViewById<View>(R.id.cvOffer) as ConstraintLayout

        fun onBind(history: History, mListener: HistoryClickListener) {
            val logo = if (history.logo != null) history.logo
            else if (history.connectionHistory != null) history.connectionHistory?.orgDetails?.logoImageUrl
                ?: ""
            else history.connectionV2?.logoImageUrl ?: ""
            try {
                Glide
                    .with(ivLogo.context)
                    .load(logo)
                    .centerCrop()
                    .placeholder(R.drawable.images)
                    .into(ivLogo)
            } catch (e: Exception) {
            }

            tvName.text = (history.name ?: "").toUpperCase()


//            tvType.text =
//                if (history.type == HistoryType.VERIFY ||
//                        history.type == HistoryType.THIRD_PARTY_SHARING) tvType.context.resources.getString(
//                    R.string.welcome_data_using_service
//                ) else tvType.context.resources.getString(R.string.welcome_data_source)

            tvType.text =
                if (history.type == HistoryType.VERIFY || history.type == HistoryType.DATA_PULL) tvType.context.resources.getString(
                    R.string.welcome_data_using_service
                ) else tvType.context.resources.getString(R.string.welcome_data_source)

            if (history.date != null && history.date != "")
                tvDate.text = DateUtils.getRelativeTime(history.date ?: "")
            else
                tvDate.text = "nil"

            cvOffer.setOnClickListener {
                mListener.onClick(
                    history
                )
            }

        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

        return VerifyViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_history,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val history = list[position]


        holder as VerifyViewHolder
        holder.onBind(history, mListener)

    }

    override fun getItemCount(): Int {
        return list.size
    }
}