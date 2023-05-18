package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.receipt.ClassifiedTaxCategory


data class Item(

    @SerializedName("name") var name: String? = null,
    @SerializedName("classifiedTaxCategory") var classifiedTaxCategory: ClassifiedTaxCategory? = ClassifiedTaxCategory()

)