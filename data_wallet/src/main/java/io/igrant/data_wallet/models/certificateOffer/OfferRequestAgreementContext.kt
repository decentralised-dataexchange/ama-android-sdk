package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class OfferRequestAgreementContext(

    @SerializedName("message_type")
    @Expose
    var messageType: String? = null,

    @SerializedName("message")
    @Expose
    var message: OfferRequestAgreementMessage? = null
)