package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.receipt.TaxCategory


data class TaxSubtotal(

    @SerializedName("taxableAmount") var taxableAmount: Double? = null,
    @SerializedName("taxAmount") var taxAmount: Int? = null,
    @SerializedName("taxCategory") var taxCategory: TaxCategory? = TaxCategory()

)