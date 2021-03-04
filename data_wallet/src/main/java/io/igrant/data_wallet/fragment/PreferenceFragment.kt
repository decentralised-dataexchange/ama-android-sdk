package io.igrant.data_wallet.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.igrant.data_wallet.R

class PreferenceFragment: BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_preference, container, false)
    }

    companion object {
        fun newInstance(): PreferenceFragment {
            return PreferenceFragment()
        }
    }

}