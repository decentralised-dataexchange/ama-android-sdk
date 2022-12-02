package io.igrant.data_wallet.utils

import android.content.Context
import io.igrant.data_wallet.indy.PoolUtils
import org.hyperledger.indy.sdk.IndyException
import org.hyperledger.indy.sdk.pool.Pool
import org.hyperledger.indy.sdk.pool.PoolJSONParameters.OpenPoolLedgerJSONParameter
import java.io.IOException
import java.util.concurrent.ExecutionException

object PoolUtils {

    fun createAndOpenPoolLedger(type: Int, context: Context?): Pool {
        val poolName = PoolUtils.createPoolLedgerConfig(type, context)
        val config = OpenPoolLedgerJSONParameter(null, null)
        Pool.setProtocolVersion(2)
        return Pool.openPoolLedger(poolName, config.toJson()).get()
    }
}