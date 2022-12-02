package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName

data class ConnectionExistRequest(
    @SerializedName("body")
    var body: ConnectionExistBody? = null,
    @SerializedName("@type")
    var type: String?,
    @SerializedName("@id")
    var id: String?,
    override var from: String?,
    @SerializedName("created_time")
    var createdTime: String?,
    override var to: String?
) : V2RequestInterface {
}