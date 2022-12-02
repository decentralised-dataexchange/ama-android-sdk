package io.igrant.data_wallet.utils

import io.igrant.data_wallet.models.certificateOffer.Attributes
import io.igrant.data_wallet.models.selfAttestedCredentials.SelfAttestedAttribute
import io.igrant.data_wallet.models.wallet.Section
import io.igrant.data_wallet.models.wallet.WalletModel


object CertificateListingUtils {

    fun getCertificateAttributeListFromWalletModel(wallet: WalletModel?): ArrayList<ArrayList<Attributes>> {
        return if (wallet?.connection?.connectionType == ConnectionTypes.EBSI_CONNECTION_NATURAL_PERSON)
            mapToList(wallet.attributes ?: mapOf(), wallet.sectionStruct)
        else
            arrayListOf(
                wallet?.credentialProposalDict?.credentialProposal?.attributes ?: ArrayList()
            )

    }

    /**
     * This function is convert the Map<String, SelfAttestedAttribute> to Arraylist of Attributes
     * this is mainly used for the ebsi credential
     */
    fun mapToList(
        map: Map<String, SelfAttestedAttribute>,
        sectionList: ArrayList<Section>?
    ): ArrayList<ArrayList<Attributes>> {
        val list: ArrayList<ArrayList<Attributes>> = ArrayList()

        if (sectionList != null) {
            for (section in sectionList) {
                val tempList: ArrayList<Attributes> = ArrayList()
                val tempMap = map.filterValues { it.parent == section.key }
                tempMap.forEach { (s, selfAttestedAttribute) ->
                    tempList.add(Attributes(s, selfAttestedAttribute.value ?: ""))
                }
                if (tempList.size > 0)
                    list.add(tempList)
            }
        }
        return list
    }
}