package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import android.util.Base64
import android.util.Log
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.tagJsons.UpdateInvitationKey
import io.igrant.data_wallet.utils.ConnectionStates
import io.igrant.data_wallet.utils.DidCommPrefixUtils
import io.igrant.data_wallet.utils.PackingUtils
import io.igrant.data_wallet.utils.SearchUtils
import io.igrant.data_wallet.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.DID_DOC
import io.igrant.data_wallet.utils.WalletRecordType.Companion.DID_KEY
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

class SaveDidDocTask(
    private val commonHandler: CommonHandler,
    private val body: String,
    private val iGrantEnabled: Boolean
) :
    AsyncTask<Void, Void, Void>() {

    private lateinit var serviceEndPoint: String
    private var typedBytes: RequestBody? = null
    private val TAG = "SaveDidDocTask"

    override fun doInBackground(vararg p0: Void?): Void? {

        //todo add a new tag in the connection with invitaion key and extra my var key then update my key not invitation key
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()

        val response = JSONObject(String(unpacked))

        val message = JSONObject(response.get("message").toString())

        val recipientKey = response.getString("recipient_verkey")

        val connectionSig = JSONObject(message.get("connection~sig").toString())
        val sigData = connectionSig.get("sig_data").toString()
        val position = Base64.decode(sigData, Base64.URL_SAFE)
            .toString(charset("UTF-8")).indexOf("{")
        val data =
            Base64.decode(sigData, Base64.URL_SAFE).toString(charset("UTF-8"))
                .substring(position)

        val didData = JSONObject(data)
        val didDoc = didData.getString("DIDDoc")
        val theirDid = didData.getString("DID")

        val didDocUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$theirDid\"}"

        WalletRecord.add(
            WalletManager.getWallet,
            DID_DOC,
            didDocUuid,
            didDoc.toString(),
            tagJson
        )

        val publicKey = JSONObject(didDoc).getJSONArray("publicKey").getJSONObject(0)
            .getString("publicKeyBase58")

        val didKeyUuid = UUID.randomUUID().toString()

        val didKeyTagJson = "{\"did\": \"$theirDid\", \"key\": \"$publicKey\"}"

        WalletRecord.add(
            WalletManager.getWallet,
            DID_KEY,
            didKeyUuid,
            publicKey,
            didKeyTagJson
        )

        val connectionSearch = SearchUtils.searchWallet(CONNECTION,
            "{\"my_key\":\"$recipientKey\"}")

        val mediatorConnectionObject: MediatorConnectionObject =
            WalletManager.getGson.fromJson(
                connectionSearch.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
        mediatorConnectionObject.theirDid = theirDid
        mediatorConnectionObject.state = ConnectionStates.CONNECTION_RESPONSE
        mediatorConnectionObject.isIGrantEnabled = iGrantEnabled

        val connectionUuid =
            connectionSearch.records?.get(0)?.id

        val value = WalletManager.getGson.toJson(mediatorConnectionObject)

        WalletRecord.updateValue(
            WalletManager.getWallet,
            CONNECTION,
            connectionUuid,
            value
        )

        val requestId = mediatorConnectionObject.requestId
        val myDid = mediatorConnectionObject.myDid

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val publicKey2 = metaObject.getString("verkey")

        val didDocSearch = SearchUtils.searchWallet(
            DID_DOC,
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

            val connectionTagJson = UpdateInvitationKey(requestId,myDid,recipientKey,theirDid,recipient)
            connectionTagJson.orgId = mediatorConnectionObject.orgId
            connectionTagJson.myKey = publicKey2

            WalletRecord.updateTags(
                WalletManager.getWallet,
                CONNECTION,
                connectionUuid,
                WalletManager.getGson.toJson(connectionTagJson)
            )

            val trustPingData = "{\n" +
                    "  \"@type\": \"${DidCommPrefixUtils.getType(didDoc.service?.get(0)?.type?:"")}/trust_ping/1.0/ping\",\n" +
                    "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "  \"comment\": \"ping\",\n" +
                    "  \"response_requested\": true\n" +
                    "}\n"

            val packedMessage = PackingUtils.packMessage(didDoc,publicKey2,trustPingData,didDoc.service?.get(0)?.type?:"")

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
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        commonHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        commonHandler.onSaveDidComplete(typedBytes,serviceEndPoint)
    }
}