package io.igrant.data_wallet.utils

import android.app.Activity
import android.content.Context
import android.util.Base64
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.events.ReceiveCertificateEvent
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.indy.PoolManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.credentialExchange.CredentialExchange
import io.igrant.data_wallet.models.credentialExchange.IssueCredential
import io.igrant.data_wallet.models.credentialExchange.RawCredential
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.models.walletSearch.SearchResponse
import io.igrant.data_wallet.tasks.SaveConnectionDetailInCertificateTask
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import io.igrant.data_wallet.utils.wrappers.CredentialTypes
import io.igrant.data_wallet.utils.wrappers.ReceiptWrapper

object UnPackIssueCredential {

    fun unPackIssueCredential(context: Context, body: JSONObject) {
        val gson = Gson()

        val issueCredential = gson.fromJson(body.getString("message"), IssueCredential::class.java)
        val rawCredential = gson.fromJson(
            Base64.decode(issueCredential.credentialsAttach[0].data?.base64, Base64.URL_SAFE)
                .toString(charset("UTF-8")), RawCredential::class.java
        )

        val recipientKey = body.getString("recipient_verkey")

        val connectionSearch = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"my_key\":\"$recipientKey\"}"
        )

        if ((connectionSearch.totalCount ?: 0) > 0) {
            val connectionObject: MediatorConnectionObject =
                WalletManager.getGson.fromJson(
                    connectionSearch.records?.get(0)?.value,
                    MediatorConnectionObject::class.java
                )

            val credentialExchangeSearch = SearchUtils.searchWallet(
                WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                "{\"thread_id\": \"${issueCredential.thread?.thid ?: ""}\"}"
            )

            if ((credentialExchangeSearch.totalCount ?: 0) > 0) {
                val credentialExchange =
                    gson.fromJson(
                        credentialExchangeSearch.records?.get(0)?.value,
                        CredentialExchange::class.java
                    )
                credentialExchange.rawCredential = rawCredential
                credentialExchange.state = CredentialExchangeStates.CREDENTIAL_CREDENTIAL_RECEIVED

                WalletMethods.updateWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "${credentialExchangeSearch.records?.get(0)?.id}",
                    gson.toJson(credentialExchange)
                )

                sendAcknoledge(
                    connectionObject.theirDid,
                    body.getString("sender_verkey"),
                    body.getString("recipient_verkey"),
                    credentialExchange.credentialOffer?.credDefId,
                    issueCredential,
                    context
                )
            }
        }
    }

    private fun sendAcknoledge(
        did: String?,
        recipientVerKey: String,
        senderVerKey: String,
        credDefId: String?,
        issueCredential: IssueCredential,
        context: Context
    ) {
        val gson = Gson()
        val data = "{\n" +
                "  \"@type\": \"${DidCommPrefixUtils.getType(issueCredential.type ?: "")}/issue-credential/1.0/ack\",\n" +
                "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                "  \"~thread\": {\n" +
                "    \"thid\": \"${issueCredential.thread?.thid ?: ""}\"\n" +
                "  },\n" +
                "  \"status\": \"OK\"\n" +
                "}"

        val searchDid = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.DID_DOC,
            "{\"did\": \"${did}\"}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val didResponse =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, searchDid, 100).get()

        WalletManager.closeSearchHandle(searchDid)

        val searchResult = gson.fromJson(didResponse, SearchResponse::class.java)

        val didDoc =
            gson.fromJson(searchResult.records?.get(0)?.value, DidDoc::class.java)

        val packedMessage = PackingUtils.packMessage(
            didDoc, senderVerKey,
            data, issueCredential.type ?: ""
        )

        Log.d(InitializeActivity.TAG, "packed message: ${String(packedMessage)}")

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }
//
        ApiManager.api.getService()
            ?.postDataWithoutData(didDoc.service?.get(0)?.serviceEndpoint ?: "", typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
//
                }
            })
//        saveDataShareHistory(senderVerKey,credDefId,)
        storeCredential(
            issueCredential.thread?.thid ?: "",
            credDefId,
            senderVerKey,
            didDoc,
            context
        )
    }

    private fun storeCredential(
        thid: String,
        credDefId: String?,
        senderVerKey: String,
        didDoc: DidDoc,
        context: Context
    ) {

        val connectionSearch = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"my_key\":\"$senderVerKey\"}"
        )
        var connection: MediatorConnectionObject? = null
        if ((connectionSearch.totalCount ?: 0) > 0) {
            connection = WalletManager.getGson.fromJson(
                connectionSearch.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
        }
        val builder = GsonBuilder()
        builder.serializeNulls()
        val gson: Gson = builder.setPrettyPrinting().create()

        val credDef =
            Ledger.buildGetCredDefRequest(
                null,
                credDefId
            ).get()

        val credDefResponse = Ledger.submitRequest(PoolManager.getPool, credDef).get()

        try {
            val parsedCredDefResponse = Ledger.parseGetCredDefResponse(credDefResponse).get()

            val credentialExchangeSearch = WalletSearch.open(
                WalletManager.getWallet,
                WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                "{\"thread_id\": \"${thid}\"}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val credentialExchangeResponse =
                WalletSearch.searchFetchNextRecords(
                    WalletManager.getWallet,
                    credentialExchangeSearch,
                    100
                ).get()

            Log.d(InitializeActivity.TAG, "credentialExchangeResult: $credentialExchangeResponse")
            WalletManager.closeSearchHandle(credentialExchangeSearch)


            val searchResponse =
                gson.fromJson(credentialExchangeResponse, SearchResponse::class.java)
            if ((searchResponse.totalCount ?: 0) > 0) {
                val credentialExchange =
                    gson.fromJson(
                        searchResponse.records?.get(0)?.value,
                        CredentialExchange::class.java
                    )

                val uuid = UUID.randomUUID().toString()
                val credentialId = Anoncreds.proverStoreCredential(
                    WalletManager.getWallet,
                    uuid,
                    gson.toJson(credentialExchange.credentialRequestMetadata),
                    gson.toJson(credentialExchange.rawCredential),
                    parsedCredDefResponse.objectJson,
                    null
                ).get()

                credentialExchange.state = CredentialExchangeStates.CREDENTIAL_CREDENTIAL_ACK

                WalletMethods.updateWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "${searchResponse.records?.get(0)?.id}",
                    gson.toJson(credentialExchange)
                )

                val data = WalletRecord.get(
                    WalletManager.getWallet, WalletRecordType.MESSAGE_RECORDS,
                    thid, "{}"
                ).get()

                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.MESSAGE_RECORDS,
                    thid
                ).get()

                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "${searchResponse.records?.get(0)?.id}"
                ).get()

                Log.d("milna", "storeCredential: checking")

                val subCredentialType = try {
                    if (ReceiptWrapper.checkCredentialType(
                            credentialExchange.credentialProposalDict?.credentialProposal?.attributes
                                ?: ArrayList()
                        ) == CredentialTypes.RECEIPT
                    ) CredentialTypes.RECEIPT else CredentialTypes.DEFAULT
                } catch (e: Exception) {
                    CredentialTypes.DEFAULT
                }

                Log.d("milna", "storeCredential: $subCredentialType")

                val walletModel = WalletModel()
                walletModel.type = WalletRecordType.CERTIFICATE_TYPE_CREDENTIALS
                walletModel.subType = subCredentialType
                walletModel.connection = connection

                try {
                    val lst = credentialExchange.rawCredential?.schemaId?.split(":")
                    val text = lst?.get(2) ?: ""
                    walletModel.searchableText = text
                } catch (e: Exception) {
                }

                walletModel.credentialId = credentialId
                walletModel.rawCredential = credentialExchange.rawCredential
                walletModel.credentialProposalDict = credentialExchange.credentialProposalDict

                val walletModelTag = "{" +
                        "\"type\":\"${WalletRecordType.CERTIFICATE_TYPE_CREDENTIALS}\"," +
                        "\"sub_type\":\"$subCredentialType\"," +
                        "\"connection_id\":\"${connection?.requestId ?: ""}\"," +
                        "\"credential_id\":\"$credentialId\"," +
                        "\"schema_id\":\"${credentialExchange.rawCredential?.schemaId ?: ""}\"" +
                        "}"

                val record = WalletManager.getGson.fromJson(data, Record::class.java)
                val notification =
                    WalletManager.getGson.fromJson(record.value, Notification::class.java)
                Log.d("milna", "storeCredential: $data")
                val isDexa =
                    DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(notification.certificateOffer?.dataAgreementContext?.message?.body);
                if (!isDexa || (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                        notification.certificateOffer?.dataAgreementContext?.message?.body
                    ).dataPolicy?.thirdPartyDataSharing == false)
                )
                    WalletMethods.addWalletRecord(
                        WalletManager.getWallet,
                        WalletRecordType.WALLET,
                        credentialId,
                        WalletManager.getGson.toJson(walletModel),
                        walletModelTag
                    )

                // save to history
                DataHistory.saveDataShareHistory(
                    connection,
                    data,
                    didDoc,
                    credDefId ?: "",
                    walletModel
                )

                if (!isDexa || (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                        notification.certificateOffer?.dataAgreementContext?.message?.body
                    ).dataPolicy?.thirdPartyDataSharing == false)
                ) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.data_successfully_added_data_to_the_data_wallet),
                        Toast.LENGTH_LONG
                    ).show()

//                    //custom toast
//                    val layout: View = context.layoutInflater.inflate(
//                        R.layout.toast,
//                        context.findViewById(R.id.toast_layout_root)
//                    )
//
//                    val text = layout.findViewById<View>(R.id.text) as TextView
//                    text.text =
//                        context.getString(R.string.data_successfully_added_data_to_the_data_wallet)
//
//                    val toast = Toast(context)
//                    toast.setGravity(Gravity.BOTTOM or Gravity.FILL_HORIZONTAL, 0, 0)
//                    toast.duration = Toast.LENGTH_LONG
//                    toast.view = layout
//                    toast.setMargin(0f, 0f)
//                    toast.show()
//                    //custom toast
                }

                EventBus.getDefault().post(ReceiveCertificateEvent())
                EventBus.getDefault()
                    .post(ReceiveExchangeRequestEvent())

                SaveConnectionDetailInCertificateTask.saveConnection(
                    connection?.requestId ?: "",
                    credentialId
                )
            }
        } catch (e: Exception) {
            Toast.makeText(
                context,
                context.resources.getString(R.string.data_invalid_ledger_you_can_choose_proper_ledger_from_settings),
                Toast.LENGTH_SHORT
            ).show()
        }

    }
}