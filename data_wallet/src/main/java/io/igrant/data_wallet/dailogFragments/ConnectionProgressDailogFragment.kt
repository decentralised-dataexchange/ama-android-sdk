package io.igrant.data_wallet.dailogFragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.kusu.loadingbutton.LoadingButton
import io.igrant.data_wallet.R
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.dailogFragments.BaseDialogFragment
import io.igrant.data_wallet.events.ConnectionSuccessEvent
import io.igrant.data_wallet.events.RefreshConnectionList
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
import io.igrant.data_wallet.tasks.SaveConnectionDetailInCertificateTask.saveConnection
import io.igrant.data_wallet.tasks.SaveConnectionTask
import io.igrant.data_wallet.tasks.SaveDidDocTask
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.ConnectionTypes.DEXA_CONNECTION
import io.igrant.data_wallet.utils.ConnectionTypes.IGRANT_ENABLED_CONNECTION
import io.igrant.data_wallet.utils.ConnectionTypes.V2_CONNECTION
import io.igrant.data_wallet.utils.DidCommPrefixUtils.PREFIX_1
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*

class ConnectionProgressDailogFragment : BaseDialogFragment() {

    private var mShowLoader: Boolean? = true
    private var protocols: ArrayList<Protocol>? = ArrayList()
    private var isFromExchange: Boolean = false
    private var isFromEbsi: Boolean = false

    private var requestId: String? = ""
    private lateinit var invitation: Invitation
    private lateinit var proposal: String
    lateinit var btnConnect: LoadingButton
    lateinit var ivClose: ImageView
    lateinit var tvDesc: TextView
    lateinit var ivLogo: ImageView
    lateinit var tvName: TextView
    lateinit var llSuccess: LinearLayout
    lateinit var clItem: ConstraintLayout
    lateinit var pbLoader: ProgressBar
    lateinit var clConnection: ConstraintLayout
    lateinit var ivSuccess: ImageView

    var myDid = ""
    var myKey = ""

    //    var isIGrantEnabled = false
//    var isDataController = false
    private var connectionType: String? = ""
    private var orgId = ""
    private var location = ""

    private var onSuccessListener: OnConnectionSuccess? = null

    private var isDexaEnabled = false
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dailog_fragment_connection_progress, container, false)
    }

    fun setListener(listener: OnConnectionSuccess) {
        onSuccessListener = listener
    }

    fun setLoader(showLoader: Boolean) {
        mShowLoader = showLoader
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFromExchange = requireArguments().getBoolean("isFromExchange", false)
        invitation = requireArguments().getSerializable("invitation") as Invitation
        proposal = requireArguments().getString("proposal", "")
        isFromEbsi = requireArguments().getBoolean("isFromEbsi",false)

        initViews(view)
        tvDesc.text = Html.fromHtml(
            resources.getString(
                R.string.connect_allow_org_name_to_connect_with_you_android,
                invitation.label ?: "Organisation"
            )
        )
        tvName.text = invitation.label ?: "Organisation"

        Glide
            .with(ivLogo.context)
            .load(invitation?.image_url ?: invitation?.imageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        if (!isFromEbsi) {
            val myDidResult =
                Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
            myDid = myDidResult.did
            myKey = myDidResult.verkey

            checkIfConnectionExisting()

        }else{
            pbLoader.visibility = View.GONE
            clConnection.visibility = View.VISIBLE
        }
        initListener(view)
    }

    /**
     * Function to check whether the connection is existing or not
     */
    private fun checkIfConnectionExisting() {

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
                }

                override fun onResponse(
                    call: Call<ConfigPostResponse>,
                    response: Response<ConfigPostResponse>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        connectionType = ""
                        val unpack =
                            Crypto.unpackMessage(
                                WalletManager.getWallet,
                                WalletManager.getGson.toJson(response.body()).toString()
                                    .toByteArray()
                            ).get()

                        val unPackedData =
                            WalletManager.getGson.fromJson(String(unpack), UnPackedData::class.java)
                        protocols = WalletManager.getGson.fromJson(
                            unPackedData.message,
                            DiscoverResponse::class.java
                        ).protocols

                        Log.d(TAG, "onResponse protocols: ${unPackedData.message}")
                        for (p in protocols ?: ArrayList()) {
                            if (p.pid.contains(
                                    "${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator",
                                    ignoreCase = true
                                )
                            ) {
                                connectionType = IGRANT_ENABLED_CONNECTION
                            }

                            if (p.pid.contains(
                                    "${DidCommPrefixUtils.getType(PREFIX_1)}/data-controller",
                                    ignoreCase = true
                                )
                            ) {
                                connectionType = V2_CONNECTION
                            }

                            if (p.pid.contains(
                                    "${DidCommPrefixUtils.getType(PREFIX_1)}/third-party-data-sharing",
                                    ignoreCase = true
                                )
                            ) {
                                isDexaEnabled = true
                            }
                        }
                    }
                    getOrganizationDetailsIfNeeded()
                }
            })
    }

    private fun getOrganizationDetailsIfNeeded() {
        when (connectionType) {
            V2_CONNECTION, DEXA_CONNECTION -> {
                requestId = UUID.randomUUID().toString()

                val orgData = if (!isDexaEnabled) WalletManager.getGson.toJson(
                    JsonLdProcessRequest(
                        type = "${DidCommPrefixUtils.getType(PREFIX_1)}/data-controller/1.0/details",
                        id = requestId,
                        from = WalletUtils.convertDidSovToMyDidWithMyData(myDid),
                        to = "did:mydata:zAqAMsVVgQcsH4keRwN5Po45nt",
                        createdTime = Date().time.toString(),
                        transport = Transport("all")
                    )
                ) else  WalletManager.getGson.toJson(
                    JsonLdProcessRequestV3(
                        type = "${DidCommPrefixUtils.getType(PREFIX_1)}/data-controller/1.0/details",
                        id = requestId,
                        transport = Transport("all")
                    )
                )

                val orgDetailPacked = PackingUtils.packMessage(invitation, myKey, orgData ?: "", "")

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
                            Logger.d(TAG, "Data controller enabled connection onFailure: ")
                            pbLoader.visibility = View.GONE
                            Toast.makeText(
                                context,
                                resources.getString(R.string.connection_unexpected_error_please_try_again),
                                Toast.LENGTH_SHORT
                            ).show()
                            dismiss()
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
                                    TAG,
                                    "Connection response: ${JSONObject(String(unpack)).getString("message")} "
                                )

                                val connectionData = WalletManager.getGson.fromJson(
                                    JSONObject(String(unpack)).getString("message"),
                                    ConnectionV2Response::class.java
                                )
                                checkExistingConnection(
                                    connectionData.body?.organisationId ?: connectionData.body?.organisationDid?:"",
                                    connectionData.body?.location ?: "",
                                    false
                                )

                            }
                        }
                    })
            }
            IGRANT_ENABLED_CONNECTION -> {
                requestId = UUID.randomUUID().toString()

                val orgData =
                    "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/organization-info\", \"@id\": \"$requestId\" , \"~transport\": {" +
                            "\"return_route\": \"all\"}\n}"

                val orgDetailPacked = PackingUtils.packMessage(invitation, myKey, orgData, "")

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
                            Logger.d(TAG, "Igrant enable connection onFailure: ")
                            pbLoader.visibility = View.GONE
                            Toast.makeText(
                                context,
                                resources.getString(R.string.connection_unexpected_error_please_try_again),
                                Toast.LENGTH_SHORT
                            ).show()
                            dismiss()
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
                                    connectionData.location ?: "",
                                    true
                                )
                            }
                        }
                    })
            }
            else -> {
                requestId = UUID.randomUUID().toString()
                pbLoader.visibility = View.GONE
                clConnection.visibility = View.VISIBLE
            }
        }
    }

    private fun checkExistingConnection(
        mOrgId: String,
        mLocation: String,
        isIgrantEnabled: Boolean
    ) {
        orgId = mOrgId
        location = mLocation

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
                sendDidToConnection(connectionObject.theirDid, isIgrantEnabled)

                if (!isFromExchange)
                    Toast.makeText(
                        context,
                        resources.getString(R.string.connection_connection_already_existing),
                        Toast.LENGTH_SHORT
                    ).show()

            } else {
                pbLoader.visibility = View.GONE
                clConnection.visibility = View.VISIBLE
            }
        } else {
            pbLoader.visibility = View.GONE
            clConnection.visibility = View.VISIBLE
        }
    }

    private fun sendDidToConnection(theirDid: String?, isIgrantEnabled: Boolean) {

        var data: String = ""
        if (isIgrantEnabled) {
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
                        type = "${DidCommPrefixUtils.getType(PREFIX_1)}/connections/1.0/exists",
                        from = WalletUtils.convertDidSovToMyDidWithMyData(myKey),
                        to = WalletUtils.convertDidSovToMyDidWithMyData(
                            didDocObj.service?.get(0)?.recipientKeys?.get(0) ?: ""
                        ),
                        createdTime = Date().time.toString(),
                        id = UUID.randomUUID().toString()
                    )
                )
            }
        }

        val orgDetailPacked = PackingUtils.packMessage(invitation, myKey, data, "")

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
                    dismiss()
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
                            onSuccessListener?.onExistingConnection(
                                connectionObject.requestId ?: ""
                            )
                            EventBus.getDefault()
                                .post(RefreshConnectionList())
                        }
                        dismiss()
                    }
                }

            })
    }

    private fun initListener(view: View) {

        ivClose.setOnClickListener {
            dialog?.dismiss()
        }

        btnConnect.setOnClickListener {
            btnConnect.isEnabled = false
            btnConnect.showLoading()

            if (isFromEbsi){
//                EbsiUtils.ebsiNaturalPersonOnBoarding(object: UrlExtractFragment.ProgressListener{
//                    override fun updateProgress(progress: Int) {
//                        CoroutineScope(Dispatchers.Main).launch {
//                            btnConnect.hideLoading()
//                            btnConnect.isEnabled = true
//                            llSuccess.visibility = View.VISIBLE
//                            Handler(Looper.getMainLooper()).postDelayed({
//                                onSuccessListener?.onEbsiConnectionSuccess(proposal)
//                                EventBus.getDefault()
//                                    .post(RefreshConnectionList())
//                                llSuccess.visibility = View.GONE
//                                dialog?.dismiss()
//                            }, 3000)
//                        }
//                    }
//
//                    override fun error(explain: String) {
//                        Toast.makeText(context,resources.getString(R.string.connection_unexpected_error_please_try_again),Toast.LENGTH_SHORT).show()
//                    }
//                })

            }else {
                SaveConnectionTask.saveConnection(object : CommonHandler {
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
                                                                                }
                                                                            })
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
                }, invitation, protocols, myDid, myKey, orgId, requestId, location, isDexaEnabled)
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ConnectionSuccessEvent) {
        btnConnect.hideLoading()
        btnConnect.isEnabled = true
        llSuccess.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({

//            GetConnectionDetailTask().execute(event.connectionId)
            onSuccessListener?.onSuccess(proposal, event.connectionId)
            EventBus.getDefault()
                .post(RefreshConnectionList())
            llSuccess.visibility = View.GONE
            dialog?.dismiss()
        }, 3000)

    }

    override fun onStart() {
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
        super.onStart()
    }

    override fun onStop() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
        super.onStop()
    }

    private fun initViews(view: View) {
        btnConnect = view.findViewById(R.id.btnConnect)
        ivClose = view.findViewById(R.id.ivClose)
        tvDesc = view.findViewById(R.id.tvDesc)
        ivLogo = view.findViewById(R.id.ivLogo)
        tvName = view.findViewById(R.id.tvName)
        llSuccess = view.findViewById(R.id.llSuccess)
        ivSuccess = view.findViewById(R.id.ivSuccess)
        pbLoader = view.findViewById(R.id.pbLoader)
        pbLoader.visibility = if (mShowLoader == true) View.VISIBLE else View.GONE
        clItem = view.findViewById(R.id.clItem)
        clConnection = view.findViewById(R.id.clConnection)
    }

    companion object {
        fun newInstance(
            isFromExchange: Boolean,
            invitation: Invitation?,
            proposal: String,
            isFromEbsi: Boolean? = false
        ): ConnectionProgressDailogFragment {
            val fragment = ConnectionProgressDailogFragment()
            val args = Bundle()
            args.putBoolean("isFromExchange", isFromExchange)
            args.putSerializable("invitation", invitation)
            args.putString("proposal", proposal)
            args.putBoolean("isFromEbsi", isFromEbsi ?: false)
            fragment.arguments = args
            return fragment
        }

        const val TAG = "CnctnPrgrsDlg"
    }

    interface OnConnectionSuccess {
        fun onSuccess(proposal: String, connectionId: String)
        fun onEbsiConnectionSuccess(data:String)
        fun onExistingConnection(connectionId: String) {}
    }

}