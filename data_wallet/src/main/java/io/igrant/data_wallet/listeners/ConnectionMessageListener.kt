package io.igrant.data_wallet.listeners

import io.igrant.data_wallet.models.walletSearch.Record


interface ConnectionMessageListener {
    fun onConnectionMessageClick(record: Record, name:String)
}