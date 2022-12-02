package io.igrant.data_wallet.models.reciept

import com.google.gson.annotations.SerializedName

data class Logs(
    @SerializedName("address") var address: String? = null,
    @SerializedName("blockHash") var blockHash: String? = null,
    @SerializedName("blockNumber") var blockNumber: Double? = null,
    @SerializedName("data") var data: String? = null,
    @SerializedName("logIndex") var logIndex: Int? = null,
    @SerializedName("removed") var removed: Boolean? = null,
    @SerializedName("topics") var topics: ArrayList<String> = arrayListOf(),
    @SerializedName("transactionHash") var transactionHash: String? = null,
    @SerializedName("transactionIndex") var transactionIndex: Int? = null
)
