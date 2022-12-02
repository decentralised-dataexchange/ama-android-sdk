package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.events.ReceiveCertificateEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.connection.Connection
import io.igrant.data_wallet.models.connection.ConnectionV2
import io.igrant.data_wallet.models.connection.ConnectionV2Response
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.ConnectionTypes.V2_CONNECTION
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object SaveConnectionDetailInCertificateTask {
    fun saveConnection(
        connectionId: String?,
        certificateId: String?
    ) {

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            handler.post {
                val searchConnection = SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION,
                    "{\"request_id\":\"$connectionId\"}"
                )

                if (searchConnection.totalCount ?: 0 > 0) {
                    val connectionObj = WalletManager.getGson.fromJson(
                        searchConnection.records?.get(0)?.value,
                        MediatorConnectionObject::class.java
                    )
                    val searchDidDoc = SearchUtils.searchWallet(
                        WalletRecordType.DID_DOC,
                        "{\"did\":\"${connectionObj.theirDid}\"}"
                    )

                    if (searchDidDoc.totalCount ?: 0 > 0) {
                        val disDoc = WalletManager.getGson.fromJson(
                            searchDidDoc.records?.get(0)?.value,
                            DidDoc::class.java
                        )
                        if (connectionObj.connectionType == V2_CONNECTION) {
                            ConnectionDetail.getV2ConnectionDetail(connectionObj.myDid ?: "",
                                connectionObj.theirDid ?: "",
                                disDoc,
                                object : ConnectionDetailCompletionListener {
                                    override fun onSuccess(connection: ConnectionV2Response) {
                                        super.onSuccess(connection)
                                        saveOrgDetails(null, connection.body, certificateId ?: "")
                                    }

                                    override fun onFailure() {
                                        super.onFailure()
                                    }
                                })
                        } else {

                            val orgData =
                                "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/organization-info\", \"@id\": \"${"$connectionId"}\" , \"~transport\": {" +
                                        "\"return_route\": \"all\"}\n}"

                            val metaString =
                                Did.getDidWithMeta(WalletManager.getWallet, connectionObj?.myDid)
                                    .get()
                            val metaObject = JSONObject(metaString)
                            val key = metaObject.getString("verkey")

                            val orgDetailPacked = PackingUtils.packMessage(
                                disDoc, key,
                                orgData, ""
                            )

                            val orgDetailTypedArray = object : RequestBody() {
                                override fun contentType(): MediaType? {
                                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                                }

                                @Throws(IOException::class)
                                override fun writeTo(sink: BufferedSink) {
                                    sink.write(orgDetailPacked)
                                }
                            }


                            ApiManager.api.getService()
                                ?.postData(
                                    disDoc.service?.get(0)?.serviceEndpoint ?: "",
                                    orgDetailTypedArray
                                )
                                ?.enqueue(object :
                                    Callback<ConfigPostResponse> {
                                    override fun onFailure(
                                        call: Call<ConfigPostResponse>,
                                        t: Throwable
                                    ) {

                                    }

                                    override fun onResponse(
                                        call: Call<ConfigPostResponse>,
                                        response: Response<ConfigPostResponse>
                                    ) {
                                        if (response.code() == 200 && response.body() != null) {
                                            try {
                                                val unpack =
                                                    Crypto.unpackMessage(
                                                        WalletManager.getWallet,
                                                        WalletManager.getGson.toJson(response.body())
                                                            .toString()
                                                            .toByteArray()
                                                    ).get()

                                                Log.d(
                                                    "milan",
                                                    "onResponse: ${
                                                        JSONObject(String(unpack)).getString(
                                                            "message"
                                                        )
                                                    }"
                                                )
                                                val connectionData = WalletManager.getGson.fromJson(
                                                    JSONObject(String(unpack)).getString("message"),
                                                    Connection::class.java
                                                )

                                                saveOrgDetails(
                                                    connectionData,
                                                    null,
                                                    certificateId ?: ""
                                                )
                                            } catch (e: Exception) {
                                            }
                                        }
                                    }
                                })
                        }
                    }
                }
            }
        }
    }

    private fun saveOrgDetails(
        connectionData: Connection?,
        connectionV2: ConnectionV2?,
        certificateId: String
    ) {
        val walletSearch = SearchUtils.searchWallet(
            WalletRecordType.WALLET,
            "{\"credential_id\":\"$certificateId\"}"
        )

        if (walletSearch.totalCount ?: 0 > 0) {
            val wallet = WalletManager.getGson.fromJson(
                walletSearch.records?.get(0)?.value,
                WalletModel::class.java
            )
            wallet.organization = connectionData
            wallet.organizationV2 = connectionV2
//
            WalletMethods.updateWalletRecord(
                WalletManager.getWallet,
                WalletRecordType.WALLET,
                certificateId,
                WalletManager.getGson.toJson(wallet)
            )

            EventBus.getDefault().post(ReceiveCertificateEvent())
        }
    }
}
