package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PersonalData (

    @SerializedName("attribute_id")
    @Expose
    var attributeId: String? = null,

    @SerializedName("attribute_name")
    @Expose
    var attributeName: String? = null,

    @SerializedName("attribute_sensitive")
    @Expose
    var attributeSensitive: Boolean? = null,

    @SerializedName("attribute_category")
    @Expose
    var attributeCategory: String? = null,

    @SerializedName("attribute_description")
    @Expose
    var attributeDescription: String? = null
): Serializable