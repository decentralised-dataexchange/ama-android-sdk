package io.igrant.data_wallet.utils

import android.content.Context
import android.util.Base64
import android.util.Log
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.presentationExchange.PresentationExchange
import io.igrant.data_wallet.models.presentationExchange.PresentationRequest
import org.greenrobot.eventbus.EventBus
import org.json.JSONObject
import java.util.*

object UnPackRequestPresentation {

    fun unPack(
        context: Context,
        jsonObject: JSONObject,
        notificationListener: NotificationListener?
    ) {
        val recipientKey = jsonObject.getString("recipient_verkey")

        val connectionSearch = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"my_key\":\"$recipientKey\"}"
        )

        val connectionObject: MediatorConnectionObject =
            WalletManager.getGson.fromJson(
                connectionSearch.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )

//        val connectionObject = ConnectionUtils.getConnection(jsonObject.getString("sender_verkey"))

        if (connectionObject != null) {
            val p = SearchUtils.searchWallet(
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                "{\"thread_id\":\"${JSONObject(jsonObject.getString("message")).getString("@id")}\"}"
            )

            val presentationRequestBase64 =
                JSONObject(
                    JSONObject(jsonObject.getString("message")).getJSONArray("request_presentations~attach")
                        .get(0).toString()
                )
                    .getJSONObject("data").getString("base64")
            val presentationRequest = WalletManager.getGson.fromJson(
                Base64.decode(presentationRequestBase64, Base64.URL_SAFE)
                    .toString(charset("UTF-8")), PresentationRequest::class.java
            )
            if ((p.totalCount ?: 0) == 0) {
                val presentationExchange = PresentationExchange()
                val message = JSONObject(jsonObject.getString("message"))
                presentationExchange.threadId =
                    message.getString("@id")

                if (message.has("qr_id"))
                    presentationExchange.qrId = message.getString("qr_id")

                try {
                    if (message.has("~data-agreement-context"))
                        presentationExchange.dataAgreementContext = WalletManager.getGson.fromJson(
                            message.getString("~data-agreement-context"),
                            DataAgreementContext::class.java
                        )
                } catch (e: Exception) {
                }
                presentationExchange.createdAt = DateUtils.getIndyFormattedDate()
                presentationExchange.updatedAt = DateUtils.getIndyFormattedDate()
                presentationExchange.connectionId = connectionObject?.requestId
                presentationExchange.initiator = "external"
                presentationExchange.presentationProposalDict = null
                presentationExchange.presentationRequest = presentationRequest
                presentationExchange.role = "prover"
                presentationExchange.state = PresentationExchangeStates.REQUEST_RECEIVED
                presentationExchange.type =
                    JSONObject(jsonObject.getString("message")).getString("@type")
                try {
                    presentationExchange.comment =
                        JSONObject(jsonObject.getString("message")).getString("comment")
                } catch (e: Exception) {
                    presentationExchange.comment = ""
                }

                val id = UUID.randomUUID().toString()
                val tag =
                    "{\"thread_id\": \"${JSONObject(jsonObject.getString("message")).getString("@id")}\"," +
                            "\"connection_id\":\"${connectionObject?.requestId}\"}"
                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.PRESENTATION_EXCHANGE_V10,
                    id,
                    WalletManager.getGson.toJson(presentationExchange),
                    tag
                )

                val notification = Notification()
                notification.type = MessageTypes.SHARE_REQUEST
                notification.stat = "Active"
                notification.presentation = presentationExchange
                notification.connection = connectionObject
                notification.date = DateUtils.getIndyFormattedDate()

                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.MESSAGE_RECORDS,
                    JSONObject(jsonObject.getString("message")).getString("@id"),
                    WalletManager.getGson.toJson(notification),
                    "{\n" +
                            "  \"type\":\"${MessageTypes.SHARE_REQUEST}\",\n" +
                            "  \"connectionId\":\"${connectionObject?.requestId}\",\n" +
                            "  \"certificateId\":\"${
                                JSONObject(jsonObject.getString("message")).getString(
                                    "@id"
                                )
                            }\",\n" +
                            "  \"stat\":\"Active\"\n" +
                            "}"
                )

                try {
                    val searchResponse = SearchUtils.searchWallet(
                        WalletRecordType.MESSAGE_RECORDS,
                        "{\"certificateId\":\"${
                            JSONObject(jsonObject.getString("message")).getString(
                                "@id"
                            )
                        }\"}"
                    )
                    if ((searchResponse.totalCount ?: 0) > 0) {

                        //go to intialize activity then start the offer certificate activity
                        val intent = ExchangeUtils.getExchangeDataIntent(
                            searchResponse.records!![0],
                            context
                        )

                        if (intent != null) {
                            if (notificationListener != null) {
                                notificationListener.receivedNotification(
                                    MessageTypes.SHARE_REQUEST,
                                    intent
                                )
                            } else {
                                MessageUtils.showNotification(
                                    intent,
                                    context,
                                    MessageTypes.TYPE_ISSUE_CREDENTIAL,
                                    context.resources.getString(R.string.data_received_exchange_request),
                                    "Received a new exchange request from the organisation ${connectionObject?.theirLabel ?: ""}"
                                )
                            }

                        }
                        EventBus.getDefault()
                            .post(ReceiveExchangeRequestEvent())
                    }
                } catch (e: Exception) {
                    Log.d(InitializeActivity.TAG, "unPackRequestPresentation: ${e.message}")
                }
            }
        }
    }
}