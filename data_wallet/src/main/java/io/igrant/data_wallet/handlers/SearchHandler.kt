package io.igrant.data_wallet.handlers

import io.igrant.data_wallet.models.walletSearch.SearchResponse

interface SearchHandler {
    fun taskCompleted(searchResponse: SearchResponse)
    fun taskStarted(){}
    fun onError(){}
}