package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class PartyLegalEntity(

    @SerializedName("registrationName") var registrationName: String? = null,
    @SerializedName("companyID") var companyID: String? = null

)