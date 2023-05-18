package io.igrant.data_wallet.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import io.igrant.data_wallet.models.credentialExchange.Thread
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.DataAgreementPolicyActivity.Companion.EXTRA_IS_SIGNATURE_VERIFIED
import io.igrant.data_wallet.activity.DataAgreementPolicyUtil.DATA_AGREEMENT_POLICY_RESULT
import io.igrant.data_wallet.adapter.SectionAdapter
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.events.ReceiveCertificateEvent
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.handlers.PoolHandler
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.indy.PoolManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.certificateOffer.*
import io.igrant.data_wallet.models.connection.ConnectionHistoryDetail
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.credentialExchange.CredentialExchange
import io.igrant.data_wallet.models.credentialExchange.CredentialRequest
import io.igrant.data_wallet.models.credentialExchange.CredentialRequestMetadata
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.models.walletSearch.SearchResponse
import io.igrant.data_wallet.tasks.LoadLibIndyTask
import io.igrant.data_wallet.tasks.PoolTask
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.tasks.OpenWalletTask
import io.igrant.data_wallet.utils.CertificateListingUtils.mapToList
import io.igrant.data_wallet.utils.DataHistory.callPurposeData
import io.igrant.data_wallet.utils.DataHistory.saveDataShareHistory
import io.igrant.data_wallet.utils.SignOfferRequestListenersV3
import io.igrant.data_wallet.utils.SignOfferRequestV3
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.ledger.Ledger
import org.hyperledger.indy.sdk.ledger.LedgerResults
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import io.igrant.data_wallet.adapter.SectionAdapterV2
import io.igrant.data_wallet.models.reciept.Reciept
import io.igrant.data_wallet.utils.wrappers.CredentialTypes
import io.igrant.data_wallet.utils.wrappers.ReceiptWrapper
import io.igrant.data_wallet.utils.wrappers.ReceiptWrapper.getAttributesFromReceipt

class OfferCertificateActivity : BaseActivity() {

    private var sectionAdapterV2: SectionAdapterV2? = null
    private var notification: Notification? = null
    private var goToHome: Boolean = false
    private var name: String = ""
    private lateinit var mConnectionId: String
    private var mCertificateOffer: CertificateOffer? = null
    private var record: Record? = null

    private lateinit var toolbar: Toolbar

    private lateinit var btAccept: Button
    private lateinit var tvHead: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var tvOrgName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout
    private lateinit var tvAgreement: TextView
    private lateinit var clAgreement: ConstraintLayout

    private var isDataAgreementAvailable = false

    private var isBlur: Boolean = true

    private var adapter: SectionAdapter? = null

    private var isTempPreview = false

    companion object {
        const val EXTRA_CERTIFICATE_PREVIEW =
            "io.igrant.mobileagent.activty.OfferCertificateActivity.certificate"
        const val EXTRA_CERTIFICATE_TEMP_PREVIEW =
            "io.igrant.mobileagent.activty.OfferCertificateActivity.tempPreview"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offer_certificate)
        initViews()
        checkPool()
        initListener()
        getIntentData()
        initValues()
        setUpToolbar()
        setUpAdapter()
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        ivLogo = findViewById(R.id.ivLogo)
        tvOrgName = findViewById(R.id.tvOrgName)
        tvLocation = findViewById(R.id.tvLocation)
        btAccept = findViewById(R.id.btAccept)
        tvHead = findViewById(R.id.tvHead)
        rvAttributes = findViewById(R.id.rvAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
        tvAgreement = findViewById(R.id.tvAgreement)
        clAgreement = findViewById(R.id.clAgreement)
    }

    private fun checkPool() {
        if (PoolManager.getPool == null) {
            goToHome = true
            llProgressBar.visibility = View.VISIBLE
            initLibIndy()
        }
    }

    private fun initLibIndy() {
        LoadLibIndyTask.loadIndyLibrary(object : CommonHandler {
            override fun taskCompleted() {
                loadPool()
            }

            override fun taskStarted() {

            }
        }, applicationContext)
    }

    private fun openWallet() {
        OpenWalletTask.openWallet(object : CommonHandler {
            override fun taskCompleted() {
                llProgressBar.visibility = View.GONE
                checkExistanceOfRecord()
            }

            override fun taskStarted() {

            }
        })
    }

    private fun checkExistanceOfRecord() {
        try {
            val searchResponse = SearchUtils.searchWallet(
                if (isTempPreview) WalletRecordType.TEMP_MESSAGE_RECORDS else WalletRecordType.MESSAGE_RECORDS,
                "{\"certificateId\":\"${record?.id}\"}"
            )
            if ((searchResponse.totalCount ?: 0) == 0) {
                onBackPressed()
            }
        } catch (e: Exception) {
        }
    }

    private fun loadPool() {
        PoolTask.executePoolTask(object : PoolHandler {
            override fun taskCompleted(pool: Pool) {
                PoolManager.setPool(pool)
                openWallet()
            }

            override fun taskStarted() {

            }
        }, LedgerNetworkType.getSelectedNetwork(this), this)
    }

    private fun initValues() {
        Glide
            .with(ivLogo.context)
            .load(notification?.connection?.theirImageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        tvOrgName.text = notification?.connection?.theirLabel ?: ""
        tvLocation.text = notification?.connection?.location ?: ""

        btAccept.setBackgroundResource(if (notification?.stat.equals("Active")) R.drawable.primary_button_background else R.drawable.secondary_button_background)

//        if (notification?.type == MessageTypes.TYPE_EBSI_CREDENTIAL) {
//            tvHead.text =
//                notification?.ebsiCertificate?.searchableText?.toUpperCase(Locale.getDefault())
//        } else {
            tvHead.text = name.uppercase(Locale.getDefault())
            fetchPurposeDetail()
//        }
    }

    private fun fetchPurposeDetail() {
        //data agreement context will be available in new protocol, otherwise follow old method
        Log.d("milna", "fetchPurposeDetail: ${WalletManager.getGson.toJson(mCertificateOffer?.dataAgreementContext?.message?.body)}")
        if (mCertificateOffer?.dataAgreementContext != null) {
            setUpDataAgreementViews(
                if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(mCertificateOffer?.dataAgreementContext?.message?.body)) {
                    DataAgreementPolicy(
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mCertificateOffer?.dataAgreementContext?.message?.body).lawfulBasis
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mCertificateOffer?.dataAgreementContext?.message?.body).dataPolicy?.policyURL
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mCertificateOffer?.dataAgreementContext?.message?.body).dataPolicy?.jurisdiction
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mCertificateOffer?.dataAgreementContext?.message?.body).dataPolicy?.industrySector
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mCertificateOffer?.dataAgreementContext?.message?.body).dataPolicy?.geographicRestriction
                            ?: "",
                        null,
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mCertificateOffer?.dataAgreementContext?.message?.body).dataPolicy?.dataRetentionPeriod.toString()
                    )
                } else {
                    DataAgreementPolicy(
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mCertificateOffer?.dataAgreementContext?.message?.body)).lawfulBasis
                            ?: "",
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mCertificateOffer?.dataAgreementContext?.message?.body)).dataPolicy?.policyURL
                            ?: "",
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mCertificateOffer?.dataAgreementContext?.message?.body)).dataPolicy?.jurisdiction
                            ?: "",
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mCertificateOffer?.dataAgreementContext?.message?.body)).dataPolicy?.industrySector
                            ?: "",
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mCertificateOffer?.dataAgreementContext?.message?.body)).dataPolicy?.geographicRestriction
                            ?: "",
                        null,
                        (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mCertificateOffer?.dataAgreementContext?.message?.body)).dataPolicy?.dataRetentionPeriod.toString()
                    )
                }
            )
        } else {
            val credentialExchangeResponse =
                SearchUtils.searchWallet(
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "{\"thread_id\": \"${mCertificateOffer?.id}\"}"
                )

            var credentialExchangeData = CredentialExchange()
            if ((credentialExchangeResponse.totalCount ?: 0) > 0) {
                credentialExchangeData = WalletManager.getGson.fromJson(
                    credentialExchangeResponse.records?.get(0)?.value,
                    CredentialExchange::class.java
                )
            }

            val resultObject =
                SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION,
                    "{\n" +
                            "  \"request_id\":\"$mConnectionId\"\n" +
                            "}"
                )
            val connectionObject = WalletManager.getGson.fromJson(
                resultObject.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )

            val didDocObject =
                SearchUtils.searchWallet(
                    WalletRecordType.DID_DOC,
                    "{\n" +
                            "  \"did\":\"${connectionObject.theirDid}\"\n" +
                            "}"
                )

            val didDoc =
                WalletManager.getGson.fromJson(
                    didDocObject.records?.get(0)?.value,
                    DidDoc::class.java
                )

            callPurposeData(
                didDoc,
                credentialExchangeData.credentialOffer?.credDefId ?: "",
                object : PurposeListener {
                    override fun onSuccess(connectionHistory: ConnectionHistoryDetail) {
                        setUpDataAgreementViews(
                            if (connectionHistory.purposeDetails?.purpose != null) DataAgreementPolicy(
                                connectionHistory.purposeDetails?.purpose?.lawfulBasisOfProcessing
                                    ?: "",
                                connectionHistory.purposeDetails?.purpose?.policyUrl ?: "",
                                connectionHistory.purposeDetails?.purpose?.jurisdiction ?: "",
                                connectionHistory.purposeDetails?.purpose?.industryScope ?: "",
                                connectionHistory.purposeDetails?.purpose?.restriction ?: "",
                                connectionHistory.purposeDetails?.purpose?.shared3pp,
                                null
                            )
                            else null
                        )
                    }

                    override fun onFailure() {
                        setUpDataAgreementViews(null)
                    }
                })
        }
    }

    private fun setUpDataAgreementViews(connectionHistory: DataAgreementPolicy?) {
        isDataAgreementAvailable = connectionHistory != null

        tvAgreement.setTextColor(
            if (isDataAgreementAvailable) ContextCompat.getColor(
                this,
                R.color.textColor
            ) else ContextCompat.getColor(this, R.color.textColorVeryLight)
        )

        clAgreement.setOnClickListener {
            if (isDataAgreementAvailable) {
                DataAgreementPolicyUtil.showDataAgreementPolicy(
                    connectionHistory, mCertificateOffer?.dataAgreementContext,
                    mConnectionId, this, notification?.isSignatureVerified
                )
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.error_data_agreement_policy_not_available),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                DATA_AGREEMENT_POLICY_RESULT -> {
                    updateCertificateWithSIgnatureVerified(
                        data?.getBooleanExtra(EXTRA_IS_SIGNATURE_VERIFIED, false) ?: false
                    )
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateCertificateWithSIgnatureVerified(isSigVerified: Boolean) {
        notification?.isSignatureVerified = isSigVerified
        if (!isTempPreview) {
            WalletMethods.updateWalletRecord(
                WalletManager.getWallet,
                WalletRecordType.MESSAGE_RECORDS,
                record?.id,
                WalletManager.getGson.toJson(notification)
            )
            EventBus.getDefault()
                .post(ReceiveExchangeRequestEvent())
        }
    }

    private fun initListener() {
        btAccept.setOnClickListener {
            if (notification?.stat.equals("Active")) {
                llProgressBar.visibility = View.VISIBLE
                btAccept.isEnabled = false

                if (notification?.type == MessageTypes.TYPE_EBSI_CREDENTIAL) {
                    addEbsiToWallet()
                } else {
                    requestCertificate()
                }
            }
        }
    }

    private fun addEbsiToWallet() {
        val walletModelTag = "{" +
                "\"type\":\"${WalletRecordType.CERTIFICATE_TYPE_EBSI_CREDENTIAL}\"," +
                "\"sub_type\":\"${WalletRecordType.CERTIFICATE_TYPE_EBSI_CREDENTIAL}\"," +
                "\"connection_id\":\"\"," +
                "\"credential_id\":\"ebsi-${notification?.ebsiCertificate?.credentialId ?: ""}\"," +
                "\"schema_id\":\"\"" +
                "}"

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            WalletRecordType.WALLET,
            "ebsi-${notification?.ebsiCertificate?.credentialId ?: ""}",
            WalletManager.getGson.toJson(notification?.ebsiCertificate),
            walletModelTag
        )

        MessageUtils.displaySnackbar(
            clAgreement, resources.getString(
                R.string.ebsi_verifiable_id_added_successfully
            )
        )

//        val connectionList = SearchUtils.searchWallet(
//            WalletRecordType.CONNECTION,
//            "{\"orgId\":\"${EBSI_CONNECTION_NATURAL_PERSON}\"}"
//        )
//        if ((connectionList.totalCount ?: 0) > 0) {
//            val connection = WalletManager.getGson.fromJson(
//                connectionList.records?.get(0)?.value,
//                MediatorConnectionObject::class.java
//            )

        saveDataShareHistory(
            notification?.connection,
            null,
            null,
            null,
            notification?.ebsiCertificate
        )
//        }


        llProgressBar.visibility = View.GONE
        btAccept.isEnabled = false

        if (!isTempPreview) {
            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.MESSAGE_RECORDS,
                record?.id
            ).get()
        }

        EventBus.getDefault()
            .post(ReceiveExchangeRequestEvent())
        EventBus.getDefault().post(ReceiveCertificateEvent())

        Handler(Looper.getMainLooper()).postDelayed({
            onBackPressed()
        }, 2000)


    }

    private fun requestCertificate() {
        requestCertificateTask(object : RequestCertificateHandler {
            override fun taskCompleted(requestBody: RequestBody?, endPoint: String?) {

                if (requestBody == null) {
                    Toast.makeText(
                        this@OfferCertificateActivity,
                        resources.getString(R.string.data_invalid_ledger_you_can_choose_proper_ledger_from_settings),
                        Toast.LENGTH_SHORT
                    ).show()
                    llProgressBar.visibility = View.GONE
                    btAccept.isEnabled = true
                } else {
                    ApiManager.api.getService()
                        ?.postDataWithoutData(endPoint ?: "", requestBody)
                        ?.enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(
                                call: Call<ResponseBody>,
                                t: Throwable
                            ) {
                                llProgressBar.visibility = View.GONE
                                btAccept.isEnabled = true
                            }

                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                llProgressBar.visibility = View.GONE
                                btAccept.isEnabled = false


                                notification?.stat = "Processed"

                                WalletMethods.updateWalletRecord(
                                    WalletManager.getWallet,
                                    WalletRecordType.MESSAGE_RECORDS,
                                    record?.id ?: "",
                                    WalletManager.getGson.toJson(notification)
                                )

                                val tagJson = "{\n" +
                                        "  \"type\":\"${MessageTypes.OFFER_REQUEST}\",\n" +
                                        "  \"connectionId\":\"${mConnectionId}\",\n" +
                                        "  \"stat\":\"Processed\"\n" +
                                        "}"

                                WalletRecord.updateTags(
                                    WalletManager.getWallet,
                                    WalletRecordType.MESSAGE_RECORDS,
                                    record?.id ?: "",
                                    tagJson
                                )
                                EventBus.getDefault()
                                    .post(ReceiveExchangeRequestEvent())

                                onBackPressed()
                            }
                        })
                }
            }

            override fun taskStarted() {

            }
        }, mCertificateOffer!!, mConnectionId)
    }

    override fun onBackPressed() {
        if (goToHome) {
            val intent = Intent(this@OfferCertificateActivity, InitializeActivity::class.java)
            startActivity(intent)
        }
        if (isTempPreview) {
            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.TEMP_MESSAGE_RECORDS,
                record?.id
            ).get()
        }
        super.onBackPressed()
    }

    private fun setUpAdapter() {
//        val width = DisplayUtils.getScreenWidth() - TextUtils.convertDpToPixel(
//            60f,
//            rvAttributes.context
//        )
        if (ReceiptWrapper.checkCredentialType(
                mCertificateOffer!!.credentialPreview!!.attributes ?: ArrayList()
            ) == CredentialTypes.RECEIPT
        ) {
            val receipt = ReceiptWrapper.convertReceipt(
                mCertificateOffer!!.credentialPreview!!.attributes ?: ArrayList()
            )
            if (receipt == null) {
                setupV1Adapter()
            } else {
                sectionAdapterV2 = SectionAdapterV2(
                    getAttributesFromReceipt(receipt),
                    isBlur,
                    ReceiptWrapper.getSections(receipt)
                )
                rvAttributes.layoutManager = LinearLayoutManager(this)
                rvAttributes.adapter = sectionAdapterV2
            }
        } else {
            setupV1Adapter()
        }

    }

    fun setupV1Adapter() {
        adapter = SectionAdapter(
            if (notification?.type == MessageTypes.TYPE_EBSI_CREDENTIAL)
                mapToList(
                    notification?.ebsiCertificate?.attributes ?: mapOf(),
                    notification?.ebsiCertificate?.sectionStruct
                )
            else
                arrayListOf(mCertificateOffer!!.credentialPreview!!.attributes ?: ArrayList()),
            isBlur,
            if (notification?.type == MessageTypes.TYPE_EBSI_CREDENTIAL)
                notification!!.ebsiCertificate?.sectionStruct
            else ArrayList()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.connection_data_agreement)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        invalidateOptionsMenu()
        menu.findItem(R.id.action_visible)
            .setIcon(if (isBlur) R.drawable.ic_visible else R.drawable.ic_invisible)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }

            R.id.action_visible -> {
                isBlur = !isBlur
                adapter?.setUpBlur(isBlur)
                sectionAdapterV2?.setUpBlur(isBlur)
                invalidateOptionsMenu()
            }

            R.id.action_delete -> {

                AlertDialog.Builder(this@OfferCertificateActivity)
                    .setTitle(resources.getString(R.string.general_app_title))
                    .setMessage(
                        resources.getString(
                            R.string.data_do_you_want_to_remove_the_offer_credential
                        )
                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        R.string.general_yes
                    ) { dialog, which ->
                        try {
                            WalletRecord.delete(
                                WalletManager.getWallet,
                                if (isTempPreview) WalletRecordType.TEMP_MESSAGE_RECORDS else WalletRecordType.MESSAGE_RECORDS,
                                record?.id
                            ).get()

                            val credentialExchangeResponse =
                                SearchUtils.searchWallet(
                                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                    "{\"thread_id\": \"${mCertificateOffer?.id}\"}"
                                )

                            if ((credentialExchangeResponse.totalCount ?: 0) > 0) {
                                WalletRecord.delete(
                                    WalletManager.getWallet,
                                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                    "${credentialExchangeResponse.records?.get(0)?.id}"
                                ).get()
                            }

                            EventBus.getDefault()
                                .post(ReceiveExchangeRequestEvent())

                            onBackPressed()
                        } catch (e: Exception) {
                        }
                    } // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(
                        R.string.general_no
                    ) { dialog, which ->

                    }
                    .show()
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getIntentData() {
        if (intent.hasExtra(EXTRA_CERTIFICATE_TEMP_PREVIEW))
            isTempPreview = intent.getBooleanExtra(EXTRA_CERTIFICATE_TEMP_PREVIEW, false)
        record = intent.extras!!.get(EXTRA_CERTIFICATE_PREVIEW) as Record

        notification = WalletManager.getGson.fromJson(record!!.value, Notification::class.java)
        name = notification?.presentation?.presentationRequest?.name
            ?: (notification?.certificateOffer?.comment ?: "")
        mCertificateOffer = notification?.certificateOffer
        mConnectionId = notification?.connection?.requestId ?: ""
        checkExistanceOfRecord()
    }

    fun requestCertificateTask(
        commonHandler: RequestCertificateHandler,
        mCertificateOffer: CertificateOffer,
        mConnectionId: String
    ) {
        var serviceEndPoint: String? = null
        var typedBytes: RequestBody? = null

        commonHandler.taskStarted()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            handler.post {
                val credentialExchangeResponse =
                    SearchUtils.searchWallet(
                        WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                        "{\"thread_id\": \"${mCertificateOffer.id}\"}"
                    )

                var credentialExchangeData = CredentialExchange()
                if ((credentialExchangeResponse.totalCount ?: 0) > 0) {
                    credentialExchangeData = WalletManager.getGson.fromJson(
                        credentialExchangeResponse.records?.get(0)?.value,
                        CredentialExchange::class.java
                    )
                }

                Pool.setProtocolVersion(2)

                val credDef =
                    Ledger.buildGetCredDefRequest(
                        null,
                        credentialExchangeData.credentialOffer?.credDefId ?: ""
                    ).get()

                val credDefResponse = Ledger.submitRequest(PoolManager.getPool, credDef).get()

                try {
                    val parsedCredDefResponse =
                        Ledger.parseGetCredDefResponse(credDefResponse).get()

                    val resultObject =
                        SearchUtils.searchWallet(
                            WalletRecordType.CONNECTION,
                            "{\n" +
                                    "  \"request_id\":\"$mConnectionId\"\n" +
                                    "}"
                        )
                    val connectionObject = WalletManager.getGson.fromJson(
                        resultObject.records?.get(0)?.value,
                        MediatorConnectionObject::class.java
                    )
                    val proverDid = connectionObject.myDid

                    val metaString =
                        Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
                    val metaObject = JSONObject(metaString)
                    val publicKey = metaObject.getString("verkey")

                    val didDocObject =
                        SearchUtils.searchWallet(
                            WalletRecordType.DID_DOC,
                            "{\n" +
                                    "  \"did\":\"${connectionObject.theirDid}\"\n" +
                                    "}"
                        )

                    val didDoc =
                        WalletManager.getGson.fromJson(
                            didDocObject.records?.get(0)?.value,
                            DidDoc::class.java
                        )

                    serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""

                    Log.d("OfferIssue", "serviceEndPoint: $serviceEndPoint")

                    if (mCertificateOffer.dataAgreementContext != null) {
                        if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(
                                mCertificateOffer.dataAgreementContext?.message?.body)){
                            SignOfferRequestV3.signRequestV3(
                                mCertificateOffer.dataAgreementContext,
                                mConnectionId,
                                object : SignOfferRequestListenersV3 {
                                    override fun onSignRequestSuccess(dataAgreementContext: DataAgreementContext?,
                                                                      updatedContext:DataAgreementContext?) {
                                        super.onSignRequestSuccess(dataAgreementContext,updatedContext)
                                        mCertificateOffer.dataAgreementContext = dataAgreementContext

                                        typedBytes = createOfferRequest(
                                            proverDid,
                                            credentialExchangeData,
                                            publicKey, didDoc,
                                            parsedCredDefResponse,
                                            credentialExchangeResponse,
                                            mCertificateOffer,
                                            connectionObject.isDexaEnabled
                                        )

                                        mCertificateOffer.dataAgreementContext = updatedContext
                                        notification?.certificateOffer = mCertificateOffer

                                        WalletMethods.updateWalletRecord(
                                            WalletManager.getWallet,
                                            WalletRecordType.MESSAGE_RECORDS,
                                            record?.id ?: "",
                                            WalletManager.getGson.toJson(notification)
                                        )

                                        commonHandler.taskCompleted(typedBytes, serviceEndPoint)
                                    }

                                    override fun onSignOfferRequestFailure() {
                                        super.onSignOfferRequestFailure()
                                        commonHandler.taskCompleted(typedBytes, serviceEndPoint)
                                    }
                                }, false
                            )
                        }else{
                            SignOfferRequest.signRequest(
                                mCertificateOffer.dataAgreementContext,
                                mConnectionId,
                                object : SignOfferRequestListeners {
                                    override fun onSignRequestSuccess(certificateOffer: DataAgreementContext?) {
                                        super.onSignRequestSuccess(certificateOffer)
                                        mCertificateOffer.dataAgreementContext = certificateOffer
                                        notification?.certificateOffer = mCertificateOffer
                                        WalletMethods.updateWalletRecord(
                                            WalletManager.getWallet,
                                            WalletRecordType.MESSAGE_RECORDS,
                                            record?.id ?: "",
                                            WalletManager.getGson.toJson(notification)
                                        )

                                        typedBytes = createOfferRequest(
                                            proverDid,
                                            credentialExchangeData,
                                            publicKey, didDoc,
                                            parsedCredDefResponse,
                                            credentialExchangeResponse,
                                            mCertificateOffer,
                                            connectionObject.isDexaEnabled
                                        )
                                        commonHandler.taskCompleted(typedBytes, serviceEndPoint)
                                    }

                                    override fun onSignOfferRequestFailure() {
                                        super.onSignOfferRequestFailure()
                                        commonHandler.taskCompleted(typedBytes, serviceEndPoint)
                                    }
                                }, false
                            )
                        }

                    } else {
                        typedBytes = createOfferRequest(
                            proverDid,
                            credentialExchangeData,
                            publicKey, didDoc,
                            parsedCredDefResponse,
                            credentialExchangeResponse,
                            mCertificateOffer,
                            connectionObject.isDexaEnabled
                        )
                        commonHandler.taskCompleted(typedBytes, serviceEndPoint)
                    }

                } catch (e: Exception) {
                    commonHandler.taskCompleted(typedBytes, serviceEndPoint)
                }

            }
        }

    }

    private fun createOfferRequest(
        proverDid: String?,
        credentialExchangeData: CredentialExchange,
        publicKey: String,
        didDoc: DidDoc,
        parsedCredDefResponse: LedgerResults.ParseResponseResult,
        credentialExchangeResponse: SearchResponse,
        mCertificateOffer: CertificateOffer,
        dexaEnabled: Boolean?
    ): RequestBody? {
        //get cred offer json:
        //offer credential base64 parameter value decoded value
        val credOfferJson = Base64.decode(
            mCertificateOffer.offersAttach?.get(0)?.data?.base64,
            Base64.URL_SAFE
        ).toString(charset("UTF-8"))

        Log.d("OfferIssue", "credOfferJson: $credOfferJson")
        val proverResponse = Anoncreds.proverCreateCredentialReq(
            WalletManager.getWallet,
            proverDid,
            credOfferJson,
            parsedCredDefResponse.objectJson,
            "IGrantMobileAgent-000001"
        ).get()

        val credentialRequest =
            WalletManager.getGson.fromJson(
                proverResponse.credentialRequestJson,
                CredentialRequest::class.java
            )
        val credentialRequestMetaData =
            WalletManager.getGson.fromJson(
                proverResponse.credentialRequestMetadataJson,
                CredentialRequestMetadata::class.java
            )
        credentialExchangeData.state = CredentialExchangeStates.CREDENTIAL_REQUEST_SENT
        credentialExchangeData.credentialRequest = credentialRequest
        credentialExchangeData.credentialRequestMetadata = credentialRequestMetaData

        WalletMethods.updateWalletRecord(
            WalletManager.getWallet,
            WalletRecordType.CREDENTIAL_EXCHANGE_V10,
            "${credentialExchangeResponse.records?.get(0)?.id}",
            WalletManager.getGson.toJson(credentialExchangeData)
        )

        //creating model for sending
        val thread = Thread()
        thread.thid = mCertificateOffer.id ?: ""

        val v = Base64.encodeToString(
            proverResponse.credentialRequestJson.toByteArray(),
            Base64.NO_WRAP
        )
        v.replace("\\n", "")
        val offerData = OfferData()
        offerData.base64 = v

        val requestAttach = OfferAttach()
        requestAttach.id = "libindy-cred-request-0"
        requestAttach.mimeType = "application/json"
        requestAttach.data = offerData
        thread.thid = mCertificateOffer.id ?: ""

        val requestAttachList = ArrayList<OfferAttach>()
        requestAttachList.add(requestAttach)


        val packedMessage = if (dexaEnabled == true) {
            val certificateOffer = RequestOfferV3()
            certificateOffer.type =
                "${DidCommPrefixUtils.getType(mCertificateOffer.type ?: "")}/issue-credential/1.0/request-credential"
            certificateOffer.id = UUID.randomUUID().toString()
            certificateOffer.thread = thread
            if (mCertificateOffer.dataAgreementContext != null)
                certificateOffer.dataAgreementContext =
                    SignOfferRequestV3.buildCertificateRequestV3(
                        mCertificateOffer.dataAgreementContext,
                        publicKey,
                        didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
                    )
            certificateOffer.offersAttach = requestAttachList

            PackingUtils.packMessage(
                didDoc, publicKey,
                WalletManager.getGson.toJson(certificateOffer),
                mCertificateOffer.type ?: ""
            )
        } else {
            val certificateOffer = RequestOffer()
            certificateOffer.type =
                "${DidCommPrefixUtils.getType(mCertificateOffer.type ?: "")}/issue-credential/1.0/request-credential"
            certificateOffer.id = UUID.randomUUID().toString()
            certificateOffer.thread = thread
            if (mCertificateOffer.dataAgreementContext != null)
                certificateOffer.dataAgreementContext =
                    SignOfferRequest.buildCertificateRequest(
                        mCertificateOffer.dataAgreementContext,
                        publicKey,
                        didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""
                    )
            certificateOffer.offersAttach = requestAttachList

            PackingUtils.packMessage(
                didDoc, publicKey,
                WalletManager.getGson.toJson(certificateOffer),
                mCertificateOffer.type ?: ""
            )
        }

        val typedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(packedMessage)
            }
        }

        return typedBytes
    }

    interface RequestCertificateHandler {
        fun taskCompleted(requestBody: RequestBody?, endPoint: String?)
        fun taskStarted()
    }
}