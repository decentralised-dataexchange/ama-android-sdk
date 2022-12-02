package io.igrant.data_wallet.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.igrant.data_wallet.fragment.ProposeAndExchangeListFragment
import io.igrant.data_wallet.models.exchange.ExchangeData


class ProposeAndExchangePagerAdapter(val list:ArrayList<ArrayList<ExchangeData>>, var isBlur:Boolean, fragmentManager: FragmentActivity?) :
    FragmentStateAdapter(fragmentManager!!) {
    // Returns total number of pages
    override fun getItemCount(): Int =  list.size

    override fun createFragment(position: Int): Fragment = ProposeAndExchangeListFragment(list[position],isBlur)

    fun setBlurValue(isBlur: Boolean){
        this.isBlur = isBlur
        notifyDataSetChanged()
    }
}