package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Event (

    @SerializedName("id")
    @Expose
    var id: String? = null,

    @SerializedName("time_stamp")
    @Expose
    var timeStamp: String? = null,

    @SerializedName("did")
    @Expose
    var did: String? = null,

    @SerializedName("state")
    @Expose
    var state: String? = null
): Serializable