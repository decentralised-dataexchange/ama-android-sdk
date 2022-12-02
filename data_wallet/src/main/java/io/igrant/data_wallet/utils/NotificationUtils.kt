package io.igrant.data_wallet.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.OfferCertificateActivity
import io.igrant.data_wallet.events.ReceiveExchangeRequestEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.Notification
import io.igrant.data_wallet.models.wallet.WalletModel
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.atomic.AtomicInteger

object NotificationUtils {

    private const val TAG = "NotificationUtils"

    fun addEbsiNotification(context: Context, ebsiWalletCredential: WalletModel){
//        val connectionResult = SearchUtils.searchWallet(
//            WalletRecordType.CONNECTION,
//            "{\"orgId\": \"${ConnectionTypes.EBSI_CONNECTION}\"}"
//        )
//
//        if ((connectionResult.totalCount ?: 0) > 0) {
//            val connecction = WalletManager.getGson.fromJson(
//                connectionResult.records?.get(0)?.value, MediatorConnectionObject::class.java
//            )

        val notification = Notification()
        notification.type = MessageTypes.TYPE_EBSI_CREDENTIAL
        notification.stat = "Active"
        notification.ebsiCertificate = ebsiWalletCredential
        notification.connection = ebsiWalletCredential.connection
        notification.date = DateUtils.getIndyFormattedDate()

        WalletMethods.addWalletRecord(
            WalletManager.getWallet,
            WalletRecordType.MESSAGE_RECORDS,
            ebsiWalletCredential.credentialId?:"",
            WalletManager.getGson.toJson(notification),
            "{\n" +
                    "  \"type\":\"${MessageTypes.TYPE_EBSI_CREDENTIAL}\",\n" +
                    "  \"connectionId\":\"${ebsiWalletCredential.connection?.requestId}\",\n" +
                    "  \"certificateId\":\"${ebsiWalletCredential.credentialId?:""}\",\n" +
                    "  \"stat\":\"Active\"\n" +
                    "}"
        )

        try {

            val searchResponse = SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{\"certificateId\":\"${ebsiWalletCredential.credentialId?:""}\"}"
            )
            if ((searchResponse.totalCount ?: 0) > 0) {

                //go to intialize activity then start the offer certificate activity
                val intent =
                    Intent(context, OfferCertificateActivity::class.java)
                intent.putExtra(
                    OfferCertificateActivity.EXTRA_CERTIFICATE_PREVIEW,
                    searchResponse.records!![0]
                )

                MessageUtils.showNotification(
                    intent,
                    context,
                    MessageTypes.TYPE_EBSI_CREDENTIAL,
                    context.resources.getString(R.string.data_received_offer_credentials),
                    context.resources.getString(R.string.data_received_offer_credential_from_organisation)
                )
                EventBus.getDefault()
                    .post(ReceiveExchangeRequestEvent())

            }

            EventBus.getDefault()
                .post(ReceiveExchangeRequestEvent())
        } catch (e: Exception) {
            Log.d(TAG, "addEbsiNotification: ")
        }
//        }
    }

    fun showNotification(context:Context,type:String,title:String,desc:String){
//        val intent = Intent(context, InitializeActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        intent.action = System.currentTimeMillis().toString()

//        val pendingIntent: PendingIntent =
//            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(
            context,
            context.resources.getString(R.string.notification_channel_id_default)
        )
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(desc)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getChannelIdForParu(type,context),
                getChannelName(type,context),
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            builder.setChannelId(getChannelIdForParu(type,context))
        }
        mNotificationManager.notify(SystemClock.uptimeMillis().toInt(), builder.build())


    }

    fun showNotification(intent:Intent,context:Context,type:String,title:String,desc:String){

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        val mNotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val builder = NotificationCompat.Builder(
            context,
            context.resources.getString(R.string.notification_channel_id_default)
        )
            .setSmallIcon(R.drawable.notification)
            .setContentTitle(title)
            .setContentIntent(pendingIntent)
            .setStyle(
                NotificationCompat.InboxStyle()
                    .setSummaryText(desc)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getChannelIdForParu(type,context),
                getChannelName(type,context),
                NotificationManager.IMPORTANCE_HIGH
            )
            mNotificationManager.createNotificationChannel(channel)
            builder.setChannelId(getChannelIdForParu(type,context))
        }
        mNotificationManager.notify(if (type == MessageTypes.TYPE_ISSUE_CREDENTIAL)1 else 0,builder.build())
//        mNotificationManager.notify(SystemClock.uptimeMillis().toInt(), builder.build())
    }
    private fun getChannelName(type: String, context: Context): String {
        when (type) {
            MessageTypes.TYPE_ISSUE_CREDENTIAL -> {
                return context.resources.getString(R.string.notification_channel_success_messages)
            }
            else -> {
                return context.resources.getString(R.string.notification_channel_success_messages)
            }
        }
    }

    private fun getChannelIdForParu(type: String,context: Context): String {
        when (type) {
            MessageTypes.TYPE_ISSUE_CREDENTIAL -> {
                return context.resources.getString(R.string.notification_channel_id_success)
            }
            else -> {
                return context.resources.getString(R.string.notification_channel_id_success)
            }
        }

    }

    private fun getNotificationID(): Int {
        val c = AtomicInteger(0)
        val iD: Int = c.incrementAndGet()
        return iD
    }
}