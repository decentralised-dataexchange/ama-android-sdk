package io.igrant.data_wallet.tasks

import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.PoolManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.certificateOffer.*
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.credentialExchange.Thread
import io.igrant.data_wallet.models.presentationExchange.CredentialValue
import io.igrant.data_wallet.models.presentationExchange.PresentationExchange
import io.igrant.data_wallet.models.presentationExchange.RequestCredential
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.DidCommPrefixUtils.PREFIX_1
import io.igrant.data_wallet.utils.SignOfferRequestV3
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.ledger.LedgerResults
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

object ExchangeDataTask {
    fun exchangeData(
        commonHandler: CommonHandler,
        mPresentationExchange: PresentationExchange?,
        requestedAttributes: HashMap<String, CredentialValue>,
        selfAttestedAttributes: HashMap<String, String>,
        dataAgreementContext: DataAgreementContext? = null,
        recordId: String?,
        mConnectionId: String?
    ) {

        var typedBytes: RequestBody? = null
        var serviceEndPoint: String? = null
        val TAG = "ExchangeDataTask"

        commonHandler.taskStarted()

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            handler.post {

                Pool.setProtocolVersion(2)

                val schemaParsedList: ArrayList<LedgerResults.ParseResponseResult> = ArrayList()
                val credParsedList: ArrayList<LedgerResults.ParseResponseResult> = ArrayList()
                var ledgerFailed: Boolean = false
                requestedAttributes.forEach() { (key, value) ->
                    val proverCred =
                        Anoncreds.proverGetCredential(WalletManager.getWallet, value.credId).get()
                    val schemaResponse =
                        Ledger.buildGetSchemaRequest(
                            null,
                            JSONObject(proverCred).getString("schema_id")
                        )
                            .get()
                    val requestResponse =
                        Ledger.submitRequest(PoolManager.getPool, schemaResponse).get()
                    try {
                        val schemaParsed = Ledger.parseGetSchemaResponse(requestResponse).get()
                        if (!schemaParsedList.contains(schemaParsed))
                            schemaParsedList.add(schemaParsed)

                        val credDefResponse =
                            Ledger.buildGetCredDefRequest(
                                null,
                                JSONObject(proverCred).getString("cred_def_id")
                            )
                                .get()

                        val credDefSubmitResponse =
                            Ledger.submitRequest(PoolManager.getPool, credDefResponse).get()

                        val creedDefParsed =
                            Ledger.parseGetCredDefResponse(credDefSubmitResponse).get()
                        if (!credParsedList.contains(creedDefParsed))
                            credParsedList.add(creedDefParsed)
                    } catch (e: Exception) {
                        ledgerFailed = true
                    }
                }

                if (!ledgerFailed) {

                    val requestCredential = RequestCredential()
                    requestCredential.requestedAttributes = requestedAttributes
                    requestCredential.requestedPredicates = Object()

                    requestCredential.selfAttestedAttributes = selfAttestedAttributes


                    val schemaMap = convertArrayListToHashMap(schemaParsedList)
                    var schema = "{\n"
                    schemaMap?.forEach { (s, jsonObject) ->
                        schema += " \"${s}\": $jsonObject ,\n"
                    }
                    schema = schema.substring(0, schema.length - 2)
                    if (schema != "")
                        schema += "}"
                    else
                        schema = "{}"

                    val credMap = convertArrayListToHashMap(credParsedList)
                    var credDef = "{\n"
                    credMap?.forEach { (s, jsonObject) ->
                        credDef += " \"${s}\": $jsonObject ,\n"
                    }
                    credDef = credDef.substring(0, credDef.length - 2)
                    if (credDef != "")
                        credDef += "}"
                    else
                        credDef = "{}"

                    val pr =
                        WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest)
                    val te = WalletManager.getGson.toJson(
                        requestCredential
                    )
                    Log.d(
                        TAG,
                        "doInBackground: \n ${WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest)} \n ${
                            WalletManager.getGson.toJson(
                                requestCredential
                            )
                        } \n $schema \n"
                    )

                    Log.d(TAG, "doInBackground: $credDef \n\n\n")
                    val proverProofResponse = Anoncreds.proverCreateProof(
                        WalletManager.getWallet,
                        WalletManager.getGson.toJson(mPresentationExchange?.presentationRequest),
                        WalletManager.getGson.toJson(requestCredential),
                        "IGrantMobileAgent-000001",
                        schema,
                        credDef,
                        "{}"
                    ).get()

                    mPresentationExchange?.presentation = JSONObject(proverProofResponse)
                    mPresentationExchange?.state = PresentationExchangeStates.PRESENTATION_SENT

                    Log.d(
                        TAG,
                        "initListener: ${WalletManager.getGson.toJson(mPresentationExchange)}"
                    )

                    if (recordId != "")
                        WalletMethods.updateWalletRecord(
                            WalletManager.getWallet, WalletRecordType.MESSAGE_RECORDS,
                            recordId, WalletManager.getGson.toJson(mPresentationExchange)
                        )

                    val connectionObjectRecord =
                        SearchUtils.searchWallet(
                            WalletRecordType.CONNECTION,
                            "{\n" +
                                    "  \"request_id\":\"${mPresentationExchange?.connectionId}\"\n" +
                                    "}"
                        )

                    val connectionObject = WalletManager.getGson.fromJson(
                        connectionObjectRecord.records?.get(0)?.value,
                        MediatorConnectionObject::class.java
                    )
                    val metaString =
                        Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
                    val metaObject = JSONObject(metaString)
                    val publicKey = metaObject.getString("verkey")

                    val didDocObject =
                        SearchUtils.searchWallet(
                            WalletRecordType.DID_DOC,
                            "{\n" +
                                    "  \"did\":\"${connectionObject.theirDid}\"\n" +
                                    "}"
                        )

                    val didDoc =
                        WalletManager.getGson.fromJson(
                            didDocObject.records?.get(0)?.value,
                            DidDoc::class.java
                        )

                    val base64 = Base64.encodeToString(
                        proverProofResponse.toByteArray(),
                        Base64.NO_WRAP
                    )

                    val offerData = OfferData()
                    offerData.base64 = base64

                    val requestAttach = OfferAttach()
                    requestAttach.id = "libindy-presentation-0"
                    requestAttach.mimeType = "application/json"
                    requestAttach.data = offerData

                    val requestAttachList = ArrayList<OfferAttach>()
                    requestAttachList.add(requestAttach)

                    val packedMessage = if (connectionObject.isDexaEnabled == true) {
                        val certificateOffer = RequestOfferV3()
                        certificateOffer.type =
                            "${DidCommPrefixUtils.getType(PREFIX_1)}/present-proof/1.0/presentation"
                        certificateOffer.id = UUID.randomUUID().toString()
                        certificateOffer.thread = Thread(mPresentationExchange?.threadId)
                        if (dataAgreementContext != null)
                            certificateOffer.dataAgreementContext =
                                SignOfferRequestV3.buildCertificateRequestV3(
                                    dataAgreementContext,
                                    publicKey,
                                    didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
                                )
                        certificateOffer.presentationsAttach = requestAttachList

                        PackingUtils.packMessage(
                            didDoc, publicKey,
                            WalletManager.getGson.toJson(certificateOffer), ""
                        )
                    }else{
                        val certificateOffer = RequestOffer()
                        certificateOffer.type =
                            "${DidCommPrefixUtils.getType(PREFIX_1)}/present-proof/1.0/presentation"
                        certificateOffer.id = UUID.randomUUID().toString()
                        certificateOffer.thread = Thread(mPresentationExchange?.threadId)
                        if (dataAgreementContext != null)
                            certificateOffer.dataAgreementContext =
                                SignOfferRequest.buildCertificateRequest(
                                    dataAgreementContext,
                                    publicKey,
                                    didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
                                )
                        certificateOffer.presentationsAttach = requestAttachList

                        PackingUtils.packMessage(
                            didDoc, publicKey,
                            WalletManager.getGson.toJson(certificateOffer), ""
                        )
                    }

                    serviceEndPoint =
                        didDoc.service?.get(0)?.serviceEndpoint ?: ""

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

                commonHandler.onExchangeDataComplete(serviceEndPoint, typedBytes)
            }
        }
    }

    private fun convertArrayListToHashMap(arrayList: ArrayList<LedgerResults.ParseResponseResult>): HashMap<String, JSONObject>? {
        val hashMap: HashMap<String, JSONObject> = HashMap()
        for (str in arrayList) {
            hashMap[str.id] = JSONObject(str.objectJson)
        }
        return hashMap
    }
}