package io.igrant.data_wallet.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Thread(

    @SerializedName("thid")
    @Expose
    var thid: String? = ""
):Serializable