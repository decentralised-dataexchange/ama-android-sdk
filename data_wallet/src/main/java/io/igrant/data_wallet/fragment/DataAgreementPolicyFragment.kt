package io.igrant.data_wallet.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.adapter.ExchangeRequestAttributeAdapter
import io.igrant.data_wallet.adapter.ProofAdapter
import io.igrant.data_wallet.adapter.SectionAdapter
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.DataAgreementPolicyUIModel
import io.igrant.data_wallet.models.certificateOffer.Attributes
import io.igrant.data_wallet.models.certificateOffer.Proof
import io.igrant.data_wallet.models.certificateOffer.ProofDexa
import io.igrant.data_wallet.models.did.DidResult
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.history.History
import io.igrant.data_wallet.models.selfAttestedCredentials.AttributeTypes
import io.igrant.data_wallet.utils.*
import org.hyperledger.indy.sdk.non_secrets.WalletRecord

class DataAgreementPolicyFragment : BaseFragment() {

    private var dataAgreementPolicyUIModel: DataAgreementPolicyUIModel? = null
    private var mPosition = 0
    private var historyData: History? = null

    private lateinit var adapter: ProofAdapter

    private lateinit var rvAttributes: RecyclerView
    private lateinit var rvProof: RecyclerView

    private var isDexa: Boolean = false

    companion object {

        private const val EXTRA_POSITION =
            "io.igrant.mobileagent.fragment.DataAgreementPolicyFragment.position"

        fun newInstance(
            position: Int
        ): DataAgreementPolicyFragment {
            val fragment = DataAgreementPolicyFragment()
            val args = Bundle()
            args.putSerializable(EXTRA_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_data_agreement_policy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        getIntentData()
    }

    private fun initViews(view: View) {
        rvAttributes = view.findViewById(R.id.rvAttributes)
        rvProof = view.findViewById(R.id.rvProof)
    }

    private fun getIntentData() {
        mPosition = requireArguments().getInt(EXTRA_POSITION)
        dataAgreementPolicyUIModel =
            PolicyCacheManager.policyList?.get(mPosition)

        if (dataAgreementPolicyUIModel?.historyId != null && dataAgreementPolicyUIModel?.historyId != "") {
            val history = WalletRecord.get(
                WalletManager.getWallet,
                WalletRecordType.DATA_HISTORY,
                dataAgreementPolicyUIModel?.historyId,
                "{}"
            ).get()


            val result = WalletManager.getGson.fromJson(history, DidResult::class.java)
            historyData = WalletManager.getGson.fromJson(result.value, History::class.java)
        }

        isDexa = DataAgreementContextBodyUtils.checkDataAgreementContextBodyIsOfDexa(
            dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body
        )

//        if ((historyData != null && historyData?.isVerified != true)
//            || (dataAgreementPolicyUIModel?.dataAgreementContext != null && historyData == null)
//            || dataAgreementPolicyUIModel?.isSignatureVerified == true
//        )
//            SignOfferRequest.verifyDataAgreement(
//                dataAgreementPolicyUIModel?.dataAgreementContext?.clone(),
//                dataAgreementPolicyUIModel?.connectionId ?: "",
//                object : VerifyRequestListeners {
//                    override fun onVerified(isVerified: Boolean) {
//                        super.onVerified(isVerified)
//                        adapter.setVerifiedStatus(
//                            isVerified
//                        )
//                        PolicyCacheManager.policyList?.get(mPosition)?.isSignatureVerified = isVerified
//                        updateHistory(isVerified)
//                    }
//                })


        setUpDataAgreementViews()
        setUpProofList()
    }

    private fun updateHistory(isVerified: Boolean) {
        if (historyData != null) {
            historyData?.isVerified = isVerified

            WalletMethods.updateWalletRecord(
                WalletManager.getWallet,
                WalletRecordType.DATA_HISTORY,
                dataAgreementPolicyUIModel?.historyId,
                WalletManager.getGson.toJson(historyData)
            )
        }
    }

    private fun setUpProofList() {
        if (dataAgreementPolicyUIModel?.dataAgreementContext != null) {
            var proofChain: ArrayList<ProofDexa> = ArrayList()
            if (isDexa) {
                if (DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body).proofChain != null)
                    proofChain =
                        DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body).proofChain
                            ?: ArrayList()
                else proofChain.add(
                    DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body).proof
                        ?: ProofDexa()
                )
            } else {
                if (DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body).proofChain != null)
                    for (proof in DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body ).proofChain
                        ?: ArrayList()) {
                        proofChain.add(
                            ProofDexa(
                                proof.id,
                                proof.type,
                                null,
                                proof.created,
                                proof.verificationMethod,
                                proof.proofPurpose,
                                proof.proofValue,
                                null
                            )
                        )
                    }
                else {
                    val proof =
                        DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body).proof
                            ?: Proof()
                    proofChain.add(
                        ProofDexa(
                            proof.id,
                            proof.type,
                            null,
                            proof.created,
                            proof.verificationMethod,
                            proof.proofPurpose,
                            proof.proofValue,
                            null
                        )
                    )
                }
            }


            adapter =
                ProofAdapter(
                    proofChain,
                    historyData?.isVerified ?: dataAgreementPolicyUIModel?.isSignatureVerified
                    ?: false
                )
            rvProof.adapter = adapter
        }
    }

    private fun setUpDataAgreementViews() {
        if (dataAgreementPolicyUIModel?.dataAgreementContext != null) {
            val mainList: ArrayList<ArrayList<Attributes>> = ArrayList()

            if (isDexa) {
                val dataAgreementBody =
                     DataAgreementContextBodyUtils.convertToDataAgreementBodyOfDexa(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body)
                var subList: ArrayList<Attributes> = ArrayList()
                if (dataAgreementBody.purpose != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_purpose),
                            dataAgreementBody.purpose
                                ?: ""
                        )
                    )
                if (dataAgreementBody.purposeDescription != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_purpose_desc),
                            dataAgreementBody.purposeDescription
                                ?: ""
                        )
                    )

                if (dataAgreementBody.lawfulBasis != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_lawful_basis_of_processing),
                            dataAgreementBody.lawfulBasis
                                ?: ""
                        )
                    )

                if (subList.size > 0)
                    mainList.add(subList)

                subList = ArrayList()

                if (dataAgreementBody.dataPolicy?.dataRetentionPeriod != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_retention_period),
                            dataAgreementBody.dataPolicy?.dataRetentionPeriod.toString()
                        )
                    )
                if (dataAgreementBody.dataPolicy?.geographicRestriction != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_geographic_restriction),
                            dataAgreementBody.dataPolicy?.geographicRestriction
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.industrySector != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_industry_scope),
                            dataAgreementBody.dataPolicy?.industrySector
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.jurisdiction != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_jurisdiction),
                            dataAgreementBody.dataPolicy?.jurisdiction
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.policyURL != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_policy_url),
                            dataAgreementBody.dataPolicy?.policyURL
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.storageLocation != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_storage_location),
                            dataAgreementBody.dataPolicy?.storageLocation
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.thirdPartyDataSharing != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_third_party_data_sharing),
                            if (dataAgreementBody.dataPolicy?.thirdPartyDataSharing == true)
                                resources.getString(R.string.general_true) else
                                resources.getString(R.string.general_false)
                        )
                    )

                if (subList.size > 0)
                    mainList.add(subList)

                subList = ArrayList()

                if (dataAgreementBody.dpia?.dpiaSummaryUrl != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_policy_dpia_summary),
                            dataAgreementBody.dpia?.dpiaSummaryUrl
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dpia?.dpiaDate != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_policy_dpia_date),
                            dataAgreementBody.dpia?.dpiaDate
                                ?: ""
                        )
                    )



                if (subList.size > 0)
                    mainList.add(subList)

                //check if reciept present and show it
//                if (historyData?.reciept != null) {
//                    subList = ArrayList()
//
//                    if (historyData?.reciept?.blink != null) {
//
//                        val index: Int? = historyData?.reciept?.blink?.lastIndexOf(':')
//
//                        val id = historyData?.reciept?.blink?.substring(
//                            (index ?: 0) + 1,
//                            historyData?.reciept?.blink?.length ?: 0
//                        )
//
//                        subList.add(
//                            Attributes(
//                                resources.getString(R.string.data_agreement_policy_blink),
//                                "${InitialSync.getBlinkUrl(requireContext(),historyData?.reciept?.blink?:"")}$id"
//                            )
//                        )
//                    }
//
//                    if (historyData?.reciept?.mydataDid != null)
//                        subList.add(
//                            Attributes(
//                                resources.getString(R.string.data_agreement_policy_my_data_did),
//                                historyData?.reciept?.mydataDid ?: ""
//                            )
//                        )
//
//                    if (subList.size > 0)
//                        mainList.add(subList)
//                }
            } else {
                val dataAgreementBody =
                    DataAgreementContextBodyUtils.convertToNormalDataAgreementBody(dataAgreementPolicyUIModel?.dataAgreementContext?.message?.body)

                var subList: ArrayList<Attributes> = ArrayList()
                if (dataAgreementBody.purpose != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_purpose),
                            dataAgreementBody.purpose
                                ?: ""
                        )
                    )
                if (dataAgreementBody.purposeDescription != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_purpose_desc),
                            dataAgreementBody.purposeDescription
                                ?: ""
                        )
                    )

                if (dataAgreementBody.lawfulBasis != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_lawful_basis_of_processing),
                            dataAgreementBody.lawfulBasis
                                ?: ""
                        )
                    )

                if (subList.size > 0)
                    mainList.add(subList)

                subList = ArrayList()

                if (dataAgreementBody.dataPolicy?.dataRetentionPeriod != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_retention_period),
                            dataAgreementBody.dataPolicy?.dataRetentionPeriod.toString()
                        )
                    )
                if (dataAgreementBody.dataPolicy?.geographicRestriction != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_geographic_restriction),
                            dataAgreementBody.dataPolicy?.geographicRestriction
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.industrySector != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_industry_scope),
                            dataAgreementBody.dataPolicy?.industrySector
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.jurisdiction != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_jurisdiction),
                            dataAgreementBody.dataPolicy?.jurisdiction
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.policyURL != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.welcome_policy_url),
                            dataAgreementBody.dataPolicy?.policyURL
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dataPolicy?.storageLocation != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_ploicy_storage_location),
                            dataAgreementBody.dataPolicy?.storageLocation
                                ?: ""
                        )
                    )

                if (subList.size > 0)
                    mainList.add(subList)

                subList = ArrayList()

                if (dataAgreementBody.dpia?.dpiaSummaryUrl != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_policy_dpia_summary),
                            dataAgreementBody.dpia?.dpiaSummaryUrl
                                ?: ""
                        )
                    )

                if (dataAgreementBody.dpia?.dpiaDate != null)
                    subList.add(
                        Attributes(
                            resources.getString(R.string.data_agreement_policy_dpia_date),
                            dataAgreementBody.dpia?.dpiaDate
                                ?: ""
                        )
                    )



                if (subList.size > 0)
                    mainList.add(subList)

                //check if reciept present and show it
                if (historyData?.reciept != null) {
                    subList = ArrayList()

                    if (historyData?.reciept?.blink != null) {

                        val index: Int? = historyData?.reciept?.blink?.lastIndexOf(':')
                        val id = historyData?.reciept?.blink?.substring(
                            (index ?: 0) + 1,
                            historyData?.reciept?.blink?.length ?: 0
                        )

                        subList.add(
                            Attributes(
                                resources.getString(R.string.data_agreement_policy_blink),
                                "https://rinkeby.etherscan.io/tx/$id"
                            )
                        )
                    }

                    if (historyData?.reciept?.mydataDid != null)
                        subList.add(
                            Attributes(
                                resources.getString(R.string.data_agreement_policy_my_data_did),
                                historyData?.reciept?.mydataDid ?: ""
                            )
                        )

                    if (subList.size > 0)
                        mainList.add(subList)
                }
            }


            val adapter = SectionAdapter(
                mainList
            )
            rvAttributes.layoutManager = LinearLayoutManager(context)
            rvAttributes.adapter = adapter

        } else
            if (dataAgreementPolicyUIModel?.dataAgreementPolicy != null) {

                val list: ArrayList<ExchangeData> = ArrayList()

                var exchangeData = ExchangeData()
                exchangeData.name = resources.getString(R.string.welcome_lawful_basis_of_processing)
                exchangeData.dataType = AttributeTypes.STRING
                exchangeData.data =
                    dataAgreementPolicyUIModel?.dataAgreementPolicy?.lawFullBasis ?: ""
                list.add(exchangeData)

                exchangeData = ExchangeData()
                exchangeData.name = resources.getString(R.string.welcome_policy_url)
                exchangeData.dataType = AttributeTypes.STRING
                exchangeData.data = dataAgreementPolicyUIModel?.dataAgreementPolicy?.policyUrl ?: ""
                list.add(exchangeData)

                exchangeData = ExchangeData()
                exchangeData.name = resources.getString(R.string.welcome_jurisdiction)
                exchangeData.dataType = AttributeTypes.STRING
                exchangeData.data =
                    dataAgreementPolicyUIModel?.dataAgreementPolicy?.jurisdiction ?: ""
                list.add(exchangeData)

                exchangeData = ExchangeData()
                exchangeData.name = resources.getString(R.string.welcome_industry_scope)
                exchangeData.dataType = AttributeTypes.STRING
                exchangeData.data =
                    dataAgreementPolicyUIModel?.dataAgreementPolicy?.industryScope ?: ""
                list.add(exchangeData)

                exchangeData = ExchangeData()
                exchangeData.name = resources.getString(R.string.welcome_geographic_restriction)
                exchangeData.dataType = AttributeTypes.STRING
                exchangeData.data =
                    dataAgreementPolicyUIModel?.dataAgreementPolicy?.restriction ?: ""
                list.add(exchangeData)

                if (dataAgreementPolicyUIModel?.dataAgreementPolicy?.shared3PP != null) {
                    exchangeData = ExchangeData()
                    exchangeData.name = resources.getString(R.string.welcome_is_shared_to_3pps)
                    exchangeData.dataType = AttributeTypes.STRING
                    exchangeData.data =
                        if (dataAgreementPolicyUIModel?.dataAgreementPolicy?.shared3PP == true) "True" else "False"
                    list.add(exchangeData)
                }

                if (dataAgreementPolicyUIModel?.dataAgreementPolicy?.dataRetention != null) {
                    exchangeData = ExchangeData()
                    exchangeData.name =
                        resources.getString(R.string.data_agreement_ploicy_retention_period)
                    exchangeData.dataType = AttributeTypes.STRING
                    exchangeData.data =
                        dataAgreementPolicyUIModel?.dataAgreementPolicy?.dataRetention
                    list.add(exchangeData)
                }


                val width = DisplayUtils.getScreenWidth() - TextUtils.convertDpToPixel(
                    60f,
                    rvAttributes.context
                )
                val adapter = ExchangeRequestAttributeAdapter(
                    list, false, width.toInt()
                )
                rvAttributes.layoutManager = LinearLayoutManager(context)
                rvAttributes.adapter = adapter

            }
    }
}