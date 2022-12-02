package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DataPolicyDexa(

    @SerializedName("dataRetentionPeriod")
    @Expose
    var dataRetentionPeriod: Int? = null,

    @SerializedName("policyUrl")
    @Expose
    var policyURL: String? = null,

    @SerializedName("jurisdiction")
    @Expose
    var jurisdiction: String? = null,

    @SerializedName("industrySector")
    @Expose
    var industrySector: String? = null,

    @SerializedName("geographicRestriction")
    @Expose
    var geographicRestriction: String? = null,

    @SerializedName("storageLocation")
    @Expose
    var storageLocation: String? = null,

    @SerializedName("thirdPartyDataSharing")
    @Expose
    var thirdPartyDataSharing: Boolean? = null

) : Serializable