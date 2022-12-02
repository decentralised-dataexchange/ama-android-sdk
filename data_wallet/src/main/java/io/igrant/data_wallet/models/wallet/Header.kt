package io.igrant.data_wallet.models.wallet

import com.google.gson.annotations.SerializedName

data class Header(
    @SerializedName("title")
    val title:String?=null,
    @SerializedName("description")
    val description:String?=null
)
