package io.igrant.data_wallet.tasks

import android.os.AsyncTask
import io.igrant.data_wallet.handlers.CommonHandler
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.presentationExchange.PresentationExchange
import io.igrant.data_wallet.utils.PresentationExchangeStates
import io.igrant.data_wallet.utils.SearchUtils
import io.igrant.data_wallet.utils.WalletRecordType
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject

class UpdatePresentProofToAckTask(val commonHandler: CommonHandler, val jsonObject: JSONObject) :
    AsyncTask<Void, Void, Void>() {

    private val TAG = "UpdatePresentProofToAckTask"

    override fun doInBackground(vararg p0: Void?): Void? {
        val presentProof = SearchUtils.searchWallet(
            WalletRecordType.PRESENTATION_EXCHANGE_V10,
            "{\"thread_id\":\"${JSONObject(jsonObject.getString("message")).getJSONObject("~thread")
                .getString("thid")}\"}"
        )

        if (presentProof.totalCount ?: 0 > 0) {
            val presentProofObject = WalletManager.getGson.fromJson(
                presentProof.records?.get(0)?.value,
                PresentationExchange::class.java
            )
            presentProofObject.state = PresentationExchangeStates.PRESENTATION_ACK

            WalletRecord.updateValue(
                WalletManager.getWallet, WalletRecordType.PRESENTATION_EXCHANGE_V10,
                presentProof.records?.get(0)?.id, WalletManager.getGson.toJson(presentProofObject)
            )
        }

        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        commonHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        commonHandler.taskCompleted()
    }
}