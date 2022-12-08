package io.igrant.data_wallet.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.events.ConnectionSuccessEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject

object UnPackPing {
    fun unPack(context: Context,jsonObject: JSONObject){
        val recipientVerKey = jsonObject.getString("sender_verkey")
        val connectionSearch = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"recipient_key\":\"$recipientVerKey\"}"
        )

        if ((connectionSearch.totalCount ?: 0) > 0) {
            val mediatorConnectionObject: MediatorConnectionObject =
                WalletManager.getGson.fromJson(
                    connectionSearch.records?.get(0)?.value,
                    MediatorConnectionObject::class.java
                )

            mediatorConnectionObject.state = ConnectionStates.CONNECTION_ACTIVE

            val connectionUuid =
                connectionSearch.records?.get(0)?.id

            val value = WalletManager.getGson.toJson(mediatorConnectionObject)

            WalletMethods.updateWalletRecord(
                WalletManager.getWallet,
                WalletRecordType.CONNECTION,
                connectionUuid,
                value
            )

            EventBus.getDefault().post(ConnectionSuccessEvent(mediatorConnectionObject.requestId ?: ""))
        }
        else {

            Log.d(
                InitializeActivity.TAG,
                "Registry Config - getRegistryConfig: entered ping unpack"
            )

            val registryConnectionSearch = SearchUtils.searchWallet(
                WalletRecordType.REGISTRY_CONNECTION,
                "{}"
            )
            if ((registryConnectionSearch.totalCount ?: 0) > 0) {
                Log.d(
                    InitializeActivity.TAG,
                    "Registry Config - getRegistryConfig: ping have items ${
                        registryConnectionSearch.records?.get(
                            0
                        )?.value
                    }"
                )

                val mediatorConnectionObject: MediatorConnectionObject =
                    WalletManager.getGson.fromJson(
                        registryConnectionSearch.records?.get(0)?.value,
                        MediatorConnectionObject::class.java
                    )

                mediatorConnectionObject.state = ConnectionStates.CONNECTION_ACTIVE

                val connectionUuid =
                    registryConnectionSearch.records?.get(0)?.id

                val value = WalletManager.getGson.toJson(mediatorConnectionObject)

                WalletMethods.updateWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.REGISTRY_CONNECTION,
                    connectionUuid,
                    value
                )

                Log.d(
                    InitializeActivity.TAG,
                    "Registry Config - getRegistryConfig: completed"
                )

                //todo anchor registry

            } else {

                Toast.makeText(
                    context,
                    context.resources.getString(R.string.connection_unexpected_error_please_try_again),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }
}