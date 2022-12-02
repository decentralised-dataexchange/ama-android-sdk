package io.igrant.data_wallet.utils

import android.content.Context
import android.preference.PreferenceManager
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.indy.LedgerNetworkType
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.LastUpdatedTimesResponse
import io.igrant.data_wallet.models.ledger.Ledger
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object InitialSync {

    private const val EXTRA_LAST_UPDATED_TIMES =
        "io.igrant.mobileagent.utils.InitialSync.lastUpdatedTIme"

    private const val EXTRA_PK_PASS_KEY_VALUES =
        "io.igrant.mobileagent.utils.InitialSync.pkpassKeyValues"

    private const val EXTRA_BLINK_KEY_VALUES =
        "io.igrant.mobileagent.utils.InitialSync.blinkKeyValues"

    fun callInitialApis(context: Context, mListener: InitialSyncListeners?) {
        ApiManager.api.getService()
            ?.getLastUpdatedTimes("https://raw.githubusercontent.com/L3-iGrant/datawallet-metadata/main/last_updated.json")
            ?.enqueue(object : Callback<LastUpdatedTimesResponse> {
                override fun onFailure(
                    call: Call<LastUpdatedTimesResponse>,
                    t: Throwable
                ) {

                }

                override fun onResponse(
                    call: Call<LastUpdatedTimesResponse>,
                    response: Response<LastUpdatedTimesResponse>
                ) {
                    val localTime = getLastUpdatedTimes(context)
                    if (localTime.ledgerNetwork != response.body()?.ledgerNetwork)
                        callLedgerNetworks(context, mListener)

//                    if (localTime.pkpassBoardingPass != response.body()?.pkpassBoardingPass)
//                        callPkPassData(context)
//
//                    if (localTime.blinks != response.body()?.blinks)
//                        callBlinkData(context)

                    saveLastUpdatedTime(context, response.body())
                }
            })
    }

    fun callLedgerNetworks(context: Context, mListener: InitialSyncListeners?) {
        ApiManager.api.getService()
            ?.getLedger("https://raw.githubusercontent.com/L3-iGrant/datawallet-metadata/main/ledgers.json")
            ?.enqueue(object : Callback<ArrayList<Ledger>> {
                override fun onFailure(
                    call: Call<ArrayList<Ledger>>,
                    t: Throwable
                ) {

                }

                override fun onResponse(
                    call: Call<ArrayList<Ledger>>,
                    response: Response<ArrayList<Ledger>>
                ) {
                    LedgerNetworkType.saveLedgerData(context, response.body())
                    mListener?.onSuccess()
                }
            })
    }

//    fun callPkPassData(context: Context) {
//        ApiManager.api.getService()
//            ?.getPkPassData("https://raw.githubusercontent.com/L3-iGrant/datawallet-metadata/main/pkpass_boarding_pass.json")
//            ?.enqueue(object : Callback<HashMap<String, ArrayList<String>>> {
//                override fun onFailure(
//                    call: Call<HashMap<String, ArrayList<String>>>,
//                    t: Throwable
//                ) {
//
//                }
//
//                override fun onResponse(
//                    call: Call<HashMap<String, ArrayList<String>>>,
//                    response: Response<HashMap<String, ArrayList<String>>>
//                ) {
//                    if (response.code() == 200)
//                        savePkPassKeyValues(context, response.body())
//                }
//            })
//    }

//    fun callBlinkData(context: Context) {
//        ApiManager.api.getService()
//            ?.getBlinkData("https://raw.githubusercontent.com/L3-iGrant/datawallet-metadata/main/blinks.json")
//            ?.enqueue(object : Callback<ArrayList<BlinkData>> {
//                override fun onFailure(
//                    call: Call<ArrayList<BlinkData>>,
//                    t: Throwable
//                ) {
//
//                }
//
//                override fun onResponse(
//                    call: Call<ArrayList<BlinkData>>,
//                    response: Response<ArrayList<BlinkData>>
//                ) {
//                    if (response.code() == 200)
//                        saveBlinkValues(context, response.body())
//                }
//            })
//    }

    fun saveLastUpdatedTime(context: Context, data: LastUpdatedTimesResponse?) {
        val str = WalletManager.getGson.toJson(data)
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString(EXTRA_LAST_UPDATED_TIMES, str)
            .apply()
    }

    fun getLastUpdatedTimes(context: Context): LastUpdatedTimesResponse {
        val sharedPreferences =
            PreferenceManager.getDefaultSharedPreferences(context)

        var str = sharedPreferences.getString(EXTRA_LAST_UPDATED_TIMES, "{}")

        if (str == "null")
            str = "{}"
        return WalletManager.getGson.fromJson(
            str,
            LastUpdatedTimesResponse::class.java
        )
    }

//    fun savePkPassKeyValues(context: Context, data: HashMap<String, ArrayList<String>>?) {
//        val str = WalletManager.getGson.toJson(data)
//        PreferenceManager.getDefaultSharedPreferences(context).edit()
//            .putString(EXTRA_PK_PASS_KEY_VALUES, str)
//            .apply()
//    }

//    fun getPkPassKeyValues(context: Context): HashMap<String, ArrayList<String>> {
//        val sharedPreferences =
//            PreferenceManager.getDefaultSharedPreferences(context)
//
//        val str = sharedPreferences.getString(EXTRA_PK_PASS_KEY_VALUES, "{}")
//
//        return WalletManager.getGson.fromJson(
//            str,
//            PkPassResponse::class.java
//        )
//    }

//    fun saveBlinkValues(context: Context, data: ArrayList<BlinkData>?) {
//        val str = WalletManager.getGson.toJson(data)
//        PreferenceManager.getDefaultSharedPreferences(context).edit()
//            .putString(EXTRA_BLINK_KEY_VALUES, str)
//            .apply()
//    }
//
//    fun getBlinkUrl(context: Context, key: String): String {
//
//        val index: Int? = key.lastIndexOf(':')
//        val id = key.substring(
//            0,
//            (index ?: 0) + 1
//        )
//        val sharedPreferences =
//            PreferenceManager.getDefaultSharedPreferences(context)
//
//        val str = sharedPreferences.getString(EXTRA_BLINK_KEY_VALUES, "[]")
//
//        val map = WalletManager.getGson.fromJson(
//            str,
//            BlinkResponse::class.java
//        )
//
//        val tempList = map.filter { it.prefix == id }
//        return if (tempList.isNotEmpty()) tempList[0].url ?: "" else ""
//    }
}

// Listeners to communicate with the parent class
interface InitialSyncListeners {
    fun onSuccess() {}

    fun onFailure() {}
}