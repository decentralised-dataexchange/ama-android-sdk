package io.igrant.data_wallet.utils.dataAgreement

import android.app.Activity
import android.content.Context
import io.igrant.data_wallet.activity.DataAgreementPolicyUtil
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.certificateOffer.DataAgreementMessage
import io.igrant.data_wallet.models.certificateOffer.DataAgreementPolicy
import io.igrant.data_wallet.models.ledger.Ledger
import io.igrant.data_wallet.utils.DataAgreementContextBodyUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object DataAgreementUtils {

    fun fetchDataAgreement(
        apiKey: String,
        orgId: String,
        dataAgreementId: String,
        context: Activity
    ) {
        val url =
            "https://cloudagent.igrant.io/v1/$orgId/admin/v1/data-agreements?data_agreement_id=$dataAgreementId&publish_flag=true"

        ApiManager.api.getService()
            ?.getDataAgreement(url, "ApiKey $apiKey")
            ?.enqueue(object : Callback<DataAgreementResponse> {
                override fun onFailure(
                    call: Call<DataAgreementResponse>,
                    t: Throwable
                ) {

                }

                override fun onResponse(
                    call: Call<DataAgreementResponse>,
                    response: Response<DataAgreementResponse>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        val dataAgreementPolicy: DataAgreementPolicy = DataAgreementPolicy(
                            response.body()?.results?.get(0)?.dataAgreement?.lawfulBasis
                                ?: "",
                            response.body()?.results?.get(0)?.dataAgreement?.dataPolicy?.policyURL
                                ?: "",
                            response.body()?.results?.get(0)?.dataAgreement?.dataPolicy?.jurisdiction
                                ?: "",
                            response.body()?.results?.get(0)?.dataAgreement?.dataPolicy?.industrySector
                                ?: "",
                            response.body()?.results?.get(0)?.dataAgreement?.dataPolicy?.geographicRestriction
                                ?: "",
                            null,
                            response.body()?.results?.get(0)?.dataAgreement?.dataPolicy?.dataRetentionPeriod.toString()
                        )

                        val dataAgreementContext = DataAgreementContext(
                            message = DataAgreementMessage(
                                body = response.body()?.results?.get(0)?.dataAgreement
                            )
                        )

                        DataAgreementPolicyUtil.showDataAgreementPolicy(
                            dataAgreementPolicy,
                            dataAgreementContext,
                            "",
                            context,
                            false,
                            isPolicyOnly = true
                        )
                    }
                }
            })
    }
}