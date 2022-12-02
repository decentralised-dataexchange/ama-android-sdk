package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.connectionRequest.Transport

data class JsonLdProcessRequestV3(
    @SerializedName("@type")
    var type: String? = null,
    @SerializedName("@id")
    var id: String? = null,
    @SerializedName("body")
    var body:JsonLdProcessBodyV3? = null,
    @SerializedName("~transport")
    var transport: Transport? = null
)