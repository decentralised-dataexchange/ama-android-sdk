package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager

class OpenWalletTask(private val commonHandler: CommonHandler) :
    AsyncTask<Void, Void, Void>() {

    private val TAG = "OpenWalletTask"

    override fun doInBackground(vararg p0: Void?): Void? {
        WalletManager.getWallet
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        commonHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        commonHandler.taskCompleted()
    }
}