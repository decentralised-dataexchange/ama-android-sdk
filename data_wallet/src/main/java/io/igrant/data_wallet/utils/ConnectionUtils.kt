package io.igrant.data_wallet.utils

import android.content.Context
import android.content.Intent
import android.util.Base64
import com.google.gson.Gson
import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.walletSearch.SearchResponse
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONObject

object ConnectionUtils {

    /**
     * to check whether the connection already existing with the invitation key
     */
    fun checkIfConnectionAvailable(invitationKey: String): Boolean {
        val gson = Gson()
        val search = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.CONNECTION,
            "{\n" +
                    "  \"invitation_key\":\"$invitationKey\"\n" +
                    "}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)
        val result = gson.fromJson(connection, SearchResponse::class.java)
        return result.totalCount ?: 0 > 0
    }

    fun getConnectionWithInvitationKey(invitationKey: String): MediatorConnectionObject? {
        val gson = Gson()
        val search = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.CONNECTION,
            "{\n" +
                    "  \"invitation_key\":\"$invitationKey\"\n" +
                    "}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)
        val result = gson.fromJson(connection, SearchResponse::class.java)
        if (result.totalCount ?: 0 > 0) {
            return WalletManager.getGson.fromJson(
                result.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
        } else {
            return null
        }
    }

    fun getConnection(senderVk: String): MediatorConnectionObject? {

        val connectionSearchResult = SearchUtils.searchWallet(
            WalletRecordType.DID_KEY,
            "{\"key\": \"$senderVk\"}"
        )

        val did = connectionSearchResult.records?.get(0)?.tags?.get("did")


        val connection =
            SearchUtils.searchWallet(WalletRecordType.CONNECTION, "{\"their_did\":\"$did\"}")

        return if (connection.totalCount ?: 0 > 0)
            WalletManager.getGson.fromJson(
                connection.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
        else
            null
    }

    fun saveConnectionAndExchangeData(
        context: Context,
        data: String,
        proofRequest: JSONObject,
        qrId: String
    ) {
        var invitation: Invitation? = null
        try {
            val json =
                Base64.decode(
                    data,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))

            invitation = WalletManager.getGson.fromJson(json, Invitation::class.java)
        } catch (e: Exception) {
        }
        if (invitation != null)
            sendProposal(context,proofRequest, invitation, qrId)
//        else
//            Toast.makeText(
//                context,
//                resources.getString(R.string.err_unexpected),
//                Toast.LENGTH_SHORT
//            ).show()
    }

    private fun sendProposal(
        context: Context,
        proofRequest: JSONObject,
        invitation: Invitation,
        qrId: String
    ) {
        val intent = Intent(context, ProposeAndExchangeDataActivity::class.java)
        intent.putExtra(ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_PROPOSAL, proofRequest.toString())
        intent.putExtra(ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_INVITATION, invitation)
        intent.putExtra(ProposeAndExchangeDataActivity.EXTRA_PRESENTATION_QR_ID, qrId)
        context.startActivity(intent)
    }
}