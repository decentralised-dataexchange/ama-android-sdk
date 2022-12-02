package io.igrant.data_wallet.models.reciept

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.reciept.BlockchainReceipt

data class Reciept(
    @SerializedName("instance_id") var instanceId: String? = null,
    @SerializedName("blockchain_receipt") var blockchainReceipt: BlockchainReceipt? = BlockchainReceipt(),
    @SerializedName("blink") var blink: String? = null,
    @SerializedName("mydata_did") var mydataDid: String? = null
)

