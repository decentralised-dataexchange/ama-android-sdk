package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.presentationExchange.ExchangeAttributes

class PresentationProposal {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("attributes")
    @Expose
    var attributes: ArrayList<ExchangeAttributes>? = ArrayList()

    @SerializedName("predicates")
    @Expose
    var predicates: ArrayList<String> = ArrayList()
}