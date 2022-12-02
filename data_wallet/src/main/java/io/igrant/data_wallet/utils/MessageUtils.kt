package io.igrant.data_wallet.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import io.igrant.data_wallet.R
import java.util.concurrent.atomic.AtomicInteger

object MessageUtils {
    private const val TAG = "MessageUtils"
    fun displaySnackbar(view: View, s: String?) {
        val snack = Snackbar.make(view, s!!, Snackbar.LENGTH_LONG)
        val sbview: View = snack.view
        sbview.setBackgroundColor(ContextCompat.getColor(view.context, R.color.green))
        val textView =
            sbview.findViewById(R.id.snackbar_text) as TextView
        textView.setTextColor(view.resources.getColor(R.color.white))
        snack.show()
    }

    fun displaySnackbar(view: View, s: String?, color:Int) {
        val snack = Snackbar.make(view, s!!, Snackbar.LENGTH_LONG)
        val sbview: View = snack.view
        sbview.setBackgroundColor(ContextCompat.getColor(view.context, color))
        val textView =
            sbview.findViewById(R.id.snackbar_text) as TextView
        textView.setTextColor(view.resources.getColor(R.color.white))
        snack.show()
    }

    fun showNotification(context:Context,type:String,title:String,desc:String){

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

    fun showNotification(intent: Intent, context:Context, type:String, title:String, desc:String){

        try {

            intent.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val notifyPendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val mNotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val builder = NotificationCompat.Builder(
                context,
                context.resources.getString(R.string.notification_channel_id_default)
            )
                .setSmallIcon(R.drawable.notification)
                .setContentTitle(title)
                .setContentIntent(notifyPendingIntent)
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
        } catch (e: Exception) {
            Log.d(TAG, "showNotification: ")
        }
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