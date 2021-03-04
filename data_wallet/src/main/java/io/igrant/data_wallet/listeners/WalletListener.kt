package io.igrant.data_wallet.listeners

import io.igrant.data_wallet.models.wallet.WalletModel


interface WalletListener {
    fun onDelete(id:String,position:Int)
    fun onItemClick(wallet: WalletModel)
}