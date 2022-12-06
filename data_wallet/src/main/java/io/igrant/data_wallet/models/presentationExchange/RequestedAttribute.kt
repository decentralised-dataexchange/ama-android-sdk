package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.presentationExchange.CredId
import java.io.Serializable

class RequestedAttribute(): Serializable {
    @SerializedName("name")
    @Expose
    var name: String? = null

    @SerializedName("names")
    @Expose
    var names: ArrayList<String>? = null

    @SerializedName("restrictions")
    @Expose
    var restrictions: ArrayList<CredId>? = ArrayList()
}