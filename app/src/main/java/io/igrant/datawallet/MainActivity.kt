package io.igrant.datawallet

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.utils.DataWallet
import io.igrant.data_wallet.utils.InitializeWalletCallback
import io.igrant.data_wallet.utils.InitializeWalletState

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wallet = findViewById<Button>(R.id.wallet)
        val connections = findViewById<Button>(R.id.btConnections)

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
    }
}