package io.igrant.data_wallet.models.prefs

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.prefs.Prefs

data class PrefsResponse (

    @SerializedName("prefs"   ) var prefs   : ArrayList<Prefs>  = arrayListOf(),
    @SerializedName("sectors" ) var sectors : ArrayList<String> = arrayListOf()

)
