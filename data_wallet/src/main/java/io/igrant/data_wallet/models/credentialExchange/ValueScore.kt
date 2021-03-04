package io.igrant.data_wallet.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.credentialExchange.Score

class ValueScore {

    @SerializedName("score")
    @Expose
    var score: Score? = Score()

}