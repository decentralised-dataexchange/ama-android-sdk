package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class Party(

    @SerializedName("endpointID") var endpointID: String? = null,
    @SerializedName("partyName") var partyName: PartyName? = PartyName(),
    @SerializedName("postaladdress") var postaladdress: Postaladdress? = Postaladdress(),
    @SerializedName("partyIdentification") var partyIdentification: PartyIdentification? = PartyIdentification()

)