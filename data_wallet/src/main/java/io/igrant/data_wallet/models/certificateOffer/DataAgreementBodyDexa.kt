package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class DataAgreementBodyDexa(

    @SerializedName("@context")
    @Expose
    var context: ArrayList<String>? = null,

    @SerializedName("@id")
    @Expose
    var id: String? = null,

    @SerializedName("@type")
    @Expose
    var type: ArrayList<String>? = null,

    @SerializedName("language")
    @Expose
    var language: String? = null,

    @SerializedName("version")
    @Expose
    var version: String? = null,

    @SerializedName("templateId")
    @Expose
    var templateId: String? = null,

    @SerializedName("templateVersion")
    @Expose
    var templateVersion: String? = null,

    @SerializedName("dataControllerName")
    @Expose
    var dataControllerName: String? = null,

    @SerializedName("dataControllerUrl")
    @Expose
    var dataControllerUrl: String? = null,

    @SerializedName("purpose")
    @Expose
    var purpose: String? = null,

    @SerializedName("purposeDescription")
    @Expose
    var purposeDescription: String? = null,

    @SerializedName("lawfulBasis")
    @Expose
    var lawfulBasis: String? = null,

    @SerializedName("methodOfUse")
    @Expose
    var methodOfUse: String? = null,

    @SerializedName("dataPolicy")
    @Expose
    var dataPolicy: DataPolicyDexa? = null,

    @SerializedName("personalData")
    @Expose
    var personalData: ArrayList<PersonalDataDexa>? = null,

    @SerializedName("dpia")
    @Expose
    var dpia: DpiaDexa? = null,

    @SerializedName("event")
    @Expose
    var event: ArrayList<Event>? = null,

    @SerializedName("proof")
    @Expose
    var proof: ProofDexa? = null,

    @SerializedName("proofChain")
    @Expose
    var proofChain: ArrayList<ProofDexa>? = null,

    @SerializedName("dataSubjectDid")
    @Expose
    var dataSubjectDid: String? = null
): Serializable {}