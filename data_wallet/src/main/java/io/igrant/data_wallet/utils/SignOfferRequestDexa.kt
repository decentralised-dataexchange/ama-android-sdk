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
import io.igrant.data_wallet.models.requests.JsonLdProcessBodyV3
import io.igrant.data_wallet.models.requests.JsonLdProcessRequestV3
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

object SignOfferRequestV3 {

    const val TAG = "SignOfferRequest"

    private fun buildJsonLDProcessingRequestV3(
        dataAgreementContext: DataAgreementContext?,
        signatureOptionsBase64: String,
        connectionObject: MediatorConnectionObject?,
        publicKey: String,
        didDoc: DidDoc,
        isVerify: Boolean,
    ): RequestBody {

        // 3. Creating data_base64
        val tempBody = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof
        tempBody?.context = "https://w3id.org/security/v2"

        val base = encode(
            WalletManager.getGson.toJson(
                tempBody
            ).toByteArray(),
            Base64.URL_SAFE
        )

        val base64 = String(base).replace("\n", "")

        Logger.d(TAG, "Json ld inputs: $base64 <<<<>>>> $signatureOptionsBase64")

        // 4. Creating Request
        val request = JsonLdProcessRequestV3(
            "${DidCommPrefixUtils.getType(dataAgreementContext?.message?.type ?: "")}/json-ld/1.0/processed-data",
            UUID.randomUUID().toString(),
            JsonLdProcessBodyV3(
                base64,
                signatureOptionsBase64
            ),
            Transport("all")
        )

        Log.d(TAG, "buildJsonLDProcessingRequest: ${WalletManager.getGson.toJson(request)}")

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

    fun signRequestV3(
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String,
        mListener: SignOfferRequestListenersV3,
        isVerify: Boolean
    ) {
        // 1. fetching connection details(connection, didDoc)
        val connectionObject = getConnectionObjectV3(mConnectionId)

        //ver key
        val publicKey = getPublicKeyForConnectionV3(connectionObject?.myDid)

        // getting did doc of the connection
        val didDoc = getDidDocOfConnectionV3(connectionObject?.theirDid)

        var proof: ProofDexa =
            if (isVerify) {
                if (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proofChain != null)
                    DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proofChain?.get(
                        (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proofChain?.size ?: 0) - 1
                    ) ?: ProofDexa()
                else
                    DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof!!
            } else {
                //todo do this is clone
                val tempProof = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof!!
                tempProof.jws = tempProof.proofValue
                tempProof.proofValue = null

                tempProof

            }
        val signatureOptionsBase64 = buildProofRequestV3(
            SigningOptions(
                id = "did:sov:${connectionObject?.myDid}#2",
                verificationMethod = publicKey,
                proofPurpose = "authentication",
                created = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof?.created
            )
        )

        Log.d(TAG, "Proof for signing: ${WalletManager.getGson.toJson(proof)}")
        val typedArray = buildJsonLDProcessingRequestV3(
            dataAgreementContext,
            signatureOptionsBase64,
            connectionObject,
            publicKey,
            didDoc,
            isVerify
        )

        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof?.proofValue = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof?.jws
        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof?.jws = null

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
                        unPackMessageV3(
                            str,
                            mListener,
                            dataAgreementContext,
                            publicKey,
                            didDoc.publicKey?.get(0)?.publicKeyBase58 ?: "",
                            ProofDexa(
                                id = "did:sov:${connectionObject?.myDid}#2",
                                type = "Ed25519Signature2018",
                                context = "https://w3id.org/security/v2",
                                created = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof?.created,
                                verificationMethod = publicKey,
                                proofPurpose = "authentication", proofValue = null
                            ), isVerify
                        )
                    }
                }
            })
    }

    fun getDidDocOfConnectionV3(theirDid: String?): DidDoc {
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

    fun getPublicKeyForConnectionV3(myDid: String?): String {
        val metaString =
            Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        return metaObject.getString("verkey")
    }

    fun getConnectionObjectV3(mConnectionId: String): MediatorConnectionObject? {
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

    private fun buildProofRequestV3(signingOptions: SigningOptions): String {
        // 2. Creating signature_options_base64
        val signatureOptions = encode(
            WalletManager.getGson.toJson(
                signingOptions
            ).toByteArray(), Base64.URL_SAFE
        )
        val signatureOptionsBase64 = String(signatureOptions).replace("\n", "")
        Logger.d(TAG, "signOfferRequest: $signatureOptionsBase64")
        return signatureOptionsBase64
    }

    private fun unPackMessageV3(
        body: String,
        mListener: SignOfferRequestListenersV3,
        mDataAgreementContext: DataAgreementContext?,
        myKey: String,
        theirKey: String,
        proof: ProofDexa,
        isVerify: Boolean
    ) {
        // 8. Unpacking the response for further signing of json ld structure
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
        Log.d(TAG, "unPackMessage response: ${JSONObject(String(unpacked)).getString("message")}")
        val processedDataResponse = WalletManager.getGson.fromJson(
            JSONObject(String(unpacked)).getString("message"),
            JsonLdProcessedDataResponse::class.java
        )
        if (isVerify)
            mListener.onProcessJsonLD(processedDataResponse)
        else
            signJsonLdStructureV3(
                processedDataResponse,
                mListener,
                mDataAgreementContext,
                myKey,
                theirKey,
                proof
            )
    }

    private fun signJsonLdStructureV3(
        processedDataResponse: JsonLdProcessedDataResponse,
        mListener: SignOfferRequestListenersV3,
        mCertificateOffer: DataAgreementContext?,
        myKey: String,
        theirKey: String,
        proof: ProofDexa
    ) {
        // 9. Json ld structure signing
        val hexHashBytes = decode(processedDataResponse.body.combinedHashBase64!!, Base64.URL_SAFE)

        Logger.d(TAG, "signJsonLdStructure: ${processedDataResponse.body.combinedHashBase64}")
        val jws = createJwsSign(hexHashBytes, myKey)

        Logger.d(TAG, "signJsonLdStructure: $jws")

        Logger.d(TAG,"TestMilna ${WalletManager.getGson.toJson(mCertificateOffer)}")
        val tempCertificateOffer = updateCertificateOfferV3(mCertificateOffer?.clone(), jws, proof)
        Logger.d(TAG,"TestMilna ${WalletManager.getGson.toJson(tempCertificateOffer)}")
        mListener.onSignRequestSuccess(mCertificateOffer,tempCertificateOffer)
    }

    private fun updateCertificateOfferV3(
        dataAgreementContext: DataAgreementContext?,
        jws: String,
        proof: ProofDexa
    ): DataAgreementContext? {
        proof.proofValue = jws
        proof.context = null
        val oldProof = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof
        val proofChain = ArrayList<ProofDexa>()
        if (oldProof != null) {
            oldProof.context = null
            proofChain.add(oldProof)
        }
        proofChain.add(proof)

        var databody = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body);
        databody.proofChain = proofChain
        databody.proof = null
        dataAgreementContext?.message?.body = databody


//        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proofChain = proofChain
//        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof = null

        Logger.d(
            TAG,
            "updateCertificateOffer: ${WalletManager.getGson.toJson(dataAgreementContext)}"
        )
        return dataAgreementContext

    }

    fun buildCertificateRequestV3(
        dataAgreementContext: DataAgreementContext?,
        myKey: String,
        theirKey: String
    ): OfferRequestAgreementContextV3 {

//        val event = dataAgreementContext?.message?.body?.event?.get(
//            (dataAgreementContext?.message?.body?.event?.size ?: 1) - 1
//        )
//
//        val proof = dataAgreementContext?.message?.body?.proofChain?.get(
//            (dataAgreementContext?.message?.body?.proofChain?.size ?: 1) - 1
//        )

//        val dataAgreementBody = OfferRequestAgreementBody(
//            id = dataAgreementContext?.message?.body?.id ?: "",
//            event = event,
//            proof = proof
//        )
        val dataAgreementMessage = OfferRequestAgreementMessageV3(
            type = "${DidCommPrefixUtils.getType(PREFIX_1)}/data-agreement-negotiation/1.0/accept",
            id = UUID.randomUUID().toString(),
            body = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body),
        )
        val tempDataAgreementContext = OfferRequestAgreementContextV3(
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
    fun hexStringToByteArrayV3(s: String): ByteArray? {
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
        var hexhash = hexStringToByteArrayV3(String(hexHashBytes))
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

    fun verifyDataAgreementV3(
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String,
        param: VerifyRequestListenersV3
    ) {
//        verifyJsonLd(dataAgreementContext, mConnectionId)
        if (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proofChain != null) {
            // 1. proof_chain = pop “proofChain” node from data agreement
            val proofChain = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proofChain
           DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proofChain = null

            // 2. genesis_proof = proof_chain[0]
            val genesisProof = proofChain?.get(0)

            // 3. construct genesis_data_agreement
            var genesisDataAgreementContext = dataAgreementContext?.clone()
            DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(genesisDataAgreementContext?.message?.body).proof = genesisProof
            // 4. genesis_data_agreement[“event”] = data_agreement[“event”][0]
            val genesisEventList: ArrayList<Event> = ArrayList()
            genesisEventList.add(DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).event?.get(0) ?: Event())
            DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(genesisDataAgreementContext?.message?.body).event = genesisEventList

            // 5. reconstruct original_data_agreement (with “proofChain”)
            DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).proof = proofChain?.get(1)

            var isVerified = true
            verifyJsonLdV3(
                genesisDataAgreementContext,
                mConnectionId,
                object : VerifyRequestListenersV3 {
                    override fun onVerified(isVerify: Boolean) {
                        super.onVerified(isVerify)
                        isVerified = isVerified && isVerify
                        verifyJsonLdV3(
                            dataAgreementContext,
                            mConnectionId,
                            object : VerifyRequestListenersV3 {
                                override fun onVerified(isVerify: Boolean) {
                                    super.onVerified(isVerify)
//                                    isVerified = isVerified && isVerify
                                    param.onVerified(isVerified)
                                }
                            })
                    }
                })
        } else {
            var isVerified = true
            verifyJsonLdV3(
                dataAgreementContext,
                mConnectionId,
                object : VerifyRequestListenersV3 {
                    override fun onVerified(isVerify: Boolean) {
                        super.onVerified(isVerify)
                        isVerified = isVerified && isVerify
                        verifyJsonLdV3(
                            dataAgreementContext,
                            mConnectionId,
                            object : VerifyRequestListenersV3 {
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

    private fun verifyJsonLdV3(
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String,
        param: VerifyRequestListenersV3
    ) {
//        val tempDataAgreementContext: DataAgreementContext? = dataAgreementContext?.clone()
//
//        // 1. drop_proof_chain = true
//        val dropProofChain = true
//
//        // 2. proof_chain = false
//        var proofChain = false
//
//        // 3. old_proof = null
//        var oldProof: Proof? = null
//
//        // 4. new_proof = null
//        var newProof: Proof? = null
//
//        // 5. if ‘proofChain’ in data_agreement
//        if (tempDataAgreementContext?.message?.body?.proofChain != null) {
//            // 5.1. proof_chain = true
//            proofChain = true
//            // 5.2. if drop_proof_chain
//            if (dropProofChain) {
//                // 5.2.1. old_proof = data_agreement[“proofChain”][0]
//                oldProof = tempDataAgreementContext.message?.body?.proofChain?.get(0)
//                // 5.2.2. new_proof = data_agreement[“proofChain”][1]
//                newProof = tempDataAgreementContext.message?.body?.proofChain?.get(1)
//                // 5.2.3. pop ‘proofChain’ node from data_agreement
//                tempDataAgreementContext.message?.body?.proofChain = null
//                // 5.2.4. add ‘proof’ node into data agreement with value of old_proof
//                tempDataAgreementContext.message?.body?.proof = newProof
//            } else {
//                // 5.3.1 new_proof = data_agreement[“proofChain”][-1] (access last index)
//                newProof =
//                    tempDataAgreementContext.message?.body?.proofChain?.get(
//                        (tempDataAgreementContext.message?.body?.proofChain?.size ?: 0) - 1
//                    )
//                // 5.3.2. remove the last item from data_agreement ‘proofChain’
//                val list = tempDataAgreementContext.message?.body?.proofChain
//                list?.removeAt((tempDataAgreementContext.message?.body?.proofChain?.size ?: 0) - 1)
//                tempDataAgreementContext.message?.body?.proofChain = list
//            }
//        }

        signRequestV3(dataAgreementContext, mConnectionId, object : SignOfferRequestListenersV3 {
            override fun onProcessJsonLD(response: JsonLdProcessedDataResponse) {
                super.onProcessJsonLD(response)

                val hexHashBytes = decode(response.body.combinedHashBase64!!, URL_SAFE)

                var verified = false
//                if (oldProof != null) {
                val constructNewProof =
                    String(decode(response.body.framedBase64, URL_SAFE))

                val proof = JSONObject(constructNewProof).getJSONObject("proof")

                verified =
                    verifyJwsV3(
                        proof.getString("proofValue"),
                        hexHashBytes,
                        WalletUtils.convertMyDidToDidSov(proof.getString("verificationMethod"))
                    )

                param.onVerified(verified)
                Log.d(TAG, "onProcessJsonLD: $verified")
            }
        }, true)
    }

    fun verifyJwsV3(jws: String, bytesHexHash: ByteArray, verKey: String): Boolean {
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
                val decodedSignature = decodeSignatureV3(signature)

                val hexHash = hexStringToByteArrayV3(String(bytesHexHash))
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

    private fun decodeSignatureV3(payload: String): ByteArray? {

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
interface SignOfferRequestListenersV3 {
    fun onSignRequestSuccess(certificateOffer: DataAgreementContext?, updatedContext:DataAgreementContext?) {}

    fun onSignOfferRequestFailure() {}

    fun onProcessJsonLD(response: JsonLdProcessedDataResponse) {}
}

interface VerifyRequestListenersV3 {
    fun onVerified(isVerified: Boolean) {}
}