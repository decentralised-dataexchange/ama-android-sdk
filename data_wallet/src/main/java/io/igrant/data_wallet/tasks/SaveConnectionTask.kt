package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.connection.Protocol
import io.igrant.data_wallet.models.connectionRequest.*
import io.igrant.data_wallet.models.tagJsons.ConnectionId
import io.igrant.data_wallet.models.tagJsons.UpdateInvitationKey
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.WalletRecordType.Companion.MEDIATOR_DID_DOC
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object SaveConnectionTask {

    fun saveConnection(
        commonHandler: CommonHandler,
        invitation: Invitation,
        protocols: ArrayList<Protocol>?,
        myDid: String?,
        key: String?,
        orgId: String?,
        requestId: String?,
        location: String?,
        isDexaEnabled: Boolean
    ) {
        lateinit var connectionRequestTypedBytes: RequestBody
        lateinit var typedBytes: RequestBody
        val TAG = "SaveConnectionTask"

        commonHandler.taskStarted()

        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            handler.post {
                var connectionObject = setUpMediatorConnectionObject(
                    invitation,
                    requestId,
                    myDid
                )
                connectionObject.orgId = orgId
                connectionObject.location = location
                connectionObject.protocols = protocols
                connectionObject.isDexaEnabled = isDexaEnabled
                val value = WalletManager.getGson.toJson(
                    connectionObject
                )

                val connectionUuid = UUID.randomUUID().toString()

                var invitationKey =
                    UpdateInvitationKey(requestId, myDid, invitation.recipientKeys!![0], "", "")
                invitationKey.state = ConnectionStates.CONNECTION_INVITATION
                invitationKey.myKey = key
                invitationKey.orgId = orgId

                val connectionTagJson =
                    WalletManager.getGson.toJson(invitationKey)

                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.CONNECTION,
                    connectionUuid,
                    value,
                    connectionTagJson.toString()
                )

                val connectionInvitationTagJson =
                    WalletManager.getGson.toJson(ConnectionId(connectionUuid))
                val connectionInvitationUuid = UUID.randomUUID().toString()

                WalletMethods.addWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.CONNECTION_INVITATION,
                    connectionInvitationUuid,
                    WalletManager.getGson.toJson(invitation),
                    connectionInvitationTagJson
                )

                val messageUuid = UUID.randomUUID().toString()

                val data = "{\n" +
                        "    \"@id\": \"$messageUuid\",\n" +
                        "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/basic-routing/1.0/add-route\",\n" +
                        "    \"routedestination\": \"$key\",\n" +
                        "    \"~transport\": {\n" +
                        "        \"return_route\": \"all\"\n" +
                        "    }\n" +
                        "}\n"

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

                typedBytes = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(packedMessage)
                    }
                }

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
                recipientsKey.add(key ?: "")

                //service
                val routis: ArrayList<String> = ArrayList()
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

                val connectionRequestData = WalletManager.getGson.toJson(connectionRequest)

                val connectionRequestPackedMessage =
                    PackingUtils.packMessage(invitation, key ?: "", connectionRequestData, "")

                connectionRequestTypedBytes = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(connectionRequestPackedMessage)
                    }
                }

                commonHandler.onSaveConnection(
                    typedBytes,
                    connectionRequestTypedBytes
                )
            }
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
}