package io.igrant.data_wallet.models.certificate

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CredData {
    @SerializedName("cred_info")
    @Expose
    var credInfo: Certificate? = null

}