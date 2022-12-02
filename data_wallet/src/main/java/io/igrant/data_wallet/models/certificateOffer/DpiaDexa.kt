package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DpiaDexa(
    @SerializedName("dpiaDate")
    val dpiaDate: String? = null,
    @SerializedName("dpiaSummaryUrl")
    val dpiaSummaryUrl: String? = null
): Serializable