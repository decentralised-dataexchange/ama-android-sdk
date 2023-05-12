package io.igrant.data_wallet.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.tbuonomo.viewpagerdotsindicator.DotsIndicator
import io.igrant.data_wallet.R
import io.igrant.data_wallet.adapter.DataAgreementPolicyPagerAdapter
import io.igrant.data_wallet.adapter.ProofAdapter
import io.igrant.data_wallet.models.DataAgreementPolicyUIModel
import io.igrant.data_wallet.models.certificateOffer.DataAgreementContext
import io.igrant.data_wallet.models.certificateOffer.DataAgreementPolicy
import io.igrant.data_wallet.models.history.History
import io.igrant.data_wallet.utils.DataAgreementContextBodyUtils
import io.igrant.data_wallet.utils.PolicyCacheManager

class DataAgreementPolicyActivity : BaseActivity() {

    private lateinit var viewPagerAdapter: DataAgreementPolicyPagerAdapter
    private var historyData: History? = null
    private lateinit var adapter: ProofAdapter
    private var purposeDetail: DataAgreementPolicy? = null

    private var dataAgreementContext: DataAgreementContext? = null

    private var connectionId: String = ""

    private var historyId: String = ""

    private lateinit var toolbar: Toolbar
    private lateinit var dotsIndicator: DotsIndicator
    private lateinit var vpAttributes: ViewPager2

//    private lateinit var rvAttributes: RecyclerView
//    private lateinit var rvProof: RecyclerView

    private var isSignatureVerified: Boolean? = false

    companion object {
        const val EXTRA_CONNECTION_ID =
            "io.igrant.mobileagent.activty.DataAgreementPolicyActivity.connectionId"
        const val EXTRA_DATA_AGREEMENT_CONTEXT =
            "io.igrant.mobileagent.activty.DataAgreementPolicyActivity.dataAgreementContext"
        const val EXTRA_POLICY_DETAILS =
            "io.igrant.mobileagent.activty.DataAgreementPolicyActivity.policyDetails"
        const val EXTRA_HISSTORY_ID =
            "io.igrant.mobileagent.activty.DataAgreementPolicyActivity.historyId"
        const val EXTRA_IS_SIGNATURE_VERIFIED =
            "io.igrant.mobileagent.activty.DataAgreementPolicyActivity.isSignatureVerified"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_data_agreement_policy)

        initViews()
        getIntentData()
        setUpToolbar()
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.certificate_data_agreement_policy)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onBackPressed() {
        val resultIntent = Intent()
        resultIntent.putExtra(
            EXTRA_IS_SIGNATURE_VERIFIED,
            PolicyCacheManager.policyList?.get(0)?.isSignatureVerified
        )
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getIntentData() {
        viewPagerAdapter = DataAgreementPolicyPagerAdapter(this)
        vpAttributes.adapter = viewPagerAdapter
        dotsIndicator.setViewPager2(vpAttributes)

        if (PolicyCacheManager.policyList != null && (PolicyCacheManager.policyList?.size
                ?: 0) > 1
        ) {
            dotsIndicator.visibility = View.VISIBLE
        } else {
            dotsIndicator.visibility = View.GONE
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        vpAttributes = findViewById(R.id.vpPolicy)
        dotsIndicator = findViewById(R.id.dotsIndicator)
    }

}

object DataAgreementPolicyUtil {
    fun showDataAgreementPolicy(mHistory: History?, context: Activity) {
        if ((mHistory?.version ?: 0) == 2) {
            val intent = Intent(context, DataAgreementPolicyActivity::class.java)
            var dataAgreementPolicyUIModel = DataAgreementPolicyUIModel(
                mHistory?.id,
                mHistory?.connectionId ?: "",
                null,
                if (DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(mHistory?.dataAgreementContext?.message?.body)) {
                    DataAgreementPolicy(
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mHistory?.dataAgreementContext?.message?.body).lawfulBasis
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.policyURL
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.jurisdiction
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.industrySector
                            ?: "",
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.geographicRestriction
                            ?: "",
                        null,
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.dataRetentionPeriod.toString()
                    )
                } else {
                    DataAgreementPolicy(
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mHistory?.dataAgreementContext?.message?.body).lawfulBasis
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.policyURL
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.jurisdiction
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.industrySector
                            ?: "",
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.geographicRestriction
                            ?: "",
                        null,
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(mHistory?.dataAgreementContext?.message?.body).dataPolicy?.dataRetentionPeriod.toString()
                    )
                },
                mHistory?.dataAgreementContext
            )

            PolicyCacheManager.policyList = arrayListOf(dataAgreementPolicyUIModel)
            context.startActivityForResult(intent, DATA_AGREEMENT_POLICY_RESULT)
        } else {
            val isDataAgreementAvailable = mHistory?.connectionHistory?.purposeDetails != null
            if (isDataAgreementAvailable) {
                val intent = Intent(context, DataAgreementPolicyActivity::class.java)

                var dataAgreementPolicyUIModel = DataAgreementPolicyUIModel(
                    null,
                    null,
                    null,
                    DataAgreementPolicy(
                        mHistory?.connectionHistory?.purposeDetails?.purpose?.lawfulBasisOfProcessing
                            ?: "",
                        mHistory?.connectionHistory?.purposeDetails?.purpose?.policyUrl ?: "",
                        mHistory?.connectionHistory?.purposeDetails?.purpose?.jurisdiction
                            ?: "",
                        mHistory?.connectionHistory?.purposeDetails?.purpose?.industryScope
                            ?: "",
                        mHistory?.connectionHistory?.purposeDetails?.purpose?.restriction ?: "",
                        mHistory?.connectionHistory?.purposeDetails?.purpose?.shared3pp,
                        null
                    ),
                    null
                )

                PolicyCacheManager.policyList = arrayListOf(dataAgreementPolicyUIModel)

                context.startActivityForResult(intent, DATA_AGREEMENT_POLICY_RESULT)
            } else {
                Toast.makeText(
                    context,
                    context.resources.getString(R.string.error_data_agreement_policy_not_available),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun showDataAgreementPolicy(
        mHistory: DataAgreementPolicy?,
        dataAgreementContext: DataAgreementContext?,
        mConnectionId: String?, context: Activity,
        isSigVerified: Boolean?,
        isPolicyOnly: Boolean? = false
    ) {
        val intent = Intent(context, DataAgreementPolicyActivity::class.java)
        var dataAgreementPolicyUIModel = DataAgreementPolicyUIModel(
            null,
            mConnectionId,
            isSigVerified,
            mHistory,
            dataAgreementContext,
            isPolicyOnly = isPolicyOnly
        )

        PolicyCacheManager.policyList = arrayListOf(dataAgreementPolicyUIModel)

        context.startActivityForResult(intent, DATA_AGREEMENT_POLICY_RESULT)
    }

    const val DATA_AGREEMENT_POLICY_RESULT = 7000
}