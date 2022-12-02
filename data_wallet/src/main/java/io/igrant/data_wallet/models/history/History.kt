package io.igrant.data_wallet.models.history

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.connection.ConnectionHistoryDetail
import io.igrant.data_wallet.models.connection.ConnectionV2
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.notification.NotificationBody
import io.igrant.data_wallet.models.reciept.Reciept
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.utils.DateUtils

class History : Comparable<History> {

    @SerializedName("id")
    @Expose
    var id: String? = null

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("name")
    @Expose
    var name: String? = null

    @SerializedName("logoUrl")
    @Expose
    var logo: String? = null

    @SerializedName("date")
    @Expose
    var date: String? = null

    @SerializedName("attributes")
    @Expose
    var attributes: ArrayList<ExchangeData>? = ArrayList()

    @SerializedName("walletModel")
    @Expose
    var wallet: WalletModel? = null

    @SerializedName("connection_history")
    @Expose
    var connectionHistory: ConnectionHistoryDetail? = null

    @SerializedName("version")
    var version: Int? = null



    @SerializedName("connection_v2_history")
    @Expose
    var connectionV2: ConnectionV2? = null

    @SerializedName("data_agreement_context")
    @Expose
    var dataAgreementContext: DataAgreementContext? = null

    @SerializedName("connection_id")
    @Expose
    var connectionId: String? = null

    @SerializedName("is_verified")
    @Expose
    var isVerified: Boolean? = null

    @SerializedName("reciept")
    @Expose
    var reciept: Reciept? = null

    @SerializedName("notification")
    @Expose
    var notification: NotificationBody? = null

    override fun compareTo(other: History): Int {
        return (DateUtils.getDate(other.date ?: "")).compareTo(DateUtils.getDate(this.date ?: ""))
    }
}