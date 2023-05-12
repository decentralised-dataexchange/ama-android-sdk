package io.igrant.datawallet

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.dataAgreement.DataAgreementUtils

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val wallet = findViewById<Button>(R.id.wallet)
        val connections = findViewById<Button>(R.id.btConnections)
        val mySharedHistory = findViewById<Button>(R.id.btMySharedHistory)
        val btNotifications = findViewById<Button>(R.id.btNotifications)
        val btShareData = findViewById<Button>(R.id.btShareData)
        val btDataAgreementPolicy = findViewById<Button>(R.id.btDataAgreementPolicy)

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

        mySharedHistory.setOnClickListener {
            DataWallet.showMySharedData(this)
        }

        btDataAgreementPolicy.setOnClickListener {
            DataAgreementUtils.fetchDataAgreement(
                "ApiKey eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VyaWQiOiI2NDVhNDE0YmI5YjA1NTAwMDE1MGIyNDciLCJvcmdpZCI6IiIsImVudiI6IiIsImV4cCI6MTcxNDc0MDg4Nn0.u6pBpv12ZfdHYMPoQHYR-oBR9ZOZVeHiChaQ8yiEMxE",
                "645a4172b9b055000150b248",
                "0900ccb0-73d5-4175-ae79-a3fc14a14e9e",
                this
            )
        }

        btNotifications.setOnClickListener {
            DataWallet.showNotifications(this)
        }

        btShareData.setOnClickListener {
            DataWallet.openShareData(this)
        }
    }

    private fun getFirebaseDynamicLink() {
        if (intent.scheme == "didcomm") {
            DataWallet.processDeepLink(this, intent.data.toString())
        }
    }
}