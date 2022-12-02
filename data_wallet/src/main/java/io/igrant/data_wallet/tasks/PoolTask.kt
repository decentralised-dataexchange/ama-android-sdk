package io.igrant.data_wallet.tasks

import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import io.igrant.data_wallet.handlers.PoolHandler
import io.igrant.data_wallet.utils.PoolUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object PoolTask {
    fun executePoolTask(
        poolHandler: PoolHandler,
        networkType: Int,
        context: Context
    ) {
        poolHandler.taskStarted()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            handler.post {
                val pool = PoolUtils.createAndOpenPoolLedger(networkType, context)

                //commenting for performance
//        val acceptanceMech = io.igrant.data_wallet.models.ledger.Ledger.buildGetAcceptanceMechanismsRequest(null, -1, null).get()
//
//        io.igrant.data_wallet.models.ledger.Ledger.submitRequest(pool, acceptanceMech).get()
//
//        val agreementResponse = io.igrant.data_wallet.models.ledger.Ledger.buildGetTxnAuthorAgreementRequest(null, null).get()
//
//        io.igrant.data_wallet.models.ledger.Ledger.submitRequest(pool, agreementResponse).get()
                //commenting for performance

                poolHandler.taskCompleted(pool)
            }
        }
    }
}