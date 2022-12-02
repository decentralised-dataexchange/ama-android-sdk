package io.igrant.data_wallet.fragment

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.CertificateDetailActivity
import io.igrant.data_wallet.activity.CertificateDetailActivity.Companion.EXTRA_WALLET_DETAIL
import io.igrant.data_wallet.activity.CertificateDetailActivity.Companion.EXTRA_WALLET_POSITION
import io.igrant.data_wallet.activity.ConnectionListActivity
import io.igrant.data_wallet.activity.ExtractListeners
import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity
import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_INVITATION
import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_PROPOSAL
import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity.Companion.EXTRA_PRESENTATION_QR_ID
import io.igrant.data_wallet.adapter.WalletCertificatesAdapter
import io.igrant.data_wallet.communication.ApiManager
import io.igrant.data_wallet.custom.WrapContentLinearLayoutManager
import io.igrant.data_wallet.events.DeleteCertificateEvent
import io.igrant.data_wallet.events.ReceiveCertificateEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.WalletListener
import io.igrant.data_wallet.models.agentConfig.Invitation
import io.igrant.data_wallet.models.qr.QrDecode
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.ConnectionUtils.saveConnectionAndExchangeData
import io.igrant.data_wallet.utils.WalletRecordType.Companion.WALLET
import io.igrant.qrcode_scanner_android.qrcode.utils.QRScanner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import kotlin.collections.ArrayList

class WalletFragment : BaseFragment() {

    lateinit var tvDataWallet: TextView
    lateinit var etSearchWallet: EditText
    lateinit var rvCertificates: RecyclerView
    lateinit var llErrorMessage: LinearLayout
    lateinit var ivAdd: ImageView
    lateinit var llProgressBar: LinearLayout
    lateinit var tvExchangeData: TextView
//    lateinit var ivMoveCategory: ImageView

    private var walletCertificateAdapter: WalletCertificatesAdapter? = null

    private var certificateList: ArrayList<WalletModel> = ArrayList()
    private var certificateListCopy: ArrayList<WalletModel> = ArrayList()

    private var mExtractListeners: ExtractListeners? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mExtractListeners = context as ExtractListeners
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wallet, container, false)

        initViews(view)
        initListener()

//        llProgressBar.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed(
            {
                setUpCertificateList()
            },
            1000
        )

        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
        return view
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ReceiveCertificateEvent) {
        loadCertificates()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun setDeleteEvent(event: DeleteCertificateEvent) {
        val wallet = certificateList[event.postion]
        certificateList.remove(wallet)
        certificateListCopy.remove(wallet)
        when {
            certificateList.size == 0 -> {
                walletCertificateAdapter?.notifyDataSetChanged()
                llErrorMessage.visibility = View.VISIBLE
                tvExchangeData.visibility = View.GONE
            }
            certificateList.size <= 2 || event.postion <= 3 -> {
                walletCertificateAdapter?.notifyDataSetChanged()
            }
            (certificateList.size - 1 == event.postion) -> {
                walletCertificateAdapter?.notifyItemChanged(event.postion)
            }
            event.postion > 3 -> {
                walletCertificateAdapter?.notifyItemRangeChanged(
                    event.postion,
                    certificateList.size - 1
                )
            }
            else -> {
                walletCertificateAdapter?.notifyItemChanged(event.postion)
            }
        }
    }

    override fun onDestroy() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

    private fun setUpCertificateList() {
        try {
            walletCertificateAdapter =
                WalletCertificatesAdapter(certificateList, object : WalletListener {

                    override fun onItemClick(wallet: WalletModel, position: Int) {
                        val intent = Intent(context, CertificateDetailActivity::class.java)
                        val wal = WalletManager.getGson.toJson(wallet)
                        intent.putExtra(EXTRA_WALLET_DETAIL, wal)
                        intent.putExtra(EXTRA_WALLET_POSITION, position)
                        startActivity(intent)

                    }
                })
            rvCertificates.layoutManager = WrapContentLinearLayoutManager(
                context,
                LinearLayoutManager.VERTICAL, false
            )
            rvCertificates.adapter = walletCertificateAdapter

            loadCertificates()
        } catch (e: Exception) {
        }
    }

    private fun loadCertificates() {
        CoroutineScope(Dispatchers.Main).launch {
            loadCertificatesFromWallet()
        }
    }

    private suspend fun loadCertificatesFromWallet() {
        try {
            withContext(Dispatchers.IO) {
                Log.d(TAG, "loadCertificates: ${Date().time}")

                val credentialList = SearchUtils.searchWallet(
                    WALLET,
                    "{ \"type\":\"${WalletRecordType.CERTIFICATE_TYPE_CREDENTIALS}\"}"
                )

                Log.d(TAG, "loadCertificates: ${Date().time}")

                var list: ArrayList<WalletModel> = ArrayList()
                list.addAll(parseArray(credentialList.records ?: ArrayList()))

                certificateList.clear()
                certificateList.addAll(list)

                certificateListCopy.clear()
                certificateListCopy.addAll(list)

            }

            walletCertificateAdapter?.notifyDataSetChanged()
            tvExchangeData.visibility =
                if (certificateList.size > 0) View.VISIBLE else View.GONE

            if (certificateList.size > 0) {
                llErrorMessage.visibility = View.GONE
            } else {
                llErrorMessage.visibility = View.VISIBLE
            }
            llProgressBar.visibility = View.GONE
        } catch (e: Exception) {
        }
    }

    private fun parseArray(arrayList: ArrayList<Record>): ArrayList<WalletModel> {
        val list: ArrayList<WalletModel> = ArrayList()

        for (record in arrayList) {
            list.add(WalletManager.getGson.fromJson(record.value, WalletModel::class.java))
        }

        list.sort()
        return list
    }

    private fun initListener() {
        try {

            ivAdd.setOnClickListener {

                if (PermissionUtils.hasPermissions(
                        requireActivity(),
                        PERMISSIONS
                    )
                ) {
                    QRScanner().withLocale(LocaleHelper.getLanguage(requireContext()))
                        .start(
                            requireContext(),
                            this,
                            REQUEST_CODE_SCAN_INVITATION
                        )
                } else {
                    requestPermissions(PERMISSIONS, PICK_IMAGE_REQUEST)
                }
            }

            tvExchangeData.setOnClickListener {

                if (PermissionUtils.hasPermissions(
                        requireActivity(),
                        PERMISSIONS
                    )
                ) {
                    QRScanner().withLocale(LocaleHelper.getLanguage(requireContext()))
                        .start(
                            requireContext(),
                            this,
                            REQUEST_CODE_SCAN_INVITATION
                        )
                } else {
                    requestPermissions(PERMISSIONS, PICK_IMAGE_REQUEST)
                }
            }

            etSearchWallet.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                }

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    CoroutineScope(Dispatchers.Main).launch {
                        filterList(s)
                    }
                }
            })
        } catch (e: Exception) {
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            QRScanner().withLocale(LocaleHelper.getLanguage(requireContext())).start(
                requireContext(),
                this,
                REQUEST_CODE_SCAN_INVITATION
            )
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        if (requestCode == REQUEST_CODE_SCAN_INVITATION && resultCode == Activity.RESULT_OK) {
            if (data == null) return

            try {
                val uri: Uri = try {
                    Uri.parse(data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult"))
                } catch (e: Exception) {
                    Uri.parse("igrant.io")
                }

                if (ConnectionUtils.isIGrnatValidUrl(uri.toString())) {
                    Firebase.dynamicLinks
                        .getDynamicLink(uri)
                        .addOnSuccessListener(requireActivity()) { pendingDynamicLinkData ->
                            // Get deep link from result (may be null if no link is found)
                            var deepLink: Uri? = null
                            if (pendingDynamicLinkData != null) {
                                deepLink = pendingDynamicLinkData.link

                                try {
                                    val deepLinkUri: Uri = deepLink ?: Uri.parse("igrant.io")

                                    extractFromUri(deepLinkUri)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.connection_unexpected_error_please_try_again),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .addOnFailureListener(requireActivity()) { e ->
                            extractFromUri(uri)
                        }

                } else {
                    extractFromUri(uri)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    resources.getString(R.string.connection_unexpected_error_please_try_again),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun extractFromUri(uri: Uri) {
        mExtractListeners?.extractUrlFunction(uri)
    }

    private suspend fun filterList(s: CharSequence?) {
        withContext(Dispatchers.IO) {
            var tempList: ArrayList<WalletModel> = ArrayList()

            if (s != "")
                for (certificate in certificateListCopy) {

                    if ((certificate.searchableText ?: "").contains(s ?: "", ignoreCase = true)) {
                        tempList.add(certificate)
                    }
                }
            else
                tempList.addAll(certificateListCopy)

            certificateList.clear()
            certificateList.addAll(tempList)

        }
        if (walletCertificateAdapter != null)
            walletCertificateAdapter?.notifyDataSetChanged()

        if (certificateList.size > 0) {
            llErrorMessage.visibility = View.GONE
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }
    }

    private fun initViews(view: View) {
        try {
            tvDataWallet = view.findViewById(R.id.tvDataWallet)
            etSearchWallet = view.findViewById(R.id.etSearch)
            rvCertificates = view.findViewById(R.id.rvCertificates)
            llErrorMessage = view.findViewById(R.id.llErrorMessage)
            ivAdd = view.findViewById(R.id.ivAdd)
            llProgressBar = view.findViewById(R.id.llProgressBar)
            tvExchangeData = view.findViewById(R.id.tvExchangeData)
        } catch (e: Exception) {
        }
    }

    companion object {
        private const val TAG = "WalletFragment"
        fun newInstance(): WalletFragment {
            return WalletFragment()
        }

        private const val PICK_IMAGE_REQUEST = 101
        val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_SCAN_INVITATION = 202
    }
}