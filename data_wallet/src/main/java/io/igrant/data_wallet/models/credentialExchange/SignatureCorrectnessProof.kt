package io.igrant.data_wallet.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class SignatureCorrectnessProof:Serializable {

    @SerializedName("se")
    @Expose
    var se: String? = null

    @SerializedName("c")
    @Expose
    var c: String? = null
}