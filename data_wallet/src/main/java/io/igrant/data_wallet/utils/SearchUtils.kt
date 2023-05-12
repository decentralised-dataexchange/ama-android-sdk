package io.igrant.data_wallet.utils

import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.walletSearch.SearchResponse
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.non_secrets.WalletSearch

object SearchUtils {

    fun searchWallet(type: String,queryJson:String) :SearchResponse{
        try {
            val search = WalletSearch.open(
                WalletManager.getWallet,
                type,
                queryJson,
                "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
            ).get()

            val searchResponse =
                WalletSearch.searchFetchNextRecords(
                    WalletManager.getWallet,
                    search,
                    1000
                ).get()

            WalletManager.closeSearchHandle(search)
            return WalletManager.getGson.fromJson(searchResponse, SearchResponse::class.java)
        } catch (e: Exception) {
            return SearchResponse()
        }
    }

    fun searchForCredentials(credDefId: String?, schemaId: String?): String {
        var filter = "{}"
        if (credDefId != null && schemaId != null) {
            filter = "{\n" +
                    "  \"cred_def_id\": \"$credDefId\",\n" +
                    "  \"schema_id\":\"$schemaId\"\n" +
                    "}"
        } else if (credDefId != null) {
            filter = "{\n" +
                    "  \"cred_def_id\": \"$credDefId\"\n" +
                    "}"
        } else if (schemaId != null) {
            filter = "{\n" +
                    "  \"schema_id\": \"$schemaId\"\n" +
                    "}"
        } else {
            filter = "{}"
        }

        return Anoncreds.proverGetCredentials(WalletManager.getWallet, filter).get();
    }
}