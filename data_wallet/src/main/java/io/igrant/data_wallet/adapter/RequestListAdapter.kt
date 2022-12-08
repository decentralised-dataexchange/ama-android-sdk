package io.igrant.data_wallet.adapter

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.data_wallet.R
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ConnectionMessageListener
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.credentialExchange.RawCredential
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.utils.DateUtils
import io.igrant.data_wallet.utils.MessageTypes
import io.igrant.data_wallet.utils.WalletRecordType
import java.util.*
import kotlin.collections.ArrayList

class RequestListAdapter(
    private val mList: ArrayList<Record>,
    private val mListener: ConnectionMessageListener
) : RecyclerView.Adapter<RequestListAdapter.BaseViewHolder>() {

    abstract class BaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    }

    class OfferViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo) as ImageView
        var tvName: TextView = itemView.findViewById<View>(R.id.tvName) as TextView
        var tvType: TextView = itemView.findViewById<View>(R.id.tvType) as TextView
        var tvDate: TextView = itemView.findViewById(R.id.tvDate) as TextView
        var cvOffer: CardView = itemView.findViewById<View>(R.id.cvOffer) as CardView
        var vDot: View = itemView.findViewById(R.id.vDot) as View

        fun onBind(message: Notification, mListener: ConnectionMessageListener, record: Record) {
            try {
                Glide
                    .with(ivLogo.context)
                    .load(message.connection?.theirImageUrl ?: "")
                    .centerCrop()
                    .placeholder(R.drawable.images)
                    .into(ivLogo)
            } catch (e: Exception) {
            }

            if (message.type == MessageTypes.SHARE_REQUEST) {
                tvName.text = (message.presentation?.presentationRequest?.name ?: "").uppercase(
                    Locale.getDefault()
                )
            } else if (message.type == MessageTypes.TYPE_EBSI_CREDENTIAL) {
                tvName.text = message.ebsiCertificate?.searchableText
            } else {
                try {
                    val schema = WalletManager.getGson.fromJson(
                        Base64.decode(
                            message.certificateOffer?.offersAttach?.get(0)?.data?.base64,
                            Base64.URL_SAFE
                        )
                            .toString(charset("UTF-8")), RawCredential::class.java
                    ).schemaId
                    val lst = schema?.split(":")
                    tvName.text = (lst?.get(2) ?: "").uppercase(Locale.getDefault())
                } catch (e: Exception) {
                    tvName.text = ""
                }
            }

            tvType.text =
                if (message.type == MessageTypes.SHARE_REQUEST) tvType.context.resources.getString(
                    R.string.data_data_exchange
                ) else tvType.context.resources.getString(R.string.general_data_agreement)

            if (message.date != null && message.date != "")
                tvDate.text = DateUtils.getRelativeTime(message.date ?: "")
            else
                tvDate.text = tvDate.context.resources.getString(R.string.general_nil)

            cvOffer.setOnClickListener {
                mListener.onConnectionMessageClick(
                    record,
                    message.presentation?.presentationRequest?.name ?: ""
                )
            }

            vDot.visibility = if (message.stat.equals("Active")) View.VISIBLE else View.INVISIBLE
        }
    }

    class CardViewHolder(itemView: View) : BaseViewHolder(itemView) {
        val ivCovid: ImageView = itemView.findViewById(R.id.ivCovid) as ImageView
        val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo) as ImageView
        var tvName: TextView = itemView.findViewById<View>(R.id.tvName) as TextView
        var tvCountryName: TextView = itemView.findViewById<View>(R.id.tvCountryName) as TextView
        var cvOffer: CardView = itemView.findViewById<View>(R.id.cvItem) as CardView

        fun onBind(message: Notification, mListener: ConnectionMessageListener, record: Record) {
            tvName.text = message.card?.name ?: ""
            tvCountryName.text = message.card?.country ?: ""

            cvOffer.setOnClickListener {
                mListener.onConnectionMessageClick(record, message.card?.name ?: "")
            }

            ivCovid.setImageResource(message.card?.logo?:-1)
            if (message.card?.countryFlag!= -1) {
                ivLogo.visibility =View.VISIBLE
                ivLogo.setImageResource(message.card?.countryFlag ?: -1)
            }else{
                ivLogo.visibility =View.GONE
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        if (viewType == 0) {
            return OfferViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_notification,
                        parent,
                        false
                    )
            )
        } else {
            return CardViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_vaccination,
                        parent,
                        false
                    )
            )
        }
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (mList[position].type == WalletRecordType.TYPE_PASSPORT
            || mList[position].type == WalletRecordType.TYPE_SINGAPORE_PASSPORT
            || mList[position].type == WalletRecordType.TYPE_INDIAN_VACCINATION_CERTIFICATE
            || mList[position].type == WalletRecordType.TYPE_EU_VACCINATION_CERTIFICATE
            || mList[position].type == WalletRecordType.TYPE_MY_VACCINATION_CERTIFICATE
            || mList[position].type == WalletRecordType.TYPE_PHILIPPINES_VACCINATION_CERTIFICATE
            || mList[position].type == WalletRecordType.TYPE_UK_VACCINATION_CERTIFICATE
            || mList[position].type == WalletRecordType.TYPE_EBSI_VERIFIABLE_ID
            || mList[position].type == WalletRecordType.TYPE_EBSI_DIPLOMA
            || mList[position].type == WalletRecordType.TYPE_EBSI_STUDENT_ID
            ||mList[position].type == WalletRecordType.TYPE_PROFILE
        ) {
            1
        } else {
            0
        }
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val message =
            WalletManager.getGson.fromJson(mList[position].value, Notification::class.java)

        when (getItemViewType(position)) {
            0 -> {
                holder as OfferViewHolder
                holder.onBind(message, mListener, mList[position])
            }
            1 -> {
                holder as CardViewHolder
                holder.onBind(message, mListener, mList[position])
            }
        }
    }
}