package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class Dpia(
    @SerializedName("dpia_date")
    val dpiaDate: String? = null,
    @SerializedName("dpia_summary_url")
    val dpiaSummaryUrl: String? = null
): Serializable