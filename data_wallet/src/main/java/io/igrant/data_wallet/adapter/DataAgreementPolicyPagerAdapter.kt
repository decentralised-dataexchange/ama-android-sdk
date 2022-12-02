package io.igrant.data_wallet.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import io.igrant.data_wallet.fragment.DataAgreementPolicyFragment
import io.igrant.data_wallet.utils.PolicyCacheManager


class DataAgreementPolicyPagerAdapter(
    fragmentManager: FragmentActivity?) :
    FragmentStateAdapter(fragmentManager!!) {
    // Returns total number of pages
    override fun getItemCount(): Int =  PolicyCacheManager.policyList?.size?:0

    override fun createFragment(position: Int): Fragment = DataAgreementPolicyFragment.newInstance(
        position
    )
}