package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificateOffer.Attributes

class ExchangeAttributes: Attributes() {
    @SerializedName("referent")
    @Expose
    var referent: String? = ""

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = ""
}