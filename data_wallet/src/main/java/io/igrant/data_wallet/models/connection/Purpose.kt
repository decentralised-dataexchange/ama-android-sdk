package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Purpose :Serializable{

    @SerializedName("id")
    @Expose
    var id  : String? = null

    @SerializedName("name")
    @Expose
    var name  : String? = null

    @SerializedName("description")
    @Expose
    var description  : String? = null

    @SerializedName("lawful_usage")
    @Expose
    var lawfulUsage  : Boolean? = null

    @SerializedName("policy_url")
    @Expose
    var policyUrl  : String? = null

    @SerializedName("lawful_basis_of_processing")
    @Expose
    var lawfulBasisOfProcessing:String? = null

    @SerializedName("attribute_type")
    @Expose
    var attributeType  : String? = null

    @SerializedName("jurisdiction")
    @Expose
    var jurisdiction  : String? = null

    @SerializedName("disclosure")
    @Expose
    var disclosure  : String? = null

    @SerializedName("industry_scope")
    @Expose
    var industryScope  : String? = null

    @SerializedName("data_retention")
    @Expose
    var dataRetention  : DataRetention? = null

    @SerializedName("restriction")
    @Expose
    var restriction  : String? = null

    @SerializedName("shared_3pp")
    @Expose
    var shared3pp  : Boolean? = null

    @SerializedName("ssi_id")
    @Expose
    var ssiId  : String? = null
}