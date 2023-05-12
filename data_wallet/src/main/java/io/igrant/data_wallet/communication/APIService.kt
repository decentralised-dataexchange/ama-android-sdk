package io.igrant.data_wallet.communication

import io.igrant.data_wallet.models.LastUpdatedTimesResponse
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.agentConfig.ConfigResponse
import io.igrant.data_wallet.models.ledger.Ledger
import io.igrant.data_wallet.models.qr.QrDecode
import io.igrant.data_wallet.utils.dataAgreement.DataAgreementResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface APIService {

    @GET(".well-known/agent-configuration")
    fun getAgentConfig(): Call<ConfigResponse>

    @GET("https://cloudagent-demo.igrant.io/v1/mydata-did-registry/admin/.well-known/did-configuration.json")
    fun getRegistryConfig(): Call<ConfigResponse>

    @POST(" ")
    fun postDetails(@Body bytes: RequestBody): Call<ResponseBody>

    @POST(" ")
    fun pollMessages(@Body bytes: RequestBody): Call<ResponseBody>

    @POST(" ")
    fun cloudConnection(@Body bytes: RequestBody): Call<ResponseBody>

    @POST
    fun postData(@Url url: String, @Body bytes: RequestBody): Call<ConfigPostResponse>

    @POST
    fun postDataWithoutData(@Url url: String, @Body bytes: RequestBody): Call<ResponseBody>

    @POST(" ")
    fun postDetailsWithBody(@Body bytes: RequestBody): Call<ConfigPostResponse>

    @GET
    fun getGenesis(@Url url: String): Call<ResponseBody>

    @POST
    fun extractUrl(@Url url: String): Call<QrDecode>

    @GET
    fun getLastUpdatedTimes(@Url url: String): Call<LastUpdatedTimesResponse>

    @GET
    fun getLedger(@Url url: String): Call<ArrayList<Ledger>>

    @GET
    fun getDataAgreement(
        @Url url: String,
        @Header("Authorization") authorization: String
    ): Call<DataAgreementResponse>
}
