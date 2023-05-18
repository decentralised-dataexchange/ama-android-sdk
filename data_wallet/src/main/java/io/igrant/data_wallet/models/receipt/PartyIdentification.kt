package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class PartyIdentification(

    @SerializedName("iD") var iD: String? = null

)