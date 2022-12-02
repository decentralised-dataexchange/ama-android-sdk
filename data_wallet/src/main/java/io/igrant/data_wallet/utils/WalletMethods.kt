package io.igrant.data_wallet.utils

import org.hyperledger.indy.sdk.non_secrets.WalletRecord
import org.hyperledger.indy.sdk.wallet.Wallet

object WalletMethods {

    fun addWalletRecord(
        wallet: Wallet?,
        type: String,
        connectionUuid: String,
        value: String,
        tags: String
    ) {
        if (wallet != null)
            WalletRecord.add(
                wallet,
                type,
                connectionUuid,
                value,
                tags
            ).get()
    }

    fun updateWalletRecord(
        wallet: Wallet?,
        type: String,
        id: String?,
        value: String
    ) {
        if (wallet != null)
            WalletRecord.updateValue(
                wallet,
                type,
                id,
                value
            ).get()
    }

    fun updateWalletRecordTag(
        wallet: Wallet?,
        type: String,
        id: String?,
        tagJson: String
    ) {
        if (wallet != null)
            WalletRecord.updateTags(
                wallet,
                type,
                id,
                tagJson
            ).get()
    }
}