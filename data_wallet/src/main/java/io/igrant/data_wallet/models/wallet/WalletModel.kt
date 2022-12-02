package io.igrant.data_wallet.models.wallet

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.connection.Connection
import io.igrant.data_wallet.models.connection.ConnectionV2
import io.igrant.data_wallet.models.credentialExchange.CredentialProposalDict
import io.igrant.data_wallet.models.credentialExchange.RawCredential
import io.igrant.data_wallet.models.selfAttestedCredentials.SelfAttestedAttribute
import java.io.Serializable

data class WalletModel(

    @SerializedName("type")
    @Expose
    var type: String? = "",

    @SerializedName("sub_type")
    @Expose
    var subType: String? = "",

    @SerializedName("version")
    @Expose
    var version: Int? = null,

    @SerializedName("searchable_text")
    @Expose
    var searchableText: String? = "",

    @SerializedName("raw_credential")
    @Expose
    var rawCredential: RawCredential? = null,

    @SerializedName("credential_id")
    @Expose
    var credentialId: String? = null,

    @SerializedName("connection")
    @Expose
    var connection: MediatorConnectionObject? = null,

    @SerializedName("credential_proposal_dict")
    @Expose
    var credentialProposalDict: CredentialProposalDict? = null,

    @SerializedName("organization")
    @Expose
    var organization: Connection? = null,

    @SerializedName("organizationV2")
    @Expose
    var organizationV2: ConnectionV2? = null,

    @SerializedName("sectionCount")
    var sectionCount: Int? = null,

    @SerializedName("sectionStruct")
    var sectionStruct: ArrayList<Section>? = null,

    @SerializedName("headerFields")
    var header: Header? = null,

    @SerializedName("qrCodeData")
    var qrCodeData: QrCodeData? = null,

    @SerializedName("attributes")
    var attributes: HashMap<String, SelfAttestedAttribute>? = null,

    @SerializedName("data")
    var data: String? = null,

    @SerializedName("Pass Logo")
    @Expose
    var logo: SelfAttestedAttribute? = null,

    @SerializedName("Pass Icon")
    @Expose
    var icon: SelfAttestedAttribute? = null

    //Individual item
    //deprecated
//    @SerializedName("passportDetails")
//    @Expose
//    var passportDetails: PassportData? = null,
//
//    @SerializedName("passData")
//    @Expose
//    var passData: PassSelfAttested? = null,
//
//    @SerializedName("indianCovidCertificate")
//    @Expose
//    var indianCertificate: IndianVaccinationCertificate? = null,
//
//    @SerializedName("philippinesCovidCertificate")
//    @Expose
//    var philippinesCovidCertificate: PhilippinesVaccinationCertificate? = null,
//
//    @SerializedName("euCovidCertificate")
//    @Expose
//    var euCertificate: EUVaccinationCertificate? = null,
//
//    @SerializedName("euTestCertificate")
//    @Expose
//    var euTestCertificate: EUTestCertificate? = null,
//
//    @SerializedName("aadharCard")
//    @Expose
//    var aadharCard: AadharCertificate? = null


) : Serializable, Comparable<WalletModel> {
    override fun compareTo(other: WalletModel): Int {
        return (this.searchableText ?: "").compareTo(other.searchableText ?: "")
    }

}