package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PersonalDataDexa (

    @SerializedName("attributeId")
    @Expose
    var attributeId: String? = null,

    @SerializedName("attributeName")
    @Expose
    var attributeName: String? = null,

    @SerializedName("attributeSensitive")
    @Expose
    var attributeSensitive: Boolean? = null,

    @SerializedName("attributeCategory")
    @Expose
    var attributeCategory: String? = null,

    @SerializedName("restrictions")
    @Expose
    var restrictions: ArrayList<Any>? = null,

    @SerializedName("attributeDescription")
    @Expose
    var attributeDescription: String? = null
): Serializable