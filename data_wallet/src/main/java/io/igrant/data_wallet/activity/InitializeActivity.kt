package io.igrant.data_wallet.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.igrant.data_wallet.R
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.data_wallet.events.ConnectionSuccessEvent
import io.igrant.data_wallet.events.ReceiveCertificateEvent
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.events.ReceiveOfferEvent
import io.igrant.data_wallet.fragment.UrlExtractFragment
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.handlers.PoolHandler
import io.igrant.data_wallet.handlers.SearchHandler
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.indy.PoolManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.InitialActivityFunctions
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.agentConfig.ConfigResponse
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.certificateOffer.Base64Extracted
import io.igrant.data_wallet.models.certificateOffer.CertificateOffer
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.connectionRequest.*
import io.igrant.data_wallet.models.credentialExchange.CredentialExchange
import io.igrant.data_wallet.models.credentialExchange.CredentialProposalDict
import io.igrant.data_wallet.models.credentialExchange.IssueCredential
import io.igrant.data_wallet.models.credentialExchange.RawCredential
import io.igrant.data_wallet.models.did.DidResult
import io.igrant.data_wallet.models.presentationExchange.PresentationExchange
import io.igrant.data_wallet.models.presentationExchange.PresentationRequest
import io.igrant.data_wallet.models.requests.AchorRegistryRequest
import io.igrant.data_wallet.models.tagJsons.ConnectionId
import io.igrant.data_wallet.models.tagJsons.ConnectionTags
import io.igrant.data_wallet.models.tagJsons.UpdateInvitationKey
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.models.walletSearch.SearchResponse
import io.igrant.data_wallet.tasks.*
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.ConnectionStates.Companion.CONNECTION_ACTIVE
import io.igrant.data_wallet.utils.ConnectionStates.Companion.CONNECTION_INVITATION
import io.igrant.data_wallet.utils.ConnectionStates.Companion.CONNECTION_REQUEST
import io.igrant.data_wallet.utils.ConnectionStates.Companion.CONNECTION_RESPONSE
import io.igrant.data_wallet.utils.CredentialExchangeStates.Companion.CREDENTIAL_CREDENTIAL_ACK
import io.igrant.data_wallet.utils.CredentialExchangeStates.Companion.CREDENTIAL_CREDENTIAL_RECEIVED
import io.igrant.data_wallet.utils.ExchangeUtils.getExchangeDataIntent
import io.igrant.data_wallet.utils.MessageTypes.Companion.TYPE_CONNECTION_RESPONSE
import io.igrant.data_wallet.utils.MessageTypes.Companion.TYPE_ISSUE_CREDENTIAL
import io.igrant.data_wallet.utils.MessageTypes.Companion.TYPE_OFFER_CREDENTIAL
import io.igrant.data_wallet.utils.MessageTypes.Companion.TYPE_PING_RESPONSE
import io.igrant.data_wallet.utils.MessageTypes.Companion.TYPE_REQUEST_PRESENTATION
import io.igrant.data_wallet.utils.MessageTypes.Companion.TYPE_REQUEST_PRESENTATION_ACK
import io.igrant.data_wallet.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.CREDENTIAL_EXCHANGE_V10
import io.igrant.data_wallet.utils.WalletRecordType.Companion.DID_DOC
import io.igrant.data_wallet.utils.WalletRecordType.Companion.DID_KEY
import io.igrant.data_wallet.utils.WalletRecordType.Companion.MEDIATOR_CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.MEDIATOR_CONNECTION_INVITATION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.MEDIATOR_DID_DOC
import io.igrant.data_wallet.utils.WalletRecordType.Companion.MEDIATOR_DID_KEY
import io.igrant.data_wallet.utils.WalletRecordType.Companion.MESSAGE_RECORDS
import io.igrant.data_wallet.utils.WalletRecordType.Companion.REGISTRY_ANCHOR
import io.igrant.data_wallet.utils.WalletRecordType.Companion.REGISTRY_CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.REGISTRY_CONNECTION_INVITATION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.REGISTRY_DID_DOC
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.apache.commons.io.IOUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList

class InitializeActivity : BaseActivity(), InitialActivityFunctions,UrlExtractFragment.ProgressListener, ExtractListeners {

    companion object {
        const val TAG = "InitializeActivity"
        const val DEEP_LINK = "io.igrant.data_wallet.activity.InitializeActivity.deep_link"
    }

    //views
    private lateinit var toolbar: Toolbar
    private lateinit var llProgressBar: LinearLayout
    private lateinit var clLoading: ConstraintLayout

    private lateinit var tvLoadingStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize)
        initViews()
        setUpToolbar()
        initFragment()
        getMediatorConfig()
        getIntentData()
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
    }

    private fun getIntentData() {
        if (intent.hasExtra(DEEP_LINK)){
            try {
                val uri: Uri = try {
                    Uri.parse(intent.getStringExtra(DEEP_LINK))
                } catch (e: Exception) {
                    Uri.parse("igrant.io")
                }

                if (ConnectionUtils.isIGrnatValidUrl(uri.toString())) {
                    Firebase.dynamicLinks
                        .getDynamicLink(uri)
                        .addOnSuccessListener(this) { pendingDynamicLinkData ->
                            // Get deep link from result (may be null if no link is found)
                            var deepLink: Uri? = null
                            if (pendingDynamicLinkData != null) {
                                deepLink = pendingDynamicLinkData.link

                                try {
                                    val deepLinkUri: Uri = deepLink ?: Uri.parse("igrant.io")

                                    extractUrlFunction(deepLinkUri)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        this,
                                        resources.getString(R.string.connection_unexpected_error_please_try_again),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .addOnFailureListener(this) { e ->
                            extractUrlFunction(uri)
                        }

                } else {
                    extractUrlFunction(uri)
                }
            } catch (e: Exception) {

            }
            intent.removeExtra(DEEP_LINK)
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        llProgressBar = findViewById(R.id.llProgressBar)
        clLoading = findViewById(R.id.clLoadingScreen)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun getMediatorConfig() {
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
                                        tvLoadingStatus.visibility = View.GONE
                                    }

                                    override fun onResponse(
                                        call: Call<ConfigResponse>,
                                        response: Response<ConfigResponse>
                                    ) {
                                        if (response.code() == 200 && response.body() != null) {
                                            saveConnectionRecord(response.body()!!.invitation, true)
                                        }
                                    }
                                })
                        } else {
                            val connectionData =
                                JSONObject(searchResponse.records?.get(0)?.value ?: "")

                            when (connectionData.getString("state")) {
                                CONNECTION_REQUEST, CONNECTION_INVITATION -> {
                                    val myDid: String = connectionData.getString("my_did")
                                    val requestId: String = connectionData.getString("request_id")

                                    packConnectionRequestMessage(myDid, requestId)
                                }
                                CONNECTION_RESPONSE -> {
                                    //GET DID DOC FROM RECORD FOR PUBLIC KEY
                                    //CALL createInbox
                                }
                                CONNECTION_ACTIVE -> {
                                    tvLoadingStatus.visibility = View.GONE
                                    val myDid: String = connectionData.getString("my_did")
                                    pollMessagesInThread(myDid)

                                    getRegistryConfig()
                                }
                            }
                        }
                    }

                    override fun taskStarted() {

                    }
                },
                MEDIATOR_CONNECTION,
                "{}"
            )
        } catch (e: Exception) {
        }
    }

    private fun saveConnectionRecord(invitation: Invitation?, isMediator: Boolean) {


        if (WalletManager.getWallet != null) {
            val value =
                WalletManager.getGson.toJson(setUpMediatorConnectionObject(invitation, null, null))
            val connectionUuid = UUID.randomUUID().toString()

            val connectionTag = ConnectionTags()
            connectionTag.invitationKey = invitation?.recipientKeys!![0]
            connectionTag.state = CONNECTION_INVITATION

            val tagJson =
                WalletManager.getGson.toJson(connectionTag)

            WalletMethods.addWalletRecord(
                WalletManager.getWallet,
                if (isMediator) MEDIATOR_CONNECTION else REGISTRY_CONNECTION,
                connectionUuid,
                value.toString(),
                tagJson.toString()
            )


            saveConnectionInvitationRecord(connectionUuid, invitation, isMediator)
        }
    }

    private fun packConnectionRequestMessage(myDid: String, requestId: String) {
        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
        val metaObject = JSONObject(metaString)
        val key = metaObject.getString("verkey")

        val search = WalletSearch.open(
            WalletManager.getWallet,
            MEDIATOR_CONNECTION_INVITATION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val value = WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        val connectionInvitationData = JSONObject(value)

        Log.d(TAG, "packConnectionRequestMessage0: $value")

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
                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ConfigPostResponse>,
                    response: Response<ConfigPostResponse>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        unPackSigMessage(WalletManager.getGson.toJson(response.body()), true)
                    }
                }
            })
    }

    private fun getRegistryConfig() {
        Log.d(TAG, "Registry Config - getRegistryConfig: starting")
        try {
            WalletSearchTask.searchWallet(
                object : SearchHandler {
                    override fun taskCompleted(searchResponse: SearchResponse) {
                        Log.d(TAG, "Registry Config - getRegistryConfig: search task completed")
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
                                                TAG,
                                                "Registry Config - getRegistryConfig: success"
                                            )
                                            saveConnectionRecord(
                                                response.body()!!.invitation,
                                                false
                                            )
                                        }
                                    }
                                })
                        } else {
                            val connectionData =
                                JSONObject(searchResponse.records?.get(0)?.value ?: "")

                            when (connectionData.getString("state")) {
                                CONNECTION_REQUEST, CONNECTION_INVITATION -> {
                                    Log.d(TAG, "Registry Config - getRegistryConfig: state 1")
                                    val myDid: String = connectionData.getString("my_did")
                                    val requestId: String = connectionData.getString("request_id")

                                    packRegistryRequestMessage(myDid, requestId)
                                }
                                CONNECTION_RESPONSE -> {
                                    Log.d(TAG, "Registry Config - getRegistryConfig: state 2")
                                    //GET DID DOC FROM RECORD FOR PUBLIC KEY
                                    //CALL createInbox
                                }
                                CONNECTION_ACTIVE -> {
                                    Log.d(TAG, "Registry Config - getRegistryConfig: state 3")
                                }
                            }
                        }
                    }

                    override fun taskStarted() {

                    }
                }, REGISTRY_CONNECTION,
                "{}"
            )
        } catch (e: Exception) {
        }
    }

    private fun packRegistryRequestMessage(myDid: String, requestId: String) {
        Log.d(TAG, "Registry Config - getRegistryConfig: packing")
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
            MEDIATOR_CONNECTION,
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

        val mediatorDidDoc = SearchUtils.searchWallet(MEDIATOR_DID_DOC, "{}")

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
                            TAG,
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
                                REGISTRY_CONNECTION_INVITATION,
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

                        Log.d(TAG, "packConnectionRequestMessage0: $value")

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
                                            TAG,
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
                                                        TAG,
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
                                                                        TAG,
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

        Log.d(TAG, "Registry Config - Anchor : packing $myDid , $myKey")

        val did = Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()

        Log.d(TAG, "Registry Config - Anchor : new ${did.did} , ${did.verkey}")

        val didDoc = WalletUtils.createDidDocForRegistry(did.did, did.verkey)

        Log.d(TAG, "anchorRegistry: ${WalletManager.getGson.toJson(didDoc)}")
        val sigDecorator =
            WalletUtils.createSignatureDecorator(WalletManager.getGson.toJson(didDoc), myKey)

        Log.d(TAG, "anchorRegistry: ${WalletManager.getGson.toJson(sigDecorator)}")

        val id = UUID.randomUUID()
        val createdTime = Date().time

        val registryDidDocSearchResponse = SearchUtils.searchWallet(REGISTRY_DID_DOC, "{}")
        if ((registryDidDocSearchResponse.records?.size ?: 0) > 0) {
            Log.d(
                TAG,
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

            Log.d(TAG, "anchorRegistry: $dataToBePacked")
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
                        Log.d(TAG, "MILMIL : onFailure: ")
                    }

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        Log.d(TAG, "MILMIL : onSuccess: ")
                        if (response.code() == 200) {
                            WalletMethods.addWalletRecord(
                                WalletManager.getWallet,
                                REGISTRY_ANCHOR,
                                UUID.randomUUID().toString(),
                                dataToBePacked,
                                "{}"
                            )
                        }
                    }
                })
        }
    }

    private fun initFragment() {
        if (supportFragmentManager != null)
            NavigationUtils.showWalletFragment(supportFragmentManager, false)
    }

    private fun deleteReadMessage(
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
                MEDIATOR_DID_DOC,
                "{}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val value =
                WalletSearch.searchFetchNextRecords(WalletManager.getWallet, didSearch, 100).get()

            WalletManager.closeSearchHandle(didSearch)
            Log.d(TAG, "did doc: $value")
            val didDoc = JSONObject(
                JSONObject(value).getJSONArray("records").get(0).toString()
            ).getString("value")
            Log.d(TAG, "did doc 2: $didDoc")
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
                        Log.d(TAG, "onFailure: ")
                    }

                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {

                    }
                })
        }
    }

    private fun pollMessagesInThread(myDid: String) {
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
            MEDIATOR_DID_DOC,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val value = WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        Log.d(TAG, "did doc: $value")
        val gson = Gson()
        val didDoc = JSONObject(
            JSONObject(value).getJSONArray("records").get(0).toString()
        ).getString("value")
        Log.d(TAG, "did doc 2: $didDoc")
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
                                Log.d(TAG, "unPackMessage: ${response.body()!!.byteStream()}")
                                val inputStream = response.body()!!.byteStream()
                                val str: String = IOUtils.toString(inputStream, "UTF-8")
                                Log.d(TAG, "onResponse: $str")
                                unPackPollMessage(str, myDid)
                            }
                        }
                    })
            }
        }, 0, 5000)
    }

    private fun unPackPollMessage(body: String, myDid: String) {
        try {
            val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
            Log.d(TAG, "for delete unPackPollMessage: ${String(unpacked)}")
            val messageList = JSONObject(String(unpacked)).getString("message")
            val item = JSONObject(messageList).getJSONArray("Items")
            for (i in 0 until item.length()) {
                val o = item.getJSONObject(i).getString("Data")

                Log.d(TAG, "unPackPollMessage: item: ${item.getJSONObject(i)}")
                val unpack =
                    Crypto.unpackMessage(WalletManager.getWallet, o.toString().toByteArray()).get()
                Log.d(TAG, "packConnectionRequest response: $i -  ${String(unpack)}")

                var type =
                    JSONObject(JSONObject(String(unpack)).getString("message")).getString("@type")

                deleteReadMessage(
                    item.getJSONObject(i).getString(
                        "@id"
                    ), myDid, type
                )

                val index: Int = type.lastIndexOf('/')
                type = type.substring(index + 1, type.length)

                when (type) {
                    TYPE_CONNECTION_RESPONSE -> {
                        unPackSigMessage(o, false)
                    }
                    TYPE_PING_RESPONSE -> {
                        processPingResponse(JSONObject(String(unpack)))
                    }
                    TYPE_OFFER_CREDENTIAL -> {
                        UnpackOfferCredential.unPackOfferCredential(
                            this,
                            JSONObject(String(unpack))
                        )
                    }
                    TYPE_ISSUE_CREDENTIAL -> {
                        UnPackIssueCredential.unPackIssueCredential(
                            this,
                            JSONObject(String(unpack))
                        )
                    }
                    TYPE_REQUEST_PRESENTATION -> {
                        unPackRequestPresentation(JSONObject(String(unpack)))
                    }
                    TYPE_REQUEST_PRESENTATION_ACK -> {
                        updatePresentProofToAck(JSONObject(String(unpack)))
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
            Log.d(TAG, "unPackPollMessage: ${e.message}")
        }

    }

    private fun processPingResponse(jsonObject: JSONObject) {
        val recipientVerKey = jsonObject.getString("sender_verkey")
        val connectionSearch = SearchUtils.searchWallet(
            CONNECTION,
            "{\"recipient_key\":\"$recipientVerKey\"}"
        )

        if (connectionSearch.totalCount ?: 0 > 0) {
            val mediatorConnectionObject: MediatorConnectionObject =
                WalletManager.getGson.fromJson(
                    connectionSearch.records?.get(0)?.value,
                    MediatorConnectionObject::class.java
                )

            mediatorConnectionObject.state = CONNECTION_ACTIVE

            val connectionUuid =
                connectionSearch.records?.get(0)?.id

            val value = WalletManager.getGson.toJson(mediatorConnectionObject)

            WalletMethods.updateWalletRecord(
                WalletManager.getWallet,
                CONNECTION,
                connectionUuid,
                value
            )

            EventBus.getDefault().post(ConnectionSuccessEvent(mediatorConnectionObject.requestId ?: ""))
        }
        else {

            Log.d(
                TAG,
                "Registry Config - getRegistryConfig: entered ping unpack"
            )

            val registryConnectionSearch = SearchUtils.searchWallet(
                REGISTRY_CONNECTION,
                "{}"
            )
            if (registryConnectionSearch.totalCount ?: 0 > 0) {
                Log.d(
                    TAG,
                    "Registry Config - getRegistryConfig: ping have items ${
                        registryConnectionSearch.records?.get(
                            0
                        )?.value
                    }"
                )

                val mediatorConnectionObject: MediatorConnectionObject =
                    WalletManager.getGson.fromJson(
                        registryConnectionSearch.records?.get(0)?.value,
                        MediatorConnectionObject::class.java
                    )

                mediatorConnectionObject.state = CONNECTION_ACTIVE

                val connectionUuid =
                    registryConnectionSearch.records?.get(0)?.id

                val value = WalletManager.getGson.toJson(mediatorConnectionObject)

                WalletMethods.updateWalletRecord(
                    WalletManager.getWallet,
                    REGISTRY_CONNECTION,
                    connectionUuid,
                    value
                )

                Log.d(
                    TAG,
                    "Registry Config - getRegistryConfig: completed"
                )

                //todo anchor registry

            } else {

                Toast.makeText(
                    this,
                    resources.getString(R.string.connection_unexpected_error_please_try_again),
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun updatePresentProofToAck(jsonObject: JSONObject) {
        try {
            WalletRecord.delete(
                WalletManager.getWallet,
                MESSAGE_RECORDS,
                JSONObject(jsonObject.getString("message")).getJSONObject("~thread")
                    .getString("thid")
            ).get()

            val presentationExchange = SearchUtils.searchWallet(
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                "{\"thread_id\":\"${
                    JSONObject(jsonObject.getString("message")).getJSONObject("~thread")
                        .getString("thid")
                }\"}"
            )
//
            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                "${presentationExchange.records?.get(0)?.id}"
            ).get()

            EventBus.getDefault()
                .post(ReceiveExchangeRequestEvent())
        } catch (e: Exception) {

        }
    }

    private fun unPackRequestPresentation(jsonObject: JSONObject) {
        val recipientKey = jsonObject.getString("recipient_verkey")

        val connectionSearch = SearchUtils.searchWallet(
            CONNECTION,
            "{\"my_key\":\"$recipientKey\"}"
        )

        val connectionObject: MediatorConnectionObject =
            WalletManager.getGson.fromJson(
                connectionSearch.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )

//        val connectionObject = ConnectionUtils.getConnection(jsonObject.getString("sender_verkey"))

        if (connectionObject != null) {
            val p = SearchUtils.searchWallet(
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                "{\"thread_id\":\"${JSONObject(jsonObject.getString("message")).getString("@id")}\"}"
            )

            val presentationRequestBase64 =
                JSONObject(
                    JSONObject(jsonObject.getString("message")).getJSONArray("request_presentations~attach")
                        .get(0).toString()
                )
                    .getJSONObject("data").getString("base64")
            val presentationRequest = WalletManager.getGson.fromJson(
                Base64.decode(presentationRequestBase64, Base64.URL_SAFE)
                    .toString(charset("UTF-8")), PresentationRequest::class.java
            )
            if (p.totalCount ?: 0 == 0) {
                val presentationExchange = PresentationExchange()
                val message = JSONObject(jsonObject.getString("message"))
                presentationExchange.threadId =
                    message.getString("@id")

                if (message.has("qr_id"))
                    presentationExchange.qrId = message.getString("qr_id")

                try {
                    if (message.has("~data-agreement-context"))
                        presentationExchange.dataAgreementContext = WalletManager.getGson.fromJson(
                            message.getString("~data-agreement-context"),
                            DataAgreementContext::class.java
                        )
                } catch (e: Exception) {
                }
                presentationExchange.createdAt = DateUtils.getIndyFormattedDate()
                presentationExchange.updatedAt = DateUtils.getIndyFormattedDate()
                presentationExchange.connectionId = connectionObject?.requestId
                presentationExchange.initiator = "external"
                presentationExchange.presentationProposalDict = null
                presentationExchange.presentationRequest = presentationRequest
                presentationExchange.role = "prover"
                presentationExchange.state = PresentationExchangeStates.REQUEST_RECEIVED
                presentationExchange.type =
                    JSONObject(jsonObject.getString("message")).getString("@type")
                try {
                    presentationExchange.comment =
                        JSONObject(jsonObject.getString("message")).getString("comment")
                } catch (e: Exception) {
                    presentationExchange.comment = ""
                }

                val id = UUID.randomUUID().toString()
                val tag =
                    "{\"thread_id\": \"${JSONObject(jsonObject.getString("message")).getString("@id")}\"," +
                            "\"connection_id\":\"${connectionObject?.requestId}\"}"
                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.PRESENTATION_EXCHANGE_V10,
                    id,
                    WalletManager.getGson.toJson(presentationExchange),
                    tag
                )

                val notification = Notification()
                notification.type = TYPE_REQUEST_PRESENTATION
                notification.stat = "Active"
                notification.presentation = presentationExchange
                notification.connection = connectionObject
                notification.date = DateUtils.getIndyFormattedDate()

                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    MESSAGE_RECORDS,
                    JSONObject(jsonObject.getString("message")).getString("@id"),
                    WalletManager.getGson.toJson(notification),
                    "{\n" +
                            "  \"type\":\"$TYPE_REQUEST_PRESENTATION\",\n" +
                            "  \"connectionId\":\"${connectionObject?.requestId}\",\n" +
                            "  \"certificateId\":\"${
                                JSONObject(jsonObject.getString("message")).getString(
                                    "@id"
                                )
                            }\",\n" +
                            "  \"stat\":\"Active\"\n" +
                            "}"
                )

                try {
                    val searchResponse = SearchUtils.searchWallet(
                        MESSAGE_RECORDS,
                        "{\"certificateId\":\"${
                            JSONObject(jsonObject.getString("message")).getString(
                                "@id"
                            )
                        }\"}"
                    )
                    if ((searchResponse.totalCount ?: 0) > 0) {

                        //go to intialize activity then start the offer certificate activity
                        val intent = getExchangeDataIntent(
                            searchResponse.records!![0],
                            this@InitializeActivity
                        )

                        if (intent != null) {
                            MessageUtils.showNotification(
                                intent,
                                this,
                                TYPE_ISSUE_CREDENTIAL,
                                resources.getString(R.string.data_received_exchange_request),
                                "Received a new exchange request from the organisation ${connectionObject?.theirLabel ?: ""}"
                            )

                        }
                        EventBus.getDefault()
                            .post(ReceiveExchangeRequestEvent())
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "unPackRequestPresentation: ${e.message}")
                }
            }
        }
    }

    private fun unPackSigMessage(body: String, isMediator: Boolean) {

        Log.d(TAG, "unPackMessage: $body")
        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
        Log.d(TAG, "packConnectionRequestMessage: ${String(unpacked)}")

        val response = JSONObject(String(unpacked))

        val message = JSONObject(response.get("message").toString())

        val connectionSig = JSONObject(message.get("connection~sig").toString())
        val sigData = connectionSig.get("sig_data").toString()
        Log.d(
            TAG,
            "unPackMessage: decoded : ${
                Base64.decode(sigData, Base64.URL_SAFE)
                    .toString(charset("UTF-8"))
            }"
        )
        val decoded = Base64.decode(sigData, Base64.URL_SAFE)
            .toString(charset("UTF-8"))
        val postion = decoded.indexOf("{\"DID\"")
        Log.d(TAG, "unPackMessage: positon : $postion")
        val data = decoded.substring(postion)

        saveDidDoc(data, isMediator)
    }

    private fun saveDidDoc(data: String, isMediator: Boolean) {
        Log.d(TAG, "saveDidDoc: $data")
        val didData = JSONObject(data)
        val didDoc = didData.getString("DIDDoc")
        val theirDid = didData.getString("DID")

        val didDocUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$theirDid\"}"

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_DID_DOC else DID_DOC,
            didDocUuid,
            didDoc.toString(),
            tagJson
        )

        val publicKey = JSONObject(didDoc).getJSONArray("publicKey").getJSONObject(0)
            .getString("publicKeyBase58")
        addDidKey(publicKey, theirDid, isMediator)
    }

    private fun addDidKey(publicKey: String, theirDid: String, isMediator: Boolean) {

        val didKeyUuid = UUID.randomUUID().toString()

        val tagJson = "{\"did\": \"$theirDid\", \"key\": \"$publicKey\"}"

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_DID_KEY else DID_KEY,
            didKeyUuid,
            publicKey,
            tagJson
        )
        updateRecord(publicKey, theirDid, isMediator)
    }

    private fun updateRecord(publicKey: String, theirDid: String, isMediator: Boolean) {

        val search = WalletSearch.open(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)

        val data = JSONObject(connection)
        Log.d(TAG, "getMediatorConfig: $connection")

        val connectionRecords = JSONArray(data.get("records").toString())

        val mediatorConnectionObject: MediatorConnectionObject =
            WalletManager.getGson.fromJson(
                connectionRecords.getJSONObject(0).getString("value"),
                MediatorConnectionObject::class.java
            )
        mediatorConnectionObject.theirDid = theirDid
        mediatorConnectionObject.state = CONNECTION_RESPONSE

        val connectionUuid =
            connectionRecords.getJSONObject(0).getString("id")

        val value = WalletManager.getGson.toJson(mediatorConnectionObject)

        WalletMethods.updateWalletRecord(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            connectionUuid,
            value
        )

        val requestId = mediatorConnectionObject.requestId
        val myDid = mediatorConnectionObject.myDid
        val invitationKey = mediatorConnectionObject.invitationKey
        updateTag(requestId, myDid, invitationKey, connectionUuid, theirDid, publicKey, isMediator)
    }

    private fun updateTag(
        requestId: String?,
        myDid: String?,
        recipient: String?,
        connectionUuid: String,
        theirDid: String,
        publicKey: String,
        isMediator: Boolean
    ) {

        val tagJson = "{\n" +
                "  \"their_did\": \"$theirDid\",\n" +
                "  \"request_id\": \"$requestId\",\n" +
                "  \"my_did\": \"$myDid\",\n" +
                "  \"invitation_key\": \"$recipient\"\n" +
                "}"
        WalletRecord.updateTags(
            WalletManager.getWallet,
            if (isMediator) MEDIATOR_CONNECTION else CONNECTION,
            connectionUuid,
            tagJson
        )

        if (isMediator)
            createInbox(myDid, publicKey)
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
            DID_DOC,
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

            Log.d(TAG, "packed message: ${String(packedMessage)}")

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
                        llProgressBar.visibility = View.GONE
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
        publicKey: String
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

        Log.d(TAG, "packed message: ${String(packedMessage)}")

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
                    llProgressBar.visibility = View.GONE
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        Log.d(TAG, "unPackMessage: ${response.body()!!.byteStream()}")
                        val inputStream = response.body()!!.byteStream()
                        val str: String = IOUtils.toString(inputStream, "UTF-8")
                        Log.d(TAG, "onResponse: $str")

                        val unpacked =
                            Crypto.unpackMessage(WalletManager.getWallet, str.toByteArray())
                                .get()
                        Log.d(TAG, "packConnectionRequestMessage: ${String(unpacked)}")

                        val message = JSONObject(String(unpacked)).getString("message")

                        val inboxId = JSONObject(message).getString("InboxId")
                        val inboxKey = JSONObject(message).getString("InboxKey")
                        //inbox
                        updateRecordWithInboxDetails(inboxId, inboxKey)
                    }
                }
            })
    }

    private fun updateRecordWithInboxDetails(inboxId: String, inboxKey: String) {
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
                        mediatorConnectionObject.state = CONNECTION_ACTIVE

                        val connectionUuid =
                            searchResponse.records?.get(0)?.id

                        val value = WalletManager.getGson.toJson(mediatorConnectionObject)

                        WalletMethods.updateWalletRecord(
                            WalletManager.getWallet,
                            MEDIATOR_CONNECTION,
                            connectionUuid,
                            value
                        )

                        getMediatorConfig()
                    }
                }, MEDIATOR_CONNECTION,
                "{}"
            )
        } catch (e: Exception) {
        }
    }

    private fun saveConnectionInvitationRecord(
        connectionUuid: String,
        invitation: Invitation?,
        isMediator: Boolean
    ) {
        if (WalletManager.getWallet != null) {
            val tagJson = WalletManager.getGson.toJson(ConnectionId(connectionUuid))
            val connectionInvitationUuid = UUID.randomUUID().toString()

            Log.d(TAG, "saveRecord2: wallet value : $tagJson")
            Log.d(TAG, "saveRecord2: wallet UUID : $connectionInvitationUuid")

            WalletMethods.addWalletRecord(
                WalletManager.getWallet,
                if (isMediator) MEDIATOR_CONNECTION_INVITATION else REGISTRY_CONNECTION_INVITATION,
                connectionInvitationUuid,
                WalletManager.getGson.toJson(invitation),
                tagJson
            )

            updateConnectionRecord(connectionUuid, invitation, isMediator)
        }
    }

    private fun updateConnectionRecord(
        connectionUuid: String,
        invitation: Invitation?,
        isMediator: Boolean
    ) {
        if (WalletManager.getWallet != null) {
            val myDidResult =
                Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
            val myDid = myDidResult.did
//        val key = Did.keyForLocalDid(WalletManager.getWallet, myDid).get()

            Log.d(TAG, "updateConnectionRecord: $myDid")
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
                if (isMediator) MEDIATOR_CONNECTION else REGISTRY_CONNECTION,
                connectionUuid,
                value
            )

            updateRecord1Tag(
                requestId,
                myDid,
                invitation?.recipientKeys?.get(0),
                connectionUuid,
                isMediator,
                invitation?.serviceEndpoint
            )
        }
    }

    private fun updateRecord1Tag(
        requestId: String?,
        myDid: String?,
        recipient: String?,
        connectionUuid: String,
        isMediator: Boolean,
        serviceEndpoint: String?
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
                if (isMediator) MEDIATOR_CONNECTION else REGISTRY_CONNECTION,
                connectionUuid,
                tagJson
            )
            if (isMediator)
                getMediatorConfig()
            else
                getRegistryConfig()
        }
    }

    private fun createRoute(
        myDid: String?,
        recipient: String?,
        serviceEndpoint: String?
    ) {
        if (WalletManager.getWallet != null) {
            val messageUuid = UUID.randomUUID().toString()

            val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
            val metaObject = JSONObject(metaString)
            val key = metaObject.getString("verkey")

            val data = "{\n" +
                    "    \"@id\": \"$messageUuid\",\n" +
                    "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/basic-routing/1.0/add-route\",\n" +
                    "    \"routedestination\": \"$key\",\n" +
                    "    \"~transport\": {\n" +
                    "        \"return_route\": \"all\"\n" +
                    "    }\n" +
                    "}\n"

            val search = WalletSearch.open(
                WalletManager.getWallet,
                MEDIATOR_CONNECTION,
                "{}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val connection =
                WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

            WalletManager.closeSearchHandle(search)

            val connectionData = JSONObject(connection)
            Log.d(TAG, "getMediatorConfig: $connection")

            val connectionRecords = JSONArray(connectionData.get("records").toString())
            val connectionRecord =
                JSONObject(connectionRecords.getJSONObject(0).getString("value"))
            val connectionDid = connectionRecord.getString("my_did")

            val connectionMetaString =
                Did.getDidWithMeta(WalletManager.getWallet, connectionDid).get()
            val connectionMetaObject = JSONObject(connectionMetaString)
            val connectedKey = connectionMetaObject.getString("verkey")

            try {
                WalletSearchTask.searchWallet(
                    object : SearchHandler {
                        override fun taskCompleted(searchResponse: SearchResponse) {
                            val didDoc = searchResponse.records?.get(0)?.value
                            val didDocObj =
                                WalletManager.getGson.fromJson(didDoc, DidDoc::class.java)

                            val packedMessage = Crypto.packMessage(
                                WalletManager.getWallet,
                                "[\"${didDocObj.publicKey!![0].publicKeyBase58}\"]",
                                connectedKey,
                                data.toByteArray()
                            ).get()

                            val typedBytes: RequestBody = object : RequestBody() {
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
                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                        llProgressBar.visibility = View.GONE
                                    }

                                    override fun onResponse(
                                        call: Call<ResponseBody>,
                                        response: Response<ResponseBody>
                                    ) {
                                        if (response.code() == 200 && response.body() != null) {
                                            sendInvitation(
                                                serviceEndpoint,
                                                myDid,
                                                key,
                                                didDocObj.service!![0].routingKeys!![0],
                                                recipient
                                            )
                                        }
                                    }
                                })
                        }
                    }, MEDIATOR_DID_DOC,
                    "{}"
                )
            } catch (e: Exception) {
            }
        }
    }

    private fun sendInvitation(
        serviceEndpoint: String?,
        myDid: String?,
        newVKey: String,
        routingKey: String,
        recipient: String?
    ) {
        if (WalletManager.getWallet != null) {
            //public keys
            val publicKey = PublicKey()
            publicKey.id = "did:sov:$myDid#1"
            publicKey.type = "Ed25519VerificationKey2018"
            publicKey.controller = "did:sov:$myDid"
            publicKey.publicKeyBase58 = newVKey

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
            recipientsKey.add(newVKey)

            //service
            val routis: ArrayList<String> = ArrayList()
            routis.add(routingKey)

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

//         transport
            val transport = Transport("all")

            //connection request
            val connectionRequest = ConnectionRequest()
            connectionRequest.type =
                "${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/connections/1.0/request"
            connectionRequest.id = UUID.randomUUID().toString()
            connectionRequest.label = DeviceUtils.getDeviceName() ?: ""
            connectionRequest.connection = did
            connectionRequest.transport = transport

            val data = WalletManager.getGson.toJson(connectionRequest)

            val packedMessage = Crypto.packMessage(
                WalletManager.getWallet,
                "[\"$recipient\"]",
                newVKey,
                data.toByteArray()
            ).get()

            Log.d(TAG, "packed message: ${String(packedMessage)}")

            val typedBytes: RequestBody = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(packedMessage)
                }
            }

            ApiManager.api.getService()?.postData(serviceEndpoint ?: "", typedBytes)
                ?.enqueue(object : Callback<ConfigPostResponse> {
                    override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                        llProgressBar.visibility = View.GONE
                    }

                    override fun onResponse(
                        call: Call<ConfigPostResponse>,
                        response: Response<ConfigPostResponse>
                    ) {
                        if (response.code() == 200 && response.body() != null) {
                            unPackSigMessage(WalletManager.getGson.toJson(response.body()), false)
                        }
                    }
                })
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
        connectionObject.state = if (did != null) CONNECTION_REQUEST else CONNECTION_INVITATION

        return connectionObject
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGoHomeEvent(event: ReceiveExchangeRequestEvent) {
    }

    override fun extractUrlFunction(uri: Uri) {
        extractUrl(
            uri
        )
    }

    private var frag: UrlExtractFragment? = null

    private fun extractUrl(uri: Uri) {

        if (supportFragmentManager
                .findFragmentByTag("counter_fragment") != null
        )
            frag = supportFragmentManager
                .findFragmentByTag("counter_fragment") as UrlExtractFragment
        if (frag == null) {
            frag = UrlExtractFragment.newInstance()
            supportFragmentManager.beginTransaction().add(frag!!, "counter_fragment")
                .commit()
            frag?.setProgressListener(this)
        }

        if (frag != null)
            frag?.extractUrl(uri)

    }

    override fun updateProgress(progress: Int) {
        llProgressBar.visibility = progress
    }

    override fun error(explain: String) {
        llProgressBar.visibility = View.GONE
        Toast.makeText(this, explain, Toast.LENGTH_SHORT).show()
    }
}

interface ExtractListeners {
    fun extractUrlFunction(uri: Uri)
}