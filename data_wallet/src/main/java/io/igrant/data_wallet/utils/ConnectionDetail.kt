package io.igrant.data_wallet.utils

import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.connection.ConnectionV2Response
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.connectionRequest.Transport
import io.igrant.data_wallet.models.requests.JsonLdProcessRequest
import io.igrant.data_wallet.models.requests.JsonLdProcessRequestV3
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*

object ConnectionDetail {
    fun getV2ConnectionDetail(
        myDid: String,
        theirDid: String,
        didDoc: DidDoc?,
        mListener: ConnectionDetailCompletionListener,
        isDexaEnabled:Boolean? = false
    ) {
        val requestId = UUID.randomUUID().toString()
        val orgData = if (isDexaEnabled == true) WalletManager.getGson.toJson(
            JsonLdProcessRequestV3(
                type = "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/data-controller/1.0/details",
                id = requestId,
                transport = Transport("all")
            )
        )else{
            WalletManager.getGson.toJson(
                JsonLdProcessRequest(
                    type = "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/data-controller/1.0/details",
                    id = requestId,
                    from = WalletUtils.convertDidSovToMyDidWithMyData(myDid),
                    to = WalletUtils.convertDidSovToMyDidWithMyData(theirDid),
                    createdTime = Date().time.toString(),
                    transport = Transport("all")
                )
            )
        }

        val myKey = Did.keyForLocalDid(WalletManager.getWallet,myDid).get()

        val orgDetailPacked = PackingUtils.packMessage(didDoc, myKey, orgData ?: "", "")

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
            ?.postData(didDoc?.service?.get(0)?.serviceEndpoint ?: "", orgDetailTypedArray)
            ?.enqueue(object :
                Callback<ConfigPostResponse> {
                override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                    mListener.onFailure()
                }

                override fun onResponse(
                    call: Call<ConfigPostResponse>,
                    response: Response<ConfigPostResponse>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        val unpack =
                            Crypto.unpackMessage(
                                WalletManager.getWallet,
                                WalletManager.getGson.toJson(response.body()).toString()
                                    .toByteArray()
                            ).get()

                        Logger.d(
                            ConnectionProgressDailogFragment.TAG,
                            "Connection response: ${JSONObject(String(unpack)).getString("message")} "
                        )

                        val connectionData = WalletManager.getGson.fromJson(
                            JSONObject(String(unpack)).getString("message"),
                            ConnectionV2Response::class.java
                        )
                        mListener.onSuccess(connectionData)
                    }
                }
            })
    }
}

interface ConnectionDetailCompletionListener {
    fun onSuccess(connection: ConnectionV2Response) {}

    fun onFailure() {}
}