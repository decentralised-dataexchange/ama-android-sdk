package io.igrant.mobileagent.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificateOffer.*
import java.io.Serializable

data class DataAgreementContextBody(

    @SerializedName("@context")
    @Expose
    var context: Any? = null,

    @SerializedName("id")
    @Expose
    var id: String? = null,

    @SerializedName("version")
    @Expose
    var version: Int? = null,

    @SerializedName("template_id")
    @Expose
    var templateId: String? = null,

    @SerializedName("template_version")
    @Expose
    var templateVersion: Int? = null,

    @SerializedName("data_controller_name")
    @Expose
    var dataControllerName: String? = null,

    @SerializedName("data_controller_url")
    @Expose
    var dataControllerUrl: String? = null,

    @SerializedName("purpose")
    @Expose
    var purpose: String? = null,

    @SerializedName("purpose_description")
    @Expose
    var purposeDescription: String? = null,

    @SerializedName("lawful_basis")
    @Expose
    var lawfulBasis: String? = null,

    @SerializedName("method_of_use")
    @Expose
    var methodOfUse: String? = null,

    @SerializedName("data_policy")
    @Expose
    var dataPolicy: DataPolicy? = null,

    @SerializedName("personal_data")
    @Expose
    var personalData: ArrayList<PersonalData>? = null,

    @SerializedName("dpia")
    @Expose
    var dpia: Dpia? = null,

    @SerializedName("event")
    @Expose
    var event: ArrayList<Event>? = null,

    @SerializedName("proof")
    @Expose
    var proof: Proof? = null,

    @SerializedName("proofChain")
    @Expose
    var proofChain: ArrayList<Proof>? = null,

    @SerializedName("data_subject_did")
    @Expose
    var dataSubjectDid: String? = null
) : Serializable {}