package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.tagJsons.UpdateInvitationKey
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.DID_DOC
import io.igrant.data_wallet.utils.WalletRecordType.Companion.DID_KEY
import io.igrant.data_wallet.utils.WalletRecordType.Companion.REGISTRY_CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.REGISTRY_DID_DOC
import io.igrant.data_wallet.utils.WalletRecordType.Companion.REGISTRY_DID_KEY
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object SaveDidDocTask {
    private lateinit var serviceEndPoint: String
    private var typedBytes: RequestBody? = null
    private const val TAG = "SaveDidDocTask"
    fun saveDidDoc(
        commonHandler: CommonHandler,
        body: String,
        connectionType: String?,
        isRegistryConnection: Boolean
    ) {

        commonHandler.taskStarted()

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            handler.post {
                val unpacked =
                    Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()

                val response = JSONObject(String(unpacked))

                val message = JSONObject(response.get("message").toString())

                val recipientKey = response.getString("recipient_verkey")

                val senderKey = response.getString("sender_verkey")

                val connectionSig = JSONObject(message.get("connection~sig").toString())
                val sigData = connectionSig.get("sig_data").toString()
                val position = Base64.decode(sigData, Base64.URL_SAFE)
                    .toString(charset("UTF-8")).indexOf("{\"DID\"")
                val data =
                    Base64.decode(sigData, Base64.URL_SAFE).toString(charset("UTF-8"))
                        .substring(position)

                val didData = JSONObject(data)
                val didDoc = didData.getString("DIDDoc")
                val theirDid = didData.getString("DID")

                val didDocUuid = UUID.randomUUID().toString()

                val tagJson = "{\"did\": \"$theirDid\"}"

                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    if (isRegistryConnection) REGISTRY_DID_DOC else DID_DOC,
                    didDocUuid,
                    didDoc.toString(),
                    tagJson
                )

                val publicKey = JSONObject(didDoc).getJSONArray("publicKey").getJSONObject(0)
                    .getString("publicKeyBase58")

                val didKeyUuid = UUID.randomUUID().toString()

                val didKeyTagJson = "{\"did\": \"$theirDid\", \"key\": \"$publicKey\"}"

                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    if (isRegistryConnection) REGISTRY_DID_KEY else DID_KEY,
                    didKeyUuid,
                    publicKey,
                    didKeyTagJson
                )

                val connectionSearch = SearchUtils.searchWallet(
                    if (isRegistryConnection) REGISTRY_CONNECTION else CONNECTION,
                    if (isRegistryConnection) "{}" else "{\"my_key\":\"$recipientKey\"}"
                )

                val mediatorConnectionObject: MediatorConnectionObject =
                    WalletManager.getGson.fromJson(
                        connectionSearch.records?.get(0)?.value,
                        MediatorConnectionObject::class.java
                    )
                mediatorConnectionObject.theirDid = theirDid
                mediatorConnectionObject.state = ConnectionStates.CONNECTION_RESPONSE
                mediatorConnectionObject.connectionType = connectionType

                val connectionUuid =
                    connectionSearch.records?.get(0)?.id

                val value = WalletManager.getGson.toJson(mediatorConnectionObject)

                WalletMethods.updateWalletRecord(
                    WalletManager.getWallet,
                    if (isRegistryConnection) REGISTRY_CONNECTION else CONNECTION,
                    connectionUuid,
                    value
                )

                val requestId = mediatorConnectionObject.requestId
                val myDid = mediatorConnectionObject.myDid

                val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
                val metaObject = JSONObject(metaString)
                val publicKey2 = metaObject.getString("verkey")

                val didDocSearch = SearchUtils.searchWallet(
                    if (isRegistryConnection) REGISTRY_DID_DOC else DID_DOC,
                    "{\"did\":\"$theirDid\"}"
                )

                serviceEndPoint = ""
                if (didDocSearch.totalCount ?: 0 > 0) {
                    val didDoc = WalletManager.getGson.fromJson(
                        didDocSearch.records?.get(0)?.value,
                        DidDoc::class.java
                    )

                    serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""
                    val recipient = didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""

                    val connectionTagJson =
                        UpdateInvitationKey(requestId, myDid, recipientKey, theirDid, recipient)
                    connectionTagJson.orgId = mediatorConnectionObject.orgId
                    connectionTagJson.myKey = publicKey2

                    WalletRecord.updateTags(
                        WalletManager.getWallet,
                        if (isRegistryConnection) REGISTRY_CONNECTION else CONNECTION,
                        connectionUuid,
                        WalletManager.getGson.toJson(connectionTagJson)
                    )

                    val trustPingData = "{\n" +
                            "  \"@type\": \"${DidCommPrefixUtils.getType(didDoc.service?.get(0)?.type ?: "")}/trust_ping/1.0/ping\",\n" +
                            "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                            "  \"comment\": \"ping\",\n" +
                            "  \"response_requested\": true\n" +
                            "}\n"

                    val packedMessage = PackingUtils.packMessage(
                        didDoc,
                        publicKey2,
                        trustPingData,
                        didDoc.service?.get(0)?.type ?: ""
                    )

                    Log.d(TAG, "packed message: ${String(packedMessage)}")

                    typedBytes = object : RequestBody() {
                        override fun contentType(): MediaType? {
                            return "application/ssi-agent-wire".toMediaTypeOrNull()
                        }

                        @Throws(IOException::class)
                        override fun writeTo(sink: BufferedSink) {
                            sink.write(packedMessage)
                        }
                    }
                }

                commonHandler.onSaveDidComplete(typedBytes, serviceEndPoint)
            }
        }

    }
}