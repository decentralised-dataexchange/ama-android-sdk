package io.igrant.data_wallet.models.decorators

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SignatureDecorator(
    @SerializedName("@type")
    @Expose
    var type: String? = "",

    @SerializedName("signature")
    @Expose
    var signature: String? = "",

    @SerializedName("sig_data")
    @Expose
    var sigData: String? = "",

    @SerializedName("signer")
    @Expose
    var signer: String? = ""
) {}