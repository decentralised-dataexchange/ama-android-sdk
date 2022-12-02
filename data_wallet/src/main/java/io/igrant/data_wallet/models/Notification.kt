package io.igrant.data_wallet.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.certificateOffer.CertificateOffer
import io.igrant.data_wallet.models.presentationExchange.PresentationExchange
import io.igrant.data_wallet.models.wallet.WalletModel

class Notification {

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("stat")
    @Expose
    var stat: String? = ""

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

    @SerializedName("ebsiCertificate")
    @Expose
    var ebsiCertificate: WalletModel? = null

    @SerializedName("card")
    @Expose
    var card: Card? = null

    @SerializedName("isSignatureVerified")
    @Expose
    var isSignatureVerified: Boolean? = false
}