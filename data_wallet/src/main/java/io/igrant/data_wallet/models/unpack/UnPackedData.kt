package io.igrant.data_wallet.models.unpack

import com.google.gson.annotations.SerializedName

data class UnPackedData(
    @SerializedName("message") var message: String? = null,
    @SerializedName("recipient_verkey") var recipientVerkey: String? = null,
    @SerializedName("sender_verkey") var senderVerkey: String? = null
)
