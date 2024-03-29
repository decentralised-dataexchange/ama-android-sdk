package io.igrant.data_wallet.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificateOffer.CredentialPreview
import java.io.Serializable

class CredentialProposalDict :Serializable{

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("comment")
    @Expose
    var comment: String? = ""

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = ""

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = ""

    @SerializedName("credential_proposal")
    @Expose
    var credentialProposal: CredentialPreview? = null
}