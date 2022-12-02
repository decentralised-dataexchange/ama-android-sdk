package io.igrant.data_wallet.utils

import android.util.Base64
import android.util.Base64.*
import android.util.Log
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.certificateOffer.*
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.connectionRequest.Transport
import io.igrant.data_wallet.models.requests.JsonLdProcessBody
import io.igrant.data_wallet.models.requests.JsonLdProcessRequest
import io.igrant.data_wallet.models.response.JsonLdProcessedDataResponse
import io.igrant.data_wallet.utils.DidCommPrefixUtils.PREFIX_1
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.apache.commons.io.IOUtils
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList

object SignOfferRequest {

    const val TAG = "SignOfferRequest"

    private fun buildJsonLDProcessingRequest(
        dataAgreementContext: DataAgreementContext?,
        signatureOptionsBase64: String,
        connectionObject: MediatorConnectionObject?,
        publicKey: String,
        didDoc: DidDoc,
        isVerify: Boolean
    ): RequestBody {

        // 3. Creating data_base64
        val tempBody = dataAgreementContext?.message?.body

        val base = encode(
            WalletManager.getGson.toJson(
                tempBody
            ).toByteArray(),
            Base64.URL_SAFE
        )

        val base64 = String(base).replace("\n", "")

        Logger.d(TAG, "signOfferRequest: $base64")

        // 4. Creating Request
        val request = JsonLdProcessRequest(
            "${DidCommPrefixUtils.getType(PREFIX_1)}/json-ld/1.0/processed-data",
            UUID.randomUUID().toString(),
            WalletUtils.convertDidSovToMyDidWithMyData(connectionObject?.myDid ?: ""),
            Date().time.toString(),
            WalletUtils.convertDidSovToMyDidWithMyData(connectionObject?.theirDid ?: ""),
            JsonLdProcessBody(
                base64,
                signatureOptionsBase64,
                if (isVerify) false else true
            ),
            Transport("all")
        )

        // 5. packing message using the did doc of connection
        val packedMessage = PackingUtils.packMessage(
            didDoc, publicKey,
            WalletManager.getGson.toJson(request),
            dataAgreementContext?.message?.type ?: ""
        )

        val typedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }
        return typedBytes
    }

    fun signRequest(
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String,
        mListener: SignOfferRequestListeners,
        isVerify: Boolean
    ) {
        // 1. fetching connection details(connection, didDoc)
        val connectionObject = getConnectionObject(mConnectionId)

        val publicKey = getPublicKeyForConnection(connectionObject?.myDid)

        val didDoc = getDidDocOfConnection(connectionObject?.theirDid)

        if (!isVerify) {
            var databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body)
            val eventList = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).event
            //todo check if it is my key or their key
            val newEvent = Event(
                "${WalletUtils.convertDidSovToMyDidWithMyData(publicKey)}#2",
                timeStamp = DateUtils.getIndyFormattedDate(),
                did = WalletUtils.convertDidSovToMyDidWithMyData(publicKey),
                state = "accept"
            )
            eventList?.add(newEvent)
            databody.event = eventList

            dataAgreementContext?.message?.body = databody

        }

        var proof: Proof =
            if (isVerify) {
                if (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proofChain != null)
                    DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proofChain?.get(
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proofChain?.size ?: 0) - 1
                    ) ?: Proof()
                else
                    DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proof!!
            } else
                Proof(
                    "${WalletUtils.convertDidSovToMyDidWithMyData(publicKey)}#2",
                    "Ed25519Signature2018",
                    DateUtils.getIndyFormattedDate(),
                    WalletUtils.convertDidSovToMyDidWithMyData(
                        publicKey
                    ),
                    "contractAgreement", null
                )
        val signatureOptionsBase64 = buildProofRequest(proof)
        val typedArray = buildJsonLDProcessingRequest(
            dataAgreementContext,
            signatureOptionsBase64,
            connectionObject,
            publicKey,
            didDoc,
            isVerify
        )

        // 6. Sending the request to the connection service end point
        ApiManager.api.getService()
            ?.postDataWithoutData(didDoc.service?.get(0)?.serviceEndpoint ?: "", typedArray)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(
                    call: Call<ResponseBody>,
                    t: Throwable
                ) {

                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        // 7. Converting byte stream to string
                        Logger.d(TAG, "unPackMessage: ${response.body()!!.byteStream()}")
                        val inputStream = response.body()!!.byteStream()
                        val str: String = IOUtils.toString(inputStream, "UTF-8")
                        Logger.d(TAG, "onResponse: $str")
                        unPackMessage(
                            str,
                            mListener,
                            dataAgreementContext,
                            publicKey,
                            didDoc.publicKey?.get(0)?.publicKeyBase58 ?: "",
                            proof, isVerify
                        )
                    }
                }
            })
    }

    fun getDidDocOfConnection(theirDid: String?): DidDoc {
        val didDocObject =
            SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\n" +
                        "  \"did\":\"${theirDid}\"\n" +
                        "}"
            )

        return WalletManager.getGson.fromJson(
            didDocObject.records?.get(0)?.value,
            DidDoc::class.java
        )
    }

    fun getPublicKeyForConnection(myDid: String?): String {
        val metaString =
            Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        return metaObject.getString("verkey")
    }

    fun getConnectionObject(mConnectionId: String): MediatorConnectionObject? {
        val resultObject =
            SearchUtils.searchWallet(
                WalletRecordType.CONNECTION,
                "{\n" +
                        "  \"request_id\":\"$mConnectionId\"\n" +
                        "}"
            )
        val connectionObject = WalletManager.getGson.fromJson(
            resultObject.records?.get(0)?.value,
            MediatorConnectionObject::class.java
        )
        return connectionObject
    }

    private fun buildProofRequest(proof: Proof): String {
        // 2. Creating signature_options_base64
        val signatureOptions = encode(
            WalletManager.getGson.toJson(
                proof
            ).toByteArray(), Base64.URL_SAFE
        )
        val signatureOptionsBase64 = String(signatureOptions).replace("\n", "")
        Logger.d(TAG, "signOfferRequest: $signatureOptionsBase64")
        return signatureOptionsBase64
    }

    private fun unPackMessage(
        body: String,
        mListener: SignOfferRequestListeners,
        mDataAgreementContext: DataAgreementContext?,
        myKey: String,
        theirKey: String,
        proof: Proof,
        isVerify: Boolean
    ) {
        // 8. Unpacking the response for further signing of json ld structure
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
        val processedDataResponse = WalletManager.getGson.fromJson(
            JSONObject(String(unpacked)).getString("message"),
            JsonLdProcessedDataResponse::class.java
        )
        if (isVerify)
            mListener.onProcessJsonLD(processedDataResponse)
        else
            signJsonLdStructure(
                processedDataResponse,
                mListener,
                mDataAgreementContext,
                myKey,
                theirKey,
                proof
            )
    }

    private fun signJsonLdStructure(
        processedDataResponse: JsonLdProcessedDataResponse,
        mListener: SignOfferRequestListeners,
        mCertificateOffer: DataAgreementContext?,
        myKey: String,
        theirKey: String,
        proof: Proof
    ) {
        // 9. Json ld structure signing
        val hexHashBytes = decode(processedDataResponse.body.combinedHashBase64!!, Base64.URL_SAFE)

        Logger.d(TAG, "signJsonLdStructure: ${processedDataResponse.body.combinedHashBase64}")
        val jws = createJwsSign(hexHashBytes, myKey)

        Logger.d(TAG, "signJsonLdStructure: $jws")
        val tempCertificateOffer = updateCertificateOffer(mCertificateOffer, jws, proof)

        mListener.onSignRequestSuccess(tempCertificateOffer)
    }

    private fun updateCertificateOffer(
        dataAgreementContext: DataAgreementContext?,
        jws: String,
        proof: Proof
    ): DataAgreementContext? {
        proof.proofValue = jws
        val oldProof = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proof
        val proofChain = ArrayList<Proof>()
        if (oldProof != null)
            proofChain.add(oldProof)
        proofChain.add(proof)

        var databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body);
        databody.proofChain = proofChain
        databody.proof = null
        dataAgreementContext?.message?.body = databody

        Logger.d(
            TAG,
            "updateCertificateOffer: ${WalletManager.getGson.toJson(dataAgreementContext)}"
        )
        return dataAgreementContext

    }

    fun buildCertificateRequest(
        dataAgreementContext: DataAgreementContext?,
        myKey: String,
        theirKey: String
    ): OfferRequestAgreementContext {

        val event = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).event?.get(
            (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).event?.size ?: 1) - 1
        )

        val proof = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proofChain?.get(
            (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proofChain?.size ?: 1) - 1
        )

        val dataAgreementBody = OfferRequestAgreementBody(
            id = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).id ?: "",
            event = event,
            proof = proof
        )
        val dataAgreementMessage = OfferRequestAgreementMessage(
            type = "${DidCommPrefixUtils.getType(PREFIX_1)}/data-agreement-negotiation/1.0/accept",
            id = UUID.randomUUID().toString(),
            body = dataAgreementBody,
            from = WalletUtils.convertDidSovToMyDidWithMyData(myKey),
            createdTime = Date().time.toString(),
            to = WalletUtils.convertDidSovToMyDidWithMyData(theirKey)
        )
        val tempDataAgreementContext = OfferRequestAgreementContext(
            messageType = "protocol",
            message = dataAgreementMessage
        )

        Logger.d(
            TAG,
            "buildCertificateRequest: ${WalletManager.getGson.toJson(tempDataAgreementContext)}"
        )
        return tempDataAgreementContext
    }

    @Throws(IllegalArgumentException::class)
    fun hexStringToByteArray(s: String): ByteArray? {
        val len = s.length
        require(len % 2 != 1) { "Hex string must have even number of characters" }
        val data = ByteArray(len / 2) // Allocate 1 byte per 2 hex characters
        var i = 0
        while (i < len) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }

    private fun createJwsSign(hexHashBytes: ByteArray, myKey: String): String {
        //FIXME : Currently using hardcoded value for header. Needed to be changed

//        // 1 - jws header
//        val jwsHeader = "{\"alg\":\"EdDSA\",\"b64\":false,\"crit\":[\"b64\"]}"
//        Logger.d(TAG, "jwsHeader: $jwsHeader")
//
//        // 2 - base 64 encode the jws header
//        val encodedJwsHeader = encode(jwsHeader.toByteArray(), Base64.URL_SAFE or Base64.NO_PADDING)
//        Logger.d(TAG, "encodedJwsHeader: ${String(encodedJwsHeader)}")


        // 3 . construct JWS payload = utf8_encode(encoded_header + “.”) + bytes_hex_hash
        var hexhash = hexStringToByteArray(String(hexHashBytes))
        val jwsPayLoad =
            ("eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0" + ".").encodeToByteArray() + hexhash!!
        Logger.d(TAG, "jwsPayLoad: $jwsPayLoad")

        // 4. Indy sign
        val signature = Crypto.cryptoSign(
            WalletManager.getWallet,
            myKey,
            jwsPayLoad
        ).get()

        // 5 - encode signature
        val encodedSignature = WalletUtils.convertBase64ToAsciiString(signature).replace("\n", "")
        Logger.d(TAG, "encodedSignature: $encodedSignature")

        // 6 - Construct JWS = encoded_header + " . . " + encoded_signature
        val jws =
            "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0" + ".." + encodedSignature
        Logger.d(TAG, "jws: ${jws.replace("\n", "")}")
        return jws.replace("\n", "")
    }

    fun verifyDataAgreement(
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String,
        param: VerifyRequestListeners
    ) {
//        verifyJsonLd(dataAgreementContext, mConnectionId)
        if (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proofChain != null) {
            // 1. proof_chain = pop “proofChain” node from data agreement
            val proofChain = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).proofChain
            var databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body)
                databody.proofChain = null
            dataAgreementContext?.message?.body = databody

            // 2. genesis_proof = proof_chain[0]
            val genesisProof = proofChain?.get(0)

            // 3. construct genesis_data_agreement
            var genesisDataAgreementContext = dataAgreementContext?.clone()
            databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(genesisDataAgreementContext?.message?.body)
                databody.proof = genesisProof
            genesisDataAgreementContext?.message?.body = databody

            // 4. genesis_data_agreement[“event”] = data_agreement[“event”][0]
            val genesisEventList: ArrayList<Event> = ArrayList()
            genesisEventList.add(DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).event?.get(0) ?: Event())
            databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(genesisDataAgreementContext?.message?.body)
                databody.event = genesisEventList
            genesisDataAgreementContext?.message?.body = databody
            // 5. reconstruct original_data_agreement (with “proofChain”)
            databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body)
                databody.proof = proofChain?.get(1)
            dataAgreementContext?.message?.body = databody
            var isVerified = true
            verifyJsonLd(
                genesisDataAgreementContext,
                mConnectionId,
                object : VerifyRequestListeners {
                    override fun onVerified(isVerify: Boolean) {
                        super.onVerified(isVerify)
                        isVerified = isVerified && isVerify
                        verifyJsonLd(
                            dataAgreementContext,
                            mConnectionId,
                            object : VerifyRequestListeners {
                                override fun onVerified(isVerify: Boolean) {
                                    super.onVerified(isVerify)
//                                    isVerified = isVerified && isVerify
                                    param.onVerified(isVerified)
                                }
                            })
                    }
                })
        }else{
            var isVerified = true
            verifyJsonLd(
                dataAgreementContext,
                mConnectionId,
                object : VerifyRequestListeners {
                    override fun onVerified(isVerify: Boolean) {
                        super.onVerified(isVerify)
                        isVerified = isVerified && isVerify
                        verifyJsonLd(
                            dataAgreementContext,
                            mConnectionId,
                            object : VerifyRequestListeners {
                                override fun onVerified(isVerify: Boolean) {
                                    super.onVerified(isVerify)
//                                    isVerified = isVerified && isVerify
                                    param.onVerified(isVerified)
                                }
                            })
                    }
                })
        }
    }

    private fun verifyJsonLd(
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String,
        param: VerifyRequestListeners
    ) {
        val tempDataAgreementContext: DataAgreementContext? = dataAgreementContext?.clone()

        // 1. drop_proof_chain = true
        val dropProofChain = true

        // 2. proof_chain = false
        var proofChain = false

        // 3. old_proof = null
        var oldProof: Proof? = null

        // 4. new_proof = null
        var newProof: Proof? = null

        // 5. if ‘proofChain’ in data_agreement
        if (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body).proofChain != null) {
            // 5.1. proof_chain = true
            proofChain = true
            // 5.2. if drop_proof_chain
            if (dropProofChain) {
                // 5.2.1. old_proof = data_agreement[“proofChain”][0]
                oldProof = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body).proofChain?.get(0)
                // 5.2.2. new_proof = data_agreement[“proofChain”][1]
                newProof = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body).proofChain?.get(1)
                // 5.2.3. pop ‘proofChain’ node from data_agreement
                val databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body)
                databody.proofChain = null
                // 5.2.4. add ‘proof’ node into data agreement with value of old_proof
                databody.proof = newProof
                tempDataAgreementContext?.message?.body = databody
            } else {
                // 5.3.1 new_proof = data_agreement[“proofChain”][-1] (access last index)
                newProof =
                    DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body).proofChain?.get(
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body).proofChain?.size ?: 0) - 1
                    )
                // 5.3.2. remove the last item from data_agreement ‘proofChain’
                val list = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body).proofChain
                list?.removeAt((DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body).proofChain?.size ?: 0) - 1)
                var databody = DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(tempDataAgreementContext?.message?.body)
                    databody.proofChain = list
                tempDataAgreementContext?.message?.body = databody
            }
        }

        signRequest(tempDataAgreementContext, mConnectionId, object : SignOfferRequestListeners {
            override fun onProcessJsonLD(response: JsonLdProcessedDataResponse) {
                super.onProcessJsonLD(response)

                val hexHashBytes = decode(response.body.combinedHashBase64!!, URL_SAFE)

                var verified = false
//                if (oldProof != null) {
                val constructNewProof =
                    String(decode(response.body.framedBase64, URL_SAFE))

                val proof = JSONObject(constructNewProof).getJSONObject("proof")

                verified =
                    verifyJws(
                        proof.getString("proofValue"),
                        hexHashBytes,
                        WalletUtils.convertMyDidToDidSov(proof.getString("verificationMethod"))
                    )

                param.onVerified(verified)
                Log.d(TAG, "onProcessJsonLD: $verified")
            }
        }, true)
    }

    fun verifyJws(jws: String, bytesHexHash: ByteArray, verKey: String): Boolean {
        // 1. split JWS using " . . " delimiter to obtain encoded header and signature.
        val splitList = jws.split("..")

        Log.d(TAG, "jws: $jws")
        if (splitList.size == 2) {
            val header = splitList[0]
            val signature = splitList[1]

            //fixme: change from hardcoded value
            //base64 decode encoded header.
            //make sure decoded header matches below content:

            return if (header == "eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0") {
                val decodedSignature = decodeSignature(signature)

                val hexHash = hexStringToByteArray(String(bytesHexHash))
                val jwsPayLoad =
                    ("eyJhbGciOiAiRWREU0EiLCAiYjY0IjogZmFsc2UsICJjcml0IjogWyJiNjQiXX0" + ".").encodeToByteArray() + hexHash!!

                Crypto.cryptoVerify(verKey, jwsPayLoad, decodedSignature).get()
            } else {
                false
            }

        } else {
            return false
        }
    }

    private fun decodeSignature(payload: String): ByteArray? {

        Log.d(TAG, "decodeSignature: $payload")
        var load = payload
        val padLen = 4 - (load.length % 4)
        if (padLen > 2) {
            Log.d(TAG, "updated payload: $load")
        } else {
            repeat(padLen) {
                load += "="
            }
            Log.d(TAG, "updated payload: $load")
        }

        Log.d(TAG, "decodeSignature: $load")

        val decodedSig = decode(load, URL_SAFE or Base64.NO_WRAP)
        var d = String(decodedSig)
//        if(d.length%2 != 0)
//           d = "0$d"
//        val updatedDecodedSig = hexStringToByteArray(d)
        Log.d(TAG, "payload: $d")
        return decodedSig
    }
}

// Listeners to communicate with the parent class
interface SignOfferRequestListeners {
    fun onSignRequestSuccess(certificateOffer: DataAgreementContext?) {}

    fun onSignOfferRequestFailure() {}

    fun onProcessJsonLD(response: JsonLdProcessedDataResponse) {}
}

interface VerifyRequestListeners {
    fun onVerified(isVerified: Boolean) {}
}