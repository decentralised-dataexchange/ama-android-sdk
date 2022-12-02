package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.connectionRequest.Transport

data class InitiateRequest(
    @SerializedName("body")
    var body: InitiateBody? = null,
    @SerializedName("@type")
    var type: String?,
    @SerializedName("@id")
    var id: String?,
    override var from: String?,
    @SerializedName("created_time")
    var createdTime: String?,
    override var to: String?,
    @SerializedName("~transport")
    var transport: Transport? = null
) : V2RequestInterface {
}