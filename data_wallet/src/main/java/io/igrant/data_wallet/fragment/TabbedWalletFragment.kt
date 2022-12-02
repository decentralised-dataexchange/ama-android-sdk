//package io.igrant.data_wallet.fragment
//
//import android.Manifest
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.net.Uri
//import android.os.Bundle
//import android.text.Editable
//import android.text.TextWatcher
//import android.util.Base64
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.*
//import androidx.constraintlayout.widget.ConstraintLayout
//import androidx.recyclerview.widget.RecyclerView
//import io.igrant.data_wallet.R
//import io.igrant.data_wallet.activity.CertificateDetailActivity
//import io.igrant.data_wallet.activity.ConnectionListActivity
//import io.igrant.data_wallet.activity.InitializeActivity
//import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity
//import io.igrant.data_wallet.adapter.WalletCertificatesAdapter
//import io.igrant.data_wallet.communication.ApiManager
//import io.igrant.data_wallet.events.ReceiveCertificateEvent
//import io.igrant.data_wallet.handlers.CommonHandler
//import io.igrant.data_wallet.handlers.PoolHandler
//import io.igrant.data_wallet.handlers.SearchHandler
//import io.igrant.data_wallet.indy.LedgerNetworkType
//import io.igrant.data_wallet.indy.PoolManager
//import io.igrant.data_wallet.indy.WalletManager
//import io.igrant.data_wallet.listeners.WalletListener
//import io.igrant.data_wallet.models.MediatorConnectionObject
//import io.igrant.data_wallet.models.agentConfig.ConfigResponse
//import io.igrant.data_wallet.models.agentConfig.Invitation
//import io.igrant.data_wallet.models.connectionRequest.*
//import io.igrant.data_wallet.models.qr.QrDecode
//import io.igrant.data_wallet.models.tagJsons.ConnectionId
//import io.igrant.data_wallet.models.tagJsons.ConnectionTags
//import io.igrant.data_wallet.models.tagJsons.UpdateInvitationKey
//import io.igrant.data_wallet.models.wallet.WalletModel
//import io.igrant.data_wallet.models.walletSearch.Record
//import io.igrant.data_wallet.models.walletSearch.SearchResponse
//import io.igrant.data_wallet.tasks.LoadLibIndyTask
//import io.igrant.data_wallet.tasks.OpenWalletTask
//import io.igrant.data_wallet.tasks.PoolTask
//import io.igrant.data_wallet.tasks.WalletSearchTask
//import io.igrant.data_wallet.utils.*
//import okhttp3.MediaType
//import okhttp3.MediaType.Companion.toMediaTypeOrNull
//import okhttp3.RequestBody
//import okhttp3.ResponseBody
//import okio.BufferedSink
//import org.apache.commons.io.IOUtils
//import org.greenrobot.eventbus.EventBus
//import org.greenrobot.eventbus.Subscribe
//import org.greenrobot.eventbus.ThreadMode
//import org.hyperledger.indy.sdk.anoncreds.Anoncreds
//import org.hyperledger.indy.sdk.crypto.Crypto
//import org.hyperledger.indy.sdk.did.Did
//import org.hyperledger.indy.sdk.non_secrets.WalletRecord
//import org.hyperledger.indy.sdk.non_secrets.WalletSearch
//import org.hyperledger.indy.sdk.pool.Pool
//import org.json.JSONArray
//import org.json.JSONObject
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import java.io.IOException
//import java.util.*
//import kotlin.collections.ArrayList
//
//class TabbedWalletFragment : BaseFragment() {
//
//    private lateinit var tvDataWallet: TextView
//    private lateinit var tvExchangeData: TextView
//    lateinit var etSearchWallet: EditText
//    lateinit var rvCertificates: RecyclerView
//    lateinit var llErrorMessage: LinearLayout
//    lateinit var ivAdd: ImageView
//    lateinit var llProgressBar: LinearLayout
//    private lateinit var clLoading: ConstraintLayout
//    private lateinit var tvLoadingStatus: TextView
//
//    lateinit var walletCertificateAdapter: WalletCertificatesAdapter
//
//    private var certificateList: ArrayList<Record> = ArrayList()
//    private var certificateListCopy: ArrayList<Record> = ArrayList()
//    override fun onCreateView(
//        inflater: LayoutInflater,
//        container: ViewGroup?,
//        savedInstanceState: Bundle?
//    ): View? {
//        return inflater.inflate(R.layout.fragment_wallet, container, false)
//    }
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        initViews(view)
//        initListener()
//
//        try {
//            EventBus.getDefault().register(this)
//        } catch (e: Exception) {
//        }
//    }
//
//    @Subscribe(threadMode = ThreadMode.MAIN)
//    fun onConnectionSuccessEvent(event: ReceiveCertificateEvent) {
//        setUpCertificateList()
//    }
//
//    override fun onDestroy() {
//        try {
//            EventBus.getDefault().unregister(this)
//        } catch (e: Exception) {
//        }
//        super.onDestroy()
//    }
//
//    private fun setUpCertificateList() {
//        val walletSearch = SearchUtils.searchWallet(WalletRecordType.WALLET, "{}")
//
//        certificateList.clear()
//        certificateList.addAll(walletSearch.records ?: ArrayList())
//
//        tvExchangeData.visibility = if (certificateList.size > 0) View.VISIBLE else View.GONE
//
//        certificateListCopy.clear()
//        certificateListCopy.addAll(walletSearch.records ?: ArrayList())
//        walletCertificateAdapter =
//            WalletCertificatesAdapter(certificateList, object : WalletListener {
//                override fun onDelete(id: String, position: Int) {
//                    try {
//                        Anoncreds.proverDeleteCredential(WalletManager.getWallet, id).get()
//                        WalletRecord.delete(WalletManager.getWallet, WalletRecordType.WALLET, id)
//                        walletCertificateAdapter.notifyItemRemoved(position)
//                        val walletSearch = SearchUtils.searchWallet(WalletRecordType.WALLET, "{}")
//                        certificateList.clear()
//                        certificateList.addAll(walletSearch.records ?: ArrayList())
//                        certificateListCopy.clear()
//                        certificateListCopy.addAll(walletSearch.records ?: ArrayList())
//                    } catch (e: Exception) {
//                    }
//                }
//
//                override fun onItemClick(wallet: WalletModel) {
//                    val intent = Intent(context, CertificateDetailActivity::class.java)
//                    val wal = WalletManager.getGson.toJson(wallet)
//                    intent.putExtra(CertificateDetailActivity.EXTRA_WALLET_DETAIL, wal)
//                    startActivity(intent)
//                }
//            })
//        rvCertificates.adapter = walletCertificateAdapter
//
//        if (certificateList.size > 0) {
//            llErrorMessage.visibility = View.GONE
//        } else {
//            llErrorMessage.visibility = View.VISIBLE
//        }
//
//    }
//
//    private fun initListener() {
//        ivAdd.setOnClickListener {
//            val intent = Intent(
//                context,
//                ConnectionListActivity::class.java
//            )
//            startActivity(intent)
//        }
//
//        tvExchangeData.setOnClickListener {
//
//            if (PermissionUtils.hasPermissions(
//                    requireActivity(),
//                    PERMISSIONS
//                )
//            ) {
////                val i = Intent(requireActivity(), QrCodeActivity::class.java)
////                startActivityForResult(
////                    i,
////                    REQUEST_CODE_SCAN_INVITATION
////                )
//            } else {
//                requestPermissions(PERMISSIONS, PICK_IMAGE_REQUEST)
//            }
//        }
//
//        etSearchWallet.addTextChangedListener(object : TextWatcher {
//            override fun afterTextChanged(s: Editable?) {
//            }
//
//            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
//            }
//
//            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
//                filterList(s)
//            }
//        })
//
//    }
//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
////            val i = Intent(requireActivity(), QrCodeActivity::class.java)
////            startActivityForResult(
////                i,
////                REQUEST_CODE_SCAN_INVITATION
////            )
//        }
//    }
//
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        if (requestCode == REQUEST_CODE_SCAN_INVITATION) {
//            if (data == null) return
//
//            try {
//
//                val uri: Uri = try {
//                    Uri.parse(data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult"))
//                } catch (e: Exception) {
//                    Uri.parse("igrant.io")
//                }
//
//                val v: String = uri.getQueryParameter("qr_p") ?: ""
//                if (v != "") {
//                    val json =
//                        Base64.decode(
//                            v,
//                            Base64.URL_SAFE
//                        ).toString(charset("UTF-8"))
//                    val data = JSONObject(json)
//                    if (data.getString("invitation_url") != "") {
//                        val invitation: String =
//                            Uri.parse(data.getString("invitation_url")).getQueryParameter("c_i")
//                                ?: ""
//                        val proofRequest = data.getJSONObject("proof_request")
//                        saveConnectionAndExchangeData(invitation, proofRequest, "")
//                    } else {
//                        Toast.makeText(
//                            context,
//                            resources.getString(R.string.connection_unexpected_error_please_try_again),
//                            Toast.LENGTH_SHORT
//                        ).show()
//                    }
//                } else {
//
//                    val bits: List<String> = uri.toString().split("/")
//
//                    val lastOne = bits[bits.size - 1]
//
//                    llProgressBar.visibility = View.VISIBLE
//
//                    ApiManager.api.getService()?.extractUrl(uri.toString())?.enqueue(object :
//                        Callback<QrDecode> {
//                        override fun onFailure(call: Call<QrDecode>, t: Throwable) {
//                            llProgressBar.visibility = View.GONE
//                            Toast.makeText(
//                                context,
//                                resources.getString(R.string.connection_unexpected_error_please_try_again),
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//
//                        override fun onResponse(
//                            call: Call<QrDecode>,
//                            response: Response<QrDecode>
//                        ) {
//                            llProgressBar.visibility = View.GONE
//                            if (response.code() == 200 && response.body() != null) {
//                                if (response.body()!!.dataExchangeUrl != null) {
//                                    //split with / and take the last element - to get qr_id
//                                    val uri: Uri = try {
//                                        Uri.parse(response.body()!!.dataExchangeUrl)
//                                    } catch (e: Exception) {
//                                        Uri.parse("igrant.io")
//                                    }
//                                    val v: String = uri.getQueryParameter("qr_p") ?: ""
//                                    if (v != "") {
//                                        val json =
//                                            Base64.decode(
//                                                v,
//                                                Base64.URL_SAFE
//                                            ).toString(charset("UTF-8"))
//                                        val data = JSONObject(json)
//                                        if (data.getString("invitation_url") != "") {
//                                            val invitation: String =
//                                                Uri.parse(data.getString("invitation_url"))
//                                                    .getQueryParameter("c_i")
//                                                    ?: ""
//                                            val proofRequest = data.getJSONObject("proof_request")
//                                            saveConnectionAndExchangeData(
//                                                invitation,
//                                                proofRequest,
//                                                lastOne
//                                            )
//                                        } else {
//                                            Toast.makeText(
//                                                context,
//                                                resources.getString(R.string.connection_unexpected_error_please_try_again),
//                                                Toast.LENGTH_SHORT
//                                            ).show()
//                                        }
//                                    } else {
//                                        Toast.makeText(
//                                            context,
//                                            resources.getString(R.string.connection_unexpected_error_please_try_again),
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                    }
//                                }
//                            }
//                        }
//                    })
//
////                    Toast.makeText(
////                        context,
////                        resources.getString(R.string.connection_unexpected_error_please_try_again),
////                        Toast.LENGTH_SHORT
////                    ).show()
//                }
//            } catch (e: Exception) {
//                Toast.makeText(
//                    context,
//                    resources.getString(R.string.connection_unexpected_error_please_try_again),
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//        super.onActivityResult(requestCode, resultCode, data)
//    }
//
//    private fun saveConnectionAndExchangeData(
//        data: String,
//        proofRequest: JSONObject,
//        qrId: String
//    ) {
//        var invitation: Invitation? = null
//        try {
//            val json =
//                Base64.decode(
//                    data,
//                    Base64.URL_SAFE
//                ).toString(charset("UTF-8"))
//
//            invitation = WalletManager.getGson.fromJson(json, Invitation::class.java)
//        } catch (e: Exception) {
//        }
//        if (invitation != null)
//            sendProposal(proofRequest, invitation, qrId)
//        else
//            Toast.makeText(
//                context,
//                resources.getString(R.string.connection_unexpected_error_please_try_again),
//                Toast.LENGTH_SHORT
//            ).show()
//    }
//
//    private fun sendProposal(
//        proofRequest: JSONObject,
//        invitation: Invitation,
//        qrId: String
//    ) {
//        val intent = Intent(requireContext(), ProposeAndExchangeDataActivity::class.java)
//        intent.putExtra(
//            ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_PROPOSAL,
//            proofRequest.toString()
//        )
//        intent.putExtra(ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_INVITATION, invitation)
//        intent.putExtra(ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_QR_ID, qrId)
//        startActivity(intent)
//    }
//
//    private fun filterList(s: CharSequence?) {
//        val tempList: ArrayList<Record> = ArrayList()
//        for (certificate in certificateListCopy) {
//            val walletModel =
//                WalletManager.getGson.fromJson(certificate.value, WalletModel::class.java)
//            val lst = walletModel.rawCredential?.schemaId?.split(":")
//            val text = lst?.get(2) ?: ""
//            if (text.contains(s ?: "", ignoreCase = true)) {
//                tempList.add(certificate)
//            }
//        }
//
//        certificateList.clear()
//        certificateList.addAll(tempList)
//
//        walletCertificateAdapter.notifyDataSetChanged()
//    }
//
//    private fun initViews(view: View) {
//        tvDataWallet = view.findViewById(R.id.tvDataWallet)
//        etSearchWallet = view.findViewById(R.id.etSearch)
//        rvCertificates = view.findViewById(R.id.rvCertificates)
//        llErrorMessage = view.findViewById(R.id.llErrorMessage)
//        ivAdd = view.findViewById(R.id.ivAdd)
//        llProgressBar = view.findViewById(R.id.llProgressBar)
//        tvExchangeData = view.findViewById(R.id.tvExchangeData)
//        clLoading = view.findViewById(R.id.clLoadingScreen)
//        tvLoadingStatus = view.findViewById(R.id.tvLoadingStatus)
//    }
//
//    companion object {
//        private const val TAG = "WalletFragment"
//        fun newInstance(): TabbedWalletFragment {
//            return TabbedWalletFragment()
//        }
//
//        private const val PICK_IMAGE_REQUEST = 101
//        val PERMISSIONS =
//            arrayOf(Manifest.permission.CAMERA)
//        private const val REQUEST_CODE_SCAN_INVITATION = 202
//    }
//
//    //mediator config and polling
//    private fun loadLibraryLogic() {
//        if (PoolManager.getPool == null) {
//            clLoading.visibility = View.VISIBLE
//            initLibIndy()
//        } else {
//            getMediatorConfig()
//        }
//    }
//
//    private fun initLibIndy() {
//        LoadLibIndyTask.loadIndyLibrary(object : CommonHandler {
//            override fun taskCompleted() {
//                loadPool()
//                tvLoadingStatus.text = resources.getString(R.string.connection_configuring_pool)
//            }
//
//            override fun taskStarted() {
//
//            }
//        }, activity!!.applicationContext)
//    }
//
//    private fun openWallet() {
//        OpenWalletTask.openWallet(object : CommonHandler {
//            override fun taskCompleted() {
//                getMediatorConfig()
//                tvLoadingStatus.text = resources.getString(R.string.connection_finishing)
//            }
//
//            override fun taskStarted() {
//
//            }
//        })
//    }
//
//    private fun loadPool() {
//        PoolTask.executePoolTask(object : PoolHandler {
//            override fun taskCompleted(pool: Pool) {
//                PoolManager.setPool(pool)
//                openWallet()
//                tvLoadingStatus.text = resources.getString(R.string.connection_finishing)
//            }
//
//            override fun taskStarted() {
//
//            }
//        }, LedgerNetworkType.getSelectedNetwork(requireContext()),requireContext())
//    }
//
//    private fun getMediatorConfig() {
//        try {
//            WalletSearchTask.searchWallet(
//                object : SearchHandler {
//                    override fun taskCompleted(searchResponse: SearchResponse) {
//                        if (searchResponse.totalCount == 0) {
//                            ApiManager.api.getService()?.getAgentConfig()
//                                ?.enqueue(object : Callback<ConfigResponse> {
//                                    override fun onFailure(
//                                        call: Call<ConfigResponse>,
//                                        t: Throwable
//                                    ) {
//                                        llProgressBar.visibility = View.GONE
//                                    }
//
//                                    override fun onResponse(
//                                        call: Call<ConfigResponse>,
//                                        response: Response<ConfigResponse>
//                                    ) {
//                                        if (response.code() == 200 && response.body() != null) {
//                                            saveConnectionRecord(response.body()!!.invitation)
//                                        }
//                                    }
//                                })
//                        } else {
//                            val connectionData =
//                                JSONObject(searchResponse.records?.get(0)?.value ?: "")
//
//                            when (connectionData.getString("state")) {
//                                ConnectionStates.CONNECTION_REQUEST, ConnectionStates.CONNECTION_INVITATION -> {
//                                    val myDid: String = connectionData.getString("my_did")
//                                    val requestId: String = connectionData.getString("request_id")
//
//                                    packConnectionRequestMessage(myDid, requestId)
//                                }
//                                ConnectionStates.CONNECTION_RESPONSE -> {
//                                    //GET DID DOC FROM RECORD FOR PUBLIC KEY
//                                    //CALL createInbox
//                                }
//                                ConnectionStates.CONNECTION_ACTIVE -> {
//                                    llProgressBar.visibility = View.GONE
//                                    val myDid: String = connectionData.getString("my_did")
////                                pollMessagesInThread(myDid)
//                                    setUpCertificateList()
//
//                                    clLoading.visibility = View.GONE
//                                }
//                            }
//                        }
//                    }
//
//                    override fun taskStarted() {
//
//                    }
//                },
//                WalletRecordType.MEDIATOR_CONNECTION,
//                "{}"
//            )
//        } catch (e: Exception) {
//        }
//    }
//
//    private fun packConnectionRequestMessage(myDid: String, requestId: String) {
//
//        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
//        val metaObject = JSONObject(metaString)
//        val key = metaObject.getString("verkey")
//
//        val search = WalletSearch.open(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION_INVITATION,
//            "{}",
//            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
//        ).get()
//
//        val value = WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()
//
//        WalletManager.closeSearchHandle(search)
//
//        val connectionInvitationData = JSONObject(value)
//
//        Log.d(InitializeActivity.TAG, "packConnectionRequestMessage0: $value")
//
//        //json object
//        val connectionInvitationRecords =
//            JSONArray(connectionInvitationData.get("records").toString())
//        val connectionInvitationRecord = connectionInvitationRecords.getJSONObject(0)
//        val connectionInvitationValue = JSONObject(connectionInvitationRecord.getString("value"))
//
//        //public keys
//        val publicKey = PublicKey()
//        publicKey.id = "did:sov:$myDid#1"
//        publicKey.type = "Ed25519VerificationKey2018"
//        publicKey.controller = "did:sov:$myDid"
//        publicKey.publicKeyBase58 = key
//
//        val publicKeys: ArrayList<PublicKey> = ArrayList()
//        publicKeys.add(publicKey)
//
//        //authentication
//        val authentication = Authentication()
//        authentication.type = "Ed25519SignatureAuthentication2018"
//        authentication.publicKey = "did:sov:$myDid#1"
//
//        val authentications: ArrayList<Authentication> = ArrayList()
//        authentications.add(authentication)
//
//        //service
//        val recipientsKey: ArrayList<String> = ArrayList()
//        recipientsKey.add(key)
//
//        val service = Service()
//        service.id = "did:sov:$myDid;indy"
//        service.type = "IndyAgent"
//        service.priority = 0
//        service.recipientKeys = recipientsKey
//        service.serviceEndpoint = ""
//
//        val services: ArrayList<Service> = ArrayList()
//        services.add(service)
//
//        //did doc
//        val didDoc = DidDoc()
//        didDoc.context = "https://w3id.org/did/v1"
//        didDoc.id = "did:sov:$myDid"
//        didDoc.publicKey = publicKeys
//        didDoc.authentication = authentications
//        didDoc.service = services
//
//        //did
//        val did = DID()
//        did.did = myDid
//        did.didDoc = didDoc
//
//        // transport
//        val transport = Transport()
//        transport.returnRoute = "all"
//
//        //connection request
//        val connectionRequest = ConnectionRequest()
//        connectionRequest.type =
//            "${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/connections/1.0/request"
//        connectionRequest.id = requestId
//        connectionRequest.label = "milan"
//        connectionRequest.connection = did
//        connectionRequest.transport = transport
//
//        val str = WalletManager.getGson.toJson(connectionRequest)
//
//        val packedMessage = PackingUtils.packMessage(
//            connectionInvitationValue.getString("recipientKeys"),
//            key,
//            str
//        )
//
//        val typedBytes: RequestBody = object : RequestBody() {
//            override fun contentType(): MediaType? {
//                return "application/ssi-agent-wire".toMediaTypeOrNull()
//            }
//
//            @Throws(IOException::class)
//            override fun writeTo(sink: BufferedSink) {
//                sink.write(packedMessage)
//            }
//        }
////
//        ApiManager.api.getService()?.postDetails(typedBytes)
//            ?.enqueue(object : Callback<ResponseBody> {
//                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                    llProgressBar.visibility = View.GONE
//                }
//
//                override fun onResponse(
//                    call: Call<ResponseBody>,
//                    response: Response<ResponseBody>
//                ) {
//                    if (response.code() == 200 && response.body() != null) {
//                        Log.d(
//                            InitializeActivity.TAG,
//                            "unPackMessage: ${response.body()!!.byteStream()}"
//                        )
//                        val inputStream = response.body()!!.byteStream()
//                        val str: String = IOUtils.toString(inputStream, "UTF-8")
//                        Log.d(InitializeActivity.TAG, "onResponse: $str")
//                        unPackSigMessage(str)
//                    }
//                }
//            })
//    }
//
//    private fun unPackSigMessage(body: String) {
//
//        Log.d(InitializeActivity.TAG, "unPackMessage: $body")
//        val unpacked = Crypto.unpackMessage(WalletManager.getWallet, body.toByteArray()).get()
//        Log.d(InitializeActivity.TAG, "packConnectionRequestMessage: ${String(unpacked)}")
//
//        val response = JSONObject(String(unpacked))
//
//        val message = JSONObject(response.get("message").toString())
//
//        val connectionSig = JSONObject(message.get("connection~sig").toString())
//        val sigData = connectionSig.get("sig_data").toString()
//        Log.d(
//            InitializeActivity.TAG,
//            "unPackMessage: decoded : ${
//                Base64.decode(sigData, Base64.URL_SAFE)
//                    .toString(charset("UTF-8"))
//            }"
//        )
//        val postion = Base64.decode(sigData, Base64.URL_SAFE)
//            .toString(charset("UTF-8")).indexOf("{")
//        Log.d(InitializeActivity.TAG, "unPackMessage: positon : $postion")
//        val data =
//            Base64.decode(sigData, Base64.URL_SAFE).toString(charset("UTF-8"))
//                .substring(postion)
//
//        saveDidDoc(data)
//    }
//
//    private fun saveDidDoc(data: String) {
//        Log.d(InitializeActivity.TAG, "saveDidDoc: $data")
//        val didData = JSONObject(data)
//        val didDoc = didData.getString("DIDDoc")
//        val theirDid = didData.getString("DID")
//
//        val didDocUuid = UUID.randomUUID().toString()
//
//        val tagJson = "{\"did\": \"$theirDid\"}"
//
//        WalletRecord.add(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_DID_DOC,
//            didDocUuid,
//            didDoc.toString(),
//            tagJson
//        )
//
//        val publicKey = JSONObject(didDoc).getJSONArray("publicKey").getJSONObject(0)
//            .getString("publicKeyBase58")
//        addDidKey(publicKey, theirDid)
//    }
//
//    private fun addDidKey(publicKey: String, theirDid: String) {
//
//        val didKeyUuid = UUID.randomUUID().toString()
//
//        val tagJson = "{\"did\": \"$theirDid\", \"key\": \"$publicKey\"}"
//
//        WalletRecord.add(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_DID_KEY,
//            didKeyUuid,
//            publicKey,
//            tagJson
//        )
//        updateRecord(publicKey, theirDid)
//    }
//
//    private fun updateRecord(publicKey: String, theirDid: String) {
//
//        val search = WalletSearch.open(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION,
//            "{}",
//            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
//        ).get()
//
//        val connection =
//            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()
//
//        WalletManager.closeSearchHandle(search)
//
//        val data = JSONObject(connection)
//        Log.d(InitializeActivity.TAG, "getMediatorConfig: $connection")
//
//        val connectionRecords = JSONArray(data.get("records").toString())
//
//        val mediatorConnectionObject: MediatorConnectionObject =
//            WalletManager.getGson.fromJson(
//                connectionRecords.getJSONObject(0).getString("value"),
//                MediatorConnectionObject::class.java
//            )
//        mediatorConnectionObject.theirDid = theirDid
//        mediatorConnectionObject.state = ConnectionStates.CONNECTION_RESPONSE
//
//        val connectionUuid =
//            connectionRecords.getJSONObject(0).getString("id")
//
//        val value = WalletManager.getGson.toJson(mediatorConnectionObject)
//
//        WalletRecord.updateValue(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION,
//            connectionUuid,
//            value
//        )
//
//        val requestId = mediatorConnectionObject.requestId
//        val myDid = mediatorConnectionObject.myDid
//        val invitationKey = mediatorConnectionObject.invitationKey
//        updateTag(requestId, myDid, invitationKey, connectionUuid, theirDid, publicKey)
//    }
//
//    private fun updateTag(
//        requestId: String?,
//        myDid: String?,
//        recipient: String?,
//        connectionUuid: String,
//        theirDid: String,
//        publicKey: String
//    ) {
//
//        val tagJson = "{\n" +
//                "  \"their_did\": \"$theirDid\",\n" +
//                "  \"request_id\": \"$requestId\",\n" +
//                "  \"my_did\": \"$myDid\",\n" +
//                "  \"invitation_key\": \"$recipient\"\n" +
//                "}"
//        WalletRecord.updateTags(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION,
//            connectionUuid,
//            tagJson
//        )
//
//
//        createInbox(myDid, publicKey)
//    }
//
//    private fun createInbox(
//        myDid: String?,
//        publicKey: String
//    ) {
//
//        val metaString = Did.getDidWithMeta(WalletManager.getWallet, myDid).get()
//        val metaObject = JSONObject(metaString)
//        val key = metaObject.getString("verkey")
//
//        val data = "\n" +
//                "{\n" +
//                "    \"@id\": \"${UUID.randomUUID().toString()}\",\n" +
//                "    \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.MEDIATOR)}/basic-routing/1.0/create-inbox\",\n" +
//                "    \"~transport\": {\n" +
//                "        \"return_route\": \"all\"\n" +
//                "    }\n" +
//                "}\n"
//
//
//        val packedMessage = Crypto.packMessage(
//            WalletManager.getWallet,
//            "[\"$publicKey\"]",
//            key,
//            data.toByteArray()
//        ).get()
//
//        Log.d(InitializeActivity.TAG, "packed message: ${String(packedMessage)}")
//
//        val typedBytes: RequestBody = object : RequestBody() {
//            override fun contentType(): MediaType? {
//                return "application/ssi-agent-wire".toMediaTypeOrNull()
//            }
//
//            @Throws(IOException::class)
//            override fun writeTo(sink: BufferedSink) {
//                sink.write(packedMessage)
//            }
//        }
//
//        ApiManager.api.getService()?.postDetails(typedBytes)
//            ?.enqueue(object : Callback<ResponseBody> {
//                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
//                    llProgressBar.visibility = View.GONE
//                }
//
//                override fun onResponse(
//                    call: Call<ResponseBody>,
//                    response: Response<ResponseBody>
//                ) {
//                    if (response.code() == 200 && response.body() != null) {
//                        Log.d(
//                            InitializeActivity.TAG,
//                            "unPackMessage: ${response.body()!!.byteStream()}"
//                        )
//                        val inputStream = response.body()!!.byteStream()
//                        val str: String = IOUtils.toString(inputStream, "UTF-8")
//                        Log.d(InitializeActivity.TAG, "onResponse: $str")
//
//                        val unpacked =
//                            Crypto.unpackMessage(WalletManager.getWallet, str.toByteArray())
//                                .get()
//                        Log.d(
//                            InitializeActivity.TAG,
//                            "packConnectionRequestMessage: ${String(unpacked)}"
//                        )
//
//                        val message = JSONObject(String(unpacked)).getString("message")
//
//                        val inboxId = JSONObject(message).getString("InboxId")
//                        val inboxKey = JSONObject(message).getString("InboxKey")
//                        //inbox
//                        updateRecordWithInboxDetails(inboxId, inboxKey)
//                    }
//                }
//            })
//    }
//
//    private fun updateRecordWithInboxDetails(inboxId: String, inboxKey: String) {
//        try {
//            WalletSearchTask.searchWallet(object : SearchHandler {
//                override fun taskCompleted(searchResponse: SearchResponse) {
//                    val mediatorConnectionObject: MediatorConnectionObject =
//                        WalletManager.getGson.fromJson(
//                            searchResponse.records?.get(0)?.value,
//                            MediatorConnectionObject::class.java
//                        )
//                    mediatorConnectionObject.inboxId = inboxId
//                    mediatorConnectionObject.inboxKey = inboxKey
//                    mediatorConnectionObject.state = ConnectionStates.CONNECTION_ACTIVE
//
//                    val connectionUuid =
//                        searchResponse.records?.get(0)?.id
//
//                    val value = WalletManager.getGson.toJson(mediatorConnectionObject)
//
//                    WalletRecord.updateValue(
//                        WalletManager.getWallet,
//                        WalletRecordType.MEDIATOR_CONNECTION,
//                        connectionUuid,
//                        value
//                    )
//
//                    getMediatorConfig()
//                }
//            },
//                WalletRecordType.MEDIATOR_CONNECTION,
//                "{}"
//            )
//        } catch (e: Exception) {
//        }
//    }
//
//    private fun saveConnectionRecord(invitation: Invitation?) {
//
//        val value =
//            WalletManager.getGson.toJson(setUpMediatorConnectionObject(invitation, null, null))
//        val connectionUuid = UUID.randomUUID().toString()
//
//        val connectionTag = ConnectionTags()
//        connectionTag.invitationKey = invitation?.recipientKeys!![0]
//        connectionTag.state = ConnectionStates.CONNECTION_INVITATION
//
//        val tagJson =
//            WalletManager.getGson.toJson(connectionTag)
//
//        WalletRecord.add(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION,
//            connectionUuid,
//            value.toString(),
//            tagJson.toString()
//        )
//
//        saveConnectionInvitationRecord(connectionUuid, invitation)
//    }
//
//    private fun saveConnectionInvitationRecord(
//        connectionUuid: String,
//        invitation: Invitation?
//    ) {
//        val tagJson = WalletManager.getGson.toJson(ConnectionId(connectionUuid))
//        val connectionInvitationUuid = UUID.randomUUID().toString()
//
//        Log.d(InitializeActivity.TAG, "saveRecord2: wallet value : $tagJson")
//        Log.d(InitializeActivity.TAG, "saveRecord2: wallet UUID : $connectionInvitationUuid")
//
//        WalletRecord.add(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION_INVITATION,
//            connectionInvitationUuid,
//            WalletManager.getGson.toJson(invitation),
//            tagJson
//        )
//
//        updateConnectionRecord(connectionUuid, invitation)
//    }
//
//    private fun updateConnectionRecord(
//        connectionUuid: String,
//        invitation: Invitation?
//    ) {
//
//        val myDidResult =
//            Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
//        val myDid = myDidResult.did
////        val key = Did.keyForLocalDid(WalletManager.getWallet, myDid).get()
//
//        Log.d(InitializeActivity.TAG, "DIDIDIDIDID:\n \n \n $myDid \n \n")
//        val requestId = UUID.randomUUID().toString()
//        val value = WalletManager.getGson.toJson(
//            setUpMediatorConnectionObject(
//                invitation,
//                requestId,
//                myDid
//            )
//        )
//
//        WalletRecord.updateValue(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION,
//            connectionUuid,
//            value
//        )
//
//        updateRecord1Tag(
//            requestId,
//            myDid,
//            invitation?.recipientKeys?.get(0),
//            connectionUuid,
//            invitation?.serviceEndpoint
//        )
//    }
//
//    private fun updateRecord1Tag(
//        requestId: String?,
//        myDid: String?,
//        recipient: String?,
//        connectionUuid: String,
//        serviceEndpoint: String?
//    ) {
//        val tagJson =
//            WalletManager.getGson.toJson(
//                UpdateInvitationKey(
//                    requestId,
//                    myDid,
//                    recipient,
//                    null,
//                    null
//                )
//            )
//        WalletRecord.updateTags(
//            WalletManager.getWallet,
//            WalletRecordType.MEDIATOR_CONNECTION,
//            connectionUuid,
//            tagJson
//        )
//        getMediatorConfig()
//    }
//
//    private fun setUpMediatorConnectionObject(
//        invitation: Invitation?,
//        requestId: String?,
//        did: String?
//    ): MediatorConnectionObject {
//        val connectionObject = MediatorConnectionObject()
//        connectionObject.theirLabel = invitation?.label ?: ""
//        connectionObject.theirImageUrl = invitation?.image_url ?: invitation?.imageUrl ?: ""
//        connectionObject.theirDid = ""
//        connectionObject.inboxId = ""
//        connectionObject.inboxKey = ""
//        connectionObject.requestId = requestId
//        connectionObject.myDid = did
//
//        if (invitation != null && !(invitation.recipientKeys.isNullOrEmpty()))
//            connectionObject.invitationKey = invitation.recipientKeys!![0]
//        else
//            connectionObject.invitationKey = ""
//
//        connectionObject.createdAt = "2020-10-22 12:20:23.188047Z"
//        connectionObject.updatedAt = "2020-10-22 12:20:23.188047Z"
//
//        connectionObject.theirLabel = invitation?.label
//        connectionObject.state =
//            if (did != null) ConnectionStates.CONNECTION_REQUEST else ConnectionStates.CONNECTION_INVITATION
//
//        return connectionObject
//    }
//}