package io.igrant.data_wallet.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.certificateOffer.CertificateOffer
import io.igrant.data_wallet.models.presentationExchange.PresentationExchange

class Notification {

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("date")
    @Expose
    var date: String? = null

    @SerializedName("connection")
    @Expose
    var connection: MediatorConnectionObject? = null

    @SerializedName("presentation")
    @Expose
    var presentation: PresentationExchange? = null

    @SerializedName("certificateOffer")
    @Expose
    var certificateOffer: CertificateOffer? = null
}