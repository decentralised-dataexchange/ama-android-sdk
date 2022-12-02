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
import io.igrant.data_wallet.listeners.ConnectionClickListener
import io.igrant.data_wallet.utils.DateUtils.INDY_DATE_FORMAT
import io.igrant.data_wallet.utils.DateUtils.formatDateString
import org.json.JSONArray
import org.json.JSONObject


class ConnectionListAdapter(
    private var connectionRecords: JSONArray,
    private val listener: ConnectionClickListener
) :
    RecyclerView.Adapter<ConnectionListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvConnectionName: TextView = itemView.findViewById<View>(R.id.tvConnection) as TextView
        var tvConnectedDate : TextView = itemView.findViewById(R.id.tvConnectedDate)
        //        var tvConnectionType: TextView =
//            itemView.findViewById<View>(R.id.tvConnectionType) as TextView
        var ivLogo: ImageView = itemView.findViewById(R.id.ivLogo)
        var clItem: ConstraintLayout = itemView.findViewById(R.id.clItem)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_connection,
                    parent,
                    false
                )
        )
    }

    override fun getItemCount(): Int {
        return connectionRecords.length()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = JSONObject(connectionRecords.getJSONObject(position).getString("value"))

        holder.clItem.setOnClickListener {
            listener.onConnectionClick(data.getString("request_id"),data.getString("my_did"))
        }
        try {
            Glide
                .with(holder.ivLogo.context)
                .load(data.getString("their_image_url"))
                .centerCrop()
                .placeholder(R.drawable.images)
                .into(holder.ivLogo)
        } catch (e: Exception) {
        }
        if (data.getString("their_label") != "")
            holder.tvConnectionName.text = data.getString("their_label")

        if (data.getString("created_at") != "")
            holder.tvConnectedDate.text = formatDateString(INDY_DATE_FORMAT,data.getString("created_at"))

//        if (data.getString("my_did") != "")
//            holder.tvConnectionType.text = "Health (${data.getString("my_did")})"
    }

    fun setList(connectionRecords: JSONArray) {
        this.connectionRecords = connectionRecords
        notifyDataSetChanged()
    }
}