package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.presentationExchange.PresentationProposal

class PresentationProposalData {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("presentation_proposal")
    @Expose
    var presentationProposal: PresentationProposal? = null

    @SerializedName("comment")
    @Expose
    var comment: String? = ""
}