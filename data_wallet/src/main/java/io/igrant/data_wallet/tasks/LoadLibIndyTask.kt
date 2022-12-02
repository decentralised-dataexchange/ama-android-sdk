package io.igrant.data_wallet.tasks

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import io.igrant.data_wallet.handlers.CommonHandler
import org.hyperledger.indy.sdk.LibIndy
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object LoadLibIndyTask {
    private const val TAG = "LoadLibIndyTask"
    fun loadIndyLibrary(initialiseHandler: CommonHandler, context: Context) {

        initialiseHandler.taskStarted()
        val executor: ExecutorService = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            //Background work here
            handler.post {
                val externalFilesDir: File? = context.getExternalFilesDir(null)
                val path: String = externalFilesDir!!.absolutePath

                Log.d(TAG, "axel externalFilesDir=$path")

                try {
                    Os.setenv("EXTERNAL_STORAGE", path, true)
                } catch (e: ErrnoException) {
                    e.printStackTrace()
                }

                System.loadLibrary("indy")
                LibIndy.init()
                initialiseHandler.taskCompleted()
            }
        }
    }
}