package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.credentialExchange.Thread
import java.io.Serializable

class RequestOffer : Serializable {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("~thread")
    @Expose
    var thread: Thread? = null

    @SerializedName("requests~attach")
    @Expose
    var offersAttach: ArrayList<OfferAttach>? = ArrayList()
}