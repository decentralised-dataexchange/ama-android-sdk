package io.igrant.data_wallet.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.presentationExchange.CredentialValue

class RequestCredential {

    @SerializedName("self_attested_attributes")
    @Expose
    var selfAttestedAttributes: Object? = Object()

    @SerializedName("requested_attributes")
    @Expose
    var requestedAttributes: Map<String, CredentialValue>? = null

    @SerializedName("requested_predicates")
    @Expose
    var requestedPredicates: Object? = Object()
}