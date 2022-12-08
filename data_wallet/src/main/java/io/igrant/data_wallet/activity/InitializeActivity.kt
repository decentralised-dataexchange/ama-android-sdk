package io.igrant.data_wallet.activity

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import io.igrant.data_wallet.R
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.fragment.UrlExtractFragment
import io.igrant.data_wallet.listeners.InitialActivityFunctions
import io.igrant.data_wallet.utils.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class InitializeActivity : BaseActivity(), InitialActivityFunctions,UrlExtractFragment.ProgressListener, ExtractListeners {

    companion object {
        const val TAG = "InitializeActivity"
        const val DEEP_LINK = "io.igrant.data_wallet.activity.InitializeActivity.deep_link"
    }

    //views
    private lateinit var toolbar: Toolbar
    private lateinit var llProgressBar: LinearLayout
    private lateinit var clLoading: ConstraintLayout

    private lateinit var tvLoadingStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initialize)
        initViews()
        setUpToolbar()
        initFragment()
        getIntentData()
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
    }

    private fun getIntentData() {
        if (intent.hasExtra(DEEP_LINK)){
            try {
                val uri: Uri = try {
                    Uri.parse(intent.getStringExtra(DEEP_LINK))
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

                                    extractUrlFunction(deepLinkUri)
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
                            extractUrlFunction(uri)
                        }

                } else {
                    extractUrlFunction(uri)
                }
            } catch (e: Exception) {

            }
            intent.removeExtra(DEEP_LINK)
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        llProgressBar = findViewById(R.id.llProgressBar)
        clLoading = findViewById(R.id.clLoadingScreen)
        tvLoadingStatus = findViewById(R.id.tvLoadingStatus)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initFragment() {
        if (supportFragmentManager != null)
            NavigationUtils.showWalletFragment(supportFragmentManager, false)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onGoHomeEvent(event: ReceiveExchangeRequestEvent) {
    }

    override fun extractUrlFunction(uri: Uri) {
        extractUrl(
            uri
        )
    }

    private var frag: UrlExtractFragment? = null

    private fun extractUrl(uri: Uri) {

        if (supportFragmentManager
                .findFragmentByTag("counter_fragment") != null
        )
            frag = supportFragmentManager
                .findFragmentByTag("counter_fragment") as UrlExtractFragment
        if (frag == null) {
            frag = UrlExtractFragment.newInstance()
            supportFragmentManager.beginTransaction().add(frag!!, "counter_fragment")
                .commit()
            frag?.setProgressListener(this)
        }

        if (frag != null)
            frag?.extractUrl(uri)

    }

    override fun updateProgress(progress: Int) {
        llProgressBar.visibility = progress
    }

    override fun error(explain: String) {
        llProgressBar.visibility = View.GONE
        Toast.makeText(this, explain, Toast.LENGTH_SHORT).show()
    }
}

interface ExtractListeners {
    fun extractUrlFunction(uri: Uri)
}