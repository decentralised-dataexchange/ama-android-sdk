package io.igrant.data_wallet.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.adapter.RequestListAdapter
import io.igrant.data_wallet.events.GoHomeEvent
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ConnectionMessageListener
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.utils.ExchangeUtils.showExchangeData
import io.igrant.data_wallet.utils.MessageTypes.Companion.SHARE_REQUEST
import io.igrant.data_wallet.utils.SearchUtils
import io.igrant.data_wallet.utils.WalletRecordType
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class RequestActivity : BaseActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var llProgressBar: LinearLayout

    private lateinit var adapter: RequestListAdapter
    private var connectionMessageList: ArrayList<Record> = ArrayList()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request)
        initViews()
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
        initToolbar()
        setUpAdapter()
        setUpConnectionMessagesList()
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.settings_notifications)
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

    private fun initViews() {
        rvRequests = findViewById(R.id.rvRequests)
        llErrorMessage = findViewById(R.id.llErrorMessage)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun setUpAdapter() {
        adapter = RequestListAdapter(connectionMessageList, object : ConnectionMessageListener {
            override fun onConnectionMessageClick(record: Record, name: String) {
                val message =
                    WalletManager.getGson.fromJson(record.value, Notification::class.java)
                if (message.type == SHARE_REQUEST) {
                    showExchangeData(record, this@RequestActivity)
                } else {
                    val intent =
                        Intent(this@RequestActivity, OfferCertificateActivity::class.java)
                    intent.putExtra(
                        OfferCertificateActivity.EXTRA_CERTIFICATE_PREVIEW,
                        record
                    )
                    startActivity(intent)
                }
            }

        })
        rvRequests.adapter = adapter
    }

    private fun setUpConnectionMessagesList() {
        try {
            val connectionMessageResponse =
                SearchUtils.searchWallet(
                    WalletRecordType.MESSAGE_RECORDS,
                    "{}"
                )
            if ((connectionMessageResponse.totalCount ?: 0) > 0) {
                llErrorMessage.visibility = View.GONE
                connectionMessageList.clear()
                connectionMessageList.addAll(connectionMessageResponse.records ?: ArrayList())
                adapter.notifyDataSetChanged()
            } else {
                llErrorMessage.visibility = View.VISIBLE
            }
        } catch (e: Exception) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ReceiveExchangeRequestEvent) {
        setUpConnectionMessagesList()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGoHomeEvent(event: GoHomeEvent) {
        onBackPressed()
    }

    override fun onDestroy() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "InitializeActivity"
    }
}