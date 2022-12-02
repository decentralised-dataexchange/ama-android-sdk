package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class OfferRequestAgreementContextV3 (

    @SerializedName("message_type")
    @Expose
    var messageType: String? = null,

    @SerializedName("message")
    @Expose
    var message: OfferRequestAgreementMessageV3? = null
)