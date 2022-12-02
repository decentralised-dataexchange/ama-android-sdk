package io.igrant.data_wallet.listeners

import io.igrant.data_wallet.models.history.History

interface HistoryClickListener {

    fun onClick(history: History)
}