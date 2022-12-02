package io.igrant.data_wallet.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.listeners.WalletListener
import io.igrant.data_wallet.models.wallet.WalletModel

class WalletCertificatesAdapter(
    private val credentialList: ArrayList<WalletModel>,
    private val listener: WalletListener
) : RecyclerView.Adapter<BaseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {

//        when (viewType) {
//            0 -> {
                return CredentialViewHolder(
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_certificate, parent, false)
                )
//            }
//            1 -> {
//                return SelfAttestedViewHolder(
//                    LayoutInflater.from(parent.context)
//                        .inflate(R.layout.item_certificate, parent, false)
//                )
//            }
//            3 -> {
//                return AadharViewHolder(
//                    LayoutInflater.from(parent.context)
//                        .inflate(R.layout.item_certificate_vaccination, parent, false)
//                )
//            }
//            4 -> {
//                return PKPassViewHolder(
//                    LayoutInflater.from(parent.context)
//                        .inflate(R.layout.item_certificate_pkpass, parent, false)
//                )
//            }
//            5->{
//                return GeneralizedViewHolder(
//                    LayoutInflater.from(parent.context)
//                        .inflate(R.layout.item_certificate,parent,false)
//                )
//            }
//            else -> {
//                return VaccinationViewHolder(
//                    LayoutInflater.from(parent.context)
//                        .inflate(R.layout.item_certificate_vaccination, parent, false)
//                )
//            }
//        }
    }

    override fun getItemViewType(position: Int): Int {
        return 0
    }

    override fun getItemCount(): Int {
        return credentialList.size
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val certificate = credentialList[position]
        when (getItemViewType(position)) {
            0 -> {
                holder as CredentialViewHolder
                holder.onBindView(certificate, listener, position, credentialList)
            }
        }
    }
}