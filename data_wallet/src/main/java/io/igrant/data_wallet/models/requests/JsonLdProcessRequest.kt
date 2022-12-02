package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.connectionRequest.Transport

data class JsonLdProcessRequest(
    @SerializedName("@type")
    var type: String? = null,
    @SerializedName("@id")
    var id: String? = null,
    @SerializedName("from")
    var from: String? = null,
    @SerializedName("created_time")
    var createdTime: String? = null,
    @SerializedName("to")
    var to: String? = null,
    @SerializedName("body")
    var body:JsonLdProcessBody? = null,
    @SerializedName("~transport")
    var transport: Transport? = null
)