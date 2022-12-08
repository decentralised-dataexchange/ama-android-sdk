package io.igrant.data_wallet.utils

import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.indy.WalletManager
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject

object UnPackPresentationAck {
    fun unPack(jsonObject: JSONObject){
        try {
            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.MESSAGE_RECORDS,
                JSONObject(jsonObject.getString("message")).getJSONObject("~thread")
                    .getString("thid")
            ).get()

            val presentationExchange = SearchUtils.searchWallet(
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                "{\"thread_id\":\"${
                    JSONObject(jsonObject.getString("message")).getJSONObject("~thread")
                        .getString("thid")
                }\"}"
            )
//
            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                "${presentationExchange.records?.get(0)?.id}"
            ).get()

            EventBus.getDefault()
                .post(ReceiveExchangeRequestEvent())
        } catch (e: Exception) {

        }
    }
}