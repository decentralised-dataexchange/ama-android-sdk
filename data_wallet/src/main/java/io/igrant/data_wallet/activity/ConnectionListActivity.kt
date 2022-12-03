package io.igrant.data_wallet.activity

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import io.igrant.data_wallet.R
import io.igrant.data_wallet.adapter.ConnectionListAdapter
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.custom.ItemDecorator
import io.igrant.data_wallet.events.RefreshConnectionList
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ConnectionClickListener
import io.igrant.data_wallet.fragment.FilterBottomSheetFragment
import io.igrant.data_wallet.fragment.UrlExtractFragment
import io.igrant.data_wallet.listeners.ConnectionFilterClickListener
import io.igrant.data_wallet.models.ConnectionFilter
import io.igrant.data_wallet.qrcode.utils.QRScanner
import io.igrant.data_wallet.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONArray
import org.json.JSONObject

class ConnectionListActivity : BaseActivity(), UrlExtractFragment.ProgressListener,
    ConnectionFilterClickListener {

    private lateinit var connectionRecords: JSONArray
    private var connectionRecordsCopy: JSONArray? = null
    private var walletCertificateAdapter: ConnectionListAdapter? = null
    private lateinit var rvConnections: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var ivAdd: ImageView
    private lateinit var llProgressBar: LinearLayout

    private var filter: ConnectionFilter? = ConnectionFilter(
        id = ConnectionFilterUtil.CONNECTION_FILTER_ALL,
        isEnabled = true,
        isSelected = true,
        logo = null
    )

    companion object {
        private const val TAG = "ConnectionListActivity"
        private const val PICK_IMAGE_REQUEST = 101
        val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_SCAN_INVITATION = 202

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_list)
        initViews()
        setUpToolbar()
        getConnectionList()
        initListener()

        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
//        supportActionBar!!.title = resources.getString(R.string.title_connection_list)
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
            R.id.action_filter -> {
                val blankFragment = FilterBottomSheetFragment.newInstance(
                    FilterType.CONNECTION,
                    filter?.id
                )
                blankFragment.show(supportFragmentManager, " blankFragment.tag")

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN_INVITATION && resultCode == Activity.RESULT_OK) {
            if (data == null) return

            try {
                val uri: Uri = try {
                    Uri.parse(data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult"))
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

                                    extractFromUri(deepLinkUri)
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
                            Log.w(
                                InitializeActivity.TAG,
                                "getDynamicLink:onFailure",
                                e
                            )
                        }
                } else {
                    extractFromUri(uri)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.connection_unexpected_error_please_try_again),
                    Toast.LENGTH_SHORT
                ).show()
            }
//
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private var frag: UrlExtractFragment? = null
    private fun extractFromUri(uri: Uri) {

        if (supportFragmentManager
                .findFragmentByTag("counter_fragment") != null
        )
            frag = supportFragmentManager
                .findFragmentByTag("counter_fragment") as UrlExtractFragment
        if (frag == null) {
            frag = UrlExtractFragment.newInstance()
            supportFragmentManager.beginTransaction().add(frag!!, "counter_fragment")
                .commitAllowingStateLoss()
            frag?.setProgressListener(this)
        }

        if (frag != null)
            frag?.extractUrl(uri)
    }

    private fun getConnectionList() {
        if (WalletManager.getWallet != null) {
            val search = WalletSearch.open(
                WalletManager.getWallet,
                WalletRecordType.CONNECTION,
                "{}",
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val connection =
                WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

            WalletSearch.closeSearch(search)
            val data = JSONObject(connection)
            if (data.getInt("totalCount") > 0) {
                llErrorMessage.visibility = View.GONE
                connectionRecords = JSONArray(data.get("records").toString())
                connectionRecordsCopy = JSONArray(data.get("records").toString())
                setUpCertificateList()
            } else {
                llErrorMessage.visibility = View.VISIBLE
            }
        }
    }

    private fun setUpCertificateList() {
        walletCertificateAdapter =
            ConnectionListAdapter(connectionRecords, object :
                ConnectionClickListener {
                override fun onConnectionClick(connection: String, did: String) {
                    val clipboard: ClipboardManager? =
                        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("DID", did)
                    clipboard?.setPrimaryClip(clip)
                    val intent =
                        Intent(this@ConnectionListActivity, ConnectionDetailActivity::class.java)
                    intent.putExtra(ConnectionDetailActivity.EXTRA_CONNECTION_DATA, connection)
                    startActivity(intent)

//                    DeleteUtils.deleteConnection(connection)
                }
            })
        val layoutManager = LinearLayoutManager(this)
        val dividerItemDecoration = ItemDecorator(
            ContextCompat.getDrawable(this, R.drawable.list_divider_layer)!!
        )
//        dividerItemDecoration.setDrawable(resources.getDrawable(R.drawable.list_divider_layer, null))
        rvConnections.addItemDecoration(dividerItemDecoration)

        rvConnections.layoutManager = layoutManager
        rvConnections.adapter = walletCertificateAdapter
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_filter, menu)
//        return true
//    }

    private fun initListener() {
        ivAdd.setOnClickListener {
            if (PermissionUtils.hasPermissions(
                    this,
                    true,
                    PICK_IMAGE_REQUEST,
                    PERMISSIONS
                )
            ) {
                QRScanner().withLocale(LocaleHelper.getLanguage(this)).start(
                    this,
                    REQUEST_CODE_SCAN_INVITATION
                )
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s)
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            QRScanner().withLocale(LocaleHelper.getLanguage(this)).start(
                this,
                REQUEST_CODE_SCAN_INVITATION
            )
        }
    }

    private fun filterList(s: CharSequence?) {
        val tempList: ArrayList<JSONObject> = ArrayList()

        if (connectionRecordsCopy != null)
            for (i in 0 until connectionRecordsCopy!!.length()) {
                try {
                    if (connectionRecordsCopy?.getJSONObject(i)?.has("value") == true) {
                        val title = JSONObject(
                            connectionRecordsCopy?.getJSONObject(i)!!.getString("value")
                        ).getString("their_label")
                        if (title.contains(s ?: "", ignoreCase = true)) {
                            tempList.add(connectionRecordsCopy?.getJSONObject(i)!!)
                        }
                    }
                } catch (e: Exception) {
                }
            }

        connectionRecords = JSONArray(tempList)

        walletCertificateAdapter?.setList(connectionRecords)

    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvConnections = findViewById(R.id.rvConnections)
        llErrorMessage = findViewById(R.id.llErrorMessage)
        etSearch = findViewById(R.id.etSearch)
        ivAdd = findViewById(R.id.ivAdd)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshList(event: RefreshConnectionList) {
        getConnectionList()
    }

    override fun updateProgress(progress: Int) {
        llProgressBar.visibility = progress
    }

    override fun error(explain: String) {
        llProgressBar.visibility = View.GONE
        Toast.makeText(this, explain, Toast.LENGTH_SHORT).show()
    }

    override fun onFilterClick(filter: ConnectionFilter) {
        this.filter = filter
    }

}