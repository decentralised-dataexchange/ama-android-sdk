package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class CredId:Serializable {
    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = null

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = null
}