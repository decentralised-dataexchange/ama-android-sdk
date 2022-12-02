package io.igrant.data_wallet.models.response

import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.BaseResponse
import io.igrant.mobileagent.models.response.JsonLdProcessedDataResponseBody

class JsonLdProcessedDataResponse(
    @SerializedName("body")
    val body: JsonLdProcessedDataResponseBody
) : BaseResponse()