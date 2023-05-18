package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.receipt.PayeeFinancialAccount


data class PaymentMeans(

    @SerializedName("paymentMeansCode") var paymentMeansCode: String? = null,
    @SerializedName("paymentID") var paymentID: String? = null,
    @SerializedName("payeeFinancialAccount") var payeeFinancialAccount: PayeeFinancialAccount? = PayeeFinancialAccount()

)