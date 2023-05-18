package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class TaxCategory(

    @SerializedName("iD") var iD: String? = null,
    @SerializedName("percent") var percent: Int? = null,
    @SerializedName("taxScheme") var taxScheme: TaxScheme? = TaxScheme()

)