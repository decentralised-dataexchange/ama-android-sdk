package io.igrant.data_wallet.utils

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.handlers.SearchHandler
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.agentConfig.ConfigResponse
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.connectionRequest.*
import io.igrant.data_wallet.models.requests.AchorRegistryRequest
import io.igrant.data_wallet.models.tagJsons.ConnectionId
import io.igrant.data_wallet.models.tagJsons.ConnectionTags
import io.igrant.data_wallet.models.tagJsons.UpdateInvitationKey
import io.igrant.data_wallet.models.walletSearch.SearchResponse
import io.igrant.data_wallet.tasks.SaveDidDocTask
import io.igrant.data_wallet.tasks.WalletSearchTask
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.apache.commons.io.IOUtils
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

interface NotificationListener {
    fun receivedNotification(notificationType: String, intent: Intent)
}

object DataWalletConfigurations {
    fun registerForSubscription(context: Context) {
        registerForSubscription(context, null)
    }

    fun registerForSubscription(context: Context, notificationListener: NotificationListener?) {
        try {
            WalletSearchTask.searchWallet(
                object : SearchHandler {
                    override fun taskCompleted(searchResponse: SearchResponse) {
                        if (searchResponse.totalCount == 0) {
                            ApiManager.api.getService()?.getAgentConfig()
                                ?.enqueue(object : Callback<ConfigResponse> {
                                    override fun onFailure(
                                        call: Call<ConfigResponse>,
                                        t: Throwable
                                    ) {
                                        //tvLoadingStatus.visibility = View.GONE
                                    }

                                    override fun onResponse(
                                        call: Call<ConfigResponse>,
                                        response: Response<ConfigResponse>
                                    ) {
                                        if (response.code() == 200 && response.body() != null) {
                                            saveConnectionRecord(
                                                response.body()!!.invitation,
                                                true,
                                                context, notificationListener
                                            )
                                        }
                                    }
                                })
                        } else {
                            val connectionData =
                                JSONObject(searchResponse.records?.get(0)?.value ?: "")

                            when (connectionData.getString("state")) {
                                ConnectionStates.CONNECTION_REQUEST, ConnectionStates.CONNECTION_INVITATION -> {
                                    val myDid: String = connectionData.getString("my_did")
                                    val requestId: String = connectionData.getString("request_id")

                                    packConnectionRequestMessage(
                                        myDid,
                                        requestId,
                                        context,
                                        notificationListener
                                    )
                                }
                                ConnectionStates.CONNECTION_RESPONSE -> {
                                    //GET DID DOC FROM RECORD FOR PUBLIC KEY
                                    //CALL createInbox
                                }
                                ConnectionStates.CONNECTION_ACTIVE -> {
//                                    tvLoadingStatus.visibility = View.GONE
                                    val myDid: String = connectionData.getString("my_did")
                                    pollMessagesInThread(myDid, context, notificationListener)

                                    getRegistryConfig(context, notificationListener)
                                }
                            }
                        }
                    }

                    override fun taskStarted() {

                    }
                },
                WalletRecordType.MEDIATOR_CONNECTION,
                "{}"
            )
        } catch (e: Exception) {
        }
    }

    private fun saveConnectionRecord(
        invitation: Invitation?,
        isMediator: Boolean,
        context: Context,
        notificationListener: NotificationListener?
    ) {


        if (WalletManager.getWallet != null) {
            val value =
                WalletManager.getGson.toJson(setUpMediatorConnectionObject(invitation, null, null))
            val connectionUuid = UUID.randomUUID().toString()

            val connectionTag = ConnectionTags()
            connectionTag.invitationKey = invitation?.recipientKeys!![0]
            connectionTag.state = ConnectionStates.CONNECTION_INVITATION

            val tagJson =
                WalletManager.getGson.toJson(connectionTag)

            WalletMethods.addWalletRecord(
                WalletManager.getWallet,
                if (isMediator) WalletRecordType.MEDIATOR_CONNECTION else WalletRecordType.REGISTRY_CONNECTION,
                connectionUuid,
                value.toString(),
                tagJson.toString()
            )


            saveConnectionInvitationRecord(
                connectionUuid,
                invitation,
                isMediator,
                context,
                notificationListener
            )
        }
    }

    private fun saveConnectionInvitationRecord(
        connectionUuid: String,
        invitation: Invitation?,
        isMediator: Boolean, context: Context, notificationListener: NotificationListener?
    ) {
        if (WalletManager.getWallet != null) {
            val tagJson = WalletManager.getGson.toJson(ConnectionId(connectionUuid))
            val connectionInvitationUuid = UUID.randomUUID().toString()

            Log.d(InitializeActivity.TAG, "saveRecord2: wallet value : $tagJson")
            Log.d(InitializeActivity.TAG, "saveRecord2: wallet UUID : $connectionInvitationUuid")

            WalletMethods.addWalletRecord(
                WalletManager.getWallet,
                if (isMediator) WalletRecordType.MEDIATOR_CONNECTION_INVITATION else WalletRecordType.REGISTRY_CONNECTION_INVITATION,
                connectionInvitationUuid,
                WalletManager.getGson.toJson(invitation),
                tagJson
            )

            updateConnectionRecord(
                connectionUuid,
                invitation,
                isMediator,
                context,
                notificationListener
            )
        }
    }

    private fun updateConnectionRecord(
        connectionUuid: String,
        invitation: Invitation?,
        isMediator: Boolean, context: Context, notificationListener: NotificationListener?
    ) {
        if (WalletManager.getWallet != null) {
            val myDidResult =
                Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
            val myDid = myDidResult.did
//        val key = Did.keyForLocalDid(WalletManager.getWallet, myDid).get()

            Log.d(InitializeActivity.TAG, "updateConnectionRecord: $myDid")
            val requestId = UUID.randomUUID().toString()
            val value = WalletManager.getGson.toJson(
                setUpMediatorConnectionObject(
                    invitation,
                    requestId,
                    myDid
                )
            )

            WalletMethods.updateWalletRecord(
                WalletManager.getWallet,
                if (isMediator) WalletRecordType.MEDIATOR_CONNECTION else WalletRecordType.REGISTRY_CONNECTION,
                connectionUuid,
                value
            )

            updateRecord1Tag(
                requestId,
                myDid,
                invitation?.recipientKeys?.get(0),
                connectionUuid,
                isMediator,
                invitation?.serviceEndpoint, context,
                notificationListener
            )
        }
    }

    private fun updateRecord1Tag(
        requestId: String?,
        myDid: String?,
        recipient: String?,
        connectionUuid: String,
        isMediator: Boolean,
        serviceEndpoint: String?,
        context: Context,
        notificationListener: NotificationListener?
    ) {
        if (WalletManager.getWallet != null) {
            val tagJson =
                WalletManager.getGson.toJson(
                    UpdateInvitationKey(
                        requestId,
                        myDid,
                        recipient,
                        null,
                        null
                    )
                )
            WalletRecord.updateTags(
                WalletManager.getWallet,
                if (isMediator) WalletRecordType.MEDIATOR_CONNECTION else WalletRecordType.REGISTRY_CONNECTION,
                connectionUuid,
                tagJson
            )
            if (isMediator)
                registerForSubscription(context, notificationListener)
            else
                getRegistryConfig(context, notificationListener)
        }
    }

    private fun setUpMediatorConnectionObject(
        invitation: Invitation?,
        requestId: String?,
        did: String?
    ): MediatorConnectionObject {
        val connectionObject = MediatorConnectionObject()
        connectionObject.theirLabel = invitation?.label ?: ""
        connectionObject.theirImageUrl = invitation?.image_url ?: invitation?.imageUrl ?: ""
        connectionObject.theirDid = ""
        connectionObject.inboxId = ""
        connectionObject.inboxKey = ""
        connectionObject.requestId = requestId
        connectionObject.myDid = did

        if (invitation != null && !(invitation.recipientKeys.isNullOrEmpty()))
            connectionObject.invitationKey = invitation.recipientKeys!![0]
        else
            connectionObject.invitationKey = ""

        connectionObject.createdAt = DateUtils.getIndyFormattedDate()
        connectionObject.updatedAt = DateUtils.getIndyFormattedDate()

        connectionObject.theirLabel = invitation?.label
        connectionObject.state =
            if (did != null) ConnectionStates.CONNECTION_REQUEST else ConnectionStates.CONNECTION_INVITATION

        return connectionObject
    }

    private fun packConnectionRequestMessage(
        myDid: String,
        requestId: String,
        context: Context, notificationListener: NotificationListener?
    ) {
        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val search = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.MEDIATOR_CONNECTION_INVITATION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val value = WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        val connectionInvitationData = JSONObject(value)

        Log.d(InitializeActivity.TAG, "packConnectionRequestMessage0: $value")

        //json object
        val connectionInvitationRecords =
            JSONArray(connectionInvitationData.get("records").toString())
        val connectionInvitationRecord = connectionInvitationRecords.getJSONObject(0)
        val connectionInvitationValue = JSONObject(connectionInvitationRecord.getString("value"))

        //public keys
        val publicKey = PublicKey()
        publicKey.id = "did:sov:$myDid#1"
        publicKey.type = "Ed25519VerificationKey2018"
        publicKey.controller = "did:sov:$myDid"
        publicKey.publicKeyBase58 = key

        val publicKeys: ArrayList<PublicKey> = ArrayList()
        publicKeys.add(publicKey)

        //authentication
        val authentication = Authentication()
        authentication.type = "Ed25519SignatureAuthentication2018"
        authentication.publicKey = "did:sov:$myDid#1"

        val authentications: ArrayList<Authentication> = ArrayList()
        authentications.add(authentication)

        //service
        val recipientsKey: ArrayList<String> = ArrayList()
        recipientsKey.add(key)

        val service = Service()
        service.id = "did:sov:$myDid;indy"
        service.type = "IndyAgent"
        service.priority = 0
        service.recipientKeys = recipientsKey
        service.serviceEndpoint = ""

        val services: ArrayList<Service> = ArrayList()
        services.add(service)

        //did doc
        val didDoc = DidDoc()
        didDoc.context = "https://w3id.org/did/v1"
        didDoc.id = "did:sov:$myDid"
        didDoc.publicKey = publicKeys
        didDoc.authentication = authentications
        didDoc.service = services

        //did
        val did = DID()
        did.did = myDid
        did.didDoc = didDoc

        // transport
        val transport = Transport("all")

        //connection request
        val connectionRequest = ConnectionRequest()
        connectionRequest.type =
            "${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/connections/1.0/request"
        connectionRequest.id = requestId
        connectionRequest.label = "milan"
        connectionRequest.connection = did
        connectionRequest.transport = transport

        val str = WalletManager.getGson.toJson(connectionRequest)

        val packedMessage = PackingUtils.packMessage(
            connectionInvitationValue.getString("recipientKeys"),
            key,
            str
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
//
        ApiManager.api.getService()?.postDetailsWithBody(typedBytes)
            ?.enqueue(object : Callback<ConfigPostResponse> {
                override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
//                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ConfigPostResponse>,
                    response: Response<ConfigPostResponse>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        unPackSigMessage(
                            WalletManager.getGson.toJson(response.body()),
                            true,
                            context, notificationListener
                        )
                    }
                }
            })
    }

    private fun unPackSigMessage(
        body: String,
        isMediator: Boolean,
        context: Context,
        notificationListener: NotificationListener?
    ) {

        Log.d(InitializeActivity.TAG, "unPackMessage: $body")
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
        Log.d(InitializeActivity.TAG, "packConnectionRequestMessage: ${String(unpacked)}")

        val response = JSONObject(String(unpacked))

        val message = JSONObject(response.get("message").toString())

        val connectionSig = JSONObject(message.get("connection~sig").toString())
        val sigData = connectionSig.get("sig_data").toString()
        Log.d(
            InitializeActivity.TAG,
            "unPackMessage: decoded : ${
                Base64.decode(sigData, Base64.URL_SAFE)
                    .toString(charset("UTF-8"))
            }"
        )
        val decoded = Base64.decode(sigData, Base64.URL_SAFE)
            .toString(charset("UTF-8"))
        val postion = decoded.indexOf("{\"DID\"")
        Log.d(InitializeActivity.TAG, "unPackMessage: positon : $postion")
        val data = decoded.substring(postion)

        saveDidDoc(data, isMediator, context, notificationListener)
    }

    private fun saveDidDoc(
        data: String,
        isMediator: Boolean,
        context: Context,
        notificationListener: NotificationListener?
    ) {
        Log.d(InitializeActivity.TAG, "saveDidDoc: $data")
        val didData = JSONObject(data)
        val didDoc = didData.getString("DIDDoc")
        val theirDid = didData.getString("DID")

        val didDocUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$theirDid\"}"

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            if (isMediator) WalletRecordType.MEDIATOR_DID_DOC else WalletRecordType.DID_DOC,
            didDocUuid,
            didDoc.toString(),
            tagJson
        )

        val publicKey = JSONObject(didDoc).getJSONArray("publicKey").getJSONObject(0)
            .getString("publicKeyBase58")
        addDidKey(publicKey, theirDid, isMediator, context, notificationListener)
    }

    private fun addDidKey(
        publicKey: String,
        theirDid: String,
        isMediator: Boolean,
        context: Context, notificationListener: NotificationListener?
    ) {

        val didKeyUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$theirDid\", \"key\": \"$publicKey\"}"

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            if (isMediator) WalletRecordType.MEDIATOR_DID_KEY else WalletRecordType.DID_KEY,
            didKeyUuid,
            publicKey,
            tagJson
        )
        updateRecord(publicKey, theirDid, isMediator, context, notificationListener)
    }

    private fun updateRecord(
        publicKey: String,
        theirDid: String,
        isMediator: Boolean,
        context: Context, notificationListener: NotificationListener?
    ) {

        val search = WalletSearch.open(
            WalletManager.getWallet,
            if (isMediator) WalletRecordType.MEDIATOR_CONNECTION else WalletRecordType.CONNECTION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        val data = JSONObject(connection)
        Log.d(InitializeActivity.TAG, "getMediatorConfig: $connection")

        val connectionRecords = JSONArray(data.get("records").toString())

        val mediatorConnectionObject: MediatorConnectionObject =
            WalletManager.getGson.fromJson(
                connectionRecords.getJSONObject(0).getString("value"),
                MediatorConnectionObject::class.java
            )
        mediatorConnectionObject.theirDid = theirDid
        mediatorConnectionObject.state = ConnectionStates.CONNECTION_RESPONSE

        val connectionUuid =
            connectionRecords.getJSONObject(0).getString("id")

        val value = WalletManager.getGson.toJson(mediatorConnectionObject)

        WalletMethods.updateWalletRecord(
            WalletManager.getWallet,
            if (isMediator) WalletRecordType.MEDIATOR_CONNECTION else WalletRecordType.CONNECTION,
            connectionUuid,
            value
        )

        val requestId = mediatorConnectionObject.requestId
        val myDid = mediatorConnectionObject.myDid
        val invitationKey = mediatorConnectionObject.invitationKey
        updateTag(
            requestId,
            myDid,
            invitationKey,
            connectionUuid,
            theirDid,
            publicKey,
            isMediator,
            context, notificationListener
        )
    }

    private fun updateTag(
        requestId: String?,
        myDid: String?,
        recipient: String?,
        connectionUuid: String,
        theirDid: String,
        publicKey: String,
        isMediator: Boolean, context: Context,
        notificationListener: NotificationListener?
    ) {

        val tagJson = "{\n" +
                "  \"their_did\": \"$theirDid\",\n" +
                "  \"request_id\": \"$requestId\",\n" +
                "  \"my_did\": \"$myDid\",\n" +
                "  \"invitation_key\": \"$recipient\"\n" +
                "}"
        WalletRecord.updateTags(
            WalletManager.getWallet,
            if (isMediator) WalletRecordType.MEDIATOR_CONNECTION else WalletRecordType.CONNECTION,
            connectionUuid,
            tagJson
        )

        if (isMediator)
            createInbox(myDid, publicKey, context, notificationListener)
        else
            trustPing(theirDid, myDid)
    }

    private fun trustPing(
        theirDid: String?,
        myDid: String?
    ) {
        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val publicKey = metaObject.getString("verkey")

        val didDocSearch = SearchUtils.searchWallet(
            WalletRecordType.DID_DOC,
            "{\"did\":\"$theirDid\"}"
        )

        var serviceEndPoint = ""
        if ((didDocSearch.totalCount ?: 0) > 0) {
            val didDoc = WalletManager.getGson.fromJson(
                didDocSearch.records?.get(0)?.value,
                DidDoc::class.java
            )

            serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""

            val data = "{\n" +
                    "  \"@type\": \"${DidCommPrefixUtils.getType(didDoc.service?.get(0)?.type ?: "")}/trust_ping/1.0/ping\",\n" +
                    "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "  \"comment\": \"ping\",\n" +
                    "  \"response_requested\": true\n" +
                    "}\n"

            val packedMessage = PackingUtils.packMessage(
                didDoc, publicKey,
                data, didDoc.service?.get(0)?.type ?: ""
            )

            Log.d(InitializeActivity.TAG, "packed message: ${String(packedMessage)}")

            val typedBytes: RequestBody = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(packedMessage)
                }
            }

            ApiManager.api.getService()
                ?.postDataWithoutData(serviceEndPoint, typedBytes)
                ?.enqueue(object : Callback<ResponseBody> {
                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                        llProgressBar.visibility = View.GONE
                    }

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {

                    }
                })
        }
    }

    private fun createInbox(
        myDid: String?,
        publicKey: String,
        context: Context,
        notificationListener: NotificationListener?
    ) {

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val data = "\n" +
                "{\n" +
                "    \"@id\": \"${UUID.randomUUID().toString()}\",\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/basic-routing/1.0/create-inbox\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}\n"

        val packedMessage = Crypto.packMessage(
            WalletManager.getWallet,
            "[\"$publicKey\"]",
            key,
            data.toByteArray()
        ).get()

        Log.d(InitializeActivity.TAG, "packed message: ${String(packedMessage)}")

        val typedBytes: RequestBody = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }

        ApiManager.api.getService()?.postDetails(typedBytes)
            ?.enqueue(object : Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        Log.d(
                            InitializeActivity.TAG,
                            "unPackMessage: ${response.body()!!.byteStream()}"
                        )
                        val inputStream = response.body()!!.byteStream()
                        val str: String = IOUtils.toString(inputStream, "UTF-8")
                        Log.d(InitializeActivity.TAG, "onResponse: $str")

                        val unpacked =
                            Crypto.unpackMessage(WalletManager.getWallet, str.toByteArray())
                                .get()
                        Log.d(
                            InitializeActivity.TAG,
                            "packConnectionRequestMessage: ${String(unpacked)}"
                        )

                        val message = JSONObject(String(unpacked)).getString("message")

                        val inboxId = JSONObject(message).getString("InboxId")
                        val inboxKey = JSONObject(message).getString("InboxKey")
                        //inbox
                        updateRecordWithInboxDetails(
                            inboxId,
                            inboxKey,
                            context,
                            notificationListener
                        )
                    }
                }
            })
    }

    private fun updateRecordWithInboxDetails(
        inboxId: String,
        inboxKey: String,
        context: Context,
        notificationListener: NotificationListener?
    ) {
        try {
            WalletSearchTask.searchWallet(
                object : SearchHandler {
                    override fun taskCompleted(searchResponse: SearchResponse) {
                        val mediatorConnectionObject: MediatorConnectionObject =
                            WalletManager.getGson.fromJson(
                                searchResponse.records?.get(0)?.value,
                                MediatorConnectionObject::class.java
                            )
                        mediatorConnectionObject.inboxId = inboxId
                        mediatorConnectionObject.inboxKey = inboxKey
                        mediatorConnectionObject.state = ConnectionStates.CONNECTION_ACTIVE

                        val connectionUuid =
                            searchResponse.records?.get(0)?.id

                        val value = WalletManager.getGson.toJson(mediatorConnectionObject)

                        WalletMethods.updateWalletRecord(
                            WalletManager.getWallet,
                            WalletRecordType.MEDIATOR_CONNECTION,
                            connectionUuid,
                            value
                        )

                        registerForSubscription(context, notificationListener)
                    }
                }, WalletRecordType.MEDIATOR_CONNECTION,
                "{}"
            )
        } catch (e: Exception) {
        }
    }


    private fun getRegistryConfig(context: Context, notificationListener: NotificationListener?) {
        Log.d(InitializeActivity.TAG, "Registry Config - getRegistryConfig: starting")
        try {
            WalletSearchTask.searchWallet(
                object : SearchHandler {
                    override fun taskCompleted(searchResponse: SearchResponse) {
                        Log.d(
                            InitializeActivity.TAG,
                            "Registry Config - getRegistryConfig: search task completed"
                        )
                        if (searchResponse.totalCount == 0) {
                            ApiManager.api.getService()?.getRegistryConfig()
                                ?.enqueue(object : Callback<ConfigResponse> {
                                    override fun onFailure(
                                        call: Call<ConfigResponse>,
                                        t: Throwable
                                    ) {

                                    }

                                    override fun onResponse(
                                        call: Call<ConfigResponse>,
                                        response: Response<ConfigResponse>
                                    ) {
                                        if (response.code() == 200 && response.body() != null) {
                                            Log.d(
                                                InitializeActivity.TAG,
                                                "Registry Config - getRegistryConfig: success"
                                            )
                                            saveConnectionRecord(
                                                response.body()!!.invitation,
                                                false, context, notificationListener
                                            )
                                        }
                                    }
                                })
                        } else {
                            val connectionData =
                                JSONObject(searchResponse.records?.get(0)?.value ?: "")

                            when (connectionData.getString("state")) {
                                ConnectionStates.CONNECTION_REQUEST, ConnectionStates.CONNECTION_INVITATION -> {
                                    Log.d(
                                        InitializeActivity.TAG,
                                        "Registry Config - getRegistryConfig: state 1"
                                    )
                                    val myDid: String = connectionData.getString("my_did")
                                    val requestId: String = connectionData.getString("request_id")

                                    packRegistryRequestMessage(myDid, requestId)
                                }
                                ConnectionStates.CONNECTION_RESPONSE -> {
                                    Log.d(
                                        InitializeActivity.TAG,
                                        "Registry Config - getRegistryConfig: state 2"
                                    )
                                    //GET DID DOC FROM RECORD FOR PUBLIC KEY
                                    //CALL createInbox
                                }
                                ConnectionStates.CONNECTION_ACTIVE -> {
                                    Log.d(
                                        InitializeActivity.TAG,
                                        "Registry Config - getRegistryConfig: state 3"
                                    )
                                }
                            }
                        }
                    }

                    override fun taskStarted() {

                    }
                }, WalletRecordType.REGISTRY_CONNECTION,
                "{}"
            )
        } catch (e: Exception) {
        }
    }

    private fun packRegistryRequestMessage(myDid: String, requestId: String) {
        Log.d(InitializeActivity.TAG, "Registry Config - getRegistryConfig: packing")
        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")


        val messageUuid = UUID.randomUUID().toString()

        val data = "{\n" +
                "    \"@id\": \"$messageUuid\",\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/basic-routing/1.0/add-route\",\n" +
                "    \"routedestination\": \"$key\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}\n"

        //getting mediator connnection
        val mediatorConnection = SearchUtils.searchWallet(
            WalletRecordType.MEDIATOR_CONNECTION,
            "{}"
        )

        val mediatorConnectionObject = WalletManager.getGson.fromJson(
            mediatorConnection.records?.get(0)?.value,
            MediatorConnectionObject::class.java
        )
        val connectionDid = mediatorConnectionObject.myDid

        val connectionMetaString =
            Did.getDidWithMeta(WalletManager.getWallet, connectionDid).get()
        val connectionMetaObject = JSONObject(connectionMetaString)
        val connectedKey = connectionMetaObject.getString("verkey")

        val mediatorDidDoc = SearchUtils.searchWallet(WalletRecordType.MEDIATOR_DID_DOC, "{}")

        val didDocObj = WalletManager.getGson.fromJson(
            mediatorDidDoc.records?.get(0)?.value,
            DidDoc::class.java
        )

        val packedMessage = PackingUtils.packMessage(
            "[\"${didDocObj.publicKey!![0].publicKeyBase58}\"]",
            connectedKey,
            data
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

        ApiManager.api.getService()?.cloudConnection(typedBytes)
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
                        Log.d(
                            InitializeActivity.TAG,
                            "Registry Config - getRegistryConfig: cloud connection success - ${
                                response.body().toString()
                            }"
                        )
                        val search =
//                            SearchUtils.searchWallet(
//                            REGISTRY_CONNECTION_INVITATION,
//                            "{}"
//                        )
                            WalletSearch.open(
                                WalletManager.getWallet,
                                WalletRecordType.REGISTRY_CONNECTION_INVITATION,
                                "{}",
                                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
                            ).get()
//
                        val value = WalletSearch.searchFetchNextRecords(
                            WalletManager.getWallet,
                            search,
                            100
                        ).get()

                        WalletManager.closeSearchHandle(search)
//
//                        val registryInvitation = WalletManager.getGson.fromJson(
//                            mediatorConnection.records?.get(0)?.value,
//                            Invitation::class.java
//                        )
//
                        val connectionInvitationData = JSONObject(value)

                        Log.d(InitializeActivity.TAG, "packConnectionRequestMessage0: $value")

                        //json object
                        val connectionInvitationRecords =
                            JSONArray(connectionInvitationData.get("records").toString())
                        val connectionInvitationRecord =
                            connectionInvitationRecords.getJSONObject(0)
                        val connectionInvitationValue =
                            JSONObject(connectionInvitationRecord.getString("value"))

                        //public keys
                        val publicKey = PublicKey()
                        publicKey.id = "did:sov:$myDid#1"
                        publicKey.type = "Ed25519VerificationKey2018"
                        publicKey.controller = "did:sov:$myDid"
                        publicKey.publicKeyBase58 = key

                        val publicKeys: ArrayList<PublicKey> = ArrayList()
                        publicKeys.add(publicKey)

                        //authentication
                        val authentication = Authentication()
                        authentication.type = "Ed25519SignatureAuthentication2018"
                        authentication.publicKey = "did:sov:$myDid#1"

                        val authentications: ArrayList<Authentication> = ArrayList()
                        authentications.add(authentication)

                        //service
                        val recipientsKey: ArrayList<String> = ArrayList()
                        recipientsKey.add(key)

                        //service
                        val routis: java.util.ArrayList<String> = java.util.ArrayList()
                        routis.add(didDocObj.service!![0].routingKeys!![0])

                        val service = Service()
                        service.id = "did:sov:$myDid;indy"
                        service.type = "IndyAgent"
                        service.priority = 0
                        service.recipientKeys = recipientsKey
                        service.routingKeys = routis
                        service.serviceEndpoint = ApiManager.API_URL

                        val services: ArrayList<Service> = ArrayList()
                        services.add(service)

                        //did doc
                        val didDoc = DidDoc()
                        didDoc.context = "https://w3id.org/did/v1"
                        didDoc.id = "did:sov:$myDid"
                        didDoc.publicKey = publicKeys
                        didDoc.authentication = authentications
                        didDoc.service = services

                        //did
                        val did = DID()
                        did.did = myDid
                        did.didDoc = didDoc

                        // transport
                        val transport = Transport("all")

                        //connection request
                        val connectionRequest = ConnectionRequest()
                        connectionRequest.type =
                            "${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/connections/1.0/request"
                        connectionRequest.id = requestId
                        connectionRequest.label = "milan"
                        connectionRequest.connection = did
                        connectionRequest.transport = transport

                        val str = WalletManager.getGson.toJson(connectionRequest)

                        val connectionRequestPackedMessage = PackingUtils.packMessage(
                            connectionInvitationValue.getString("recipientKeys"),
                            key,
                            str
                        )

                        val connectionRequestTypedBytes: RequestBody = object : RequestBody() {
                            override fun contentType(): MediaType? {
                                return "application/ssi-agent-wire".toMediaTypeOrNull()
                            }

                            @Throws(IOException::class)
                            override fun writeTo(sink: BufferedSink) {
                                sink.write(connectionRequestPackedMessage)
                            }
                        }
//
                        ApiManager.api.getService()
                            ?.postData(
                                connectionInvitationValue.getString("serviceEndpoint"),
                                connectionRequestTypedBytes
                            )
                            ?.enqueue(object :
                                Callback<ConfigPostResponse> {
                                override fun onFailure(
                                    call: Call<ConfigPostResponse>,
                                    t: Throwable
                                ) {

                                }

                                override fun onResponse(
                                    call: Call<ConfigPostResponse>,
                                    response: Response<ConfigPostResponse>
                                ) {
                                    if (response.code() == 200 && response.body() != null) {
                                        Log.d(
                                            InitializeActivity.TAG,
                                            "Registry Config - getRegistryConfig: save did doc - ${
                                                WalletManager.getGson.toJson(response.body())
                                            }"
                                        )
                                        SaveDidDocTask.saveDidDoc(
                                            object : CommonHandler {
                                                override fun taskStarted() {

                                                }

                                                override fun onSaveDidComplete(
                                                    typedBytes: RequestBody?,
                                                    serviceEndPoint: String
                                                ) {
                                                    Log.d(
                                                        InitializeActivity.TAG,
                                                        "Registry Config - getRegistryConfig: saved did doc"
                                                    )

                                                    if (typedBytes != null)
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

                                                                }

                                                                override fun onResponse(
                                                                    call: Call<ResponseBody>,
                                                                    response: Response<ResponseBody>
                                                                ) {
                                                                    Log.d(
                                                                        InitializeActivity.TAG,
                                                                        "Registry Config - getRegistryConfig: ping request success"
                                                                    )

                                                                    anchorRegistry(myDid)
                                                                }
                                                            })
                                                }
                                            },
                                            WalletManager.getGson.toJson(
                                                response.body()
                                            ), "", isRegistryConnection = true
                                        )
                                    }
                                }
                            })
                    }
                }
            })
    }

    private fun anchorRegistry(myDid: String) {

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val myKey = metaObject.getString("verkey")

        Log.d(InitializeActivity.TAG, "Registry Config - Anchor : packing $myDid , $myKey")

        val did = Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()

        Log.d(InitializeActivity.TAG, "Registry Config - Anchor : new ${did.did} , ${did.verkey}")

        val didDoc = WalletUtils.createDidDocForRegistry(did.did, did.verkey)

        Log.d(InitializeActivity.TAG, "anchorRegistry: ${WalletManager.getGson.toJson(didDoc)}")
        val sigDecorator =
            WalletUtils.createSignatureDecorator(WalletManager.getGson.toJson(didDoc), myKey)

        Log.d(
            InitializeActivity.TAG,
            "anchorRegistry: ${WalletManager.getGson.toJson(sigDecorator)}"
        )

        val id = UUID.randomUUID()
        val createdTime = Date().time

        val registryDidDocSearchResponse =
            SearchUtils.searchWallet(WalletRecordType.REGISTRY_DID_DOC, "{}")
        if ((registryDidDocSearchResponse.records?.size ?: 0) > 0) {
            Log.d(
                InitializeActivity.TAG,
                "anchorRegistry: ${registryDidDocSearchResponse?.records?.get(0)?.value ?: ""}"
            )
            val registryDidDoc = WalletManager.getGson.fromJson(
                registryDidDocSearchResponse?.records?.get(0)?.value ?: "{}", DidDoc::class.java
            )

            val dataToBePacked = WalletManager.getGson.toJson(
                AchorRegistryRequest(
                    "DidCommPrefixUtils.getType()}/mydata-did/1.0/create-did",
                    id.toString(),
                    createdTime.toString(),
                    "did:mydata:${
                        WalletUtils.convertDidSovToMyDid(
                            registryDidDoc.service?.get(0)?.recipientKeys?.get(
                                0
                            ) ?: ""
                        )
                    }",
                    "did:mydata:${WalletUtils.convertDidSovToMyDid(myKey)}",
                    WalletManager.getGson.toJson(sigDecorator),
                    Transport("all")
                )
            )

            Log.d(InitializeActivity.TAG, "anchorRegistry: $dataToBePacked")
            val packedData = PackingUtils.packMessage(registryDidDoc, myKey, dataToBePacked, "")

            val packedDataRequestTypedBytes = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(packedData)
                }
            }

            ApiManager.api.getService()
                ?.postDataWithoutData(
                    registryDidDoc.service?.get(0)?.serviceEndpoint ?: "",
                    packedDataRequestTypedBytes
                )
                ?.enqueue(object :
                    Callback<ResponseBody> {
                    override fun onFailure(
                        call: Call<ResponseBody>,
                        t: Throwable
                    ) {
                        Log.d(InitializeActivity.TAG, "MILMIL : onFailure: ")
                    }

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        Log.d(InitializeActivity.TAG, "MILMIL : onSuccess: ")
                        if (response.code() == 200) {
                            WalletMethods.addWalletRecord(
                                WalletManager.getWallet,
                                WalletRecordType.REGISTRY_ANCHOR,
                                UUID.randomUUID().toString(),
                                dataToBePacked,
                                "{}"
                            )
                        }
                    }
                })
        }
    }

    private fun pollMessagesInThread(
        myDid: String,
        context: Context,
        notificationListener: NotificationListener?
    ) {
        val uuid = UUID.randomUUID().toString()
        val data = "\n" +
                "{\n" +
                "    \"@id\": \"$uuid\",\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/basic-routing/1.0/get-inbox-items\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}\n"

        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val search = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.MEDIATOR_DID_DOC,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val value = WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        Log.d(InitializeActivity.TAG, "did doc: $value")
        val gson = Gson()
        val didDoc = JSONObject(
            JSONObject(value).getJSONArray("records").get(0).toString()
        ).getString("value")
        Log.d(InitializeActivity.TAG, "did doc 2: $didDoc")
        val test = gson.fromJson(didDoc, DidDoc::class.java)

        val packedMessage = PackingUtils.packMessage(
            "[\"${test.publicKey?.get(0)?.publicKeyBase58}\"]",
            key,
            data
        )

        Timer().scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
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
                        }

                        override fun onResponse(
                            call: Call<ResponseBody>,
                            response: Response<ResponseBody>
                        ) {
                            if (response.code() == 200 && response.body() != null) {
                                Log.d(
                                    InitializeActivity.TAG,
                                    "unPackMessage: ${response.body()!!.byteStream()}"
                                )
                                val inputStream = response.body()!!.byteStream()
                                val str: String = IOUtils.toString(inputStream, "UTF-8")
                                Log.d(InitializeActivity.TAG, "onResponse: $str")
                                unPackPollMessage(str, myDid, context, notificationListener)
                            }
                        }
                    })
            }
        }, 0, 5000)
    }

    private fun unPackPollMessage(
        body: String,
        myDid: String,
        context: Context,
        notificationListener: NotificationListener?
    ) {
        try {
            val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
            Log.d(InitializeActivity.TAG, "for delete unPackPollMessage: ${String(unpacked)}")
            val messageList = JSONObject(String(unpacked)).getString("message")
            val item = JSONObject(messageList).getJSONArray("Items")
            for (i in 0 until item.length()) {
                val o = item.getJSONObject(i).getString("Data")

                Log.d(InitializeActivity.TAG, "unPackPollMessage: item: ${item.getJSONObject(i)}")
                val unpack =
                    Crypto.unpackMessage(WalletManager.getWallet, o.toString().toByteArray()).get()
                Log.d(
                    InitializeActivity.TAG,
                    "packConnectionRequest response: $i -  ${String(unpack)}"
                )

                var type =
                    JSONObject(JSONObject(String(unpack)).getString("message")).getString("@type")

                DeletePollMessage.delete(
                    item.getJSONObject(i).getString(
                        "@id"
                    ), myDid, type
                )

                val index: Int = type.lastIndexOf('/')
                type = type.substring(index + 1, type.length)

                when (type) {
                    MessageTypes.TYPE_CONNECTION_RESPONSE -> {
                        unPackSigMessage(o, false, context, notificationListener)
                    }
                    MessageTypes.TYPE_PING_RESPONSE -> {
                        UnPackPing.unPack(context, JSONObject(String(unpack)))
                    }
                    MessageTypes.OFFER_REQUEST -> {
                        UnpackOfferCredential.unPackOfferCredential(
                            context,
                            JSONObject(String(unpack)), notificationListener
                        )
                    }
                    MessageTypes.TYPE_ISSUE_CREDENTIAL -> {
                        UnPackIssueCredential.unPackIssueCredential(
                            context,
                            JSONObject(String(unpack))
                        )
                    }
                    MessageTypes.SHARE_REQUEST -> {
                        UnPackRequestPresentation.unPack(
                            context,
                            JSONObject(String(unpack)),
                            notificationListener
                        )
                    }
                    MessageTypes.TYPE_REQUEST_PRESENTATION_ACK -> {
                        UnPackPresentationAck.unPack(JSONObject(String(unpack)))
                    }
//                    TYPE_RECIEPT->{
//                        UnPackReciept.unPackReciept(JSONObject(String(unpack)))
//                    }
//                    TYPE_NOTIFICATION->{
//                        UnPackNotification.unPackNotification(JSONObject(String(unpack)))
//                    }
                }
            }
        } catch (e: Exception) {
            Log.d(InitializeActivity.TAG, "unPackPollMessage: ${e.message}")
        }

    }
}