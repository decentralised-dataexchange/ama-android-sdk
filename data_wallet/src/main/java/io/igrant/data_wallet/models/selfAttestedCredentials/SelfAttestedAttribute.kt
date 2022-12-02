package io.igrant.data_wallet.models.selfAttestedCredentials

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class SelfAttestedAttribute : Serializable {

    constructor(value: String, type: String, parent: String, label: String) {
        this.value = value
        this.type = type
        this.imageType = AttributeTypes.BASE_64_DEFAULT
        this.parent = parent
        this.label = label
    }

    constructor(value: String, type: String, imageType: String, parent: String, label: String) {
        this.value = value
        this.type = type
        this.imageType = imageType
        this.parent = parent
        this.label = label
    }

    @SerializedName("value")
    @Expose
    var value: String? = null

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("imageType")
    @Expose
    var imageType: String? = null

    @SerializedName("parent")
    @Expose
    var parent: String? = null

    @SerializedName("label")
    @Expose
    var label: String? = null

}