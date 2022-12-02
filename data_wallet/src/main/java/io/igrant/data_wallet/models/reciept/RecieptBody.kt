package io.igrant.data_wallet.models.reciept

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.reciept.Reciept

data class RecieptBody(

    @SerializedName("@type") var type: String? = null,
    @SerializedName("@id") var id: String? = null,
    @SerializedName("body") var body: Reciept? = Reciept()

)