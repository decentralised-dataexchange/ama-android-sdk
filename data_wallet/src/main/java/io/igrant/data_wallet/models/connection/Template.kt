package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Template:Serializable {

    @SerializedName("id")
    @Expose
    var id  : String? = null

    @SerializedName("consent")
    @Expose
    var consent  : String? = ""

}