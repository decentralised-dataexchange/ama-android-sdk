package io.igrant.data_wallet.utils

import android.content.Context
import android.content.Intent
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import io.igrant.data_wallet.activity.ConnectionListActivity
import io.igrant.data_wallet.activity.HistoryActivity
import io.igrant.data_wallet.activity.InitializeActivity
import io.igrant.data_wallet.activity.InitializeActivity.Companion.DEEP_LINK
import io.igrant.data_wallet.activity.RequestActivity
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.indy.PoolManager
import io.igrant.data_wallet.indy.WalletManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hyperledger.indy.sdk.LibIndy
import java.io.File
import io.igrant.data_wallet.indy.PoolUtils

object DataWallet {

    fun initializeSdk(
        context: Context,
        mListener: InitializeWalletCallback,
        ledgerNetworkType: Int
    ) {
        if (LedgerNetworkType.getLedgerData(context).isNotEmpty()) {
            initWallet(context, mListener, ledgerNetworkType)
            InitialSync.callInitialApis(context, null)
        } else {
            InitialSync.callInitialApis(context, object : InitialSyncListeners {
                override fun onSuccess() {
                    super.onSuccess()
                    initWallet(context, mListener, ledgerNetworkType)
                }
            })
        }
    }

    fun releaseSdk() {
        WalletManager.closeWallet
        PoolManager.getPool?.close()
        PoolManager.removePool
    }

    fun showWallet(context: Context) {
        if (PoolManager.getPool == null) {

        } else {
            val intent = Intent(context, InitializeActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun showConnections(context: Context) {
        if (PoolManager.getPool == null) {

        } else {
            val intent = Intent(context, ConnectionListActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun showMySharedData(context: Context) {
        if (PoolManager.getPool == null) {

        } else {
            val intent = Intent(context, HistoryActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun showNotifications(context: Context) {
        if (PoolManager.getPool == null) {

        } else {
            val intent = Intent(context, RequestActivity::class.java)
            context.startActivity(intent)
        }
    }

    fun processDeepLink(context: Context, deeplink: String) {
        if (PoolManager.getPool == null) {

        } else {
            val intent = Intent(context, InitializeActivity::class.java)
            intent.putExtra(DEEP_LINK, deeplink)
            context.startActivity(intent)
        }
    }

    fun openShareData(context: Context) {
        if (PoolManager.getPool == null) {

        } else {
            val intent = Intent(context, InitializeActivity::class.java)
            intent.putExtra(InitializeActivity.SHOW_SHARE_DATA_SCANNER, true)
            context.startActivity(intent)
        }
    }
    private fun initWallet(
        context: Context,
        mListener: InitializeWalletCallback,
        ledgerNetworkType: Int
    ) {
        CoroutineScope(Dispatchers.IO).launch {

            if (PoolManager.getPool == null) {
                withContext(Dispatchers.Main) {
                    mListener.progressUpdate(InitializeWalletState.INITIALIZE_WALLET_STARTED)
                }

                withContext(Dispatchers.Default) {
                    val externalFilesDir: File? = context.getExternalFilesDir(null)
                    val path: String = externalFilesDir!!.absolutePath

                    try {
                        Os.setenv("EXTERNAL_STORAGE", path, true)
                    } catch (e: ErrnoException) {
                        e.printStackTrace()
                    }

                    System.loadLibrary("indy")
                    LibIndy.init()
                }

                withContext(Dispatchers.Main) {
                    mListener.progressUpdate(InitializeWalletState.INITIALIZE_WALLET_EXTERNAL_FILES_LOADED)
                }


                val pool = PoolUtils.createAndOpenPoolLedger(ledgerNetworkType, context)
                PoolManager.setPool(pool)

                withContext(Dispatchers.Main) {
                    mListener.progressUpdate(InitializeWalletState.POOL_CREATED)
                }

                WalletManager.getWallet

            }
            withContext(Dispatchers.Main) {
                mListener.progressUpdate(InitializeWalletState.WALLET_OPENED)
            }

        }
    }
}

interface InitializeWalletCallback {
    fun progressUpdate(progress: Int) {}
}

object InitializeWalletState {
    const val INITIALIZE_WALLET_STARTED = 0
    const val INITIALIZE_WALLET_EXTERNAL_FILES_LOADED = 1
    const val POOL_CREATED = 2
    const val WALLET_OPENED = 3
}