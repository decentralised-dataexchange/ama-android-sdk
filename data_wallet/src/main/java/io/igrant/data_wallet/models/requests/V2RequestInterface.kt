package io.igrant.data_wallet.models.requests

import com.google.gson.annotations.SerializedName

interface V2RequestInterface {

    @get:SerializedName("from")
    var from: String?

    @get:SerializedName("to")
    var to: String?
}