package io.igrant.data_wallet.models

import com.google.gson.annotations.SerializedName

data class LastUpdatedTimesResponse(
    @SerializedName("ledger_network")
    val ledgerNetwork: String? = null,
    @SerializedName("pkpass_boarding_pass")
    val pkpassBoardingPass: String? = null,
    @SerializedName("blinks")
    val blinks: String? = null
)
