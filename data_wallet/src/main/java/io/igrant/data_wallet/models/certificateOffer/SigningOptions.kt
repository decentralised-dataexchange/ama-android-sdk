package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class SigningOptions(
    @SerializedName("id")
    @Expose
    var id: String? = null,

    @SerializedName("verificationMethod")
    @Expose
    var verificationMethod: String? = null,

    @SerializedName("proofPurpose")
    @Expose
    var proofPurpose: String? = null,

    @SerializedName("created")
    @Expose
    var created: String? = null,
)
