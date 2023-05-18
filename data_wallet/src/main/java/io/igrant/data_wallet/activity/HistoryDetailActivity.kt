package io.igrant.data_wallet.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.data_wallet.R
import io.igrant.data_wallet.adapter.ExchangeRequestAttributeAdapter
import io.igrant.data_wallet.adapter.SectionAdapter
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.history.History
import io.igrant.data_wallet.utils.*
import io.igrant.data_wallet.utils.CertificateListingUtils.getCertificateAttributeListFromWalletModel
import io.igrant.data_wallet.adapter.SectionAdapterV2
import io.igrant.data_wallet.utils.wrappers.CredentialTypes
import io.igrant.data_wallet.utils.wrappers.ReceiptWrapper

class HistoryDetailActivity : BaseActivity() {

    private lateinit var adapter: ExchangeRequestAttributeAdapter
    private lateinit var attributeAdapter: SectionAdapter
    private var sectionAdapterV2: SectionAdapterV2? = null
    private var mHistory: History? = null

    private lateinit var toolbar: Toolbar
    private lateinit var rvAttributes: RecyclerView
    private lateinit var tvHead: TextView
    private lateinit var tvDate: TextView
    private lateinit var ivCoverUrl: ImageView
    private lateinit var ivLogo: ImageView
    private lateinit var tvName: TextView
    private lateinit var tvLocation: TextView
    private lateinit var tvAgreement: TextView
    private lateinit var tvDataUsingService: TextView
    private lateinit var cvCompany: ConstraintLayout
    private lateinit var ivCompanyLogo: ImageView
    private lateinit var tvCompanyName: TextView
    private lateinit var tvType: TextView
    private lateinit var tvShareDate: TextView
    private var isBlur: Boolean = true

    private lateinit var clAgreement: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_detail)
        initView()
        getIntentData()
        setUpToolbar()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_blur, menu)
        return true
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
                if (mHistory?.wallet?.connection?.connectionType == ConnectionTypes.EBSI_CONNECTION_NATURAL_PERSON) {
                    attributeAdapter.setUpBlur(isBlur)
                } else if (mHistory?.type == HistoryType.VERIFY) {
                    adapter.setBlurValue(isBlur)
                } else {
                    attributeAdapter.setUpBlur(isBlur)
                }
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

    private fun initView() {
        toolbar = findViewById(R.id.toolbar)
        rvAttributes = findViewById(R.id.rvAttributes)
        tvHead = findViewById(R.id.tvHead)
        tvDate = findViewById(R.id.tvDate)
        ivCoverUrl = findViewById(R.id.ivCoverUrl)
        ivLogo = findViewById(R.id.ivLogo)
        tvName = findViewById(R.id.tvName)
        tvLocation = findViewById(R.id.tvLocation)
        tvAgreement = findViewById(R.id.tvAgreement)
        clAgreement = findViewById(R.id.clAgreement)

        tvDataUsingService = findViewById(R.id.tvDataUsingService)
        cvCompany = findViewById(R.id.cvCompany)
        ivCompanyLogo = findViewById(R.id.ivCompanyLogo)
        tvCompanyName = findViewById(R.id.tvCompanyName)
        tvType = findViewById(R.id.tvType)
        tvShareDate = findViewById(R.id.tvShareDate)
    }

    private fun getIntentData() {
        val history = intent.getStringExtra(EXTRA_DATA_HISTORY)
        mHistory = WalletManager.getGson.fromJson(history, History::class.java)
        setUpView()
        setUpList()
        setUpDataAgreementViews()
    }

    private fun setUpDataAgreementViews() {

        tvAgreement.setTextColor(
            if (mHistory?.dataAgreementContext == null
                && mHistory?.connectionHistory?.purposeDetails == null
            ) ContextCompat.getColor(
                this@HistoryDetailActivity,
                R.color.textColorVeryLight
            ) else ContextCompat.getColor(this@HistoryDetailActivity, R.color.textColor)
        )

        clAgreement.setOnClickListener {
            if (!(mHistory?.dataAgreementContext == null
                        && mHistory?.connectionHistory?.purposeDetails == null
                        )
            )
                DataAgreementPolicyUtil.showDataAgreementPolicy(mHistory, this)
        }
    }

    private fun setUpList() {

        tvHead.text = mHistory?.name ?: ""

        tvDate.text = if (mHistory?.type == HistoryType.VERIFY) resources.getString(
            R.string.welcome_verified_at, DateUtils.getRelativeTime(
                mHistory?.date
                    ?: ""
            )
        )
        else resources.getString(
            R.string.welcome_issued_at, DateUtils.getRelativeTime(
                mHistory?.date
                    ?: ""
            )
        )


        if (mHistory?.wallet?.connection?.connectionType == ConnectionTypes.EBSI_CONNECTION_NATURAL_PERSON) {
            val width = DisplayUtils.getScreenWidth() - TextUtils.convertDpToPixel(
                60f,
                rvAttributes.context
            )
            attributeAdapter = SectionAdapter(
                getCertificateAttributeListFromWalletModel(mHistory?.wallet),
                isBlur,
                if (mHistory?.wallet?.sectionStruct != null && (mHistory?.wallet?.sectionStruct?.size
                        ?: 0) > 0
                ) mHistory?.wallet?.sectionStruct else ArrayList()
            )
            rvAttributes.layoutManager = LinearLayoutManager(this)
            rvAttributes.adapter = attributeAdapter
        } else if (mHistory?.type == HistoryType.VERIFY) {
            if ((mHistory?.attributes ?: ArrayList()).size > 0 && ReceiptWrapper.checkExchangeType(
                    mHistory?.attributes ?: ArrayList()
                ) == CredentialTypes.RECEIPT
            ) {

                val receipt =
                    ReceiptWrapper.convertReceiptFromExchange(mHistory?.attributes ?: ArrayList())
                if (receipt == null) {
                    setUpV1VerficationList()
                } else {
                    sectionAdapterV2 = SectionAdapterV2(
                        ReceiptWrapper.getAttributesFromReceipt(receipt),
                        isBlur,
                        ReceiptWrapper.getSections(receipt)
                    )
                    rvAttributes.layoutManager = LinearLayoutManager(this)
                    rvAttributes.adapter = sectionAdapterV2
                }
            } else {
                setUpV1VerficationList()
            }

        } else {
            if (ReceiptWrapper.checkCredentialType(
                    mHistory?.wallet?.credentialProposalDict?.credentialProposal?.attributes
                        ?: ArrayList()
                ) == CredentialTypes.RECEIPT
            ) {
                val receipt = ReceiptWrapper.convertReceipt(
                    mHistory?.wallet?.credentialProposalDict?.credentialProposal?.attributes
                        ?: ArrayList()
                )
                if (receipt == null) {
                    setupV1OfferList()
                } else {
                    sectionAdapterV2 = SectionAdapterV2(
                        ReceiptWrapper.getAttributesFromReceipt(receipt),
                        isBlur,
                        ReceiptWrapper.getSections(receipt)
                    )
                    rvAttributes.layoutManager = LinearLayoutManager(this)
                    rvAttributes.adapter = sectionAdapterV2
                }
            } else {
                setupV1OfferList()
            }
        }
    }

    private fun setupV1OfferList() {
        attributeAdapter = SectionAdapter(
            getCertificateAttributeListFromWalletModel(mHistory?.wallet),
            isBlur,
            if (mHistory?.wallet?.sectionStruct != null && (mHistory?.wallet?.sectionStruct?.size
                    ?: 0) > 0
            ) mHistory?.wallet?.sectionStruct else ArrayList()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = attributeAdapter
    }

    private fun setUpV1VerficationList() {
        rvAttributes.setBackgroundResource(R.drawable.primary_background)
        val width = DisplayUtils.getScreenWidth() - TextUtils.convertDpToPixel(
            60f,
            rvAttributes.context
        )
        adapter = ExchangeRequestAttributeAdapter(
            mHistory?.attributes ?: ArrayList(), false, width.toInt()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

    private fun setUpView() {
        if ((mHistory?.version ?: 0) == 2) {
            setUpView(
                mHistory?.connectionV2?.logoImageUrl ?: mHistory?.logo ?: "",
                mHistory?.connectionV2?.coverImageUrl ?: "",
                mHistory?.connectionV2?.organisationName ?: mHistory?.wallet?.connection?.theirLabel
                ?: "",
                mHistory?.connectionV2?.description
            )
        } else {
            setUpView(
                mHistory?.connectionHistory?.orgDetails?.logoImageUrl ?: mHistory?.logo ?: "",
                mHistory?.connectionHistory?.orgDetails?.coverImageUrl ?: "",
                mHistory?.connectionHistory?.orgDetails?.name
                    ?: mHistory?.wallet?.connection?.theirLabel ?: "",
                mHistory?.connectionHistory?.orgDetails?.location
            )
        }

        if (mHistory?.notification != null) {
            tvDate.visibility = View.GONE
            tvDataUsingService.visibility = View.VISIBLE

            cvCompany.visibility = View.VISIBLE

            Glide
                .with(ivCompanyLogo.context)
                .load(mHistory?.notification?.controllerDetails?.logoImageUrl ?: "")
                .centerCrop()
                .placeholder(R.drawable.images)
                .into(ivCompanyLogo)

            tvCompanyName.text = mHistory?.notification?.controllerDetails?.organisationName ?: ""
            tvType.text = mHistory?.notification?.controllerDetails?.location ?: ""
            tvShareDate.text = DateUtils.getRelativeTime(
                mHistory?.date
                    ?: ""
            )
        }
    }

    private fun setUpView(logo: String?, cover: String?, title: String?, desc: String?) {
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

        tvName.text = title
        tvLocation.text = desc
    }

    override fun onDestroy() {
        super.onDestroy()
        isBlur = true
    }

    companion object {
        const val EXTRA_DATA_HISTORY = "io.igrant.mobileagent.activty.history"
    }
}