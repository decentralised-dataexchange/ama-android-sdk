package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.receipt.FinancialInstitutionBranch


data class PayeeFinancialAccount(

    @SerializedName("iD") var iD: String? = null,
    @SerializedName("name") var name: String? = null,
    @SerializedName("financialInstitutionBranch") var financialInstitutionBranch: FinancialInstitutionBranch? = FinancialInstitutionBranch()

)