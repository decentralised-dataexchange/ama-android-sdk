package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Proof(

    @SerializedName("id")
    @Expose
    var id: String? = null,

    @SerializedName("type")
    @Expose
    var type: String? = null,

    @SerializedName("created")
    @Expose
    var created: String? = null,

    @SerializedName("verificationMethod")
    @Expose
    var verificationMethod: String? = null,

    @SerializedName("proofPurpose")
    @Expose
    var proofPurpose: String? = null,

    @SerializedName("proofValue")
    @Expose
    var proofValue: String? = null

): Serializable