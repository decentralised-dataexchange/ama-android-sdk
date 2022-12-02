package io.igrant.data_wallet.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class ConnectionHistoryDetail:Serializable {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id  : String? = ""

    @SerializedName("purpose_details")
    @Expose
    var purposeDetails: PurposeDetail? = null

    @SerializedName("org_details")
    @Expose
    var orgDetails: Connection? = null

}