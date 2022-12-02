package io.igrant.data_wallet.models.wallet

import com.google.gson.annotations.SerializedName

data class Section(
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("key")
    val key: String? = null
)
