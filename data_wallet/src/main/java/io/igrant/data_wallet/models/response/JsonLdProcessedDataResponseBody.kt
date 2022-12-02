package io.igrant.mobileagent.models.response

import com.google.gson.annotations.SerializedName

class JsonLdProcessedDataResponseBody (
    @SerializedName("framed_base64")
    var framedBase64: String? = null,
    @SerializedName("combined_hash_base64")
    var combinedHashBase64: String? = null
)