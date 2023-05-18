package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class PartyName(

    @SerializedName("name") var name: String? = null

)