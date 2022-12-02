package io.igrant.data_wallet.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.HistoryActivity.Companion.EXTRA_ORG_ID
import io.igrant.data_wallet.adapter.ConnectionMessageAdapter
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.events.ReceiveOfferEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ConnectionMessageListener
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.certificateOffer.Attributes
import io.igrant.data_wallet.models.connection.*
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.credentialExchange.RawCredential
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.tags.TagDataShareHistory
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.ConnectionDetail.getV2ConnectionDetail
import io.igrant.data_wallet.utils.ConnectionTypes.EBSI_CONNECTION_NATURAL_PERSON
import io.igrant.data_wallet.utils.ConnectionTypes.IGRANT_ENABLED_CONNECTION
import io.igrant.data_wallet.utils.ConnectionTypes.V2_CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.CONNECTION
import io.igrant.data_wallet.utils.WalletRecordType.Companion.DID_DOC
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
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

class ConnectionDetailActivity : BaseActivity() {

    private var orgId: String? = null
    private var connectionCertList: ConnectionCerListResponse? = null
    private var mConnectionId: String = ""

    private lateinit var llErrorMessage: LinearLayout
    private lateinit var ivCoverUrl: ImageView
    private lateinit var ivLogo: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvRemove: TextView
    private lateinit var tvMySharedData: TextView
    private lateinit var tv3ppDataSharing: TextView
    private lateinit var llProgressBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_detail)
        initViews()
        getIntentData()
        setUpToolbar()
        initListener()
        checkIfIgrantSupportedConnection()
    }

    private fun checkIfIgrantSupportedConnection() {

        val connection = SearchUtils.searchWallet(CONNECTION, "{\"request_id\":\"$mConnectionId\"}")

        if ((connection.totalCount ?: 0) > 0) {
            val connectionObject = WalletManager.getGson.fromJson(
                connection.records?.get(0)?.value ?: "",
                MediatorConnectionObject::class.java
            )

            val connectionType = AnalyseProtocol.checkConnectionType(connectionObject.protocols)
            when {
                connectionType == IGRANT_ENABLED_CONNECTION ||
                        connectionObject.isIGrantEnabled == true -> {
                    getConnectionDetail(connectionObject)
                }
                connectionType == V2_CONNECTION -> {
                    val didDoc =
                        SearchUtils.searchWallet(
                            DID_DOC,
                            "{\"did\":\"${connectionObject.theirDid}\"}"
                        )

                    if ((didDoc.totalCount ?: 0) > 0) {
                        val didDocObj = WalletManager.getGson.fromJson(
                            didDoc.records?.get(0)?.value,
                            DidDoc::class.java
                        )
                        getV2ConnectionDetail(connectionObject.myDid ?: "",
                            connectionObject.theirDid ?: "",
                            didDocObj,
                            object : ConnectionDetailCompletionListener {
                                override fun onSuccess(connection: ConnectionV2Response) {
                                    super.onSuccess(connection)
                                    initDataValues(connection.body)
                                }

                                override fun onFailure() {
                                    super.onFailure()
                                    setDefaultValues(connectionObject)
                                }
                            })
                    }
                }
                connectionObject.connectionType == EBSI_CONNECTION_NATURAL_PERSON -> {
                    initDataValues(
                        Connection(
                            orgId = connectionObject.orgId,
                            logoImageUrl = connectionObject.theirImageUrl,
                            description = "EBSI is a joint initiative from the European Commission and the European Blockchain Partnership. The vision is to leverage blockchain to accelerate the creation of cross-border services for public administrations and their ecosystems to verify information and to make services more trustworthy.",
                            name = connectionObject.theirLabel,
                            location = connectionObject.location
                        )
                    )
                }
                else -> {
                    setDefaultValues(connectionObject)
                }
            }
        }
    }

    private fun setDefaultValues(connectionObject: MediatorConnectionObject) {
        Glide
            .with(ivLogo.context)
            .load(connectionObject.theirImageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        tvName.text = connectionObject.theirLabel ?: ""
        tvLocation.text = connectionObject.location ?: ""
        llProgressBar.visibility = View.GONE
    }

    private fun getConnectionDetail(connectionObject: MediatorConnectionObject) {
        val orgData =
            "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/organization-info\", \"@id\": \"$mConnectionId\" , \"~transport\": {" +
                    "\"return_route\": \"all\"}\n}"
//
//        val cerData =
//            "{ \"@type\": \"${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator/1.0/list-data-certificate-types\", \"@id\": \"$mConnectionId\" , \"~transport\": {" +
//                    "\"return_route\": \"all\"}\n}"

        val didDoc =
            SearchUtils.searchWallet(DID_DOC, "{\"did\":\"${connectionObject.theirDid}\"}")

        if ((didDoc.totalCount ?: 0) > 0) {
            val didDocObj = WalletManager.getGson.fromJson(
                didDoc.records?.get(0)?.value,
                DidDoc::class.java
            )

            val serviceEndPoint = didDocObj.service?.get(0)?.serviceEndpoint ?: ""

            if (WalletManager.getWallet != null) {
                val metaString =
                    Did.getDidWithMeta(WalletManager.getWallet, connectionObject.myDid).get()
                val metaObject = JSONObject(metaString)
                val key = metaObject.getString("verkey")

                val orgDetailPacked = PackingUtils.packMessage(didDocObj, key, orgData, "")

                val orgDetailTypedArray = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(orgDetailPacked)
                    }
                }
                ApiManager.api.getService()?.postData(serviceEndPoint, orgDetailTypedArray)
                    ?.enqueue(object :
                        Callback<ConfigPostResponse> {
                        override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                            Log.d("https", "onFailure: ")
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

                                Log.d(
                                    "milan",
                                    "onResponse: ${JSONObject(String(unpack)).getString("message")}"
                                )
                                val connectionData = WalletManager.getGson.fromJson(
                                    JSONObject(String(unpack)).getString("message"),
                                    Connection::class.java
                                )
                                initDataValues(connectionData)
                            }
                        }
                    })
            }
        }
    }

    private fun initDataValues(connectionData: Connection?) {
        initDataValues(
            connectionData?.orgId,
            connectionData?.logoImageUrl,
            connectionData?.coverImageUrl,
            connectionData?.description,
            connectionData?.name,
            connectionData?.location
        )
    }

    private fun initDataValues(connectionData: ConnectionV2?) {
        initDataValues(
            connectionData?.organisationId ?:connectionData?.organisationDid,
            connectionData?.logoImageUrl,
            connectionData?.coverImageUrl,
            connectionData?.description,
            connectionData?.organisationName,
            connectionData?.location
        )
    }

    private fun initDataValues(
        mOrgId: String?,
        logoImageUrl: String?,
        coverImageUrl: String?,
        desc: String?,
        title: String?,
        location: String?
    ) {
        orgId = mOrgId
        Glide
            .with(ivLogo.context)
            .load(logoImageUrl)
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        Glide
            .with(ivCoverUrl.context)
            .load(coverImageUrl)
            .centerCrop()
            .placeholder(R.drawable.default_cover_image)
            .into(ivCoverUrl)

        tvDescription.text = desc

        tvName.text = title
        tvLocation.text = location
        llProgressBar.visibility = View.GONE

        update3ppButton()
    }

    private fun initViews() {
        llErrorMessage = findViewById(R.id.llErrorMessage)
        ivCoverUrl = findViewById(R.id.ivCoverUrl)
        ivLogo = findViewById(R.id.ivLogo)
        tvName = findViewById(R.id.tvName)
        tvLocation = findViewById(R.id.tvLocation)
        tvDescription = findViewById(R.id.tvDescription)
        tvRemove = findViewById(R.id.tvRemove)
        tvMySharedData = findViewById(R.id.tvMySharedData)
        tv3ppDataSharing = findViewById(R.id.tv3ppDataSharing)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun getIntentData() {
        if (intent.extras != null) {
            mConnectionId = intent.getStringExtra(EXTRA_CONNECTION_DATA) ?: ""
        }
    }

    private fun setUpToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolBarCommon)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back_bg)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun initListener() {
        tvRemove.setOnClickListener {
            if (WalletManager.getWallet != null) {
                AlertDialog.Builder(this)
                    .setTitle(resources.getString(R.string.general_app_title))
                    .setMessage(
                        resources.getString(
                            R.string.data_do_you_want_to_remove_the_organisation
                        )
                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        R.string.general_yes
                    ) { dialog, which ->
                        DeleteUtils.deleteConnection(mConnectionId)
                        finish()
                    } // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(
                        R.string.general_no
                    ) { dialog, which ->

                    }
                    .show()
            }

        }

        tvMySharedData.setOnClickListener {
            if (orgId != null) {
                val intent = Intent(this, HistoryActivity::class.java)
                intent.putExtra(EXTRA_ORG_ID, orgId)
                startActivity(intent)
            }
        }

        tv3ppDataSharing.setOnClickListener {
            if (orgId != null) {
                val intent = Intent(this, ThirdPartyDataSharingActivity::class.java)
                intent.putExtra(ThirdPartyDataSharingActivity.EXTRA_ORG_ID, orgId)
                startActivity(intent)
            }
        }
    }

    private fun update3ppButton() {
        if (orgId != null) {
            val query = TagDataShareHistory(
                orgId = orgId,
                thirdParty = "true"
            )
            val searchResponse = SearchUtils.searchWallet(
                WalletRecordType.DATA_HISTORY,
                WalletManager.getGson.toJson(query)
            )
            if ((searchResponse.totalCount ?: 0) > 0) {
                enableDisableButton(tv3ppDataSharing, true)
            } else {
                enableDisableButton(tv3ppDataSharing, false)
            }

        } else {
            enableDisableButton(tv3ppDataSharing, false)
        }
    }

    private fun enableDisableButton(view: TextView, enable: Boolean) {
        view.setTextColor(
            if (!enable
            ) ContextCompat.getColor(
                this@ConnectionDetailActivity,
                R.color.textColorVeryLight
            ) else ContextCompat.getColor(this@ConnectionDetailActivity, R.color.textColor)
        )
        view.isEnabled = enable
    }

    companion object {
        const val EXTRA_CONNECTION_DATA =
            "io.igrant.mobileagent.fragment.ConnectionMessagesFragment.connection"
    }
}