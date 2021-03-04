package io.igrant.data_wallet.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificateOffer.OfferAttach

class IssueCredential {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("~thread")
    @Expose
    var thread: Thread? = null

    @SerializedName("credentials~attach")
    @Expose
    var credentialsAttach:ArrayList<OfferAttach> = ArrayList()

    @SerializedName("comment")
    @Expose
    var comment: String? = ""
}