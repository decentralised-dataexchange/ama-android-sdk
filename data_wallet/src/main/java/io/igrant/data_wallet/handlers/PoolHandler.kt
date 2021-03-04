package io.igrant.data_wallet.handlers

import org.hyperledger.indy.sdk.pool.Pool

interface PoolHandler {
    fun taskCompleted(pool: Pool)
    fun taskStarted()
}