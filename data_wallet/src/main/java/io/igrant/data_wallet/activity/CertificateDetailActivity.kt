package io.igrant.data_wallet.activity

import android.content.DialogInterface
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.data_wallet.R
import io.igrant.data_wallet.adapter.CertificateAttributeAdapter
import io.igrant.data_wallet.adapter.SectionAdapter
import io.igrant.data_wallet.events.DeleteCertificateEvent
import io.igrant.data_wallet.events.ReceiveCertificateEvent
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.connection.Connection
import io.igrant.data_wallet.models.connection.ConnectionV2
import io.igrant.data_wallet.models.wallet.WalletModel
import io.igrant.data_wallet.utils.CertificateListingUtils.getCertificateAttributeListFromWalletModel
import io.igrant.data_wallet.utils.ConnectionTypes
import io.igrant.data_wallet.utils.DisplayUtils
import io.igrant.data_wallet.utils.TextUtils
import io.igrant.data_wallet.utils.TextUtils.convertDpToPixel
import io.igrant.data_wallet.utils.WalletRecordType
import io.igrant.data_wallet.utils.WalletRecordType.Companion.CERTIFICATE_TYPE_EBSI_CREDENTIAL
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.IndyException
import org.hyperledger.indy.sdk.anoncreds.Anoncreds
import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import io.igrant.data_wallet.adapter.SectionAdapterV2
import io.igrant.data_wallet.utils.wrappers.CredentialTypes
import io.igrant.data_wallet.utils.wrappers.ReceiptWrapper

class CertificateDetailActivity : BaseActivity() {

    private var address: String? = null
    private var sectionAdapterV2: SectionAdapterV2? = null
    private var adapter: SectionAdapter? = null
    private var wallet: WalletModel? = null

    private lateinit var toolbar: Toolbar
    private lateinit var rvAttributes: RecyclerView
    private lateinit var tvHead: TextView
    private lateinit var tvRemove: TextView
    private lateinit var ivCoverUrl: ImageView
    private lateinit var ivLogo: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvLocation: TextView
//    private lateinit var tvDescription: TextView

    private var isBlur: Boolean = true

    private var mFrom: String = "view"

    private var mPosition: Int? = null

    companion object {
        const val EXTRA_WALLET_DETAIL = "CertificateDetailActivity.wallet"
        const val EXTRA_WALLET_POSITION = "CertificateDetailActivity.position"
        const val EXTRA_FROM = "CertificateDetailActivity.from"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate_detail)
        initViews()
        getIntentData()
        initValues()
        initListener()
        setUpToolbar()
        setUpAdapter()
        getConnectionDetail()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_blur, menu)
        return true
    }

    private fun getIntentData() {
        val wal = intent.extras!!.getString(EXTRA_WALLET_DETAIL)
        wallet = WalletManager.getGson.fromJson(wal, WalletModel::class.java)
        if (intent.extras!!.containsKey(EXTRA_FROM))
            mFrom = intent.getStringExtra(EXTRA_FROM) ?: "view"

        if (intent.hasExtra(EXTRA_WALLET_POSITION))
            mPosition = intent.getIntExtra(EXTRA_WALLET_POSITION, -1)

        tvRemove.visibility = if (mFrom != "exchange") View.VISIBLE else View.GONE
    }

    private fun getConnectionDetail() {
        if (wallet?.organization != null) {
            initDataValues(wallet?.organization)
        } else if (wallet?.organizationV2 != null) {
            initDataValues(wallet?.organizationV2)
        } else {
            initDataValueWithConnection(wallet?.connection)
        }
    }

    private fun initDataValueWithConnection(connectionData: MediatorConnectionObject?) {
        initDataValues(
            connectionData?.theirImageUrl,
            null,
            connectionData?.theirLabel,
            connectionData?.location ?: "Nil"
        )
    }

    private fun initDataValues(connectionData: Connection?) {
        initDataValues(
            connectionData?.logoImageUrl,
            connectionData?.coverImageUrl,
            connectionData?.name,
            connectionData?.location
        )
    }

    private fun initDataValues(connectionData: ConnectionV2?) {
        initDataValues(
            connectionData?.logoImageUrl,
            connectionData?.coverImageUrl,
            connectionData?.organisationName,
            connectionData?.location
        )
    }

    private fun initDataValues(logo: String?, cover: String?, name: String?, location: String?) {
        Glide
            .with(ivLogo.context)
            .load(logo)
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        Glide
            .with(ivCoverUrl.context)
            .load(cover)
            .centerCrop()
            .placeholder(R.drawable.default_cover_image)
            .into(ivCoverUrl)

        tvName.text = name
        tvLocation.text = address?:location
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvAttributes = findViewById(R.id.rvAttributes)
        tvHead = findViewById(R.id.tvHead)
        tvRemove = findViewById(R.id.tvRemove)
        ivCoverUrl = findViewById(R.id.ivCoverUrl)
        ivLogo = findViewById(R.id.ivLogo)
        tvName = findViewById(R.id.tvName)
        tvLocation = findViewById(R.id.tvLocation)
//        tvDescription = findViewById(R.id.tvDescription)
    }

    private fun initValues() {
        try {
            tvHead.text = ((wallet?.rawCredential?.schemaId ?: "").split(":")[2]).toUpperCase()
        } catch (e: Exception) {
        }
    }

    private fun initListener() {
        tvRemove.setOnClickListener {
            if (WalletManager.getWallet != null) {
                AlertDialog.Builder(this@CertificateDetailActivity)
                    .setTitle(resources.getString(R.string.general_app_title))
                    .setMessage(
                        resources.getString(
                            R.string.connect_are_you_sure_you_want_to_delete_this_item
                        )
                    ) // Specifying a listener allows you to take an action before dismissing the dialog.
                    // The dialog is automatically dismissed when a dialog button is clicked.
                    .setPositiveButton(
                        R.string.general_yes
                    ) { dialog, which ->
                        if (WalletManager.getWallet != null) {
                            try {
                                WalletRecord.delete(
                                    WalletManager.getWallet,
                                    WalletRecordType.WALLET,
                                    if (wallet?.type == CERTIFICATE_TYPE_EBSI_CREDENTIAL)
                                        "ebsi-${wallet?.credentialId}"
                                    else
                                        wallet?.credentialId
                                )

                                if (mPosition != null && mPosition != -1)
                                    EventBus.getDefault()
                                        .post(DeleteCertificateEvent(mPosition!!))

                                if (wallet?.type != CERTIFICATE_TYPE_EBSI_CREDENTIAL)
                                    Anoncreds.proverDeleteCredential(
                                        WalletManager.getWallet,
                                        wallet?.credentialId
                                    ).get()

                                finish()

                            } catch (e: IndyException) {
                                Toast.makeText(
                                    this,
                                    "${e.sdkErrorCode} ${e.sdkMessage}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    this,
                                    "${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } // A null listener allows the button to dismiss the dialog and take no further action.
                    .setNegativeButton(
                        R.string.general_no
                    ) { dialog, which ->

                    }
                    .show()
            }
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_back_bg)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            R.id.action_visible -> {
                isBlur = !isBlur
                adapter?.setUpBlur(isBlur)
                sectionAdapterV2?.setUpBlur(isBlur)
                invalidateOptionsMenu()
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        invalidateOptionsMenu()
        menu.findItem(R.id.action_visible)
            .setIcon(if (isBlur) R.drawable.ic_visible_bg else R.drawable.ic_invisible_bg)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setUpAdapter() {
        val width = DisplayUtils.getScreenWidth() - convertDpToPixel(60f, rvAttributes.context)
        if (ReceiptWrapper.checkCredentialType(
                wallet?.credentialProposalDict?.credentialProposal?.attributes ?: ArrayList()
            ) == CredentialTypes.RECEIPT
        ) {
            val receipt = ReceiptWrapper.convertReceipt(
                wallet?.credentialProposalDict?.credentialProposal?.attributes ?: ArrayList()
            )
            if (receipt == null) {
                setUpV1Adapter()
            } else {
                sectionAdapterV2 = SectionAdapterV2(
                    ReceiptWrapper.getAttributesFromReceipt(receipt),
                    isBlur,
                    ReceiptWrapper.getSections(
                        receipt
                    )
                )
                rvAttributes.layoutManager = LinearLayoutManager(this)
                rvAttributes.adapter = sectionAdapterV2

                address =  ReceiptWrapper.getShopAddress(receipt)
                tvHead.visibility = View.INVISIBLE
            }
        } else {
            setUpV1Adapter()
        }
    }

    fun setUpV1Adapter() {
        val attributes = getCertificateAttributeListFromWalletModel(wallet)
        adapter = SectionAdapter(
            attributes,
            isBlur,
            if (wallet?.connection?.connectionType == ConnectionTypes.EBSI_CONNECTION_NATURAL_PERSON)
                wallet?.sectionStruct
            else ArrayList()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }
}