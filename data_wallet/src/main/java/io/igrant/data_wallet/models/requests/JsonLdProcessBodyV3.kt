package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName

data class JsonLdProcessBodyV3(
    @SerializedName("data_base64")
    var dataBase64: String? = null,
    @SerializedName("signature_options_base64")
    var signatureOptionsBase64: String? = null
)