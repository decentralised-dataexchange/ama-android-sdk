package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificateOffer.Attributes
import java.io.Serializable

class CredentialPreview :Serializable{

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("attributes")
    @Expose
    var attributes: ArrayList<Attributes>? = ArrayList()


}