package io.igrant.data_wallet.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.ExchangeDataActivity
import io.igrant.data_wallet.activity.ProposeAndExchangeDataActivity
import io.igrant.data_wallet.adapter.ExchangeRequestAttributeAdapter
import io.igrant.data_wallet.events.BlurEvent
import io.igrant.data_wallet.listeners.ViewPagerHeightListener
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.utils.DisplayUtils
import io.igrant.data_wallet.utils.TextUtils
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*
import io.igrant.data_wallet.adapter.SectionAdapterV2
import io.igrant.data_wallet.utils.wrappers.CredentialTypes
import io.igrant.data_wallet.utils.wrappers.ReceiptWrapper


class ProposeAndExchangeListFragment(
    val list: ArrayList<ExchangeData>,
    var isBlur: Boolean
) : BaseFragment() {

    private lateinit var mListener: ViewPagerHeightListener
    private var adapter: ExchangeRequestAttributeAdapter? = null
    private var sectionAdapter: SectionAdapterV2? = null
    private lateinit var rvAttribute: RecyclerView
    private lateinit var rvSections: RecyclerView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mListener = context as ViewPagerHeightListener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_propose_and_exchange_list, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isBlur = try {
            ProposeAndExchangeDataActivity.isBlur
        } catch (e: Exception) {
            ExchangeDataActivity.isBlur
        }
        initViews(view)
        setUpCertificateList()
    }

    private fun setUpCertificateList() {
        if (list.size > 0 && ReceiptWrapper.checkExchangeType(list) == CredentialTypes.RECEIPT) {

            val receipt = ReceiptWrapper.convertReceiptFromExchange(list)
            if (receipt == null) {
                setUpV1Data()
            } else {
                rvSections.visibility = View.VISIBLE
                sectionAdapter = SectionAdapterV2(
                    ReceiptWrapper.getAttributesFromReceipt(receipt),
                    isBlur,
                    ReceiptWrapper.getSections(receipt)
                )
                rvSections.layoutManager = LinearLayoutManager(context)
                rvSections.adapter = sectionAdapter

                rvSections.viewTreeObserver.addOnGlobalLayoutListener {
                    mListener.setHeight(rvSections.height) //height is ready
                }
            }
        } else {
            setUpV1Data()
        }

    }

    fun setUpV1Data() {
        rvAttribute.visibility = View.VISIBLE
        val width = DisplayUtils.getScreenWidth() - TextUtils.convertDpToPixel(
            60f,
            rvAttribute.context
        )
        adapter = ExchangeRequestAttributeAdapter(
            list, isBlur, width.toInt()
        )
        rvAttribute.layoutManager = LinearLayoutManager(context)
        rvAttribute.adapter = adapter

        rvAttribute.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mListener.setHeight(rvAttribute.height) //height is ready
            }
        })
    }

    private fun initViews(view: View) {
        rvAttribute = view.findViewById(R.id.rvAttributes)
        rvSections = view.findViewById(R.id.rvSections)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun setBlurEvent(event: BlurEvent) {
        if (adapter != null)
            adapter?.setBlurValue(event.isBlur)

        sectionAdapter?.setUpBlur(event.isBlur)
    }

    override fun onDestroy() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
        super.onDestroy()
    }

}