package io.igrant.data_wallet.models.reciept

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.reciept.Logs

data class BlockchainReceipt(
    @SerializedName("blockHash") var blockHash: String? = null,
    @SerializedName("blockNumber") var blockNumber: Double? = null,
    @SerializedName("contractAddress") var contractAddress: String? = null,
    @SerializedName("cumulativeGasUsed") var cumulativeGasUsed: Double? = null,
    @SerializedName("effectiveGasPrice") var effectiveGasPrice: Double? = null,
    @SerializedName("from") var from: String? = null,
    @SerializedName("gasUsed") var gasUsed: Double? = null,
    @SerializedName("logs") var logs: ArrayList<Logs> = arrayListOf(),
    @SerializedName("logsBloom") var logsBloom: String? = null,
    @SerializedName("status") var status: Int? = null,
    @SerializedName("to") var to: String? = null,
    @SerializedName("transactionHash") var transactionHash: String? = null,
    @SerializedName("transactionIndex") var transactionIndex: Int? = null,
    @SerializedName("type") var type: String? = null
)
