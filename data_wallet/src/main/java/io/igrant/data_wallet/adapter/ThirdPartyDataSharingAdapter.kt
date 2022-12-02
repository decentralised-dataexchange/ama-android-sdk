package io.igrant.data_wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.listeners.PrefsClickListeners
import io.igrant.data_wallet.models.prefs.Prefs

class ThirdPartyDataSharingAdapter(
    var prefs: ArrayList<Prefs>,
    val mListener: PrefsClickListeners
) :
    RecyclerView.Adapter<ThirdPartyDataSharingAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvSectorName: TextView = itemView.findViewById(R.id.tvSectorName) as TextView
        var info: ImageView = itemView.findViewById(R.id.info) as ImageView
        val swSector: SwitchCompat = itemView.findViewById(R.id.swSector) as SwitchCompat
        val rvCompanies: RecyclerView = itemView.findViewById(R.id.rvCompanies)
        val clItem: ConstraintLayout = itemView.findViewById(R.id.clItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_third_party_data_sharing,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvSectorName.text = prefs[position].purposeName
        val adapter = ThirdPartyDataSharingCompanyAdapter(
            prefs[position].dus,
            mListener,
            prefs[position].instanceId ?: ""
        )
        holder.rvCompanies.adapter = adapter

        holder.swSector.isChecked =
            prefs[position].instancePermissionState.equals("allow", ignoreCase = true)
        holder.rvCompanies.visibility =
            if (prefs[position].dus.size > 0) View.VISIBLE else View.GONE

        holder.clItem.setOnClickListener {
            mListener.onAgreementLevelClick(
                prefs[position].instanceId ?: "",
                if (prefs[position].instancePermissionState == "allow") "disallow" else "allow"
            )
        }

        holder.info.setOnClickListener {
            mListener.onInfoClick(prefs[position].instanceId ?: "")
        }
    }

    override fun getItemCount(): Int {
        return prefs.size
    }

    fun updateList(tempList: java.util.ArrayList<Prefs>) {
        prefs = tempList
        notifyDataSetChanged()
    }
}