package io.igrant.data_wallet.models.prefs

import com.google.gson.annotations.SerializedName

data class Dus(

    @SerializedName("dda_instance_permission_state") var ddaInstancePermissionState: String? = null,
    @SerializedName("controller_details") var controllerDetails: ControllerDetails? = ControllerDetails(),
    @SerializedName("dda_instance_id") var ddaInstanceId: String? = null

)