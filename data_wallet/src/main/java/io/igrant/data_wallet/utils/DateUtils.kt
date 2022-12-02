package io.igrant.data_wallet.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    const val INDY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS'Z'"

    private const val DISPLAY_FORMAT = "yyyy-MM-dd"
    fun getIndyFormattedDate(): String {
        val sdf = SimpleDateFormat(INDY_DATE_FORMAT)
        return sdf.format(Date())
//        Log.d(TAG, "onCreate: date ::::: $date")
    }

    fun formatDate(date: Date?): String {
        val sdf2 = SimpleDateFormat(DISPLAY_FORMAT)

        if (date != null) {
            return sdf2.format(date)
        }
        return ""
    }

    fun formatDateString(inputFormat: String, dateString: String): String {
        val sdf = SimpleDateFormat(inputFormat)
        val sdf2 = SimpleDateFormat(DISPLAY_FORMAT)
        var date: Date? = null
        try {
            date = sdf.parse(dateString)
        } catch (e: Exception) {

        }
        if (date != null) {
            return sdf2.format(date)
        }
        return dateString
    }

    fun formatPassportDate(dateString: String): String {
        val sdf = SimpleDateFormat("dd.MM.yyyy")
        val sdf2 = SimpleDateFormat("yyMMdd")
        var date: Date? = null
        try {
            date = sdf.parse(dateString)
        } catch (e: Exception) {

        }
        if (date != null) {
            return sdf2.format(date)
        }
        return dateString
    }

    fun getRelativeTime(date: String): String {
        val sdf = SimpleDateFormat(
            INDY_DATE_FORMAT,
            Locale.ENGLISH
        )
        var dDate: Date?
        dDate = try {
            sdf.parse(date)
        } catch (e: Exception) {
            return "nil"
        }
        return DateUtils.getRelativeTimeSpanString(dDate!!.time).toString()
    }

    fun getDate(date: String): Date {
        val sdf = SimpleDateFormat(
            INDY_DATE_FORMAT,
            Locale.ENGLISH
        )
        var dDate: Date?
        dDate = try {
            sdf.parse(date)
        } catch (e: Exception) {
            return Date()
        }
        return dDate ?: Date()
    }
}