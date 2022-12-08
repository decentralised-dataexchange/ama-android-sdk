package io.igrant.data_wallet.utils

import android.util.Log
import com.google.gson.Gson
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*

object DeletePollMessage {
    fun delete(
        inboxItemId: String,
        myDid: String,
        type: String
    ) {
        if (WalletManager.getWallet != null) {
            val data = "\n" +
                    "{\n" +
                    "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "  \"@type\": \"${DidCommPrefixUtils.getType(type)}/basic-routing/1.0/delete-inbox-items\",\n" +
                    "  \"inboxitemids\": [\n" +
                    "    \"$inboxItemId\"\n" +
                    "  ],\n" +
                    "  \"~transport\": {\n" +
                    "    \"return_route\": \"all\"\n" +
                    "  }\n" +
                    "}\n"

            val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
            val metaObject = JSONObject(metaString)
            val key = metaObject.getString("verkey")

            val gson = Gson()
            val didSearch = WalletSearch.open(
                WalletManager.getWallet,
                WalletRecordType.MEDIATOR_DID_DOC,
                "{}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val value =
                WalletSearch.searchFetchNextRecords(WalletManager.getWallet, didSearch, 100).get()

            WalletManager.closeSearchHandle(didSearch)
            Log.d(InitializeActivity.TAG, "did doc: $value")
            val didDoc = JSONObject(
                JSONObject(value).getJSONArray("records").get(0).toString()
            ).getString("value")
            Log.d(InitializeActivity.TAG, "did doc 2: $didDoc")
            val test = gson.fromJson(didDoc, DidDoc::class.java)

            val packedMessage = PackingUtils.packMessage(
                "[\"${test.publicKey!![0].publicKeyBase58}\"]",
                key,
                data
            )

            val typedBytes: RequestBody = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(packedMessage)
                }
            }
            ApiManager.api.getService()?.pollMessages(typedBytes)
                ?.enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        Log.d(InitializeActivity.TAG, "onFailure: ")
                    }

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {

                    }
                })
        }
    }
}