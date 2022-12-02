package io.igrant.data_wallet.models

import com.google.gson.annotations.SerializedName

open class BaseResponse (
    @SerializedName("@type")
    var type: String? = null,
    @SerializedName("@id")
    var id: String? = null,
    @SerializedName("from")
    var from: String? = null,
    @SerializedName("created_time")
    var createdTime: String? = null,
    @SerializedName("to")
    var to: String? = null
)