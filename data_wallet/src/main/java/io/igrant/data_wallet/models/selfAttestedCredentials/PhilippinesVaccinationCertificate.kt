//package io.igrant.data_wallet.models.selfAttestedCredentials
//
//import com.google.gson.annotations.Expose
//import com.google.gson.annotations.SerializedName
//import java.io.Serializable
//
//class PhilippinesVaccinationCertificate : Serializable {
//
//    constructor(certificate: IndianCovidCertificate, data: String) {
//        this.fullName =
//                SelfAttestedAttribute(
//                    certificate.credentialSubject?.name
//                            ?: "", AttributeTypes.STRING, "", ""
//                )
//        this.dob =
//                SelfAttestedAttribute(
//                    certificate.credentialSubject?.dob
//                            ?: "", AttributeTypes.STRING, "", ""
//                )
//        this.certificateId =
//                SelfAttestedAttribute(
//                    certificate.evidence?.get(0)?.certificateId
//                            ?: "", AttributeTypes.STRING, "", ""
//                )
//        this.batchNumber =
//                SelfAttestedAttribute(
//                    certificate.evidence?.get(0)?.batch
//                            ?: "", AttributeTypes.STRING, "", ""
//                )
//        this.vaccineManufacturer =
//                SelfAttestedAttribute(
//                    certificate.evidence?.get(0)?.manufacturer
//                            ?: "", AttributeTypes.STRING, "", ""
//                )
//        this.gender = SelfAttestedAttribute(
//            if (certificate.credentialSubject?.gender != "") certificate.credentialSubject?.gender
//                    ?: certificate.credentialSubject?.sex ?: ""
//            else certificate.credentialSubject?.sex ?: "",
//            AttributeTypes.STRING,
//            "",
//            ""
//        )
//        this.beneficiaryRefId =
//                SelfAttestedAttribute(
//                    certificate.credentialSubject?.refId ?: "",
//                    AttributeTypes.STRING,
//                    "",
//                    ""
//                )
//        this.vaccineName = SelfAttestedAttribute(
//            certificate.evidence?.get(0)?.vaccine ?: "",
//            AttributeTypes.STRING,
//            "",
//            ""
//        )
//        this.dateOfDose = SelfAttestedAttribute(
//            certificate.evidence?.get(0)?.effectiveStart ?: certificate.evidence?.get(0)?.date
//            ?: "",
//            AttributeTypes.STRING,
//            "",
//            ""
//        )
//        this.vaccinatedAt = SelfAttestedAttribute(
//            certificate.evidence?.get(0)?.facility?.name ?: "",
//            AttributeTypes.STRING,
//            "",
//            ""
//        )
//        this.vaccinationDosage = SelfAttestedAttribute(
//            "${certificate.evidence?.get(0)?.dose ?: ""} of ${certificate.evidence?.get(0)?.totalDoses ?: ""}",
//            AttributeTypes.STRING,
//            "",
//            ""
//        )
//        this.qrData = SelfAttestedAttribute(data, AttributeTypes.STRING, "", "")
//    }
//
//    @SerializedName("Covid PH Beneficiary Name")
//    @Expose
//    var fullName: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Date of Birth")
//    @Expose
//    var dob: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Gender")
//    @Expose
//    var gender: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Beneficiary Reference ID")
//    @Expose
//    var beneficiaryRefId: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Vaccination Certificate ID")
//    @Expose
//    var certificateId: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Vaccine Batch Number")
//    @Expose
//    var batchNumber: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Vaccine Name")
//    @Expose
//    var vaccineName: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Vaccine Manufacturer")
//    @Expose
//    var vaccineManufacturer: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Date of Dose")
//    @Expose
//    var dateOfDose: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Vaccinated At")
//    @Expose
//    var vaccinatedAt: SelfAttestedAttribute? = null
//
//    @SerializedName("Covid PH Vaccination Dosage")
//    @Expose
//    var vaccinationDosage: SelfAttestedAttribute? = null
//
//    @SerializedName("QR_CODE_DATA")
//    @Expose
//    var qrData: SelfAttestedAttribute? = null
//}