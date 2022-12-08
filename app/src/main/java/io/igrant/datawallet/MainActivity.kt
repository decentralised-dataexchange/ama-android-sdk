package io.igrant.datawallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.utils.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wallet = findViewById<Button>(R.id.wallet)
        val connections = findViewById<Button>(R.id.btConnections)
        val btNotifications = findViewById<Button>(R.id.btNotifications)

        wallet.visibility = View.GONE
        DataWallet.initializeSdk(
            this,
            object : InitializeWalletCallback {
                override fun progressUpdate(progress: Int) {
                    when (progress) {
                        InitializeWalletState.INITIALIZE_WALLET_STARTED -> {
//                            tvLoadingStatus.text =
//                                resources.getString(io.igrant.data_wallet.R.string.connection_configuring_pool)
                        }
                        InitializeWalletState.INITIALIZE_WALLET_EXTERNAL_FILES_LOADED -> {
//                            tvLoadingStatus.text =
//                                resources.getString(io.igrant.data_wallet.R.string.connection_configuring_pool)
                        }
                        InitializeWalletState.POOL_CREATED -> {
//                            tvLoadingStatus.text =
//                                resources.getString(io.igrant.data_wallet.R.string.connection_finishing)
                        }
                        InitializeWalletState.WALLET_OPENED -> {
                            wallet.visibility = View.VISIBLE
                            DataWalletConfigurations.registerForSubscription(this@MainActivity)
                            getFirebaseDynamicLink()
                        }
                    }
                }
            }, LedgerNetworkType.getSelectedNetwork(this)
        )
        wallet.setOnClickListener {
            DataWallet.showWallet(this)
        }

        connections.setOnClickListener {
            DataWallet.showConnections(this)
        }

        btNotifications.setOnClickListener {
            DataWallet.showNotifications(this)
        }
    }

    private fun getFirebaseDynamicLink() {
        if (intent.scheme == "didcomm") {
            DataWallet.processDeepLink(this, intent.data.toString())
        }
    }
}