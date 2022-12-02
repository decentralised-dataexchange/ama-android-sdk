package io.igrant.data_wallet.models.certificateOffer

import java.io.Serializable

data class DataAgreementPolicy(
    val lawFullBasis: String?,
    val policyUrl: String?,
    val jurisdiction: String?,
    val industryScope: String?,
    val restriction: String?,
    val shared3PP: Boolean?,
    val dataRetention:String?
) : Serializable
