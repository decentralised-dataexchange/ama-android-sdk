package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object OpenWalletTask {
    fun openWallet(commonHandler: CommonHandler) {
        commonHandler.taskStarted()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            //Background work here
            handler.post {
                WalletManager.getWallet
                commonHandler.taskCompleted()
            }
        }

    }
}