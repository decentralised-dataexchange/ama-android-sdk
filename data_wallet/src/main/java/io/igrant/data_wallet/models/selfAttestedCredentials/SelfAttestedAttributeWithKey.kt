package io.igrant.data_wallet.models.selfAttestedCredentials

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SelfAttestedAttributeWithKey {

    constructor(key: String, subType: String, attribute: SelfAttestedAttribute) {
        this.key = key
        this.subtype = subType
        this.attribute = attribute
    }

    @SerializedName("key")
    @Expose
    var key: String? = null

    @SerializedName("subType")
    @Expose
    var subtype: String? = null

    @SerializedName("attribute")
    @Expose
    var attribute: SelfAttestedAttribute? = null
}