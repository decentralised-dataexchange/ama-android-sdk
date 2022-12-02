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
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import io.igrant.data_wallet.R
import io.igrant.data_wallet.adapter.ProposeAndExchangePagerAdapter
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.data_wallet.events.BlurEvent
import io.igrant.data_wallet.events.GoHomeEvent
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ViewPagerHeightListener
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.certificate.CredList
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.certificateOffer.DataAgreementPolicy
import io.igrant.data_wallet.models.connection.ConnectionHistoryDetail
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.presentationExchange.*
import io.igrant.data_wallet.models.selfAttestedCredentials.AttributeTypes
import io.igrant.data_wallet.tasks.ExchangeDataTask
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.DataHistory.callPurposeData
import io.igrant.data_wallet.utils.DataHistory.saveDataShareHistory
import io.igrant.data_wallet.utils.ExchangeUtils.updateNamesWithName
import io.igrant.data_wallet.utils.SelfAttestedDataSearch.searchSelfAttestedCredentialsForData
import io.igrant.data_wallet.utils.ShowCardUtil.showCard
import io.igrant.data_wallet.utils.SignOfferRequestListenersV3
import io.igrant.data_wallet.utils.SignOfferRequestV3.signRequestV3
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.anoncreds.CredentialsSearchForProofReq
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ProposeAndExchangeDataActivity : BaseActivity(),
    ConnectionProgressDailogFragment.OnConnectionSuccess, ViewPagerHeightListener {

    private var connectionHistory: ConnectionHistoryDetail? = null
    private lateinit var viewPagerAdapter: ProposeAndExchangePagerAdapter
    private var qrId: String? = ""
    private var mConnectionId: String = ""
    private var proposalData: PresentationRequest? = null
    private var dataAgreementContext: DataAgreementContext? = null
    private lateinit var invitation: Invitation
    private lateinit var proposal: String

    private var recordId: String? = null

    //views
    private lateinit var clView: ConstraintLayout
    private lateinit var toolbar: Toolbar
    private lateinit var tvDesc: TextView
    private lateinit var tvHead: TextView
    private lateinit var ivLogo: ImageView
    private lateinit var tvOrgName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvMultipleData: TextView
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var btAccept: Button
    private lateinit var vpAttributes: ViewPager2
    private lateinit var llProgressBar: LinearLayout
    private lateinit var ivCard: ImageView
    private lateinit var clAgreement: ConstraintLayout
    private lateinit var tvAgreement: TextView

    private var isDataAgreementAvailable = false

    private var mPresentationExchange: PresentationExchange? = null

    private var requestedAttributes: HashMap<String, CredentialValue> = HashMap()

    private var selfAttestedAttributes: HashMap<String, String> = HashMap()

    private var isInsufficientData = false

    private var isFromSingle: Boolean = false

    //created in this activity
    private var exchangeDataList: ArrayList<ExchangeData> = ArrayList()
    private var mergedExchangeDataList: ArrayList<ArrayList<ExchangeData>> = ArrayList()

    //persistence of signature verified
    private var isSigVerified: Boolean = false

    companion object {
        private const val TAG = "ExchangeDataActivity"
        const val EXTRA_PRESENTATION_PROPOSAL =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.proposal"
        const val EXTRA_PRESENTATION_INVITATION =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.invitation"
        const val EXTRA_PRESENTATION_QR_ID =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.qrId"
        const val EXTRA_PRESENTATION_EXCHANGE =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.presentationExchange"

        const val EXTRA_PRESENTATION_RECORD_ID =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.recordId"

        const val EXTRA_IS_PROOF_VERIFIED =
            "io.igrant.mobileagent.activty.ProposeAndExchangeDataActivity.isSigVerified"

        var isBlur: Boolean = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exchange_data)
        initViews()
        initListener()
        getIntentData()
        setUpToolbar()
        initValues()
    }

    private fun initValues() {

        tvDesc.text =
            resources.getString(R.string.connect_by_choosing_confirm_you_agree_to_the_requested_data_to_org_name_android, invitation.label ?: "")

        tvHead.text = (proposalData?.name ?: "").toUpperCase()
        Glide
            .with(ivLogo.context)
            .load(invitation.image_url ?: invitation.imageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        tvOrgName.text = invitation.label ?: ""
        tvLocation.text = ""

        getValuesFromProofAndSelfAttestedAttributes(proposalData)
    }

    private fun getValuesFromProofAndSelfAttestedAttributes(presentationRequest: PresentationRequest?) {
        if (presentationRequest != null) {
            // filter out the list(presentationRequest.requestedAttributes) with attribute "names" -> loop through the names and create a new entry in the map
            presentationRequest.requestedAttributes =
                updateNamesWithName(presentationRequest.requestedAttributes)

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
                            val data = if ((it.credInfo?.attrs?.get(value.name ?: "") ?: " ") != "")
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
                                    if ((it.credInfo?.attrs?.get(name) ?: " ") != "")
                                        it.credInfo?.attrs?.get(name) ?: ""
                                    else
                                        " "
                                } catch (e: Exception) {
                                    " "
                                }

                                if (data != "") {

                                    val exchangeData = ExchangeData()
                                    exchangeData.key = key
                                    exchangeData.value = value
                                    exchangeData.name = name
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
                if (value.name != null && (value.restrictions == null || value.restrictions!!.size == 0)) {
                    val selfAttestedValue = searchSelfAttestedCredentialsForData(value.name ?: "")
                    for (item in selfAttestedValue) {

                        val exchangeData = ExchangeData()
                        exchangeData.key = key
                        exchangeData.value = value
                        exchangeData.name = value.name ?: ""
                        exchangeData.type = 1
                        exchangeData.data = if ((item.attribute?.value ?: "") != "")
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
        if (requestedAttributes != null) {
            this.requestedAttributes = HashMap()
            this.selfAttestedAttributes = HashMap()

            var credentialValue: CredentialValue

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
    }

    private fun setUpViewPager() {
        viewPagerAdapter = ProposeAndExchangePagerAdapter(mergedExchangeDataList, isBlur, this)
        vpAttributes.adapter = viewPagerAdapter
        dotsIndicator.setViewPager2(vpAttributes)

        ivCard.visibility = if (mergedExchangeDataList.size < 2) View.INVISIBLE else View.VISIBLE
        tvMultipleData.visibility =
            if (mergedExchangeDataList.size < 2) View.GONE else View.VISIBLE
        dotsIndicator.visibility =
            if (mergedExchangeDataList.size < 2) View.GONE else View.VISIBLE
    }

    private fun getIntentData() {

        invitation = intent.extras!!.getSerializable(EXTRA_PRESENTATION_INVITATION) as Invitation
        qrId = intent.extras!!.getString(EXTRA_PRESENTATION_QR_ID)

        //if record id is there then it is from the exchange requests
        if (intent.hasExtra(EXTRA_PRESENTATION_RECORD_ID))
            recordId = intent.getStringExtra(EXTRA_PRESENTATION_RECORD_ID)

        if (intent.hasExtra(EXTRA_IS_PROOF_VERIFIED))
            isSigVerified = intent.getBooleanExtra(EXTRA_IS_PROOF_VERIFIED, false)
        if (intent.hasExtra(EXTRA_PRESENTATION_EXCHANGE)) {
            mPresentationExchange =
                intent.getSerializableExtra(EXTRA_PRESENTATION_EXCHANGE) as PresentationExchange
            dataAgreementContext = mPresentationExchange?.dataAgreementContext
            mConnectionId = mPresentationExchange?.connectionId ?: ""
        }

        if (intent.hasExtra(EXTRA_PRESENTATION_PROPOSAL)) {
            proposal = intent.extras!!.getString(EXTRA_PRESENTATION_PROPOSAL).toString()
            proposalData = WalletManager.getGson.fromJson(
                proposal, PresentationRequest::class.java
            )
        }

        if (dataAgreementContext == null) {
            llProgressBar.visibility = View.VISIBLE
            callPurposeData(invitation, qrId ?: "", object : PurposeListener {
                override fun onSuccess(connectionHistoryDetail: ConnectionHistoryDetail) {
                    connectionHistory = connectionHistoryDetail
                    setUpDataAgreementViews()
                    llProgressBar.visibility = View.GONE
                }

                override fun onFailure() {
                    llProgressBar.visibility = View.GONE
                }
            })
        }
    }

    private fun setUpDataAgreementViews() {
        if (connectionHistory != null) {
            isDataAgreementAvailable = connectionHistory?.purposeDetails?.purpose != null
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.general_data_agreement)
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
                EventBus.getDefault().post(BlurEvent(isBlur))
                viewPagerAdapter.setBlurValue(isBlur)
                invalidateOptionsMenu()
            }
            R.id.action_delete -> {
                AlertDialog.Builder(this@ProposeAndExchangeDataActivity)
                    .setTitle(resources.getString(R.string.general_app_title))
                    .setMessage(
                        resources.getString(
                            R.string.data_do_you_want_to_cancel_the_exchange_request
                        )
                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        R.string.general_yes
                    ) { dialog, which ->
                        if (recordId != null && WalletManager.getWallet != null) {
                            WalletRecord.delete(
                                WalletManager.getWallet,
                                WalletRecordType.MESSAGE_RECORDS,
                                recordId ?: ""
                            ).get()

                            val credentialExchangeResponse =
                                SearchUtils.searchWallet(
                                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                                    "{\"thread_id\": \"${mPresentationExchange?.threadId}\"}"
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

                        }
                        onBackPressed()
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

    private fun initViews() {
        clView = findViewById(R.id.clView)
        toolbar = findViewById(R.id.toolbar)
        tvDesc = findViewById(R.id.tvDesc)
        tvHead = findViewById(R.id.tvHead)
        btAccept = findViewById(R.id.btAccept)
        ivLogo = findViewById(R.id.ivLogo)
        tvOrgName = findViewById(R.id.tvOrgName)
        tvLocation = findViewById(R.id.tvLocation)
        vpAttributes = findViewById(R.id.vpAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
        dotsIndicator = findViewById(R.id.dotsIndicator)
        tvMultipleData = findViewById(R.id.tvMultipleData)
        ivCard = findViewById(R.id.ivCard)

        clAgreement = findViewById(R.id.clAgreement)
        tvAgreement = findViewById(R.id.tvAgreement)
        tvAgreement.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.textColor
            )
        )
    }

    private fun initListener() {

        clAgreement.setOnClickListener {
            val dataAgreementPolicy: DataAgreementPolicy? = if (dataAgreementContext != null) {
                if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(dataAgreementContext?.message?.body)) {
                    DataAgreementPolicy(
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).lawfulBasis
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).dataPolicy?.policyURL
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).dataPolicy?.jurisdiction
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).dataPolicy?.industrySector
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).dataPolicy?.geographicRestriction
                            ?: "",
                        null,
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementContext?.message?.body).dataPolicy?.dataRetentionPeriod.toString()
                    )
                } else {
                    DataAgreementPolicy(
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).lawfulBasis
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).dataPolicy?.policyURL
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).dataPolicy?.jurisdiction
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).dataPolicy?.industrySector
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).dataPolicy?.geographicRestriction
                            ?: "",
                        null,
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementContext?.message?.body).dataPolicy?.dataRetentionPeriod.toString()
                    )
                }
            } else if (isDataAgreementAvailable) {
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
            } else {
                null
            }

            DataAgreementPolicyUtil.showDataAgreementPolicy(
                dataAgreementPolicy,
                dataAgreementContext, mConnectionId, this, isSigVerified
            )
        }

        ivCard.setOnClickListener {
            val exchangeData = mergedExchangeDataList[vpAttributes.currentItem][0]
            showCard(exchangeData, this)
        }

        btAccept.setOnClickListener {
            buildAttributeList(proposalData?.requestedAttributes)
            if (!isInsufficientData) {

                val connectionList = SearchUtils.searchWallet(
                    WalletRecordType.CONNECTION,
                    "{\"request_id\":\"$mConnectionId\"}"
                )
                if ((connectionList.totalCount ?: 0) > 0) {
                    llProgressBar.visibility = View.VISIBLE
                    btAccept.isEnabled = false
                    if (dataAgreementContext == null) {
                        exchangeOrProposeData()
                    } else {
                        signDataAgreementContext(mConnectionId)
                    }
                } else {
                    saveConnection(invitation)
                }
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.connection_insufficient_data_requested),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun exchangeOrProposeData() {
        if (mPresentationExchange != null) {
            exchangeData(UUID.randomUUID().toString())
        } else {
            sendProposal(mConnectionId, false)
        }
    }

    private fun signDataAgreementContext(mConnectionId: String) {
        if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(dataAgreementContext?.message?.body)) {
            signRequestV3(
                dataAgreementContext, mConnectionId,
                object : SignOfferRequestListenersV3 {
                    override fun onSignRequestSuccess(mDataAgreementContext: DataAgreementContext?, updatedContext:DataAgreementContext?) {
                        super.onSignRequestSuccess(mDataAgreementContext, updatedContext)
                        dataAgreementContext = mDataAgreementContext
                        exchangeOrProposeData()
                    }

                    override fun onSignOfferRequestFailure() {
                        super.onSignOfferRequestFailure()
                    }
                }, false
            )
        }else{
            SignOfferRequest.signRequest(
                dataAgreementContext,mConnectionId,
                object : SignOfferRequestListeners{
                    override fun onSignRequestSuccess(certificateOffer: DataAgreementContext?) {
                        super.onSignRequestSuccess(certificateOffer)
                        dataAgreementContext = certificateOffer
                        exchangeOrProposeData()
                    }
                    override fun onSignOfferRequestFailure() {
                        super.onSignOfferRequestFailure()
                    }
                },false
            )
        }
    }

    private fun sendProposal(mConnectionId: String, isOrgId: Boolean) {
        val threadId = UUID.randomUUID().toString()
        Log.d(TAG, "sendProposal: Milan")
        var data = "{\n" +
                "  \"@type\": \"${DidCommPrefixUtils.getType(invitation.type ?: "")}/present-proof/1.0/propose-presentation\",\n" +
                "  \"@id\": \"$threadId\",\n" +
                "  \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    },\n" +
                "  \"presentation_proposal\": {\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType(invitation.type ?: "")}/present-proof/1.0/presentation-preview\",\n" +
                "    \"attributes\": [ \n"

        for (attribute in mergedExchangeDataList[vpAttributes.currentItem]) {
            if (attribute.credData?.credInfo?.credDefId != null && !attribute.credData?.credInfo?.credDefId.equals(
                    ""
                )
            ) {
                data = data + "{\n" +
                        "        \"name\": \"${attribute.name}\",\n" +
                        "        \"cred_def_id\": \"${attribute.credData?.credInfo?.credDefId}\",\n" +
                        "        \"value\": \"${if (attribute.data != null && attribute.data != "") attribute.data else " "}\",\n" +
                        "        \"referent\": \"${attribute.credData?.credInfo?.referent}\"\n" +
                        "      },\n"
            } else {
                data = data + "{\n" +
                        "        \"name\": \"${attribute.name}\",\n" +
                        "        \"value\": \"${if (attribute.data != null && attribute.data != "") attribute.data else " "}\"\n" +
                        "      },\n"
            }
        }

        if (mergedExchangeDataList[vpAttributes.currentItem].size > 0)
            data = data.substring(0, data.length - 2)

        data = data +
                "    ],\n" +
                "    \"predicates\": []\n" +
                "  },\n" +
                "  \"comment\": \"Proposing credentials\""

        if (qrId == "")
            data += "\n}"
        else
            data = "$data,\n\"qr_id\":\"$qrId\"\n}"

        val connectionList = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            if (isOrgId)
                "{\"orgId\":\"$mConnectionId\"}"
            else
                "{\"request_id\":\"$mConnectionId\"}"
        )

        if ((connectionList.totalCount ?: 0) > 0) {
            val connectionObject = WalletManager.getGson.fromJson(
                connectionList.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )

            val metaString =
                Did.getDidWithMeta(WalletManager.getWallet, connectionObject?.myDid).get()
            val metaObject = JSONObject(metaString)
            val publicKey = metaObject.getString("verkey")

            val didDocSearch = SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\"did\":\"${connectionObject?.theirDid}\"}"
            )

            var serviceEndPoint = ""
            var recipient = ""
            if ((didDocSearch.totalCount ?: 0) > 0) {
                val didDoc = WalletManager.getGson.fromJson(
                    didDocSearch.records?.get(0)?.value,
                    DidDoc::class.java
                )

                serviceEndPoint = didDoc.service?.get(0)?.serviceEndpoint ?: ""
                recipient = didDoc.publicKey?.get(0)?.publicKeyBase58 ?: ""

                Log.d(TAG, "sendProposal: $recipient \n $publicKey \n $data")
                val packedMessage = PackingUtils.packMessage(
                    didDoc, publicKey,
                    data, invitation.type ?: ""
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

                ApiManager.api.getService()?.postData(serviceEndPoint, typedBytes)
                    ?.enqueue(object : Callback<ConfigPostResponse> {
                        override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                            llProgressBar.visibility = View.GONE
                        }

                        override fun onResponse(
                            call: Call<ConfigPostResponse>,
                            response: Response<ConfigPostResponse>
                        ) {
                            Log.d(TAG, "onResponse: " + response.body())

                            val unpack =
                                Crypto.unpackMessage(
                                    WalletManager.getWallet,
                                    WalletManager.getGson.toJson(response.body()).toString()
                                        .toByteArray()
                                ).get()

                            val message = JSONObject(String(unpack)).getString("message")

                            val presentationRequestBase64 =
                                JSONObject(
                                    JSONObject(message).getJSONArray("request_presentations~attach")
                                        .get(0).toString()
                                ).getJSONObject("data").getString("base64")
                            val presentationRequest = WalletManager.getGson.fromJson(
                                Base64.decode(presentationRequestBase64, Base64.URL_SAFE)
                                    .toString(charset("UTF-8")), PresentationRequest::class.java
                            )

                            val presentationExchange = PresentationExchange()
                            presentationExchange.threadId =
                                JSONObject(message).getJSONObject("~thread").getString("thid")
                            presentationExchange.createdAt = DateUtils.getIndyFormattedDate()
                            presentationExchange.updatedAt = DateUtils.getIndyFormattedDate()
                            presentationExchange.connectionId = connectionObject?.requestId
                            presentationExchange.initiator = "external"
                            presentationExchange.presentationProposalDict = null
                            presentationExchange.presentationRequest = presentationRequest
                            presentationExchange.role = "prover"
                            presentationExchange.state = PresentationExchangeStates.REQUEST_RECEIVED
                            presentationExchange.comment =
                                JSONObject(message).getString("comment")
                            presentationExchange.type =
                                JSONObject(message).getString("@type")

                            mPresentationExchange = presentationExchange

                            buildAttributeList(presentationRequest.requestedAttributes)

                            exchangeData(threadId)
                        }
                    })
            }
        }
    }

    private fun exchangeData(threadId: String) {
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

                                        EventBus.getDefault().post(GoHomeEvent())

                                        MessageUtils.displaySnackbar(
                                            clView, resources.getString(
                                                R.string.data_data_has_been_shared_successfully
                                            )
                                        )

                                        saveDataShareHistory(
                                            connectionHistory,
                                            proposalData?.name ?: "",
                                            mergedExchangeDataList[vpAttributes.currentItem],
                                            dataAgreementContext, mConnectionId
                                        )
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            finish()
                                        }, 1000)

                                        btAccept.visibility = View.GONE
                                    }
                                }
                            })
                    } else {
                        Toast.makeText(
                            this@ProposeAndExchangeDataActivity,
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
            selfAttestedAttributes,
            dataAgreementContext,
            "",
            mConnectionId
        )
    }

    private fun saveConnection(
        invitation: Invitation
    ) {
        Handler(Looper.getMainLooper()).postDelayed({
            val connectionSuccessDialogFragment: ConnectionProgressDailogFragment =
                ConnectionProgressDailogFragment.newInstance(
                    true,
                    invitation,
                    ""
                )
            connectionSuccessDialogFragment.show(supportFragmentManager, "fragment_edit_name")
            connectionSuccessDialogFragment.setListener(this)
        }, 200)
    }

    override fun onSuccess(proposal: String, orgId: String) {
        llProgressBar.visibility = View.VISIBLE
        btAccept.isEnabled = false
        if (proposalData != null) {
            if (mPresentationExchange != null) {
                exchangeData(UUID.randomUUID().toString())
            } else {
                sendProposal(orgId, true)
            }
        }
    }

    override fun onEbsiConnectionSuccess(data: String) {

    }

    override fun onExistingConnection(connectionId: String) {
        mConnectionId = connectionId
        val connectionList = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"request_id\":\"$connectionId\"}"
        )
        if ((connectionList.totalCount ?: 0) > 0) {
            llProgressBar.visibility = View.VISIBLE
            btAccept.isEnabled = false
            if (proposalData != null) {
                if (mPresentationExchange != null) {
                    exchangeData(UUID.randomUUID().toString())
                } else {
                    sendProposal(connectionId, false)
                }
            }
        }
    }

    private var vpHeight = 0
    override fun setHeight(height: Int) {
        Log.d(TAG, "setHeight:1 $height $vpHeight")
        if (height > vpHeight) {
            Log.d(TAG, "setHeight:3 $height $vpHeight")
            vpHeight = height
            Log.d(TAG, "setHeight:2 $height $vpHeight")
            val params =
                ConstraintLayout.LayoutParams(0, ConstraintLayout.LayoutParams.WRAP_CONTENT)
            params.height = height + 50 //left, top, right, bottom
            params.topToBottom = tvHead.id
            params.bottomToTop = dotsIndicator.id
            params.startToStart = tvDesc.id
            params.endToEnd = tvDesc.id
            params.setMargins(0, 30, 0, 30)
            vpAttributes.layoutParams = params
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isBlur = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                DataAgreementPolicyUtil.DATA_AGREEMENT_POLICY_RESULT -> {
                    updateCertificateWithSIgnatureVerified(
                        data?.getBooleanExtra(
                            DataAgreementPolicyActivity.EXTRA_IS_SIGNATURE_VERIFIED,
                            false
                        ) ?: false
                    )
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun updateCertificateWithSIgnatureVerified(isSignatureVerified: Boolean) {
        isSigVerified = isSignatureVerified
        if (recordId != null) {
            val messageRecordSearch = SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{\"certificateId\":\"$recordId\"}"
            )

            if ((messageRecordSearch.totalCount ?: 0) > 0) {
                val notification = WalletManager.getGson.fromJson(
                    messageRecordSearch.records?.get(0)?.value,
                    Notification::class.java
                )
                notification.isSignatureVerified = isSignatureVerified

                WalletMethods.updateWalletRecord(
                    WalletManager.getWallet,
                    WalletRecordType.MESSAGE_RECORDS,
                    recordId,
                    WalletManager.getGson.toJson(notification)
                )
                EventBus.getDefault()
                    .post(ReceiveExchangeRequestEvent())
            }
        }

    }
}