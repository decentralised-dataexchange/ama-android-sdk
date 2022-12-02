package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.credentialExchange.Thread

data class DiscoverResponse(
    @SerializedName("@type") var type: String? = null,
    @SerializedName("@id") var id: String? = null,
    @SerializedName("~thread") var thread: Thread? = null,
    @SerializedName("protocols") var protocols: ArrayList<Protocol>? = null
)