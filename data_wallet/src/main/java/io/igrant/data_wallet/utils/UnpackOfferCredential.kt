package io.igrant.data_wallet.utils

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.activity.OfferCertificateActivity
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.certificateOffer.Base64Extracted
import io.igrant.data_wallet.models.certificateOffer.CertificateOffer
import io.igrant.data_wallet.models.credentialExchange.CredentialExchange
import io.igrant.data_wallet.models.credentialExchange.CredentialProposalDict
import io.igrant.data_wallet.models.did.DidResult
import io.igrant.data_wallet.models.walletSearch.Record
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONObject
import java.util.*

object UnpackOfferCredential {

    fun unPackOfferCredential(
        context: Context,
        body: JSONObject,
        notificationListener: NotificationListener?
    ) {
        val message = JSONObject(body.getString("message"))
        val certificateOffer =
            WalletManager.getGson.fromJson(message.toString(), CertificateOffer::class.java)

        val senderKey = body.getString("sender_verkey")

        val searchDid = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.DID_KEY,
            "{\"key\": \"${senderKey}\"}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val didResponse =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDid, 100).get()

        Log.d(InitializeActivity.TAG, "searchDid: $didResponse")
        WalletManager.closeSearchHandle(searchDid)

        if (JSONObject(didResponse).getInt("totalCount") > 0) {
            val didData = JSONObject(didResponse).getJSONArray("records").get(0).toString()
            val didResult = WalletManager.getGson.fromJson(didData, DidResult::class.java)

            val connectionResult = SearchUtils.searchWallet(
                WalletRecordType.CONNECTION,
                "{\"their_did\": \"${didResult.tags!!.did}\"}"
            )

            if ((connectionResult.totalCount ?: 0) > 0) {
                val connecction = WalletManager.getGson.fromJson(
                    connectionResult.records?.get(0)?.value, MediatorConnectionObject::class.java
                )

                val credentialExchangeSearch = SearchUtils.searchWallet(
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "{\"thread_id\": \"${certificateOffer.id}\"}"
                )

                if (credentialExchangeSearch.totalCount == 0) {
                    saveCredentialExchange(
                        certificateOffer,
                        connectionResult.records?.get(0)
                    )
                }

                val notification = Notification()
                notification.type = MessageTypes.OFFER_REQUEST
                notification.stat = "Active"
                notification.certificateOffer = certificateOffer
                notification.connection = connecction
                notification.date = DateUtils.getIndyFormattedDate()

                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.MESSAGE_RECORDS,
                    certificateOffer.id ?: "",
                    WalletManager.getGson.toJson(notification),
                    "{\n" +
                            "  \"type\":\"${MessageTypes.OFFER_REQUEST}\",\n" +
                            "  \"connectionId\":\"${connecction.requestId}\",\n" +
                            "  \"certificateId\":\"${certificateOffer.id}\",\n" +
                            "  \"stat\":\"Active\"\n" +
                            "}"
                )

                try {

                    val searchResponse = SearchUtils.searchWallet(
                        WalletRecordType.MESSAGE_RECORDS,
                        "{\"certificateId\":\"${certificateOffer.id}\"}"
                    )
                    if ((searchResponse.totalCount ?: 0) > 0) {

                        //go to intialize activity then start the offer certificate activity
                        val intent =
                            Intent(context, OfferCertificateActivity::class.java)
                        intent.putExtra(
                            OfferCertificateActivity.EXTRA_CERTIFICATE_PREVIEW,
                            searchResponse.records!![0]
                        )

                        if (notificationListener != null) {
                            notificationListener.receivedNotification(
                                MessageTypes.OFFER_REQUEST,
                                intent
                            )
                        } else {
                            MessageUtils.showNotification(
                                intent,
                                context,
                                MessageTypes.TYPE_ISSUE_CREDENTIAL,
                                context.getString(R.string.data_received_offer_credentials),
                                context.getString(R.string.data_received_offer_credential_from_organisation)
                            )
                        }
                        EventBus.getDefault()
                            .post(ReceiveExchangeRequestEvent())

                    }

                    EventBus.getDefault()
                        .post(ReceiveExchangeRequestEvent())
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun saveCredentialExchange(
        certificateOffer: CertificateOffer,
        connectionRecord: Record?
    ) {
        val base64Sting =
            Base64.decode(certificateOffer.offersAttach!![0].data!!.base64, Base64.URL_SAFE)
                .toString(charset("UTF-8"))

        val credentialProposal =
            WalletManager.getGson.fromJson(base64Sting, Base64Extracted::class.java)
        val credentialProposalDict = CredentialProposalDict()
        credentialProposalDict.type =
            "${DidCommPrefixUtils.getType(certificateOffer.type ?: "")}/issue-credential/1.0/propose-credential"
        credentialProposalDict.id = UUID.randomUUID().toString()
        credentialProposalDict.comment = "string"
        credentialProposalDict.schemaId = credentialProposal.schemaId
        credentialProposalDict.credDefId = credentialProposal.credDefId
        credentialProposalDict.credentialProposal = certificateOffer.credentialPreview

        val credentialExchange = CredentialExchange()
        credentialExchange.threadId = certificateOffer.id
        credentialExchange.createdAt = DateUtils.getIndyFormattedDate()
        credentialExchange.updatedAt = DateUtils.getIndyFormattedDate()
        credentialExchange.connectionId =
            connectionRecord?.tags?.get("request_id")
        credentialExchange.state = CredentialExchangeStates.CREDENTIAL_OFFER_RECEIVED
        credentialExchange.credentialProposalDict = credentialProposalDict
        credentialExchange.credentialOffer = credentialProposal

        Log.d(
            InitializeActivity.TAG,
            "saveCredentialExchange: ${WalletManager.getGson.toJson(credentialExchange)}"
        )
        val uudi = UUID.randomUUID().toString()
        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            WalletRecordType.CREDENTIAL_EXCHANGE_V10,
            uudi,
            WalletManager.getGson.toJson(credentialExchange).toString(),
            "{\"thread_id\": \"${certificateOffer.id}\"," +
                    "\"connection_id\":\"${connectionRecord?.tags?.get("request_id")}\"}"
        )
    }
}