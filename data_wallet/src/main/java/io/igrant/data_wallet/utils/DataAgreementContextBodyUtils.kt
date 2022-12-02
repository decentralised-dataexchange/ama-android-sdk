package io.igrant.data_wallet.utils

import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.certificateOffer.DataAgreementBodyDexa
import io.igrant.mobileagent.models.certificateOffer.DataAgreementContextBody

object DataAgreementContextBodyUtils {

    fun checkDataAgreementContextBodyIsOfDexa(body: Any?): Boolean {
        try {
            val tempBody = body as Map<String, Any>
            return tempBody.containsKey("@id")
        } catch (e: Exception) {
            return body is DataAgreementBodyDexa
        }
    }

    fun convertToDataAgreementBodyOfDexa(body: Any?): DataAgreementBodyDexa {
        return WalletManager.getGson.fromJson(WalletManager.getGson.toJson(body),DataAgreementBodyDexa::class.java)
    }

    fun convertToNormalDataAgreementBody(body: Any?): DataAgreementContextBody {
        return WalletManager.getGson.fromJson(WalletManager.getGson.toJson(body),DataAgreementContextBody::class.java)
    }

}