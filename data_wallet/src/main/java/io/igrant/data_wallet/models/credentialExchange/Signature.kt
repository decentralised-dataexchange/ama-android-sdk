package io.igrant.data_wallet.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.credentialExchange.PCredentials
import java.io.Serializable

class Signature:Serializable {

    @SerializedName("p_credential")
    @Expose
    var pCredential: PCredentials? = null

    @SerializedName("r_credential")
    @Expose
    var rCredential: String? = null
}