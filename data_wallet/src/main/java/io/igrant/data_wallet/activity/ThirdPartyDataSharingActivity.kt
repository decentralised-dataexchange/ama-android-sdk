package io.igrant.data_wallet.activity

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.DataAgreementPolicyUtil.showDataAgreementPolicy
import io.igrant.data_wallet.adapter.ThirdPartyDataSharingAdapter
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.fragment.FilterBottomSheetFragment
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ConnectionFilterClickListener
import io.igrant.data_wallet.listeners.PrefsClickListeners
import io.igrant.data_wallet.models.ConnectionFilter
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.history.History
import io.igrant.data_wallet.models.prefs.Prefs
import io.igrant.data_wallet.models.prefs.PrefsResponse
import io.igrant.data_wallet.models.unpack.UnPackedData
import io.igrant.data_wallet.tags.TagDataShareHistory
import io.igrant.data_wallet.utils.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
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

class ThirdPartyDataSharingActivity : BaseActivity(), ConnectionFilterClickListener {

    private lateinit var rvList: RecyclerView
    private lateinit var llProgressBar: LinearLayout
    private lateinit var adapter: ThirdPartyDataSharingAdapter

    private var orgId = ""
    private var prefResponse: PrefsResponse? = null

    companion object {
        const val TAG = "tpdsActivity"
        const val EXTRA_ORG_ID = "io.igrant.mobileagent.activty.ThirdPartyDataSharingActivity.orgId"
    }

    private var filter: ConnectionFilter? = ConnectionFilter(
        id = ConnectionFilterUtil.THIRD_PARTY_FILTER_ALL,
        isEnabled = true,
        isSelected = true,
        logo = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_third_party_data_sharing)
        getIntentData()
        initToolbar()
        initViews()
        getPrefs()
    }

    private fun getPrefs() {
        llProgressBar.visibility = View.VISIBLE
        val connection = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"orgId\":\"$orgId\"}"
        )

        if ((connection.totalCount ?: 0) > 0) {
            val connectionObject = WalletManager.getGson.fromJson(
                connection.records?.get(0)?.value ?: "",
                MediatorConnectionObject::class.java
            )

            val didDoc =
                SearchUtils.searchWallet(
                    WalletRecordType.DID_DOC,
                    "{\"did\":\"${connectionObject.theirDid}\"}"
                )

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
                    val data = "{\n" +
                            "    \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/third-party-data-sharing/1.0/fetch-preferences\",\n" +
                            "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                            "    \"~transport\": {\n" +
                            "        \"return_route\": \"all\"\n" +
                            "    }\n" +
                            "}"

                    val queryFeaturePacked = PackingUtils.packMessage(didDocObj, key, data, "")

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
                        ?.postData(serviceEndPoint, queryFeaturePackedBytes)
                        ?.enqueue(object : Callback<ConfigPostResponse> {
                            override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                                llProgressBar.visibility = View.GONE
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

                                    val unpackedDate = WalletManager.getGson.fromJson(
                                        String(unpack),
                                        UnPackedData::class.java
                                    )

                                    prefResponse = WalletManager.getGson.fromJson(
                                        JSONObject(unpackedDate.message).getJSONObject("body")
                                            .toString(),
                                        PrefsResponse::class.java
                                    )

                                    setUpList(updatePrefList(prefResponse?.prefs ?: ArrayList()))
                                }
                                llProgressBar.visibility = View.GONE
                            }
                        })
                } else {
                    llProgressBar.visibility = View.GONE
                }
            } else {
                llProgressBar.visibility = View.GONE
            }
        } else {
            llProgressBar.visibility = View.GONE
        }
    }

    //state - allow/disallow
    private fun updateAgreementLevelPref(instanceId: String, state: String) {
        llProgressBar.visibility = View.VISIBLE
        val connection = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"orgId\":\"$orgId\"}"
        )

        if ((connection.totalCount ?: 0) > 0) {
            val connectionObject = WalletManager.getGson.fromJson(
                connection.records?.get(0)?.value ?: "",
                MediatorConnectionObject::class.java
            )

            val didDoc =
                SearchUtils.searchWallet(
                    WalletRecordType.DID_DOC,
                    "{\"did\":\"${connectionObject.theirDid}\"}"
                )

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
                    val data = "{\n" +
                            "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/data-agreement/1.0/permissions\",\n" +
                            "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                            "  \"body\": {\n" +
                            "    \"instance_id\": \"$instanceId\",\n" +
                            "    \"state\": \"$state\"\n" +
                            "  }\n" +
                            "}"

                    Log.d(TAG, "updateAgreementLevelPref: $data")
                    val queryFeaturePacked = PackingUtils.packMessage(didDocObj, key, data, "")

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
                        ?.postDataWithoutData(serviceEndPoint, queryFeaturePackedBytes)
                        ?.enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                llProgressBar.visibility = View.GONE
                            }

                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                if (response.code() == 200) {
                                    prefResponse?.prefs?.onEach {
                                        if (it.instanceId == instanceId)
                                            it.instancePermissionState =
                                                if (state == "allow") "allow" else "disallow"
                                    }
                                    filter?.let { filterList(it) }
                                }
                                llProgressBar.visibility = View.GONE
                            }
                        })
                } else {
                    llProgressBar.visibility = View.GONE
                }
            } else {
                llProgressBar.visibility = View.GONE
            }
        } else {
            llProgressBar.visibility = View.GONE
        }
    }

    private fun updateCompanyLevelPref(
        ddaInstanceId: String,
        instanceId: String,
        state: String
    ) {
        llProgressBar.visibility = View.VISIBLE
        val connection = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"orgId\":\"$orgId\"}"
        )

        if ((connection.totalCount ?: 0) > 0) {
            val connectionObject = WalletManager.getGson.fromJson(
                connection.records?.get(0)?.value ?: "",
                MediatorConnectionObject::class.java
            )

            val didDoc =
                SearchUtils.searchWallet(
                    WalletRecordType.DID_DOC,
                    "{\"did\":\"${connectionObject.theirDid}\"}"
                )

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
                    val data = "{\n" +
                            "  \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/third-party-data-sharing/1.0/update-preferences\",\n" +
                            "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                            "  \"body\": {\n" +
                            "    \"dda_instance_id\": \"$ddaInstanceId\",\n" +
                            "    \"da_instance_id\": \"$instanceId\",\n" +
                            "    \"state\": \"$state\"\n" +
                            "  }\n" +
                            "}"
                    Log.d(TAG, "updateAgreementLevelPref: $data")
                    val queryFeaturePacked = PackingUtils.packMessage(didDocObj, key, data, "")

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
                        ?.postDataWithoutData(serviceEndPoint, queryFeaturePackedBytes)
                        ?.enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                llProgressBar.visibility = View.GONE
                            }

                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                if (response.code() == 200) {
                                    prefResponse?.prefs?.onEach {
                                        if (it.instanceId == instanceId) {
                                            it.dus.forEach { it2 ->
                                                it2.ddaInstancePermissionState =
                                                    if (state == "allow") "allow" else "disallow"
                                            }
                                        }
                                    }
                                    filter?.let { filterList(it) }
                                }
                                llProgressBar.visibility = View.GONE
                            }
                        })
                } else {
                    llProgressBar.visibility = View.GONE
                }
            } else {
                llProgressBar.visibility = View.GONE
            }
        } else {
            llProgressBar.visibility = View.GONE
        }
    }

    private fun updatePrefList(prefs: ArrayList<Prefs>): ArrayList<Prefs> {
        var tempList: ArrayList<Prefs> = ArrayList()
        for (pref in prefs) {
            val query = TagDataShareHistory(
                contextId = pref.instanceId
            )
            val searchResponse = SearchUtils.searchWallet(
                WalletRecordType.DATA_HISTORY,
                WalletManager.getGson.toJson(query)
            )
            if ((searchResponse.totalCount ?: 0) > 0) {
                val history =
                    WalletManager.getGson.fromJson(
                        searchResponse.records?.get(0)?.value ?: "{}",
                        History::class.java
                    )

                if (history.dataAgreementContext != null && DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(history.dataAgreementContext?.message?.body).purpose != null) {
                    pref.purposeName = DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(history.dataAgreementContext?.message?.body).purpose
                    tempList.add(pref)
                }
            }
        }

        prefResponse?.prefs = tempList

        return prefResponse?.prefs ?: ArrayList()
    }

    private fun getIntentData() {
        if (intent.hasExtra(EXTRA_ORG_ID))
            orgId = intent.getStringExtra(EXTRA_ORG_ID) ?: ""
    }

    private fun setUpList(prefs: ArrayList<Prefs>) {
        adapter = ThirdPartyDataSharingAdapter(prefs, object : PrefsClickListeners {
            override fun onAgreementLevelClick(instanceId: String, state: String) {
                updateAgreementLevelPref(instanceId, state)
            }

            override fun onCompanyLevelClick(
                ddaInstanceId: String,
                instanceId: String,
                state: String
            ) {
                updateCompanyLevelPref(ddaInstanceId, instanceId, state)
            }

            override fun onInfoClick(instanceId: String) {
                val query = TagDataShareHistory(
                    contextId = instanceId
                )
                val searchResponse = SearchUtils.searchWallet(
                    WalletRecordType.DATA_HISTORY,
                    WalletManager.getGson.toJson(query)
                )
                if ((searchResponse.totalCount ?: 0) > 0) {
                    val history =
                        WalletManager.getGson.fromJson(
                            searchResponse.records?.get(0)?.value ?: "{}",
                            History::class.java
                        )

                    if (history.dataAgreementContext != null && DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(history.dataAgreementContext?.message?.body).purpose != null) {
                        showDataAgreementPolicy(history,this@ThirdPartyDataSharingActivity)
                    }
                }
            }
        })
        rvList.adapter = adapter
    }

    private fun initViews() {
        rvList = findViewById(R.id.rvThirdPartySectors)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.connection_third_party_data_sharing)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_third_party_data_sharing, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_filter -> {
                if (prefResponse != null) {
                    val blankFragment = FilterBottomSheetFragment.newInstance(
                        FilterType.THIRD_PARTY_DATA_SHARING,
                        filter?.id,
                        prefResponse?.sectors
                    )
                    blankFragment.show(supportFragmentManager, " blankFragment.tag")
                }
                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onFilterClick(filter: ConnectionFilter) {
        this.filter = filter
        filterList(filter)
    }

    private fun filterList(filter: ConnectionFilter) {
        var tempList: ArrayList<Prefs> = ArrayList()
        if (filter.id == ConnectionFilterUtil.THIRD_PARTY_FILTER_ALL) {
            tempList = prefResponse?.prefs ?: ArrayList()
        } else {
            for (pref in prefResponse?.prefs ?: ArrayList()) {
                if (pref.sector == filter.name)
                    tempList.add(pref)
            }
        }

        adapter.updateList(tempList)
    }
}