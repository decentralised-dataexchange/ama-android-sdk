package io.igrant.data_wallet.models.certificateOffer

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

open class Attributes :Serializable {
    constructor() {

    }

    constructor(name: String, value: String) {
        this.name = name
        this.value = value
    }

    @SerializedName("name")
    @Expose
    var name: String? = ""

    @SerializedName("value")
    @Expose
    var value: String? = ""
}