package io.igrant.data_wallet.adapter

import android.media.Image
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.data_wallet.R
import io.igrant.data_wallet.listeners.PrefsClickListeners
import io.igrant.data_wallet.models.prefs.Dus

class ThirdPartyDataSharingCompanyAdapter(
    val dusList: ArrayList<Dus>,
    val mListener: PrefsClickListeners,
    val instanceId: String
) :
    RecyclerView.Adapter<ThirdPartyDataSharingCompanyAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvCompanyName: TextView = itemView.findViewById(R.id.tvCompanyName) as TextView
        val swCompany: SwitchCompat = itemView.findViewById(R.id.swCompany) as SwitchCompat
        val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
        val clItem: ConstraintLayout = itemView.findViewById(R.id.clItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_third_party_data_sharing_companies,
                    parent,
                    false
                )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvCompanyName.text = dusList[position].controllerDetails?.organisationName ?: ""
        Glide
            .with(holder.ivLogo.context)
            .load(dusList[position].controllerDetails?.logoImageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(holder.ivLogo)

        holder.swCompany.isChecked = dusList[position].ddaInstancePermissionState.equals("allow", ignoreCase = true)

        holder.clItem.setOnClickListener {
            mListener.onCompanyLevelClick(
                dusList[position].ddaInstanceId ?: "",
                instanceId, if(dusList[position].ddaInstancePermissionState == "allow")"disallow" else "allow"
            )
        }
    }

    override fun getItemCount(): Int {
        return dusList.size
    }
}