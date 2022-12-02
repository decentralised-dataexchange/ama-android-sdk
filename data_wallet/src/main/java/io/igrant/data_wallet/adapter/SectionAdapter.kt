package io.igrant.data_wallet.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.models.certificateOffer.Attributes
import io.igrant.data_wallet.models.wallet.Section
import io.igrant.data_wallet.utils.DisplayUtils
import io.igrant.data_wallet.utils.TextUtils.convertDpToPixel

class SectionAdapter(
    private var mainList: ArrayList<java.util.ArrayList<Attributes>>,
    private var isBlur:Boolean = false,
    private var sectionList:ArrayList<Section>? = ArrayList()
) : RecyclerView.Adapter<SectionAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        var tvSectionTitle :TextView = itemView.findViewById(R.id.tvSectionTitle)
        var rvSubSections: RecyclerView =
            itemView.findViewById<RecyclerView>(R.id.rvSubSections) as RecyclerView

        fun onBindView(
            subSections: ArrayList<Attributes>,
            isBlur: Boolean,
            position: Int,
            sectionList: ArrayList<Section>?
        ) {

            if (sectionList!=null && sectionList.size> position){
                tvSectionTitle.text = sectionList[position].title
                tvSectionTitle.visibility = View.VISIBLE
            }else{
                tvSectionTitle.visibility = View.GONE
            }

            val width = DisplayUtils.getScreenWidth() - convertDpToPixel(60f,rvSubSections.context)
            val adapter = CertificateAttributeAdapter(
                subSections, isBlur = isBlur, labelColor = null, textColor = null, width = width.toInt()
            )
            rvSubSections.layoutManager = LinearLayoutManager(rvSubSections.context)
            rvSubSections.adapter = adapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_section, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBindView(mainList[position],isBlur,position,sectionList)
    }

    override fun getItemCount(): Int {
        return mainList.size
    }

    fun setUpBlur(isBlur: Boolean){
        this.isBlur = isBlur
        mainList.mapIndexed { index, _ ->
            notifyItemChanged(index)
        }
    }
}