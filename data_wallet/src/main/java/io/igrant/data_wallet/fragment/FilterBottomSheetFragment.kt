package io.igrant.data_wallet.fragment

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.igrant.data_wallet.R
import io.igrant.data_wallet.adapter.ConnectionFilterAdapter
import io.igrant.data_wallet.custom.ItemDecorator
import io.igrant.data_wallet.listeners.ConnectionFilterClickListener
import io.igrant.data_wallet.models.ConnectionFilter
import io.igrant.data_wallet.utils.ConnectionFilterUtil.getFilterList

class FilterBottomSheetFragment : BottomSheetDialogFragment() {

    private var mListener: ConnectionFilterClickListener? = null
    lateinit var rvFilter: RecyclerView

    private var filterType : Int? = 0
    private var selectedId : Int? = 0
    private var extraList : ArrayList<String>? = ArrayList()

    companion object {
        fun newInstance(
            filterType: Int,
            selectedId: Int?=0,
            extraList:ArrayList<String>? = ArrayList()
        ): FilterBottomSheetFragment {
            val fragment = FilterBottomSheetFragment()
            val args = Bundle()
            args.putInt(EXTRA_FILTER_FROM, filterType)
            args.putInt(EXTRA_FILTER_SELECTED, selectedId?:0)
            args.putStringArrayList(EXTRA_FILTER_LIST, extraList)
            fragment.arguments = args
            return fragment
        }
        private const val EXTRA_FILTER_FROM = "HistoryFilterBottomSheetFragment.filter"
        private const val EXTRA_FILTER_SELECTED = "HistoryFilterBottomSheetFragment.selected"
        private const val EXTRA_FILTER_LIST = "HistoryFilterBottomSheetFragment.list"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view =
            inflater.inflate(R.layout.fragment_filter_bottom_sheet, container, false)
        initView(view)
        getIntentData()
        setUpList()
        return view
    }

    private fun getIntentData() {
        filterType  = requireArguments().getInt(EXTRA_FILTER_FROM)
        selectedId = requireArguments().getInt(EXTRA_FILTER_SELECTED)
        extraList = requireArguments().getStringArrayList(EXTRA_FILTER_LIST)
    }

    private fun initView(view: View) {
        rvFilter = view.findViewById(R.id.rvFilter)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mListener = context as ConnectionFilterClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException(
                activity.toString()
                        + " must implement OnListItemSelectedListener"
            )
        }
    }

    private fun setUpList() {
        val list = getFilterList(requireContext(),filterType, extraList)
        val adapter = ConnectionFilterAdapter(list, object : ConnectionFilterClickListener {
            override fun onFilterClick(filter: ConnectionFilter) {
                if (mListener != null)
                    mListener?.onFilterClick(filter)

                Handler(Looper.getMainLooper()).postDelayed({
                    dismiss()
                }, 500)
            }
        }, selectedId)
        val layoutManager = LinearLayoutManager(requireContext())
        val dividerItemDecoration = ItemDecorator(
            ContextCompat.getDrawable(requireContext(), R.drawable.list_divider_layer)!!
        )
        rvFilter.addItemDecoration(dividerItemDecoration)
        rvFilter.layoutManager = layoutManager
        rvFilter.adapter = adapter
    }

    override fun getTheme(): Int {
        return R.style.CustomBottomSheetDialog;
    }
}