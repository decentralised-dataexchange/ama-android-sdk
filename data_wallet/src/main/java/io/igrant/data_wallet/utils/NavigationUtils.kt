package io.igrant.data_wallet.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import io.igrant.data_wallet.R
import io.igrant.data_wallet.fragment.WalletFragment

object NavigationUtils {

    private fun showFragment(
        fragment: Fragment,
        fragmentManager: FragmentManager,
        addToBackStack: Boolean
    ) {
        val transaction =
            fragmentManager.beginTransaction()
        transaction.replace(R.id.myContainer, fragment)
        if (addToBackStack) {
            transaction.addToBackStack("")
        }
        transaction.commit()
    }

    fun showWalletFragment(
        fragmentManager: FragmentManager?,
        addToBackStack: Boolean,
        showScanner: Boolean
    ) {
        val walletFragment: WalletFragment = WalletFragment.newInstance(showScanner)
        showFragment(walletFragment, fragmentManager!!, false)
    }

    fun popBack(fragmentManager: FragmentManager) {
        fragmentManager.popBackStack()
    }
}