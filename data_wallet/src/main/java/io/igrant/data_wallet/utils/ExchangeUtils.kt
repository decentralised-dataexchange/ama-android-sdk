package io.igrant.data_wallet.utils

import android.content.Context
import android.content.Intent
import io.igrant.data_wallet.activity.ExchangeDataActivity
import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.presentationExchange.RequestedAttribute
import io.igrant.data_wallet.models.walletSearch.Record

object ExchangeUtils {

    fun getExchangeDataIntent(record: Record, context: Context): Intent? {
        val message =
            WalletManager.getGson.fromJson(record.value, Notification::class.java)
        if (message.presentation?.dataAgreementContext != null) {

            val connectionObject =
                SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION,
                    "{\n" +
                            "  \"request_id\":\"${message.connection?.requestId ?: ""}\"\n" +
                            "}"
                )
            if ((connectionObject.totalCount ?: 0) > 0) {

                val connectionInvitaitonObject =
                    SearchUtils.searchWallet(
                        WalletRecordType.CONNECTION_INVITATION,
                        "{\n" +
                                "  \"connection_id\":\"${connectionObject.records?.get(0)?.id ?: ""}\"\n" +
                                "}"
                    )

                message.presentation?.connectionId = message.connection?.requestId

                if ((connectionInvitaitonObject.totalCount ?: 0) > 0) {
                    val invitation = WalletManager.getGson.fromJson(
                        connectionInvitaitonObject.records?.get(0)?.value,
                        Invitation::class.java
                    )
                    val intent = Intent(
                        context,
                        ProposeAndExchangeDataActivity::class.java
                    )
                    intent.putExtra(
                        ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_EXCHANGE,
                        message.presentation
                    )
                    intent.putExtra(
                        ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_RECORD_ID,
                        record.id
                    )
                    intent.putExtra(
                        ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_PROPOSAL,
                        WalletManager.getGson.toJson(message.presentation?.presentationRequest)
                    )
                    intent.putExtra(
                        ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_QR_ID,
                        message.presentation?.qrId ?: ""
                    )
                    intent.putExtra(
                        ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_INVITATION,
                        invitation
                    )
                    intent.putExtra(
                        ProposeAndExchangeDataActivity.EXTRA_IS_PROOF_VERIFIED,
                        message.isSignatureVerified
                    )
                    return intent
                }
            }

            return null

        } else {
            val intent =
                Intent(context, ExchangeDataActivity::class.java)
            intent.putExtra(
                ExchangeDataActivity.EXTRA_PRESENTATION_RECORD,
                record
            )
            return intent
        }
    }

    /**
     * Update Names with individual Name
     *
     * If a requested attribute contains names field,
     * then iterate through the names field to create a multiple attributes with name field
     * and update the incoming presentation request.
     */
    fun updateNamesWithName(requestedAttributes: Map<String, RequestedAttribute>?): Map<String, RequestedAttribute> {
        val tempRequestedAttributes: MutableMap<String, RequestedAttribute> = HashMap()

        requestedAttributes?.forEach { (name, requestedAttribute) ->
            if (requestedAttribute.names != null) {
                requestedAttribute.names?.forEach { string ->
                    var tempRequestedAttribute = RequestedAttribute()
                    tempRequestedAttribute.name = string
                    tempRequestedAttribute.restrictions = requestedAttribute.restrictions
                    tempRequestedAttribute.names = null

                    tempRequestedAttributes += Pair(string, tempRequestedAttribute)
                }
            } else {
                tempRequestedAttributes += Pair(name, requestedAttribute)
            }
        }

        return tempRequestedAttributes
    }

    fun showExchangeData(record: Record, context: Context) {
        val intent = getExchangeDataIntent(record, context)
        context.startActivity(intent)
    }
}