package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.connectionRequest.Transport

data class AchorRegistryRequest(
    @SerializedName("@type")
    var type: String? = null,
    @SerializedName("@id")
    var id: String? = null,
    @SerializedName("created_time")
    var createdTime: String? = null,
    @SerializedName("to")
    var to: String? = null,
    @SerializedName("from")
    var from: String? = null,
    @SerializedName("body~sig")
    var bodySig: String? = null,
    @SerializedName("transport")
    var transport: Transport? = null
)
