package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.SerializedName

data class ConnectionV2(
    @SerializedName("organisation_did") var organisationDid: String? = null,
    @SerializedName("organisation_id") var organisationId: String? = null,
    @SerializedName("organisation_name") var organisationName: String? = null,
    @SerializedName("cover_image_url") var coverImageUrl: String? = null,
    @SerializedName("logo_image_url") var logoImageUrl: String? = null,
    @SerializedName("location") var location: String? = null,
    @SerializedName("organisation_type") var organisationType: String? = null,
    @SerializedName("description") var description: String? = null,
    @SerializedName("policy_url") var policyUrl: String? = null,
    @SerializedName("eula_url") var eulaUrl: String? = null
)
