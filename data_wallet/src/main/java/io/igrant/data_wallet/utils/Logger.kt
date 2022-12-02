package io.igrant.data_wallet.utils

import android.util.Log
import java.lang.RuntimeException

/**
 * Android Log wrapper class that can use [String.format] in logging message
 */
object Logger {
    private val TAG = Logger::class.java.simpleName
    private const val EMPTY = ""

    /**
     * Send a VERBOSE log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun v(tag: String?, msg: String?, e: Throwable?): Int {
        return Log.v(tag, msg, e)
    }

    /**
     * Send a DEBUG log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun d(tag: String?, msg: String?, e: Throwable?): Int {
        return Log.d(tag, msg, e)
    }

    /**
     * Send a WARN log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun w(tag: String?, msg: String?, e: Throwable?): Int {
        return Log.w(tag, msg, e)
    }

    /**
     * Send a INFO log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun i(tag: String?, msg: String?, e: Throwable?): Int {
        return Log.i(tag, msg, e)
    }

    /**
     * Send a ERROR log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun e(tag: String?, msg: String?, e: Throwable?): Int {
        return Log.e(tag, msg, e)
    }

    /**
     * Send a VERBOSE log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun v(tag: String?, msg: String?): Int {
        return Log.v(tag, msg ?: "")
    }

    /**
     * Send a DEBUG log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun d(tag: String?, msg: String?): Int {
        return Log.d(tag, msg ?: "")
    }

    /**
     * Send a WARN log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun w(tag: String?, msg: String?): Int {
        return Log.w(tag, msg ?: "")
    }

    /**
     * Send a INFO log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun i(tag: String?, msg: String?): Int {
        return Log.i(tag, msg ?: "")
    }

    /**
     * Send a ERROR log message and log the exception.
     * @param tag
     * @param msg
     * @param e
     * @return
     */
    fun e(tag: String?, msg: String?): Int {
        return Log.e(tag, msg ?: "")
    }

}