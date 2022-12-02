package io.igrant.data_wallet.models.ledger

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

data class Ledger(

    @SerializedName("ID")
    @Expose
    var id: Int? = 0,

    @SerializedName("Str")
    @Expose
    var name: String? = "",

    @SerializedName("GenesisURL")
    @Expose
    var genesisURL: String? = "",

    @SerializedName("IsAutoDID")
    @Expose
    var isAutoDID: Boolean? = null,

    @SerializedName("RegisterDIDHTMLText")
    @Expose
    var registerDIDHTMLText: String? = "",

    @SerializedName("GenesisString")
    @Expose
    var genesisString: String? = ""

)