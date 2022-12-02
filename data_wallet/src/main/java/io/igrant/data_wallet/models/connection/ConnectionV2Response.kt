package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.SerializedName

data class ConnectionV2Response(
    @SerializedName("@type") var type: String? = null,
    @SerializedName("@id") var id: String? = null,
    @SerializedName("from") var from: String? = null,
    @SerializedName("to") var to: String? = null,
    @SerializedName("created_time") var createdTime: String? = null,
    @SerializedName("body") var body: ConnectionV2? = null
)
