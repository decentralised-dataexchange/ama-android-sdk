package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class Price(

    @SerializedName("priceAmount") var priceAmount: Double? = null

)