package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable
import io.igrant.data_wallet.models.credentialExchange.Thread
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

    @SerializedName("~data-agreement-context")
    @Expose
    var dataAgreementContext: OfferRequestAgreementContext? = null

    @SerializedName("requests~attach")
    @Expose
    var offersAttach: ArrayList<OfferAttach>? = null

    @SerializedName("presentations~attach")
    @Expose
    var presentationsAttach: ArrayList<OfferAttach>? = null
}