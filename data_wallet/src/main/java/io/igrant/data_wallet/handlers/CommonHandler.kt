package io.igrant.data_wallet.handlers

import okhttp3.RequestBody

interface CommonHandler {
    fun taskCompleted(){}
    fun onExchangeDataComplete(serviceEndPoint: String?, typedBytes: RequestBody?) {}
    fun onSaveConnection(
        typedBytes: RequestBody,
        connectionRequest: RequestBody
    ){
    }
    fun onSaveDidComplete(typedBytes:RequestBody?,serviceEndPoint:String){
    }
    fun taskStarted()
}