package io.igrant.data_wallet.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import io.igrant.data_wallet.R
import io.igrant.data_wallet.activity.HistoryDetailActivity.Companion.EXTRA_DATA_HISTORY
import io.igrant.data_wallet.adapter.HistoryAdapter
import io.igrant.data_wallet.fragment.FilterBottomSheetFragment
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.listeners.ConnectionFilterClickListener
import io.igrant.data_wallet.listeners.HistoryClickListener
import io.igrant.data_wallet.models.ConnectionFilter
import io.igrant.data_wallet.models.history.History
import io.igrant.data_wallet.models.walletSearch.Record
import io.igrant.data_wallet.tags.TagDataShareHistory
import io.igrant.data_wallet.utils.ConnectionFilterUtil
import io.igrant.data_wallet.utils.FilterType
import io.igrant.data_wallet.utils.SearchUtils
import io.igrant.data_wallet.utils.WalletRecordType

class HistoryActivity : BaseActivity(), ConnectionFilterClickListener {

    private val list: ArrayList<History> = ArrayList()
    private val listCopy: ArrayList<History> = ArrayList()
    private lateinit var adapter: HistoryAdapter
    private lateinit var rvHistory: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var etSearch: EditText

    private var orgId = ""
    private var filter: ConnectionFilter? = ConnectionFilter(
        id = ConnectionFilterUtil.HISTORY_FILTER_ALL,
        isEnabled = true,
        isSelected = true,
        logo = null
    )

    companion object {
        const val TAG = "HistoryActivity"
        const val EXTRA_ORG_ID = "io.igrant.mobileagent.activty.HistoryActivity.orgId"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)
        getIntentData()
        initToolbar()
        initView()
        getHistory()
        initListener()
    }

    private fun getIntentData() {
        if (intent.hasExtra(EXTRA_ORG_ID))
            orgId = intent.getStringExtra(EXTRA_ORG_ID) ?: ""
    }

    private fun initListener() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s)
            }
        })
    }

    private fun filterList(s: CharSequence?) {
        val tempList: ArrayList<History> = ArrayList()
        for (certificate in listCopy) {

            if ((certificate.name ?: "").contains(s ?: "", ignoreCase = true)) {
                tempList.add(certificate)
            }
        }

        list.clear()
        list.addAll(tempList)

        adapter.notifyDataSetChanged()
    }

    private fun initView() {
        rvHistory = findViewById(R.id.rvHistory)
        llErrorMessage = findViewById(R.id.llErrorMessage)
        etSearch = findViewById(R.id.etSearch)
    }

    private fun initToolbar() {
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.welcome_my_shared_data)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_filter, menu)
//        return true
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.action_filter -> {
                val blankFragment =
                    FilterBottomSheetFragment.newInstance(FilterType.MY_SHARED_HISTORY,filter?.id)
                blankFragment.show(supportFragmentManager, " blankFragment.tag")

                true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setUpAdapter() {
        adapter = HistoryAdapter(list, object : HistoryClickListener {
            override fun onClick(history: History) {
                val intent = Intent(this@HistoryActivity, HistoryDetailActivity::class.java)
                intent.putExtra(EXTRA_DATA_HISTORY, WalletManager.getGson.toJson(history))
                startActivity(intent)
            }
        })
        rvHistory.adapter = adapter

        if (list.size > 0) {
            llErrorMessage.visibility = View.GONE
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }

    }

    private fun getHistory() {
        val tagDataShareHistory = TagDataShareHistory()
        var tagJson = "{}"

        if (filter?.id != ConnectionFilterUtil.HISTORY_FILTER_ALL) {
            tagDataShareHistory.thirdParty =
                if (filter?.id == ConnectionFilterUtil.HISTORY_FILTER_ACTIVE) "false" else "true"
        }
        if (orgId != "")
            tagDataShareHistory.orgId = orgId

        tagJson = WalletManager.getGson.toJson(tagDataShareHistory)

        val history = SearchUtils.searchWallet(WalletRecordType.DATA_HISTORY, tagJson)

        list.clear()
        list.addAll(parseArray(history.records ?: ArrayList()))

        list.sort()
        listCopy.addAll(list)
        setUpAdapter()
    }

    private fun parseArray(arrayList: ArrayList<Record>): ArrayList<History> {
        val temp: ArrayList<History> = ArrayList()

        for (record in arrayList) {
            temp.add(WalletManager.getGson.fromJson(record.value, History::class.java))
        }

        return temp
    }

    override fun onFilterClick(filter: ConnectionFilter) {
        this.filter = filter
        getHistory()
    }

    private fun updateList(filter: ConnectionFilter) {
        Toast.makeText(this, "${filter.name}", Toast.LENGTH_SHORT).show()
    }
}