package io.igrant.data_wallet.tasks

import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.system.ErrnoException
import android.system.Os
import android.util.Log
import io.igrant.data_wallet.handlers.CommonHandler
import org.hyperledger.indy.sdk.LibIndy
import java.io.File

class LoadLibIndyTask(private val commonHandler: CommonHandler, val context: Context) :
    AsyncTask<Void, Void, Void>() {

    private val TAG = "LoadLibIndyTask"

    override fun doInBackground(vararg p0: Void?): Void? {
        val dataDir: File = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.dataDir
        } else {
            TODO("VERSION.SDK_INT < N")
        }
        Log.d(TAG, "DataDir=" + dataDir.absolutePath)
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
        return null
    }

    override fun onPreExecute() {
        super.onPreExecute()
        commonHandler.taskStarted()
    }

    override fun onPostExecute(result: Void?) {
        super.onPostExecute(result)
        commonHandler.taskCompleted()
    }
}