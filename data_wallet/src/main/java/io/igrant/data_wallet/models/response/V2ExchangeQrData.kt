package io.igrant.data_wallet.models.response

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.agentConfig.Invitation

data class V2ExchangeQrData(
    @SerializedName("qr_id")
    var qrId: String? = null,
    @SerializedName("invitation")
    var invitation: Invitation? = null
)
