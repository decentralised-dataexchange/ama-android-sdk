package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class PurposeDetail : Serializable {

    @SerializedName("purpose")
    @Expose
    var purpose: Purpose? = null

    @SerializedName("templates")
    @Expose
    var templates: ArrayList<Template>? = ArrayList()

}