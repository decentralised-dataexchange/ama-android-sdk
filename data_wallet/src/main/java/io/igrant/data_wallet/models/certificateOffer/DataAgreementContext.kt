package io.igrant.data_wallet.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.indy.WalletManager
import java.io.Serializable

class DataAgreementContext(

    @SerializedName("message_type")
    @Expose
    var messageType: String? = null,

    @SerializedName("message")
    @Expose
    var message: DataAgreementMessage? = null
) : Serializable {
    fun clone(): DataAgreementContext {
        val stringAnimal = WalletManager.getGson.toJson(this, DataAgreementContext::class.java)
        return WalletManager.getGson.fromJson<DataAgreementContext>(
            stringAnimal,
            DataAgreementContext::class.java
        )
    }
}