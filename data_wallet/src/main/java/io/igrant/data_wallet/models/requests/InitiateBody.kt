package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName

data class InitiateBody(
    @SerializedName("qr_id")
    var qr_id: String? = null
)
