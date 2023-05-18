package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class PaymentTerms(

    @SerializedName("note") var note: String? = null

)