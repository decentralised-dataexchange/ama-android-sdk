package io.igrant.data_wallet.utils

import android.util.Log
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.certificateOffer.CertificateOffer
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.connection.ConnectionHistoryDetail
import io.igrant.data_wallet.models.connection.ConnectionV2Response
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.history.History
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.tags.TagDataShareHistory
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
import kotlin.collections.ArrayList

object DataHistory {

    private val TAG = "DATA_HISTORY"

    fun callPurposeData(
        invitation: Invitation,
        qrId: String,
        listener: PurposeListener
    ) {
        // 1. Get my did and key
        val myDidResult =
            Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
        val myKey = myDidResult.verkey

        // 2. Build the request
        val queryData = "{\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/igrantio-operator/1.0/fetch-data-agreement\",\n" +
                "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                "    \"exchange_template_id\": \"$qrId\",\n" +
                "    \"exchange_mode\": \"verify\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}"

        Log.d(TAG, "saveDataShareHistory: $queryData")
        // 3. pack message
        val queryPacked = PackingUtils.packMessage(invitation, myKey, queryData, "")

        // 4. converting packed message to bytes
        val queryPackedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(queryPacked)
            }
        }

        // 5. call protocol api
        ApiManager.api.getService()
            ?.postData(invitation.serviceEndpoint ?: "", queryPackedBytes)
            ?.enqueue(object : Callback<ConfigPostResponse> {
                override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                    listener.onFailure()
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

                        Log.d(TAG, "onResponse: ${String(unpack)}")

                        listener.onSuccess(
                            WalletManager.getGson.fromJson(
                                JSONObject(String(unpack)).getString("message"),
                                ConnectionHistoryDetail::class.java
                            )
                        )
                    }
                }
            })
    }

    fun saveDataShareHistory(
        connectionHistoryDetail: ConnectionHistoryDetail?,
        name: String,
        exchangeDataList: ArrayList<ExchangeData>,
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String?
    ) {
        if (dataAgreementContext != null) {
            saveV2DataShareHistory(
                connectionHistoryDetail,
                name,
                exchangeDataList,
                dataAgreementContext,
                mConnectionId
            )
        } else {
            saveDataShareHistory(
                connectionHistoryDetail,
                name,
                exchangeDataList,
                connectionHistoryDetail?.orgDetails?.logoImageUrl
            )
        }

    }

    fun saveV2DataShareHistory(
        connectionHistoryDetail: ConnectionHistoryDetail?,
        name: String,
        exchangeDataList: ArrayList<ExchangeData>,
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String?
    ) {

        val connectionObject = SignOfferRequestV3.getConnectionObjectV3(mConnectionId ?: "")

        val didDoc = SignOfferRequestV3.getDidDocOfConnectionV3(connectionObject?.theirDid)

        ConnectionDetail.getV2ConnectionDetail(
            connectionObject?.myDid ?: "",
            connectionObject?.theirDid ?: "",
            didDoc,
            object : ConnectionDetailCompletionListener {
                override fun onSuccess(connection: ConnectionV2Response) {
                    super.onSuccess(connection)

                    updateTag(dataAgreementContext)
                    // 6. Saving history to Record
                    val id = UUID.randomUUID().toString()

                    val history = History()
                    history.attributes = exchangeDataList
                    history.name = name
                    history.logo = connectionHistoryDetail?.orgDetails?.logoImageUrl
                    history.date = DateUtils.getIndyFormattedDate()
                    history.connectionHistory = connectionHistoryDetail
                    history.connectionV2 = connection.body
                    history.dataAgreementContext = dataAgreementContext
                    history.connectionId = mConnectionId
                    history.version = 2
                    history.type = HistoryType.VERIFY
                    history.id = id

                    var contextId = ""
                    var industrySector = ""
                    var thirdPartySharing = ""

                    if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(
                            dataAgreementContext?.message?.body
                        )
                    ) {
                        contextId =
                            DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                                dataAgreementContext?.message?.body
                            ).id ?: ""
                        industrySector =
                            DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                                dataAgreementContext?.message?.body
                            ).dataPolicy?.industrySector
                                ?: ""
                        thirdPartySharing =
                            if (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                                    dataAgreementContext?.message?.body
                                ).dataPolicy?.thirdPartyDataSharing == true
                            ) "true" else "false"
                    } else {
                        contextId =
                            DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(
                                dataAgreementContext?.message?.body
                            ).id
                                ?: ""
                        industrySector =
                            DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(
                                dataAgreementContext?.message?.body
                            ).dataPolicy?.industrySector
                                ?: ""
                        thirdPartySharing = "false"
                    }
                    val tags = TagDataShareHistory(
                        type = HistoryType.VERIFY,
                        id = id,
                        orgId = connection.body?.organisationId ?: connection.body?.organisationDid
                        ?: "",
                        isActive = "true",
                        contextId = contextId,
                        industryScope = industrySector,
                        thirdParty = thirdPartySharing
                    )

                    WalletMethods.addWalletRecord(
                        WalletManager.getWallet,
                        WalletRecordType.DATA_HISTORY,
                        id,
                        WalletManager.getGson.toJson(history),
                        WalletManager.getGson.toJson(tags)
                    )
                }

                override fun onFailure() {
                    super.onFailure()

                }
            })
    }

    private fun updateTag(dataAgreementContext: DataAgreementContext?) {
        //updating active history to false
        if (dataAgreementContext?.message?.body != null) {
            val industrySector =
                if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(
                        dataAgreementContext.message?.body
                    )
                ) {
                    DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                        dataAgreementContext.message?.body
                    ).dataPolicy?.industrySector
                } else {
                    DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(
                        dataAgreementContext.message?.body
                    ).dataPolicy?.industrySector
                }
            val query = TagDataShareHistory(
                industryScope = industrySector
            )
            val searchResponse = SearchUtils.searchWallet(
                WalletRecordType.DATA_HISTORY,
                WalletManager.getGson.toJson(query)
            )

            if ((searchResponse.totalCount ?: 0) > 0) {
                for (record in searchResponse.records ?: ArrayList()) {
                    var tags = record.tags?.toMutableMap()
                    tags?.set("isActive", "false")
                    WalletMethods.updateWalletRecordTag(
                        WalletManager.getWallet,
                        WalletRecordType.DATA_HISTORY,
                        record.id,
                        JSONObject(tags as Map<String, String>?).toString()
                    )
                }
            }
        }

    }

    fun saveDataShareHistory(
        connectionHistoryDetail: ConnectionHistoryDetail?,
        name: String,
        exchangeDataList: ArrayList<ExchangeData>, logo: String?
    ) {
        // 6. Saving history to Record
        val id = UUID.randomUUID().toString()
        val history = History()
        history.attributes = exchangeDataList
        history.name = name
        history.logo = logo
        history.date = DateUtils.getIndyFormattedDate()
        history.connectionHistory = connectionHistoryDetail
        history.type = HistoryType.VERIFY
        history.id = id

        val tags = TagDataShareHistory(
            type = HistoryType.VERIFY,
            id = id,
            orgId = connectionHistoryDetail?.orgDetails?.orgId ?: "",
            contextId = "",
            isActive = "false",
            industryScope = "",
            thirdParty = "false"
        )

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            WalletRecordType.DATA_HISTORY,
            id,
            WalletManager.getGson.toJson(history),
            WalletManager.getGson.toJson(tags)
        )
    }

    fun callPurposeData(
        didDoc: DidDoc,
        credDefId: String,
        listener: PurposeListener
    ) {
        // 1. Get my did and key
        val myDidResult =
            Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
        val myKey = myDidResult.verkey

        // 2. Build the request
        val queryData = "{\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/igrantio-operator/1.0/fetch-data-agreement\",\n" +
                "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                "    \"exchange_template_id\": \"$credDefId\",\n" +
                "    \"exchange_mode\": \"issue\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}"

        Log.d(TAG, "saveDataShareHistory: $queryData")
        // 3. pack message
        val queryPacked = PackingUtils.packMessage(didDoc, myKey, queryData, "")

        // 4. converting packed message to bytes
        val queryPackedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(queryPacked)
            }
        }

        // 5. call protocol api
        ApiManager.api.getService()
            ?.postData(didDoc.service?.get(0)?.serviceEndpoint ?: "", queryPackedBytes)
            ?.enqueue(object : Callback<ConfigPostResponse> {
                override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                    listener.onFailure()
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

                        Log.d(TAG, "onResponse: ${String(unpack)}")

                        // 6. Saving history to Record
                        listener.onSuccess(
                            WalletManager.getGson.fromJson(
                                JSONObject(String(unpack)).getString("message"),
                                ConnectionHistoryDetail::class.java
                            )
                        )
                    } else {
                        listener.onFailure()
                    }
                }
            })
    }

    fun saveDataShareHistory(
        connectionObject: MediatorConnectionObject?,
        certificateOfferString: String?,
        didDoc: DidDoc?,
        credDefId: String?,
        walletModel: WalletModel?,
        isEbsiVerification: Boolean? = false
    ) {
        if (walletModel != null) {
            var certificateOffer: Notification? = null
            if (certificateOfferString != null && certificateOfferString.trim() != "") {
                val value = JSONObject(certificateOfferString).getString("value")
                certificateOffer = WalletManager.getGson.fromJson(value, Notification::class.java)
            }
            if (certificateOffer != null && certificateOffer.certificateOffer?.dataAgreementContext != null) {
                saveV2DataShareHistory(
                    connectionObject,
                    didDoc,
                    walletModel,
                    certificateOffer?.certificateOffer
                )
            } else if (didDoc == null) {
                saveDataHistory(connectionObject, walletModel, isEbsiVerification)
            } else {
                saveDataShareHistory(didDoc, credDefId, walletModel, connectionObject)
            }
        }
    }

    private fun saveDataHistory(
        connectionObject: MediatorConnectionObject?,
        walletModel: WalletModel,
        isVerification: Boolean? = false
    ) {
        // 6. Saving history to Record
        val id = UUID.randomUUID().toString()
        val history = History()
        history.wallet = walletModel
        history.name = walletModel.searchableText
        history.logo = connectionObject?.theirImageUrl
        history.date = DateUtils.getIndyFormattedDate()
        history.connectionId = connectionObject?.requestId
        history.version = 2
        history.type = if (isVerification == true) HistoryType.VERIFY else HistoryType.ISSUE
        history.id = id

        val tags = TagDataShareHistory(
            type = if (isVerification == true) HistoryType.VERIFY else HistoryType.ISSUE,
            id = id,
            orgId = ConnectionTypes.EBSI_CONNECTION_NATURAL_PERSON,
            isActive = "false",
            connectionDid = connectionObject?.theirDid,
            contextId = "",
            industryScope = "",
            thirdParty = "false"
        )

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            WalletRecordType.DATA_HISTORY,
            id,
            WalletManager.getGson.toJson(history),
            WalletManager.getGson.toJson(tags)
        )
    }

    private fun saveV2DataShareHistory(
        connectionObject: MediatorConnectionObject?,
        didDoc: DidDoc?,
        walletModel: WalletModel,
        certificateOffer: CertificateOffer?
    ) {
        var type = HistoryType.ISSUE

        if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(certificateOffer?.dataAgreementContext?.message?.body))
            type = HistoryType.THIRD_PARTY_SHARING
        ConnectionDetail.getV2ConnectionDetail(connectionObject?.myDid ?: "",
            connectionObject?.theirDid ?: "",
            didDoc,
            object : ConnectionDetailCompletionListener {
                override fun onSuccess(connection: ConnectionV2Response) {
                    super.onSuccess(connection)

                    updateTag(certificateOffer?.dataAgreementContext)
                    // 6. Saving history to Record
                    val id = UUID.randomUUID().toString()
                    val history = History()
                    history.wallet = walletModel
                    history.name = walletModel.searchableText
                    history.logo = connectionObject?.theirImageUrl
                    history.date = DateUtils.getIndyFormattedDate()
                    history.connectionV2 = connection.body
                    history.connectionId = connectionObject?.requestId
                    history.version = 2
                    history.dataAgreementContext = certificateOffer?.dataAgreementContext
                    history.type = type
                    history.id = id

                    var contextId = ""
                    var industrySector = ""
                    var thirdPartySharing = ""

                    if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(
                            certificateOffer?.dataAgreementContext?.message?.body
                        )
                    ) {
                        contextId =
                            DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                                certificateOffer?.dataAgreementContext?.message?.body
                            ).id
                                ?: ""
                        industrySector =
                            DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                                certificateOffer?.dataAgreementContext?.message?.body
                            ).dataPolicy?.industrySector
                                ?: ""
                        thirdPartySharing =
                            if (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(
                                    certificateOffer?.dataAgreementContext?.message?.body
                                ).dataPolicy?.thirdPartyDataSharing == true
                            ) "true" else "false"
                    } else {
                        contextId =
                            DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(
                                certificateOffer?.dataAgreementContext?.message?.body
                            ).id
                                ?: ""
                        industrySector =
                            DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(
                                certificateOffer?.dataAgreementContext?.message?.body
                            ).dataPolicy?.industrySector
                                ?: ""
                        thirdPartySharing = "false"
                    }

                    val tags = TagDataShareHistory(
                        type = type,
                        id = id,
                        orgId = connection.body?.organisationId ?: connection.body?.organisationDid
                        ?: "",
                        isActive = "true",
                        contextId = contextId,
                        industryScope = industrySector,
                        thirdParty = thirdPartySharing
                    )

                    WalletMethods.addWalletRecord(
                        WalletManager.getWallet,
                        WalletRecordType.DATA_HISTORY,
                        id,
                        WalletManager.getGson.toJson(history),
                        WalletManager.getGson.toJson(tags)
                    )
                }

                override fun onFailure() {
                    super.onFailure()

                }
            })
    }

    private fun saveDataShareHistory(
        didDoc: DidDoc,
        credDefId: String?,
        walletModel: WalletModel,
        connectionObject: MediatorConnectionObject?
    ) {

        // 1. Get my did and key
        val myDidResult =
            Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
        val myKey = myDidResult.verkey

        // 2. Build the request
        val queryData = "{\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/igrantio-operator/1.0/fetch-data-agreement\",\n" +
                "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                "    \"exchange_template_id\": \"$credDefId\",\n" +
                "    \"exchange_mode\": \"issue\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}"

        Log.d(TAG, "saveDataShareHistory: $queryData")
        // 3. pack message
        val queryPacked = PackingUtils.packMessage(didDoc, myKey, queryData, "")

        // 4. converting packed message to bytes
        val queryPackedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(queryPacked)
            }
        }

        // 5. call protocol api
        ApiManager.api.getService()
            ?.postData(didDoc.service?.get(0)?.serviceEndpoint ?: "", queryPackedBytes)
            ?.enqueue(object : Callback<ConfigPostResponse> {
                override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                    Log.d(TAG, "onFailure: ")
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

                        Log.d(TAG, "onResponse: ${String(unpack)}")

                        // 6. Saving history to Record
                        val connectionHistory = WalletManager.getGson.fromJson(
                            JSONObject(String(unpack)).getString("message"),
                            ConnectionHistoryDetail::class.java
                        )
                        val id = UUID.randomUUID().toString()
                        val history = History()
                        history.wallet = walletModel
                        history.name = walletModel.searchableText
                        history.logo = if (connectionHistory?.orgDetails?.logoImageUrl != null)
                            connectionHistory.orgDetails?.logoImageUrl else connectionObject?.theirImageUrl
                        history.date = DateUtils.getIndyFormattedDate()
                        history.connectionHistory = connectionHistory
                        history.type = HistoryType.ISSUE
                        history.id = id

                        val tags = TagDataShareHistory(
                            type = HistoryType.ISSUE,
                            id = id,
                            orgId = connectionHistory.orgDetails?.orgId ?: connectionObject?.orgId
                            ?: "",
                            isActive = "false",
                            contextId = "",
                            industryScope = "",
                            thirdParty = "false"
                        )

                        WalletMethods.addWalletRecord(
                            WalletManager.getWallet,
                            WalletRecordType.DATA_HISTORY,
                            id,
                            WalletManager.getGson.toJson(history),
                            WalletManager.getGson.toJson(tags)
                        )

                    }
                }
            })
    }

    fun callPurposeData(
        connectionObject: MediatorConnectionObject?,
        qrId: String,
        listener: PurposeListener
    ) {
        // 1. Get my did and key
        val didDoc =
            SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\"did\":\"${connectionObject?.theirDid}\"}"
            )

        if ((didDoc.totalCount ?: 0) > 0) {
            val didDocObj = WalletManager.getGson.fromJson(
                didDoc.records?.get(0)?.value,
                DidDoc::class.java
            )

            val serviceEndPoint = didDocObj.service?.get(0)?.serviceEndpoint ?: ""

            val metaString =
                Did.getDidWithMeta(WalletManager.getWallet, connectionObject?.myDid).get()
            val metaObject = JSONObject(metaString)
            val key = metaObject.getString("verkey")


            // 2. Build the request
            val queryData = "{\n" +
                    "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/igrantio-operator/1.0/fetch-data-agreement\",\n" +
                    "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "    \"exchange_template_id\": \"$qrId\",\n" +
                    "    \"exchange_mode\": \"verify\",\n" +
                    "    \"~transport\": {\n" +
                    "        \"return_route\": \"all\"\n" +
                    "    }\n" +
                    "}"

            Log.d(TAG, "saveDataShareHistory: $queryData")
            // 3. pack message
            val queryPacked = PackingUtils.packMessage(didDocObj, key, queryData, "")

            // 4. converting packed message to bytes
            val queryPackedBytes = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(queryPacked)
                }
            }

            // 5. call protocol api
            ApiManager.api.getService()
                ?.postData(serviceEndPoint, queryPackedBytes)
                ?.enqueue(object : Callback<ConfigPostResponse> {
                    override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                        listener.onFailure()
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

                            Log.d(TAG, "onResponse: ${String(unpack)}")

                            listener.onSuccess(
                                WalletManager.getGson.fromJson(
                                    JSONObject(String(unpack)).getString("message"),
                                    ConnectionHistoryDetail::class.java
                                )
                            )
                        } else {
                            listener.onFailure()
                        }
                    }
                })
        }
    }
}

interface PurposeListener {
    fun onSuccess(connectionHistoryDetail: ConnectionHistoryDetail)
    fun onFailure()
}