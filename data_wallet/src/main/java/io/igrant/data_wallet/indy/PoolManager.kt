package io.igrant.data_wallet.indy

import org.hyperledger.indy.sdk.pool.Pool

object PoolManager {
    fun setPool(pool: Pool) {
        PoolManager.pool = pool
    }

    private var pool: Pool? = null

    val getPool: Pool?
        get() {
//            if (pool == null) {
//
//            }
            return pool
        }

    val removePool:Int get() {
        pool =null
        return 0
    }
}