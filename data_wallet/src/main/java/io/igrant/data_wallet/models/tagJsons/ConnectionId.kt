package io.igrant.data_wallet.models.tagJsons

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ConnectionId(id: String) {

    @SerializedName("connection_id")
    @Expose
    var connectionId: String? = id

}