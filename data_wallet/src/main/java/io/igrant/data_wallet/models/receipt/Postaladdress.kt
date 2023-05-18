package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.receipt.Country


data class Postaladdress(

    @SerializedName("streetName") var streetName: String? = null,
    @SerializedName("cityName") var cityName: String? = null,
    @SerializedName("postalZone") var postalZone: String? = null,
    @SerializedName("country") var country: Country? = Country()

)