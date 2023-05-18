package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.receipt.TaxSubtotal


data class TaxTotal(

    @SerializedName("taxAmount") var taxAmount: Int? = null,
    @SerializedName("taxSubtotal") var taxSubtotal: TaxSubtotal? = TaxSubtotal()

)