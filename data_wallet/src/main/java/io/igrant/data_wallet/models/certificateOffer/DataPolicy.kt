package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DataPolicy (

    @SerializedName("data_retention_period")
    @Expose
    var dataRetentionPeriod: Int? = null,

    @SerializedName("policy_URL")
    @Expose
    var policyURL: String? = null,

    @SerializedName("jurisdiction")
    @Expose
    var jurisdiction: String? = null,

    @SerializedName("industry_sector")
    @Expose
    var industrySector: String? = null,

    @SerializedName("geographic_restriction")
    @Expose
    var geographicRestriction: String? = null,

    @SerializedName("storage_location")
    @Expose
    var storageLocation: String? = null

):Serializable