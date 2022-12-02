package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class OfferRequestAgreementBodyDexa(

    @SerializedName("id")
    @Expose
    var id: String? = null,

    @SerializedName("event")
    @Expose
    var event: Event? = null,

    @SerializedName("proof")
    @Expose
    var proof: ProofDexa? = null

    ) {}