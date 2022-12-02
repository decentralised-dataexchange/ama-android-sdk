package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.SerializedName

data class Protocol(
    @SerializedName("pid") var pid: String = "",
    @SerializedName("roles") var roles: ArrayList<String>? = null
)
