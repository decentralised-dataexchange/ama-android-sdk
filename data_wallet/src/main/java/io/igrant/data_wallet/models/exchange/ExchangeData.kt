package io.igrant.data_wallet.models.exchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificate.CredData
import io.igrant.data_wallet.models.presentationExchange.RequestedAttribute
import io.igrant.data_wallet.models.selfAttestedCredentials.AttributeTypes

class ExchangeData {

    @SerializedName("key")
    @Expose
    var key: String? = null

    @SerializedName("value")
    @Expose
    var value: RequestedAttribute? = null

    @SerializedName("name")
    @Expose
    var name: String? = null

    // 0 - credential
    // 1 - self attested
    @SerializedName("type")
    @Expose
    var type: Int = 0

    @SerializedName("credData")
    @Expose
    var credData: CredData? = null

    @SerializedName("selfAttestedSubType")
    @Expose
    var selfAttestedSubType: String? = null

    @SerializedName("recordId")
    @Expose
    var recordId: String? = null

    @SerializedName("data")
    @Expose
    var data: String? = null

    @SerializedName("dataType")
    @Expose
    var dataType: String? = AttributeTypes.STRING

    @SerializedName("imageType")
    @Expose
    var imageType: String? = AttributeTypes.BASE_64_DEFAULT
}