package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class AccountingCustomerParty(

    @SerializedName("party") var party: Party? = Party()

)