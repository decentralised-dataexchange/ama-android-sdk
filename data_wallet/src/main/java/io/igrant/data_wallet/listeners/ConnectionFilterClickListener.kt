package io.igrant.data_wallet.listeners

import io.igrant.data_wallet.models.ConnectionFilter


interface ConnectionFilterClickListener {
    fun onFilterClick(filter: ConnectionFilter)
}