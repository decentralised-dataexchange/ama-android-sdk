package io.igrant.data_wallet.models.notification

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.prefs.ControllerDetails


data class NotificationBody(

    @SerializedName("@type") var type: String? = null,
    @SerializedName("@id") var id: String? = null,
    @SerializedName("controller_details") var controllerDetails: ControllerDetails? = ControllerDetails(),
    @SerializedName("da_instance_id") var daInstanceId: String? = null

)