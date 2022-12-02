package io.igrant.data_wallet.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Card {

    constructor(id: String, name: String,country:String,logo:Int,countryFlag: Int,countryId:String) {
        this.id = id
        this.name = name
        this.country = country
        this.logo = logo
        this.countryFlag = countryFlag
        this.countryId = countryId
    }

    @SerializedName("id")
    @Expose
    var id: String? = null

    @SerializedName("name")
    @Expose
    var name: String? = null

    @SerializedName("countryId")
    @Expose
    var countryId: String? = ""

    @SerializedName("country")
    @Expose
    var country: String? = ""

    @SerializedName("logo")
    @Expose
    var logo: Int? = -1

    @SerializedName("countryFlag")
    @Expose
    var countryFlag: Int? = -1
}