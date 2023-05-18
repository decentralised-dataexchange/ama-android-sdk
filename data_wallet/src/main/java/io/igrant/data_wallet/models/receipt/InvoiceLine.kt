package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class InvoiceLine(

    @SerializedName("iD") var iD: String? = null,
    @SerializedName("invoicedQuantity") var invoicedQuantity: String? = null,
    @SerializedName("lineExtensionAmount") var lineExtensionAmount: Double? = null,
    @SerializedName("item") var item: Item? = Item(),
    @SerializedName("price") var price: Price? = Price()

)