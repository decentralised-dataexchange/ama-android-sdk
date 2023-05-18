package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class PartyTaxScheme(

    @SerializedName("companyID") var companyID: String? = null,
    @SerializedName("taxScheme") var taxScheme: TaxScheme? = TaxScheme()

)