package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class OfferRequestAgreementMessageV3 (

    @SerializedName("@type")
    @Expose
    var type: String? = null,

    @SerializedName("@id")
    @Expose
    var id: String? = null,

    @SerializedName("body")
    @Expose
    var body: DataAgreementBodyDexa? = null
)