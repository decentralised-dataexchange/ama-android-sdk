package io.igrant.data_wallet.models

import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.certificateOffer.DataAgreementPolicy
import java.io.Serializable

data class DataAgreementPolicyUIModel(
    val historyId: String?,
    val connectionId: String?,
    var isSignatureVerified: Boolean?,
    val dataAgreementPolicy: DataAgreementPolicy?,
    val dataAgreementContext: DataAgreementContext?
): Serializable
