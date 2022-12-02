package io.igrant.data_wallet.fragment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.widget.Toast
import io.igrant.data_wallet.R
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.agentConfig.ConfigPostResponse
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.connectionRequest.DidDoc
import io.igrant.data_wallet.models.connectionRequest.Transport
import io.igrant.data_wallet.models.presentationExchange.PresentationExchange
import io.igrant.data_wallet.models.presentationExchange.PresentationRequest
import io.igrant.data_wallet.models.requests.InitiateBody
import io.igrant.data_wallet.models.requests.InitiateRequest
import io.igrant.data_wallet.utils.*
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okio.BufferedSink
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*


class UrlExtractFragment : Fragment(), ConnectionProgressDailogFragment.OnConnectionSuccess {

    private var mProgressListener: ProgressListener? = null

    interface ProgressListener {
        fun updateProgress(progress: Int)
        fun error(explain: String)
    }

    fun setProgressListener(context: Context) {
        mProgressListener = context as ProgressListener
    }

    companion object {
        fun newInstance(): UrlExtractFragment {
            return UrlExtractFragment()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
    }

    fun extractUrl(uri: Uri?) {
        if (uri != null)
            mProgressListener?.updateProgress(View.VISIBLE)
        ExtractUrlUtil.extractUrl(uri, object : ExtractUrlListeners {
            override fun onFailureRequest(reason: Int?) {
                mProgressListener?.updateProgress(View.GONE)
                Toast.makeText(
                    context,
                    resources.getString(reason ?: R.string.error_invalid_qr_code),
                    Toast.LENGTH_SHORT
                ).show()
            }

            override fun onSuccessFullyExecutedExchangeRequest(
                data: String,
                proofRequest: JSONObject?,
                qrId: String
            ) {
                mQrId = qrId

                if (proofRequest != null) {
                    mProgressListener?.updateProgress(View.GONE)
                    ConnectionUtils.saveConnectionAndExchangeData(
                        requireContext(),
                        data,
                        proofRequest,
                        qrId
                    )
                } else {
                    saveConnection(data)
                }
                mProgressListener?.updateProgress(View.GONE)
            }

            override fun onSuccessFullyExecutedConnectionRequest(
                invitation: Invitation,
                proposal: String
            ) {
                mProgressListener?.updateProgress(View.GONE)
                Handler(Looper.getMainLooper()).postDelayed({
                    val connectionSuccessDialogFragment: ConnectionProgressDailogFragment =
                        ConnectionProgressDailogFragment.newInstance(
                            false,
                            invitation,
                            proposal
                        )
                    connectionSuccessDialogFragment.show(
                        requireFragmentManager(),
                        "fragment_edit_name"
                    )
                }, 200)
            }

            override fun onEBSIConnectionRequest() {
            }

            override fun onIssueEBSIRequest(
                conformance: String?,
                credentialType: String?,
                issuer: String?
            ) {
                Handler(Looper.getMainLooper()).postDelayed({
                    ebsiIssueCredential(conformance, credentialType, issuer)
                },200)
            }

            override fun onVerifyEBSIRequest(url: String) {
//                mProgressListener?.updateProgress(View.GONE)
//                Handler(Looper.getMainLooper()).postDelayed({
//                    val intent = Intent(context, EbsiExchangeScreen::class.java)
//
//                    intent.putExtra(EbsiExchangeScreen.EXTRA_EBSI_EXCHANGE_URL, url)
//                    requireContext().startActivity(intent)
//                }, 200)
            }
        })
    }

    var invitation: Invitation? = null
    var mQrId: String? = null

    private fun saveConnection(
        data: String
    ) {
        try {
            val json =
                Base64.decode(
                    data,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))

            invitation = WalletManager.getGson.fromJson(json, Invitation::class.java)
        } catch (e: Exception) {
        }

        if (invitation != null)
            Handler(Looper.getMainLooper()).postDelayed({
                val connectionSuccessDialogFragment: ConnectionProgressDailogFragment =
                    ConnectionProgressDailogFragment.newInstance(
                        true,
                        invitation,
                        ""
                    )

                connectionSuccessDialogFragment.show(childFragmentManager, "fragment_edit_name")
                connectionSuccessDialogFragment.setListener(this)
                connectionSuccessDialogFragment.setLoader(false)
            }, 200)
    }

    private fun ebsiIssueCredential(
        conformance: String?,
        credentialType: String?,
        issuer: String?
    ) {

//        if (EbsiUtils.checkEbsiNaturalPersonConnectionExisting()) {
//            EbsiUtils.ebsiIssueCredential(
//                requireContext(),
//                false,
//                conformance,
//                credentialType,
//                issuer,
//                object : ProgressListener {
//                    override fun updateProgress(progress: Int) {
//                        mProgressListener?.updateProgress(progress)
//                    }
//
//                    override fun error(explain: String) {
//                        mProgressListener?.error(explain)
//                    }
//
//                }
//            )
//        } else {
//            mProgressListener?.updateProgress(View.GONE)
//            Handler(Looper.getMainLooper()).postDelayed({
//                val connectionSuccessDialogFragment: ConnectionProgressDailogFragment =
//                    ConnectionProgressDailogFragment.newInstance(
//                        true,
//                        EbsiUtils.getEbsiInvitation(),
//                        "$conformance<><>$credentialType<><>$issuer",
//                        true
//                    )
//
//                connectionSuccessDialogFragment.show(childFragmentManager, "fragment_edit_name")
//                connectionSuccessDialogFragment.setListener(this)
//                connectionSuccessDialogFragment.setLoader(false)
//            }, 200)
//        }
    }

    private fun initiateProtocol(id: String) {
        val connectionList = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"request_id\":\"$id\"}"
        )

        if ((connectionList.totalCount ?: 0) > 0) {
            val connectionObject = WalletManager.getGson.fromJson(
                connectionList.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )

            val metaString =
                Did.getDidWithMeta(WalletManager.getWallet, connectionObject?.myDid).get()
            val metaObject = JSONObject(metaString)
            val myKey = metaObject.getString("verkey")

            val didDoc =
                SearchUtils.searchWallet(
                    WalletRecordType.DID_DOC,
                    "{\"did\":\"${connectionObject?.theirDid}\"}"
                )
            if ((didDoc.totalCount ?: 0) > 0) {
                val didDocObj = WalletManager.getGson.fromJson(
                    didDoc.records?.get(0)?.value,
                    DidDoc::class.java
                )

                val initiateRequest = InitiateRequest(
                    body = InitiateBody(mQrId),
                    id = UUID.randomUUID().toString(),
                    from = WalletUtils.convertDidSovToMyDidWithMyData(connectionObject.myDid ?: ""),
                    to = WalletUtils.convertDidSovToMyDidWithMyData(
                        connectionObject.theirDid ?: ""
                    ),
                    createdTime = Date().time.toString(),
                    type = "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/data-agreement-qr-code/1.0/initiate",
                    transport = Transport("all")
                )

                val orgDetailPacked = PackingUtils.packMessage(
                    didDocObj,
                    myKey,
                    WalletManager.getGson.toJson(initiateRequest),
                    ""
                )

                val orgDetailTypedArray = object : RequestBody() {
                    override fun contentType(): MediaType? {
                        return "application/ssi-agent-wire".toMediaTypeOrNull()
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        sink.write(orgDetailPacked)
                    }
                }

                ApiManager.api.getService()
                    ?.postData(
                        didDocObj?.service?.get(0)?.serviceEndpoint ?: "",
                        orgDetailTypedArray
                    )
                    ?.enqueue(object :
                        Callback<ConfigPostResponse> {
                        override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                            mProgressListener?.error(resources.getString(R.string.connection_unexpected_error_please_try_again))
                        }

                        override fun onResponse(
                            call: Call<ConfigPostResponse>,
                            response: Response<ConfigPostResponse>
                        ) {
                            if (response.code() == 200 && response.body() != null) {
                                val unpacked = Crypto.unpackMessage(
                                    WalletManager.getWallet,
                                    (WalletManager.getGson.toJson(response.body())).toByteArray()
                                ).get()
                                var explain =
                                    resources.getString(R.string.connection_unexpected_error_please_try_again)
                                val message = JSONObject(String(unpacked)).getString("message")
                                try {

                                    val type = JSONObject(message).getString("@type")
                                    if (type.contains("problem-report")) {

                                        JSONObject(message).has("explain")
                                        explain = JSONObject(message).getString("explain")
                                        mProgressListener?.error(explain)
                                    } else {
                                        val presentationRequestBase64 =
                                            JSONObject(
                                                JSONObject(message).getJSONArray("request_presentations~attach")
                                                    .get(0).toString()
                                            ).getJSONObject("data").getString("base64")
                                        val presentationRequest = WalletManager.getGson.fromJson(
                                            Base64.decode(
                                                presentationRequestBase64,
                                                Base64.URL_SAFE
                                            )
                                                .toString(charset("UTF-8")),
                                            PresentationRequest::class.java
                                        )

                                        val presentationExchange = PresentationExchange()
                                        presentationExchange.threadId =
                                            JSONObject(message).getString("@id")
                                        presentationExchange.createdAt =
                                            DateUtils.getIndyFormattedDate()
                                        presentationExchange.updatedAt =
                                            DateUtils.getIndyFormattedDate()
                                        presentationExchange.connectionId =
                                            connectionObject?.requestId
                                        presentationExchange.initiator = "external"
                                        presentationExchange.presentationProposalDict = null
                                        presentationExchange.presentationRequest =
                                            presentationRequest
                                        presentationExchange.role = "prover"
                                        presentationExchange.state =
                                            PresentationExchangeStates.REQUEST_RECEIVED
                                        presentationExchange.comment =
                                            JSONObject(message).getString("comment")
                                        presentationExchange.type =
                                            JSONObject(message).getString("@type")

                                        if (JSONObject(message).has(
                                                "~data-agreement-context"
                                            )
                                        ) {
                                            presentationExchange.dataAgreementContext =
                                                WalletManager.getGson.fromJson(
                                                    JSONObject(message).getString(
                                                        "~data-agreement-context"
                                                    ), DataAgreementContext::class.java
                                                )
                                        }

                                        mProgressListener?.updateProgress(View.GONE)
                                        ConnectionUtils.saveConnectionAndExchangeData(
                                            requireContext(),
                                            invitation,
                                            presentationRequest,
                                            mQrId ?: "",
                                            presentationExchange
                                        )
                                    }
                                } catch (e: Exception) {
                                    mProgressListener?.error(explain)
                                }
                            } else {
                                mProgressListener?.error(resources.getString(R.string.connection_unexpected_error_please_try_again))
                            }
                        }
                    })
            }
        }
    }

    override fun onSuccess(proposal: String, connectionId: String) {
        initiateProtocol(connectionId)
    }

    //data will be split with "<><>"
    //1st - conformance
    //2nd - credentialType
    //3rd - issuer
    override fun onEbsiConnectionSuccess(data: String) {
//        val dataList = data.split("<><>")
//        if (dataList.size == 3)
//            EbsiUtils.ebsiIssueCredential(
//                requireContext(),
//                false,
//                dataList[0],
//                dataList[1],
//                dataList[2],
//                object : ProgressListener {
//                    override fun updateProgress(progress: Int) {
//                        mProgressListener?.updateProgress(progress)
//                    }
//
//                    override fun error(explain: String) {
//                        mProgressListener?.updateProgress(View.GONE)
//                        mProgressListener?.error(explain)
//                    }
//
//                }
//            )
    }

    override fun onExistingConnection(connectionId: String) {
        initiateProtocol(connectionId)
    }
}