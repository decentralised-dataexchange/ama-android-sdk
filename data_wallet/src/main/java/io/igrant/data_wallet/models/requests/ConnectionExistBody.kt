package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName

data class ConnectionExistBody(
    @SerializedName("theirdid")
    var theirdid: String? = null
)
