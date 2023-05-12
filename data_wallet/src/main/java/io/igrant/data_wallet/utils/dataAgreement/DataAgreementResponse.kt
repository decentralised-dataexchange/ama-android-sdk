package io.igrant.data_wallet.utils.dataAgreement

import com.google.gson.annotations.SerializedName

data class DataAgreementResponse(

    @SerializedName("results") var results: ArrayList<Results> = arrayListOf(),

    )