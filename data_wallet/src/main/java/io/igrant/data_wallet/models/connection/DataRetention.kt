package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class DataRetention :Serializable{

    @SerializedName("retention_period")
    @Expose
    var retentionPeriod  : Long? = null

    @SerializedName("enabled")
    @Expose
    var enabled  : Boolean? = null
}