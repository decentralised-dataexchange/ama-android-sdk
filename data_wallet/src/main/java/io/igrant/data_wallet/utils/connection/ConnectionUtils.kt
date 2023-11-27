package io.igrant.data_wallet.utils.connection

import android.net.Uri
import android.util.Base64
import android.util.Log
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.connection.Connection
import io.igrant.data_wallet.models.connection.ConnectionV2Response
import io.igrant.data_wallet.models.connection.DiscoverResponse
import io.igrant.data_wallet.models.connection.Protocol
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.connectionRequest.Transport
import io.igrant.data_wallet.models.requests.ConnectionExistBody
import io.igrant.data_wallet.models.requests.ConnectionExistRequest
import io.igrant.data_wallet.models.requests.JsonLdProcessRequest
import io.igrant.data_wallet.models.requests.JsonLdProcessRequestV3
import io.igrant.data_wallet.models.unpack.UnPackedData
import io.igrant.data_wallet.tasks.SaveConnectionTask
import io.igrant.data_wallet.tasks.SaveDidDocTask
import io.igrant.data_wallet.utils.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*

object ConnectionUtils {

    private var requestId: String? = ""

    fun saveConnection(uri: Uri, connectionCallback: ConnectionCallback) {
        val invitationString: String = uri.getQueryParameter("c_i") ?: ""
        var invitation: Invitation? = null
        var connectionType: String? = ""
        try {
            val json =
                Base64.decode(
                    invitationString,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))
            invitation = WalletManager.getGson.fromJson(json, Invitation::class.java)
        } catch (e: Exception) {
        }

        if (invitation != null) {
            val myDidResult =
                Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
            val myDid = myDidResult.did
            val myKey = myDidResult.verkey

            fetchProtocols(invitation, myKey, object : ConnectionUtilCallback {
                override fun returnProtocols(protocol: ArrayList<Protocol>?) {
                    connectionType = getConnectionType(protocol)
                    checkIfExistingConnection(connectionType, invitation, myDid, myKey,
                        object : ConnectionUtilCallback {
                            override fun returnProtocols(protocol: ArrayList<Protocol>?) {

                            }

                            override fun error(message: String?) {
                            }

                            override fun isExistingConnectionPresent(
                                isPresent: Boolean,
                                orgId: String?,
                                location: String?
                            ) {
                                if (isPresent) {
                                    if (orgId != "") {
                                        var connectionListSearch =
                                            SearchUtils.searchWallet(
                                                WalletRecordType.CONNECTION,
                                                "{\"orgId\":\"$orgId\"}"
                                            )

                                        if ((connectionListSearch.totalCount ?: 0) > 0) {

                                            val connectionObject = WalletManager.getGson.fromJson(
                                                connectionListSearch.records?.get(0)?.value,
                                                MediatorConnectionObject::class.java
                                            )
                                            sendDidToConnection(
                                                connectionObject.theirDid,
                                                connectionType,
                                                myKey,
                                                invitation,
                                                orgId,
                                                connectionCallback
                                            )
                                        }
                                    }
                                } else {
                                    saveConnection(
                                        invitation,
                                        connectionType,
                                        protocol,
                                        myDid,
                                        myKey,
                                        orgId,
                                        requestId,
                                        location,
                                        connectionCallback
                                    )
                                }
                            }

                        })
                }

                override fun error(message: String?) {
                }

                override fun isExistingConnectionPresent(
                    isPresent: Boolean,
                    orgId: String?,
                    location: String?
                ) {

                }
            })
        }

    }

    private fun fetchProtocols(
        invitation: Invitation,
        myKey: String,
        connectionUtilCallback: ConnectionUtilCallback
    ) {
        val queryFeatureData = "{\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/discover-features/1.0/query\",\n" +
                "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                "    \"query\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/*\",\n" +
                "    \"comment\": \"Querying features available.\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}"

        val queryFeaturePacked = PackingUtils.packMessage(invitation, myKey, queryFeatureData, "")

        val queryFeaturePackedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(queryFeaturePacked)
            }
        }

        ApiManager.api.getService()
            ?.postData(invitation.serviceEndpoint ?: "", queryFeaturePackedBytes)
            ?.enqueue(object : Callback<ConfigPostResponse> {
                override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                    connectionUtilCallback.error("")
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

                        val unPackedData =
                            WalletManager.getGson.fromJson(String(unpack), UnPackedData::class.java)
                        val protocols = WalletManager.getGson.fromJson(
                            unPackedData.message,
                            DiscoverResponse::class.java
                        ).protocols

                        connectionUtilCallback.returnProtocols(protocols)
                    }
                }
            })
    }

    private fun getConnectionType(protocols: ArrayList<Protocol>?): String {
        var connectionType = ""

        for (p in protocols ?: ArrayList()) {
            if (p.pid.contains(
                    "${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator",
                    ignoreCase = true
                )
            ) {
                connectionType = ConnectionTypes.IGRANT_ENABLED_CONNECTION
            }

            if (p.pid.contains(
                    "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/data-controller",
                    ignoreCase = true
                )
            ) {
                connectionType = ConnectionTypes.V2_CONNECTION
            }

            if (p.pid.contains(
                    "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/third-party-data-sharing",
                    ignoreCase = true
                )
            ) {
                connectionType = ConnectionTypes.DEXA_CONNECTION
            }
        }
        return connectionType
    }

    private fun checkIfExistingConnection(
        connectionType: String?, invitation: Invitation,
        myDid: String?,
        myKey: String?,
        connectionUtilCallback: ConnectionUtilCallback
    ) {
        requestId = ""
        when (connectionType) {
            ConnectionTypes.V2_CONNECTION, ConnectionTypes.DEXA_CONNECTION -> {
                requestId = UUID.randomUUID().toString()

                val orgData =
                    if (connectionType == ConnectionTypes.V2_CONNECTION) WalletManager.getGson.toJson(
                        JsonLdProcessRequest(
                            type = "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/data-controller/1.0/details",
                            id = requestId,
                            from = WalletUtils.convertDidSovToMyDidWithMyData(myDid ?: ""),
                            to = "did:mydata:zAqAMsVVgQcsH4keRwN5Po45nt",
                            createdTime = Date().time.toString(),
                            transport = Transport("all")
                        )
                    ) else WalletManager.getGson.toJson(
                        JsonLdProcessRequestV3(
                            type = "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/data-controller/1.0/details",
                            id = requestId,
                            transport = Transport("all")
                        )
                    )

                val orgDetailPacked =
                    PackingUtils.packMessage(invitation, myKey ?: "", orgData ?: "", "")

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
                    ?.postData(invitation.serviceEndpoint ?: "", orgDetailTypedArray)
                    ?.enqueue(object :
                        Callback<ConfigPostResponse> {
                        override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                            connectionUtilCallback.error("")
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
                                checkExistingConnection(
                                    connectionData.body?.organisationId
                                        ?: connectionData.body?.organisationDid ?: "",
                                    connectionData.body?.location,
                                    connectionUtilCallback
                                )

                            }
                        }
                    })
            }
            ConnectionTypes.IGRANT_ENABLED_CONNECTION -> {
                requestId = UUID.randomUUID().toString()

                val orgData =
                    "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/organization-info\", \"@id\": \"$requestId\" , \"~transport\": {" +
                            "\"return_route\": \"all\"}\n}"

                val orgDetailPacked = PackingUtils.packMessage(invitation, myKey ?: "", orgData, "")

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
                    ?.postData(invitation.serviceEndpoint ?: "", orgDetailTypedArray)
                    ?.enqueue(object :
                        Callback<ConfigPostResponse> {
                        override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                            connectionUtilCallback.error("")
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

                                val connectionData = WalletManager.getGson.fromJson(
                                    JSONObject(String(unpack)).getString("message"),
                                    Connection::class.java
                                )

                                checkExistingConnection(
                                    connectionData.orgId ?: "",
                                    connectionData.location?:"",
                                    connectionUtilCallback
                                )
                            }
                        }
                    })
            }
            else -> {
                connectionUtilCallback.isExistingConnectionPresent(false, null,null)
            }
        }
    }

    private fun checkExistingConnection(
        mOrgId: String,
        location: String?,
        connectionUtilCallback: ConnectionUtilCallback
    ) {

        if (mOrgId != "") {
            var connectionListSearch =
                SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION,
                    "{\"orgId\":\"$mOrgId\"}"
                )

            if ((connectionListSearch.totalCount ?: 0) > 0) {

                connectionUtilCallback.isExistingConnectionPresent(true, mOrgId,location)

            } else {
                connectionUtilCallback.isExistingConnectionPresent(false, mOrgId,location)
            }
        } else {
            connectionUtilCallback.isExistingConnectionPresent(false, mOrgId,location)
        }
    }

    private fun sendDidToConnection(
        theirDid: String?,
        connectionType: String?,
        myKey: String?,
        invitation: Invitation,
        orgId: String?,
        connectionCallback: ConnectionCallback
    ) {

        var data: String = ""
        if (connectionType == ConnectionTypes.IGRANT_ENABLED_CONNECTION) {
            data = "{\n" +
                    "  \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/org-multiple-connections\",\n" +
                    "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "  \"theirdid\": \"${theirDid ?: ""}\"\n" +
                    "}\n"

        } else {
            val didDoc =
                SearchUtils.searchWallet(
                    WalletRecordType.DID_DOC,
                    "{\"did\":\"$theirDid\"}"
                )
            if (didDoc.totalCount ?: 0 > 0) {
                val didDocObj = WalletManager.getGson.fromJson(
                    didDoc.records?.get(0)?.value,
                    DidDoc::class.java
                )
                data = WalletManager.getGson.toJson(
                    ConnectionExistRequest(
                        body = ConnectionExistBody(theirDid ?: ""),
                        type = "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/connections/1.0/exists",
                        from = WalletUtils.convertDidSovToMyDidWithMyData(myKey ?: ""),
                        to = WalletUtils.convertDidSovToMyDidWithMyData(
                            didDocObj.service?.get(0)?.recipientKeys?.get(0) ?: ""
                        ),
                        createdTime = Date().time.toString(),
                        id = UUID.randomUUID().toString()
                    )
                )
            }
        }

        val orgDetailPacked = PackingUtils.packMessage(invitation, myKey ?: "", data, "")

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
            ?.postDataWithoutData(invitation.serviceEndpoint ?: "", orgDetailTypedArray)
            ?.enqueue(object :
                Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    connectionCallback.failure("")
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {

                        var connectionListSearch =
                            SearchUtils.searchWallet(
                                WalletRecordType.CONNECTION,
                                "{\"orgId\":\"$orgId\"}"
                            )

                        if (connectionListSearch.totalCount ?: 0 > 0) {
                            val connectionObject = WalletManager.getGson.fromJson(
                                connectionListSearch.records?.get(0)?.value,
                                MediatorConnectionObject::class.java
                            )
                            connectionCallback.success(connectionObject.requestId ?: "")
                        }
                    }
                }
            })
    }

    private fun saveConnection(
        invitation: Invitation,
        connectionType: String?,
        protocols: ArrayList<Protocol>?,
        myDid: String?,
        myKey: String?,
        orgId: String?,
        requestId: String?,
        location: String?,
        connectionCallback: ConnectionCallback
    ) {
        SaveConnectionTask.saveConnection(
            object : CommonHandler {
                override fun taskStarted() {

                }

                override fun onSaveConnection(
                    typedBytes: RequestBody,
                    connectionRequest: RequestBody
                ) {

                    ApiManager.api.getService()?.cloudConnection(typedBytes)
                        ?.enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(
                                call: Call<ResponseBody>,
                                t: Throwable
                            ) {
                                connectionCallback.failure("")
                            }

                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                if (response.code() == 200 && response.body() != null) {
                                    ApiManager.api.getService()
                                        ?.postData(
                                            invitation.serviceEndpoint ?: "",
                                            connectionRequest
                                        )
                                        ?.enqueue(object :
                                            Callback<ConfigPostResponse> {
                                            override fun onFailure(
                                                call: Call<ConfigPostResponse>,
                                                t: Throwable
                                            ) {
                                                connectionCallback.failure("")
                                            }

                                            override fun onResponse(
                                                call: Call<ConfigPostResponse>,
                                                response: Response<ConfigPostResponse>
                                            ) {
                                                if (response.code() == 200 && response.body() != null) {
                                                    SaveDidDocTask.saveDidDoc(
                                                        object : CommonHandler {
                                                            override fun taskStarted() {
                                                            }

                                                            override fun onSaveDidComplete(
                                                                typedBytes: RequestBody?,
                                                                serviceEndPoint: String
                                                            ) {
                                                                if (typedBytes != null) {
                                                                    ApiManager.api.getService()
                                                                        ?.postDataWithoutData(
                                                                            serviceEndPoint,
                                                                            typedBytes
                                                                        )
                                                                        ?.enqueue(object :
                                                                            Callback<ResponseBody> {
                                                                            override fun onFailure(
                                                                                call: Call<ResponseBody>,
                                                                                t: Throwable
                                                                            ) {
                                                                                connectionCallback.failure("")
                                                                            }

                                                                            override fun onResponse(
                                                                                call: Call<ResponseBody>,
                                                                                response: Response<ResponseBody>
                                                                            ) {
                                                                                connectionCallback.success(
                                                                                    requestId ?: ""
                                                                                )
                                                                            }
                                                                        })
                                                                }
                                                            }
                                                        },
                                                        WalletManager.getGson.toJson(
                                                            response.body()
                                                        ), connectionType, false
                                                    )
                                                }
                                            }
                                        })
                                }
                            }
                        })
                }
            },
            invitation = invitation,
            protocols = protocols,
            myDid = myDid,
            key = myKey,
            orgId = orgId,
            requestId = requestId,
            location = location,
            connectionType == ConnectionTypes.DEXA_CONNECTION
        )
    }
}

interface ConnectionUtilCallback {
    fun returnProtocols(protocol: ArrayList<Protocol>?)
    fun error(message: String?)
    fun isExistingConnectionPresent(isPresent: Boolean, orgId: String?,location:String?)
}