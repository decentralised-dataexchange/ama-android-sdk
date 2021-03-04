package io.igrant.data_wallet.listeners

interface ConnectionClickListener {
    fun onConnectionClick(connection: String, did: String)
}