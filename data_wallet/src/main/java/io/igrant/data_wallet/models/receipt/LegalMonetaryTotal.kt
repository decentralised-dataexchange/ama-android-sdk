package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class LegalMonetaryTotal(

    @SerializedName("lineExtensionAmount") var lineExtensionAmount: Double? = null,
    @SerializedName("taxExclusiveAmount") var taxExclusiveAmount: Double? = null,
    @SerializedName("taxInclusiveAmount") var taxInclusiveAmount: Double? = null,
    @SerializedName("chargeTotalAmount") var chargeTotalAmount: Double? = null,
    @SerializedName("prepaidAmount") var prepaidAmount: Double? = null,
    @SerializedName("payableAmount") var payableAmount: Int? = null

)