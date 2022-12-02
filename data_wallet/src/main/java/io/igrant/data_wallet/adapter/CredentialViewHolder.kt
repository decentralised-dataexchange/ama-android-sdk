package io.igrant.data_wallet.adapter

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import io.igrant.data_wallet.R
import io.igrant.data_wallet.listeners.WalletListener
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.utils.ConnectionTypes
import io.igrant.data_wallet.utils.TextUtils
import java.util.*
import kotlin.collections.ArrayList

class CredentialViewHolder(itemView: View) : BaseViewHolder(itemView) {
    var tvCertificateName: TextView =
        itemView.findViewById<View>(R.id.tvCertificateName) as TextView
    var tvCompanyName: TextView =
        itemView.findViewById<View>(R.id.tvCompanyName) as TextView
    var tvLocation: TextView = itemView.findViewById(R.id.tvLocation) as TextView
    var ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
    var cvItem: ConstraintLayout = itemView.findViewById(R.id.cvItem)

    fun onBindView(
        certificate: WalletModel,
        listener: WalletListener,
        position: Int,
        credentialList: ArrayList<WalletModel>
    ) {
        if (certificate.connection?.connectionType == ConnectionTypes.EBSI_CONNECTION_NATURAL_PERSON) {
            tvCertificateName.text = certificate.searchableText?:
                tvCertificateName.resources.getString(R.string.ebsi_verifiable_id)
                    .uppercase(Locale.getDefault())
        } else {
            val lst = certificate.rawCredential?.schemaId?.split(":")
            tvCertificateName.text = (lst?.get(2) ?: "").uppercase(Locale.getDefault())
        }
        tvCompanyName.text = certificate.connection?.theirLabel ?: ""
        try {
            Glide
                .with(ivLogo.context)
                .load(certificate.connection?.theirImageUrl ?: "")
                .centerCrop()
                .placeholder(R.drawable.images)
                .into(ivLogo)
        } catch (e: Exception) {
        }
        cvItem.setOnClickListener {
            listener.onItemClick(certificate, position)
        }

        if (certificate.organization != null && certificate.organization?.location != null) {
            tvLocation.text = certificate.organization?.location ?: ""
        } else if (certificate.organizationV2 != null && certificate.organizationV2?.location != null) {
            tvLocation.text = certificate.organizationV2?.location ?: ""
        } else {
            tvLocation.text = ""
        }


        if (position == 0) {
            cvItem.setBackgroundResource(R.drawable.wallet_card_bg_white)
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 0)
            cvItem.layoutParams = params
            cvItem.setPadding(30, 30, 30, 30)
        } else if (position == 1) {
            cvItem.setBackgroundResource(
                if (position + 1 == credentialList.size) R.drawable.wallet_card_bg_white
                else R.drawable.wallet_alternate_card_bg
            )
            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 20, 0, 0)
            cvItem.layoutParams = params
            cvItem.setPadding(30, 30, 30, 30)
        } else {
            cvItem.setBackgroundResource(if (position % 2 == 0) R.drawable.wallet_card_bg_end else R.drawable.wallet_alternate_card_bg_end)

            val params = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(
                0,
//                    if (credentialList[position - 1].type == WalletRecordType.CERTIFICATE_TYPE_CREDENTIALS) dpToPxFun(-40f,cvItem.context).toInt() else dpToPxFun(-40f,cvItem.context).toInt(),
                TextUtils.dpToPxFun(-68f, cvItem.context),
                0,
                0
            )
            cvItem.layoutParams = params
            cvItem.setPadding(30, 30, 30, 30)
        }
    }
}