package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import org.json.JSONObject
import java.io.Serializable
import java.util.*

class PresentationExchange():Serializable {

    @SerializedName("thread_id")
    @Expose
    var threadId: String? = ""

    @SerializedName("~data-agreement-context")
    @Expose
    var dataAgreementContext:DataAgreementContext?=null

    @SerializedName("qr_id")
    @Expose
    var qrId: String? = ""

    @SerializedName("created_at")
    @Expose
    var createdAt: String? = ""

    @SerializedName("updated_at")
    @Expose
    var updatedAt: String? = ""

    @SerializedName("connection_id")
    @Expose
    var connectionId: String? = ""

    @SerializedName("initiator")
    @Expose
    var initiator: String? = ""

    @SerializedName("presentation_proposal_dict")
    @Expose
    var presentationProposalDict: Object? = null

    @SerializedName("presentation_request")
    @Expose
    var presentationRequest: PresentationRequest? = null

    @SerializedName("presentation_request_dict")
    @Expose
    var presentationRequestDict: Object? = null

    @SerializedName("presentation")
    @Expose
    var presentation: JSONObject? = null

    @SerializedName("role")
    @Expose
    var role: String? = ""

    @SerializedName("state")
    @Expose
    var state: String? = ""

    @SerializedName("auto_present")
    @Expose
    var autoPresent: Boolean? = true

    @SerializedName("error_msg")
    @Expose
    var errorMsg: String? = null

    @SerializedName("verified")
    @Expose
    var verified: String? = null

    @SerializedName("trace")
    @Expose
    var trace: Boolean? = false

    @SerializedName("comment")
    @Expose
    var comment: String? = ""

    @SerializedName("type")
    @Expose
    var type:String?=""
}