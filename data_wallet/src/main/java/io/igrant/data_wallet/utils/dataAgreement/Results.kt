package io.igrant.data_wallet.utils.dataAgreement

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.presentationExchange.PresentationRequest
import io.igrant.mobileagent.models.certificateOffer.DataAgreementContextBody

data class Results(
    @SerializedName("data_agreement_id") var dataAgreementId: String? = null,
    @SerializedName("state") var state: String? = null,
    @SerializedName("method_of_use") var methodOfUse: String? = null,
    @SerializedName("data_agreement") var dataAgreement: DataAgreementContextBody? = DataAgreementContextBody(),
    @SerializedName("publish_flag") var publishFlag: Boolean? = null,
    @SerializedName("delete_flag") var deleteFlag: Boolean? = null,
    @SerializedName("schema_id") var schemaId: String? = null,
    @SerializedName("cred_def_id") var credDefId: String? = null,
    @SerializedName("presentation_request") var presentationRequest: PresentationRequest? = PresentationRequest(),
    @SerializedName("is_existing_schema") var isExistingSchema: Boolean? = null,
    @SerializedName("created_at") var createdAt: Int? = null,
    @SerializedName("updated_at") var updatedAt: Int? = null
)