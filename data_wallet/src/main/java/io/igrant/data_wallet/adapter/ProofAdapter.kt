package io.igrant.data_wallet.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.text.Spannable

import android.text.style.ForegroundColorSpan

import android.text.SpannableString
import android.widget.ImageView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.models.certificateOffer.ProofDexa


class ProofAdapter(var list: ArrayList<ProofDexa>, var isVerified: Boolean) :
    RecyclerView.Adapter<ProofAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var ivLock: ImageView = itemView.findViewById(R.id.ivLock) as ImageView
        var pbLoader: ProgressBar =
            itemView.findViewById<ProgressBar>(R.id.pbLoader) as ProgressBar
        var tvHead: TextView = itemView.findViewById(R.id.tvHead) as TextView
        var txtDid: TextView = itemView.findViewById(R.id.txtDid) as TextView
        var tvDid: TextView = itemView.findViewById(R.id.tvDid) as TextView
        var tvSignature: TextView = itemView.findViewById(R.id.tvSignature) as TextView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_proof, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val proof = list[position]
        holder.pbLoader.visibility = if (position == 0) {
            if (isVerified) View.INVISIBLE else View.INVISIBLE
        } else View.GONE
        holder.tvHead.visibility = if (position == 0) View.VISIBLE else View.GONE

        holder.ivLock.visibility = if (position == 0) View.VISIBLE else View.GONE
        holder.tvDid.text = proof.verificationMethod

        val proofValue: Spannable =
            SpannableString("Signature : ${proof.proofValue}")

        proofValue.setSpan(
            ForegroundColorSpan(Color.BLACK),
            0,
            12,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        holder.tvSignature.text = proofValue

        holder.txtDid.text =
            if (position == 0) holder.txtDid.context.resources.getString(R.string.certificate_controller_decentralised_identifier) else
                holder.txtDid.context.resources.getString(R.string.certificate_individual_decentralised_identifier)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun setVerifiedStatus(isVerified: Boolean) {
        this.isVerified = isVerified
        notifyItemChanged(0)
    }
}