package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CredentialValue {

    @SerializedName("cred_id")
    @Expose
    var credId: String? = ""

    @SerializedName("revealed")
    @Expose
    var revealed: Boolean? = true

}