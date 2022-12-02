package io.igrant.data_wallet.activity

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.DataAgreementPolicyUtil.DATA_AGREEMENT_POLICY_RESULT
import io.igrant.data_wallet.adapter.ExchangeRequestAttributeAdapter
import io.igrant.data_wallet.adapter.ProposeAndExchangePagerAdapter
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.events.BlurEvent
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.handlers.PoolHandler
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.indy.PoolManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ViewPagerHeightListener
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.certificate.CredList
import io.igrant.data_wallet.models.certificateOffer.DataAgreementPolicy
import io.igrant.data_wallet.models.connection.ConnectionHistoryDetail
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.presentationExchange.*
import io.igrant.data_wallet.models.selfAttestedCredentials.AttributeTypes
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.tasks.ExchangeDataTask
import io.igrant.data_wallet.tasks.LoadLibIndyTask
import io.igrant.data_wallet.tasks.PoolTask
import io.igrant.data_wallet.tasks.OpenWalletTask
import io.igrant.data_wallet.utils.*
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.pool.Pool
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


class ExchangeDataActivity : BaseActivity(), ViewPagerHeightListener {

    private var connectionHistory: ConnectionHistoryDetail? = null
    private var notification: Notification? = null
    private var goToHome: Boolean = false
    private var connection: MediatorConnectionObject? = null
    private lateinit var mConnectionId: String
    private var record: Record? = null

    private var mPresentationExchange: PresentationExchange? = null

    private lateinit var clView: ConstraintLayout
    private lateinit var toolbar: Toolbar
    private lateinit var tvDesc: TextView
    private lateinit var tvHead: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var tvOrgName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var btAccept: Button
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var vpAttributes: ViewPager2
    private lateinit var llProgressBar: LinearLayout
    private lateinit var ivCard: ImageView
    private lateinit var tvMultipleData: TextView
    private lateinit var tvAgreement: TextView

    private lateinit var clAgreement: ConstraintLayout

    private var isDataAgreementAvailable = false

    private lateinit var viewPagerAdapter: ProposeAndExchangePagerAdapter

    private var requestedAttributes: HashMap<String, CredentialValue> = HashMap()

    private var selfAttestedAttributes: HashMap<String, String> = HashMap()

    private var isInsufficientData = false

    private var isFromSingle: Boolean = false

    private var exchangeDataList: ArrayList<ExchangeData> = ArrayList()

    private var mergedExchangeDataList: ArrayList<ArrayList<ExchangeData>> = ArrayList()

    companion object {
        private const val TAG = "ExchangeDataActivity"
        const val EXTRA_PRESENTATION_RECORD =
            "io.igrant.mobileagent.activty.ExchangeDataActivity.record"

        var isBlur: Boolean = true
    }

    override fun setHeight(height: Int) {
        val params = ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        params.height = height + 50 //left, top, right, bottom
        params.topToBottom = tvHead.id
        params.bottomToTop = dotsIndicator.id
        params.startToStart = tvDesc.id
        params.endToEnd = tvDesc.id
        params.setMargins(0, 30, 0, 30)
        vpAttributes.layoutParams = params
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_data)
        initViews()
        checkPool()
        initListener()
        getIntentData()
        setUpToolbar()
        initValues()
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
            }

            override fun taskStarted() {

            }
        })
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

    private fun checkExistanceOfRecord() {
        val searchResponse = SearchUtils.searchWallet(
            WalletRecordType.MESSAGE_RECORDS,
            "{\"certificateId\":\"${record?.id}\"}"
        )
        if (searchResponse.totalCount ?: 0 == 0) {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_delete, menu)
        return true
    }

    private fun initValues() {
        if (connection != null) {
            tvDesc.text =
                resources.getString(
                    R.string.connect_by_choosing_confirm_you_agree_to_the_requested_data_to_org_name_android,
                    connection?.theirLabel ?: resources.getString(R.string.connection_organisations)
                )
        }

        tvHead.text = (mPresentationExchange?.presentationRequest?.name ?: "").toUpperCase()

        Glide
            .with(ivLogo.context)
            .load(notification?.connection?.theirImageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        tvOrgName.text = notification?.connection?.theirLabel ?: ""
        tvLocation.text = notification?.connection?.location ?: ""

        btAccept.setBackgroundResource(if (notification?.stat.equals("Active")) R.drawable.primary_button_background else R.drawable.secondary_button_background)

        if (mPresentationExchange?.presentationRequest != null) {
            getValuesFromProofAndSelfAttestedAttributes(mPresentationExchange?.presentationRequest!!)
        }
    }

    private fun getValuesFromProofAndSelfAttestedAttributes(presentationRequest: PresentationRequest) {
        Logger.d(
            TAG,
            "getValuesFromProofAndSelfAttestedAttributes: ${
                WalletManager.getGson.toJson(presentationRequest)
            }"
        )
        if (WalletManager.getWallet != null) {
            // filter out the list(presentationRequest.requestedAttributes) with attribute "names" -> loop through the names and create a new entry in the map
            presentationRequest.requestedAttributes =
                ExchangeUtils.updateNamesWithName(presentationRequest.requestedAttributes)

            val searchHandle = CredentialsSearchForProofReq.open(
                WalletManager.getWallet,
                WalletManager.getGson.toJson(presentationRequest),
                "{}"
            ).get()

            selfAttestedAttributes = HashMap()
            requestedAttributes = HashMap()

            presentationRequest.requestedAttributes?.forEach { (key, value) ->

                // 1. Looping all the items in credential list
                val searchResult = searchHandle.fetchNextCredentials(key, 100).get()

                if (JSONArray(searchResult).length() > 0) {

                    val list = WalletManager.getGson.fromJson(searchResult, CredList::class.java)

                    list.map {
                        if (value.name != null && value.name != "") {
                            val data = if (it.credInfo?.attrs?.get(value.name ?: "") ?: " " != "")
                                it.credInfo?.attrs?.get(value.name ?: "") ?: " "
                            else " "

                            val exchangeData = ExchangeData()
                            exchangeData.key = key
                            exchangeData.value = value
                            exchangeData.name = value.name ?: ""
                            exchangeData.type = 0
                            exchangeData.credData = it
                            exchangeData.data = data
                            exchangeData.recordId = it.credInfo?.referent

                            exchangeDataList.add(exchangeData)

                        } else {
                            for (name in value.names ?: ArrayList()) {
                                val data = try {
                                    if (it.credInfo?.attrs?.get(name ?: "") ?: " " != "")
                                        it.credInfo?.attrs?.get(name ?: "") ?: ""
                                    else
                                        " "
                                } catch (e: Exception) {
                                    " "
                                }

                                if (data != "") {

                                    val exchangeData = ExchangeData()
                                    exchangeData.key = key
                                    exchangeData.value = value
                                    exchangeData.name = name ?: ""
                                    exchangeData.type = 0
                                    exchangeData.credData = it
                                    exchangeData.data = data
                                    exchangeData.recordId = it.credInfo?.referent

                                    exchangeDataList.add(exchangeData)

                                    break
                                }
                            }
                        }
                    }
                }

                // 2. Looping through all the data in self attested credentials
                if (value.name != null && (value.restrictions == null || value.restrictions!!.isEmpty())) {
                    var selfAttestedValue =
                        SelfAttestedDataSearch.searchSelfAttestedCredentialsForData(
                            value.name
                                ?: ""
                        )
                    for (item in selfAttestedValue) {

                        val exchangeData = ExchangeData()
                        exchangeData.key = key
                        exchangeData.value = value
                        exchangeData.name = value.name ?: ""
                        exchangeData.type = 1
                        exchangeData.data = if (item.attribute?.value ?: "" != "")
                            item.attribute?.value ?: ""
                        else " "
                        exchangeData.recordId = item.key
                        exchangeData.selfAttestedSubType = item.subtype
                        exchangeData.dataType = item.attribute?.type ?: AttributeTypes.STRING
                        exchangeData.imageType =
                            item.attribute?.imageType ?: AttributeTypes.BASE_64_DEFAULT
                        exchangeDataList.add(exchangeData)

                        selfAttestedAttributes[key] = item.attribute?.value ?: ""
                    }
                }
            }

            // 3. check if values for all keys present in wallet
            var list = exchangeDataList.distinctBy { it.key }
            if (list.size < (presentationRequest.requestedAttributes ?: HashMap()).size) {
                isInsufficientData = true
                // 4. add to list when specific key not present
                presentationRequest.requestedAttributes?.forEach { (key, value) ->
                    val tempList = exchangeDataList.filter { it.key == key }
                    if (tempList.isEmpty()) {
                        val exchangeData = ExchangeData()
                        exchangeData.key = key
                        exchangeData.value = value
                        exchangeData.name = value.name ?: ""
                        exchangeData.type = -1
                        exchangeData.data = "Nil"
                        exchangeData.recordId = ""
                        exchangeData.selfAttestedSubType = ""

                        exchangeDataList.add(exchangeData)
                    }

                }
            } else {
                isInsufficientData = false
            }

            // 4. Get unique record id
            list = exchangeDataList.distinctBy { it.recordId }

            // 5. Filter out the data from same cards
            list.map { exchangeData ->
                val tempList = exchangeDataList.filter { it.recordId == exchangeData.recordId }
                if (tempList.size == (presentationRequest.requestedAttributes ?: HashMap()).size)
                    mergedExchangeDataList.add(ArrayList(tempList))
            }

            // 6. Create the list for viewpager
            if (mergedExchangeDataList.size > 0) {
                setUpViewPager()
                isFromSingle = true
            } else {

                // 7. fetch first elements from the list with unique key
                isFromSingle = false
                list = exchangeDataList.distinctBy { it.key }
                mergedExchangeDataList.add(ArrayList(list))
                setUpViewPager()
            }

            searchHandle.closeSearch()
        }
    }

    /**
     * to create the requested attribute hash map and self attested attributes hash map
     */
    private fun buildAttributeList(requestedAttributes: Map<String, RequestedAttribute>?) {
        this.requestedAttributes = HashMap()
        this.selfAttestedAttributes = HashMap()

        var credentialValue = CredentialValue()

        requestedAttributes?.forEach { (key, value) ->
            val name = if (value.name != null) value.name
            else if (value.names != null && (value.names?.size ?: 0) > 0)
                value.names!![0]
            else ""
            val list = mergedExchangeDataList[vpAttributes.currentItem].filter {
                val tempName = if (it.value?.name != null) it.value!!.name
                else if (it.value?.names != null && (it.value!!.names?.size ?: 0) > 0)
                    it.value!!.names!![0]
                else ""

                tempName == name
            }

            if (list != null && list.isNotEmpty()) {
                if (list[0].type == 0) {
                    credentialValue = CredentialValue()
                    credentialValue.credId = list[0].recordId
                    credentialValue.revealed = true

                    this.requestedAttributes[key] = credentialValue
                } else {
                    this.selfAttestedAttributes[key] = list[0].data ?: ""
                }
            }
        }
    }

    private fun setUpViewPager() {
        viewPagerAdapter = ProposeAndExchangePagerAdapter(mergedExchangeDataList, isBlur, this)
        vpAttributes.adapter = viewPagerAdapter
        dotsIndicator.setViewPager2(vpAttributes)

        ivCard.visibility = if (mergedExchangeDataList.size < 2) View.INVISIBLE else View.VISIBLE
        tvMultipleData.visibility = if (mergedExchangeDataList.size < 2) View.GONE else View.VISIBLE
        dotsIndicator.visibility = if (mergedExchangeDataList.size < 2) View.GONE else View.VISIBLE
    }

    private fun getIntentData() {
        record = intent.extras!!.get(EXTRA_PRESENTATION_RECORD) as Record
        notification = WalletManager.getGson.fromJson(record!!.value, Notification::class.java)
        mPresentationExchange = notification?.presentation
        connection = notification?.connection
        mConnectionId = mPresentationExchange?.connectionId ?: ""
        checkExistanceOfRecord()

        DataHistory.callPurposeData(notification?.connection, notification?.presentation?.qrId
            ?: "", object : PurposeListener {
            override fun onSuccess(connectionHistoryDetail: ConnectionHistoryDetail) {
                connectionHistory = connectionHistoryDetail
                setUpDataAgreementViews()
                llProgressBar.visibility = View.GONE
            }

            override fun onFailure() {
                llProgressBar.visibility = View.GONE
                tvAgreement.setTextColor(
                    if (isDataAgreementAvailable) ContextCompat.getColor(
                        this@ExchangeDataActivity,
                        R.color.textColor
                    ) else ContextCompat.getColor(
                        this@ExchangeDataActivity,
                        R.color.textColorVeryLight
                    )
                )
            }
        })
    }

    private fun setUpDataAgreementViews() {
        if (connectionHistory != null) {
            isDataAgreementAvailable = connectionHistory?.purposeDetails?.purpose != null
        }

        tvAgreement.setTextColor(
            if (isDataAgreementAvailable) ContextCompat.getColor(
                this,
                R.color.textColor
            ) else ContextCompat.getColor(this, R.color.textColorVeryLight)
        )
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.general_data_agreement)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
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
                EventBus.getDefault().post(BlurEvent(isBlur))
                viewPagerAdapter.setBlurValue(isBlur)
                invalidateOptionsMenu()
            }
            R.id.action_delete -> {
                AlertDialog.Builder(this@ExchangeDataActivity)
                    .setTitle(resources.getString(R.string.general_app_title))
                    .setMessage(
                        resources.getString(
                            R.string.data_do_you_want_to_cancel_the_exchange_request
                        )
                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        R.string.general_yes,
                        DialogInterface.OnClickListener { dialog, which ->
                            if (WalletManager.getWallet != null) {
                                WalletRecord.delete(
                                    WalletManager.getWallet,
                                    WalletRecordType.MESSAGE_RECORDS,
                                    mPresentationExchange?.threadId ?: ""
                                ).get()

                                val credentialExchangeResponse =
                                    SearchUtils.searchWallet(
                                        WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                        "{\"thread_id\": \"${mPresentationExchange?.threadId}\"}"
                                    )

                                if (credentialExchangeResponse.totalCount ?: 0 > 0) {
                                    WalletRecord.delete(
                                        WalletManager.getWallet,
                                        WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                        "${credentialExchangeResponse.records?.get(0)?.id}"
                                    ).get()
                                }

                                EventBus.getDefault()
                                    .post(ReceiveExchangeRequestEvent())

                                onBackPressed()
                            }
                        }) // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(
                        R.string.general_no,
                        DialogInterface.OnClickListener { dialog, which ->

                        })
                    .show()


            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initViews() {
        clView = findViewById(R.id.clView)
        toolbar = findViewById(R.id.toolbar)
        tvDesc = findViewById(R.id.tvDesc)
        tvHead = findViewById(R.id.tvHead)
        btAccept = findViewById(R.id.btAccept)
        ivLogo = findViewById(R.id.ivLogo)
        tvOrgName = findViewById(R.id.tvOrgName)
        tvLocation = findViewById(R.id.tvLocation)
        llProgressBar = findViewById(R.id.llProgressBar)
        vpAttributes = findViewById(R.id.vpAttributes)
        dotsIndicator = findViewById(R.id.dotsIndicator)
        tvMultipleData = findViewById(R.id.tvMultipleData)
        tvAgreement = findViewById(R.id.tvAgreement)
        ivCard = findViewById(R.id.ivCard)

        clAgreement = findViewById(R.id.clAgreement)
    }

    private fun initListener() {

        clAgreement.setOnClickListener {
            if (isDataAgreementAvailable) {
                val intent = Intent(this, DataAgreementPolicyActivity::class.java)
                intent.putExtra(
                    DataAgreementPolicyActivity.EXTRA_POLICY_DETAILS,
                    DataAgreementPolicy(
                        connectionHistory?.purposeDetails?.purpose?.lawfulBasisOfProcessing
                            ?: "",
                        connectionHistory?.purposeDetails?.purpose?.policyUrl ?: "",
                        connectionHistory?.purposeDetails?.purpose?.jurisdiction ?: "",
                        connectionHistory?.purposeDetails?.purpose?.industryScope ?: "",
                        connectionHistory?.purposeDetails?.purpose?.restriction ?: "",
                        connectionHistory?.purposeDetails?.purpose?.shared3pp,
                        null
                    )
                )
                intent.putExtra(
                    DataAgreementPolicyActivity.EXTRA_IS_SIGNATURE_VERIFIED,
                    notification?.isSignatureVerified
                )
                startActivityForResult(intent, DATA_AGREEMENT_POLICY_RESULT)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.error_data_agreement_policy_not_available),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        ivCard.setOnClickListener {
            val exchangeData = mergedExchangeDataList[vpAttributes.currentItem][0]
            ShowCardUtil.showCard(exchangeData, this)
        }

        btAccept.setOnClickListener {
            if (WalletManager.getWallet != null) {
                buildAttributeList(mPresentationExchange?.presentationRequest?.requestedAttributes)
                if (notification?.stat.equals("Active")) {
                    if (!isInsufficientData) {
                        llProgressBar.visibility = View.VISIBLE
                        btAccept.isEnabled = false
//                btReject.isEnabled = false

                        ExchangeDataTask.exchangeData(
                            object : CommonHandler {
                                override fun taskStarted() {

                                }

                                override fun onExchangeDataComplete(
                                    serviceEndPoint: String?,
                                    typedBytes: RequestBody?
                                ) {
                                    if (typedBytes != null) {
                                        ApiManager.api.getService()
                                            ?.postDataWithoutData(
                                                serviceEndPoint ?: "",
                                                typedBytes
                                            )
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
                                                    if (response.code() == 200 && response.body() != null) {
                                                        llProgressBar.visibility = View.GONE
                                                        btAccept.isEnabled = true

                                                        WalletRecord.delete(
                                                            WalletManager.getWallet,
                                                            WalletRecordType.MESSAGE_RECORDS,
                                                            record?.id ?: ""
                                                        ).get()

                                                        val credentialExchangeResponse =
                                                            SearchUtils.searchWallet(
                                                                WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                                                "{\"thread_id\": \"${mPresentationExchange?.threadId}\"}"
                                                            )

                                                        if (credentialExchangeResponse.totalCount ?: 0 > 0) {
                                                            WalletRecord.delete(
                                                                WalletManager.getWallet,
                                                                WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                                                "${
                                                                    credentialExchangeResponse.records?.get(
                                                                        0
                                                                    )?.id
                                                                }"
                                                            ).get()
                                                        }

                                                        EventBus.getDefault()
                                                            .post(ReceiveExchangeRequestEvent())


                                                        MessageUtils.displaySnackbar(
                                                            clView!!, resources.getString(
                                                                R.string.data_data_has_been_shared_with_organization_successfully_android,
                                                                connection?.theirLabel ?: ""
                                                            )
                                                        )

                                                        DataHistory.saveDataShareHistory(
                                                            connectionHistory,
                                                            mPresentationExchange?.presentationRequest?.name
                                                                ?: "",
                                                            mergedExchangeDataList[vpAttributes.currentItem],
                                                            connection?.theirImageUrl
                                                        )
                                                        Handler(Looper.getMainLooper()).postDelayed(
                                                            {
                                                                finish()
                                                            },
                                                            1000
                                                        )

                                                        btAccept.visibility = View.GONE
                                                    }
                                                }
                                            })
                                    } else {
                                        Toast.makeText(
                                            this@ExchangeDataActivity,
                                            resources.getString(R.string.data_invalid_ledger_you_can_choose_proper_ledger_from_settings),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        llProgressBar.visibility = View.GONE
                                        btAccept.isEnabled = true
                                    }
                                }
                            },
                            mPresentationExchange,
                            requestedAttributes,
                            selfAttestedAttributes, null, record?.id, mConnectionId
                        )
                    } else {
                        Toast.makeText(
                            this,
                            resources.getString(R.string.connection_insufficient_data_requested),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                DATA_AGREEMENT_POLICY_RESULT -> {
                    updateCertificateWithSIgnatureVerified(
                        data?.getBooleanExtra(DataAgreementPolicyActivity.EXTRA_IS_SIGNATURE_VERIFIED, false) ?: false
                    )
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateCertificateWithSIgnatureVerified(isSigVerified: Boolean) {
        notification?.isSignatureVerified = isSigVerified
        WalletMethods.updateWalletRecord(
            WalletManager.getWallet,
            WalletRecordType.MESSAGE_RECORDS,
            record?.id,
            WalletManager.getGson.toJson(notification)
        )
        EventBus.getDefault()
            .post(ReceiveExchangeRequestEvent())
    }

    override fun onBackPressed() {
        if (goToHome) {
            val intent = Intent(this@ExchangeDataActivity, InitializeActivity::class.java)
            startActivity(intent)
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        isBlur = true
    }
}