package io.igrant.data_wallet.indy

import android.content.Context
import android.preference.PreferenceManager
import io.igrant.data_wallet.models.ledger.Ledger
import io.igrant.data_wallet.models.ledger.LedgerList

object LedgerNetworkType {

//    const val IGRANT_SANDBOX = 0
//    const val SOVRIN_BUILDER = 1
//    const val SOVRIN_LIVE = 2
//    const val SOVRIN_SANDBOX = 3
//
//    //    const val B_COVRIN_TEST = 4
////    const val B_COVRIN_DEV = 5
////    const val B_COVRIN_PRODUCTION = 6
//    const val IGRANT_SANDBOX_OLD = 7

    const val SELECTED_NETWORK = "LedgerNetworkType.selected_network"
    const val LEDGER_DATA = "LedgerNetworkType.ledgerData"

    @JvmStatic
    fun getConfigName(): String {
        return "default"
    }

    @JvmStatic
    fun getFileName(): String {
        return "temp.txt"
    }

    @JvmStatic
    fun saveLedgerData(context: Context, data: ArrayList<Ledger>?) {
        val str = WalletManager.getGson.toJson(data)
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(LEDGER_DATA, str)
            .apply()
    }

    @JvmStatic
    fun getLedgerData(context: Context): ArrayList<Ledger> {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

        var str = sharedPreferences.getString(LEDGER_DATA, "[]")

        if(str == "null")
            str = "[]"
        return WalletManager.getGson.fromJson(
            str,
            LedgerList::class.java
        )
    }

    @JvmStatic
    fun getTransactionV2(context: Context, type: Int): String {

        var data = ""
        val list = getLedgerData(context).filter { it.id == type }

        if (list.isNotEmpty()) {
            data = list[0].genesisString ?: ""
        }else {
            data = "{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"IGRANTNode1\",\"blskey\":\"gMYJ7XBmKqbsefCvSwE3Qa3SWGYeT2zgTbyLd6uP8gzsCFVko8SupotKfuPFEHCp9dMjQMe71cYBeHJ1JwqSWHWxyjTcgB5izFWKfqZTLQ9NVYLDwN2MgncGqMcgazbzaoj5C9yX3hb9CnvGMUBpy12pRuy83pD792nhPbev1Meodo\",\"blskey_pop\":\"RDHuxy4TdBGkJG2C9DhR66bKPGuuC28TCJyDMJ4csYUVVCtUSbWxoXfbZN6XWqmNMGPDfW2eU1eVeGu4ApXVkuhBS56LbqucZJS9sjWEskUPFbax2EfKp1qzK3iDQmzQScwQmHFhro1Xuq1UGtzbDngBn66J4uipvWn9o7kcGEunmd\",\"client_ip\":\"35.232.158.72\",\"client_port\":9702,\"node_ip\":\"35.232.158.72\",\"node_port\":9701,\"services\":[\"VALIDATOR\"]},\"dest\":\"DWMSWWfar9XijjTbVuQFYPywUCcuWYZ9jqQ1jwLqEHDa\"},\"metadata\":{\"from\":\"WXmt6S9fn64dm9z4jJQnSa\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":1,\"txnId\":\"5f3ae0491e0b3af4498408d0a226b7940637976627f8b125bf4c1bb80a271576\"},\"ver\":\"1\"}\\n{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"IGRANTNode2\",\"blskey\":\"uvmCbHagzwjm9kZYu17Q3zurpF7ZnSc22u4JH3ToKocaHyDcrTHsb3TYekf1ssMw9Tueu3q61MYK8J32RHGjjS4jdj6H1y6aoKBitDRoYCGXkAF5pFwrcGGXGaDy5qhNn1LU66QNQpZmCP5z1NmcXZCNEoYfVuLM4sZ5TkW6RTVM8S\",\"blskey_pop\":\"QnQEWdUyNp5DHC5FURX1nPGwbw3yE6cX7MLiGdsprnQzUHnUZMnmzrsR4aDEpywS4ETeV4jEKwD7bQvZTYwD3BKnFtzuHW2itHWnKnsL9Y3xA7HzaBWdtPHKqaXdjcDma1U8o88PgNXLHDfDJeWhzjvj3r5uEwyBDPX5a3EMZJnCDd\",\"client_ip\":\"35.232.158.72\",\"client_port\":9704,\"node_ip\":\"35.232.158.72\",\"node_port\":9703,\"services\":[\"VALIDATOR\"]},\"dest\":\"GJMk4wzvz6vsKmMvRu8deeTya4yDNsoyKpCkeUgAdfaQ\"},\"metadata\":{\"from\":\"KE9LjBPAUyNqU6LQH65MNG\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":2,\"txnId\":\"e8467b4aa9f153607127ce4a0dfd883d98f8dc7865c7ffd60cc8e263a881e09b\"},\"ver\":\"1\"}\\n{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"IGRANTNode3\",\"blskey\":\"4LXv3PQkPmtTgmEsbwghEpaPoo221NiH6NNQ6XxBbmLidAMg3EUWHHw4FdV18H2JY4yt3NMVS19tPPAhSUP8FFb7dokDje82umtBDNai8Yh4vxUXMxFqpWULBpDXzKZr11JyFCQFVCKfMn8eDMykHmzTSDnQJhk4aquX9HqGjovcSkE\",\"blskey_pop\":\"RDSLtTfPNpeBERtE8jSZj9BQzVBZd6yJTtkmcMphFrm97vX2RtaxzemMkJdvDBb6JEiBVH9GNVRa2wGCFLgMuohBRjaS2oXiAyqC5ZtMZZmQWR2ifT6jHQ5FREgU5ykzRP5yegget9FWFadKuVrBivjioESpiPjzq4BuC969bTj3u1\",\"client_ip\":\"35.232.158.72\",\"client_port\":9706,\"node_ip\":\"35.232.158.72\",\"node_port\":9705,\"services\":[\"VALIDATOR\"]},\"dest\":\"4wVLGL5sGG6XiMyGk8j2tdWFoDuNQaVdUQrdAYS7wfmz\"},\"metadata\":{\"from\":\"V7jJr1qrrQWn1UrWtWqFCc\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":3,\"txnId\":\"2337341c1bd30581fbb0deb50673d72b416a8075c94901b5f88d87a4bcc4487b\"},\"ver\":\"1\"}\\n{\"reqSignature\":{},\"txn\":{\"data\":{\"data\":{\"alias\":\"IGRANTNode4\",\"blskey\":\"2SBBqEMHG6wHHh29nzDTisjh85DCQtHWLsDhz6UUnLXNBxg9JpLQir8yakkG28nyoXGTV2mvoiT4hyn4r15yysooFk9KZs1hNzskoKZtyn1f9q4xJsUtuqYGdvDdPSmS6ENGXZJxafGdxG3QSEYNSuWcQBz54vHQyZrVw3TisCodwyp\",\"blskey_pop\":\"QwJyEtv8yBghyHq25UnsE5oSRCT6FzC5xNZ6dgu3ggqjPDipCNKx8HgrJxQwAuAxGacWDbAVMKwdWEZXmXNamAktir4ennWGM5wgwdEZ7fZ7jnc8xC2h8zRDXbXW1gVq4uCAaXrQRZiiDDDW4eY6DGXZ3UPCyyNiUpc58CGH619e9V\",\"client_ip\":\"35.232.158.72\",\"client_port\":9708,\"node_ip\":\"35.232.158.72\",\"node_port\":9707,\"services\":[\"VALIDATOR\"]},\"dest\":\"EzJ2GxbTkjwf47G3YidVCtdhvuDnn7vhXy64zdKpLGie\"},\"metadata\":{\"from\":\"YUN2gwCXcexykNDgz18joq\"},\"type\":\"0\"},\"txnMetadata\":{\"seqNo\":4,\"txnId\":\"1f70171ba83eee751d8fcffb3c9f0fcb38913bd5aa4659f42cf0574be3c7a04a\"},\"ver\":\"1\"}\n"
        }

        return data
    }

    @JvmStatic
    fun saveSelectedNetwork(
        context: Context,
        value: Int?
    ) {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPreferences.edit()
        editor.putInt(SELECTED_NETWORK, value ?: 0)
        editor.commit()
    }

    @JvmStatic
    fun getSelectedNetwork(context: Context): Int {
        return try {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            sharedPreferences.getInt(SELECTED_NETWORK, 0)
        } catch (e: Exception) {
            0
        }
    }

    @JvmStatic
    fun getSelectedNetworkName(context: Context): String {
        var type: Int
        try {
            val sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(context)
            type = sharedPreferences.getInt(SELECTED_NETWORK, 0)
        } catch (e: Exception) {
            type = 0
        }

        val list = getLedgerData(context).filter { it.id == type }

        return if (list.isNotEmpty())
            list[0].name ?: "Igrant Sandbox"
        else
            "Igrant Sandbox"
    }
}