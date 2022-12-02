package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class OfferRequestAgreementMessage (

    @SerializedName("@type")
    @Expose
    var type: String? = null,

    @SerializedName("@id")
    @Expose
    var id: String? = null,

    @SerializedName("created_time")
    @Expose
    var createdTime: String? = null,

    @SerializedName("to")
    @Expose
    var to: String? = null,

    @SerializedName("body")
    @Expose
    var body: Any? = null,

    @SerializedName("from")
    @Expose
    var from: String? = null

)