package io.igrant.data_wallet.models.prefs

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.prefs.Dus

data class Prefs(

    @SerializedName("instance_id") var instanceId: String? = null,
    @SerializedName("instance_permission_state") var instancePermissionState: String? = null,
    @SerializedName("sector") var sector: String? = null,
    @SerializedName("purposeName") var purposeName: String? = null,
    @SerializedName("dus") var dus: ArrayList<Dus> = arrayListOf()

)
