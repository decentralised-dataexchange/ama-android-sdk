package io.igrant.data_wallet.utils

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.Toast
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.qr.QrDecode
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object ExtractUrlUtil {

    fun extractUrl(uri: Uri, listener: ExtractUrlListeners) {
        if (uri.toString().contains("data-exchange")) {
            val v: String = uri.getQueryParameter("qr_p") ?: ""
            if (v != "") {
                val json =
                    Base64.decode(
                        v,
                        Base64.URL_SAFE
                    ).toString(charset("UTF-8"))
                val data = JSONObject(json)
                if (data.getString("invitation_url") != "") {
                    val invitation: String =
                        Uri.parse(data.getString("invitation_url")).getQueryParameter("c_i")
                            ?: ""
                    val proofRequest = data.getJSONObject("proof_request")
                    listener.onSuccessFullyExecutedExchangeRequest(invitation, proofRequest, "")
                } else {
                    listener.onFailureRequest()
                }
            } else {

                val bits: List<String> = uri.toString().split("/")

                val lastOne = bits[bits.size - 1]

                ApiManager.api.getService()?.extractUrl(uri.toString())?.enqueue(object :
                    Callback<QrDecode> {
                    override fun onFailure(call: Call<QrDecode>, t: Throwable) {
                        listener.onFailureRequest()
                    }

                    override fun onResponse(
                        call: Call<QrDecode>,
                        response: Response<QrDecode>
                    ) {
                        if (response.code() == 200 && response.body() != null) {
                            if (response.body()!!.dataExchangeUrl != null) {
                                //split with / and take the last element - to get qr_id
                                val uri: Uri = try {
                                    Uri.parse(response.body()!!.dataExchangeUrl)
                                } catch (e: Exception) {
                                    Uri.parse("igrant.io")
                                }
                                val v: String = uri.getQueryParameter("qr_p") ?: ""
                                if (v != "") {
                                    val json =
                                        Base64.decode(
                                            v,
                                            Base64.URL_SAFE
                                        ).toString(charset("UTF-8"))
                                    val data = JSONObject(json)
                                    if (data.getString("invitation_url") != "") {
                                        val invitation: String =
                                            Uri.parse(data.getString("invitation_url"))
                                                .getQueryParameter("c_i")
                                                ?: ""
                                        val proofRequest = data.getJSONObject("proof_request")
                                        listener.onSuccessFullyExecutedExchangeRequest(
                                            invitation,
                                            proofRequest,
                                            lastOne
                                        )
                                    } else {
                                        listener.onFailureRequest()
                                    }
                                } else {
                                    listener.onFailureRequest()
                                }
                            }
                        }
                    }
                })
            }
        }else if(uri.toString().contains("connection")){
            val v: String = uri.getQueryParameter("c_i") ?: ""
            if (v != "") {
                saveConnection(v,listener)
            }else{
                ApiManager.api.getService()?.extractUrl(uri.toString())?.enqueue(object :
                    Callback<QrDecode> {
                    override fun onFailure(call: Call<QrDecode>, t: Throwable) {
                        listener.onFailureRequest()
                    }

                    override fun onResponse(call: Call<QrDecode>, response: Response<QrDecode>) {
                        if (response.code() == 200 && response.body() != null) {
                            if (response.body()!!.invitationUrl != null) {
                                val uri: Uri = try {
                                    Uri.parse(response.body()!!.invitationUrl)
                                } catch (e: Exception) {
                                    Uri.parse("igrant.io")
                                }
                                val v: String = uri.getQueryParameter("c_i") ?: ""
                                if (v != "") {
                                    saveConnection(v,listener)
                                } else {
                                    listener.onFailureRequest()
                                }
                            }
                        }
                    }
                })
            }
        }
    }

    private fun saveConnection(data:String,listener: ExtractUrlListeners){
        var invitation: Invitation? = null
        try {
            val json =
                Base64.decode(
                    data,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))
            invitation = WalletManager.getGson.fromJson(json, Invitation::class.java)
        } catch (e: Exception) {
        }

        if (invitation != null) {
            listener.onSuccessFullyExecutedConnectionRequest(invitation,"")
        } else {
            listener.onFailureRequest()
        }
    }
}


interface ExtractUrlListeners {
    fun onSuccessFullyExecutedExchangeRequest(
        data: String,
        proofRequest: JSONObject,
        qrId: String
    ) {
    }

    fun onSuccessFullyExecutedConnectionRequest(
        invitation: Invitation,
        proposal: String
    ) {
    }

    fun onFailureRequest() {}
}