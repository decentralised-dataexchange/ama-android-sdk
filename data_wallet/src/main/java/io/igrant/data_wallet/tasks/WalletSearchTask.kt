package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import io.igrant.data_wallet.handlers.SearchHandler
import io.igrant.data_wallet.models.walletSearch.SearchResponse
import io.igrant.data_wallet.utils.SearchUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object WalletSearchTask {
    private var searchResponseObj = SearchResponse()
    fun searchWallet(
        searchHandler: SearchHandler,
        type: String?,
        queryJson: String?
    ) {

        searchHandler.taskStarted()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        executor.execute {
            //Background work here
            handler.post {
                val search = SearchUtils.searchWallet(
                    type ?: "",
                    queryJson ?: "{}"
                )
                searchResponseObj = search

                searchHandler.taskCompleted(searchResponseObj)
            }
        }
    }
}