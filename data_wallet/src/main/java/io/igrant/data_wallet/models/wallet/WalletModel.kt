package io.igrant.data_wallet.models.wallet

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.data_wallet.models.MediatorConnectionObject
import io.igrant.data_wallet.models.connection.Connection
import io.igrant.data_wallet.models.credentialExchange.CredentialProposalDict
import io.igrant.data_wallet.models.credentialExchange.RawCredential
import java.io.Serializable

class WalletModel : Serializable {

    @SerializedName("raw_credential")
    @Expose
    var rawCredential: RawCredential? = null

    @SerializedName("credential_id")
    @Expose
    var credentialId: String? = null

    @SerializedName("connection")
    @Expose
    var connection: MediatorConnectionObject? = null

    @SerializedName("credential_proposal_dict")
    @Expose
    var credentialProposalDict: CredentialProposalDict? = null

    @SerializedName("organization")
    @Expose
    var organization: Connection? = null

}