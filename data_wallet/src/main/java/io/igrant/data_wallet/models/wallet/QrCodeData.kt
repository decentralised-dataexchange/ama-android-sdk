package io.igrant.data_wallet.models.wallet

import com.google.gson.annotations.SerializedName

data class QrCodeData(
    @SerializedName("rawData")
    val rawData: String? = null,
    @SerializedName("imageData")
    val imageData: String? = null
)
