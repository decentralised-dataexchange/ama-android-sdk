package io.igrant.data_wallet.utils

import io.igrant.data_wallet.models.selfAttestedCredentials.AttributeTypes
import io.igrant.data_wallet.models.selfAttestedCredentials.SelfAttestedAttribute
import io.igrant.data_wallet.models.selfAttestedCredentials.SelfAttestedAttributeWithKey
import io.igrant.data_wallet.models.walletSearch.Record
import org.json.JSONObject

object SelfAttestedDataSearch {

    fun searchSelfAttestedCredentialsForData(name:String?):ArrayList<SelfAttestedAttributeWithKey>{
        val list: ArrayList<SelfAttestedAttributeWithKey> = ArrayList()
        val records:ArrayList<Record> = ArrayList()
        var walletSearch =
            SearchUtils.searchWallet(WalletRecordType.WALLET, "{\"type\":\"${WalletRecordType.CERTIFICATE_TYPE_SELF_ATTESTED}\"}")

        records.addAll(walletSearch.records?:ArrayList())

        walletSearch =
                SearchUtils.searchWallet(WalletRecordType.WALLET, "{\"type\":\"${WalletRecordType.CERTIFICATE_TYPE_ID_CARDS}\"}")

        records.addAll(walletSearch.records?:ArrayList())

        walletSearch =
            SearchUtils.searchWallet(WalletRecordType.WALLET, "{\"type\":\"${WalletRecordType.CERTIFICATE_TYPE_PROFILE}\"}")

        records.addAll(walletSearch.records?:ArrayList())


        for (record in records) {
            val jsonObject = JSONObject(record.value)
            var subObj: JSONObject
            when (jsonObject.getString("sub_type")) {
                WalletRecordType.TYPE_PASSPORT -> {
                    subObj = jsonObject.getJSONObject("passportDetails")
                    if (subObj.has(name)) {
                        val value = if (subObj.getJSONObject(name).getString("type") == AttributeTypes.IMAGE) subObj.getJSONObject(name).getString("value").replace("\n", "") else subObj.getJSONObject(name).getString("value")
                        list.add(SelfAttestedAttributeWithKey(record.id
                                ?: "", jsonObject.getString("sub_type"), SelfAttestedAttribute(value, subObj.getJSONObject(name).getString("type"), subObj.getJSONObject(name).getString("imageType"),"","")
                        ))
                    }
                }
                WalletRecordType.TYPE_PASS_BOARDING_PASS,
                WalletRecordType.TYPE_PROFILE-> {
                    if (jsonObject.has("attributes")) {
                        subObj = jsonObject.getJSONObject("attributes")
                        if (subObj.has(name)) {
                            val value = if (subObj.getJSONObject(name)
                                    .getString("type") == AttributeTypes.IMAGE
                            ) subObj.getJSONObject(name).getString("value")
                                .replace("\n", "") else subObj.getJSONObject(name)
                                .getString("value")
                            list.add(
                                SelfAttestedAttributeWithKey(
                                    record.id
                                        ?: "",
                                    jsonObject.getString("sub_type"),
                                    SelfAttestedAttribute(
                                        value,
                                        subObj.getJSONObject(name).getString("type"),
                                        subObj.getJSONObject(name).getString("imageType"),
                                        ""
                                    )
                                )
                            )
                        }
                    }
                }
                WalletRecordType.TYPE_INDIAN_VACCINATION_CERTIFICATE -> {
                    subObj = jsonObject.getJSONObject("indianCovidCertificate")
                    if (subObj.has(name))
                        list.add(SelfAttestedAttributeWithKey(record.id
                                ?: "", jsonObject.getString("sub_type"), SelfAttestedAttribute(
                            subObj.getJSONObject(name).getString("value"),
                            subObj.getJSONObject(name).getString("type"),
                            "",
                            ""
                        )))
                }
                WalletRecordType.TYPE_PHILIPPINES_VACCINATION_CERTIFICATE -> {
                    subObj = jsonObject.getJSONObject("philippinesCovidCertificate")
                    if (subObj.has(name))
                        list.add(SelfAttestedAttributeWithKey(record.id
                            ?: "", jsonObject.getString("sub_type"), SelfAttestedAttribute(
                            subObj.getJSONObject(name).getString("value"),
                            subObj.getJSONObject(name).getString("type"),
                            "",
                            ""
                        )))
                }
                WalletRecordType.TYPE_EU_VACCINATION_CERTIFICATE, WalletRecordType.TYPE_UK_VACCINATION_CERTIFICATE,
                WalletRecordType.TYPE_MY_VACCINATION_CERTIFICATE-> {
                    subObj = jsonObject.getJSONObject("euCovidCertificate")
                    if (subObj.has(name))
                        list.add(SelfAttestedAttributeWithKey(record.id
                                ?: "", jsonObject.getString("sub_type"), SelfAttestedAttribute(
                            subObj.getJSONObject(name).getString("value"),
                            subObj.getJSONObject(name).getString("type"),
                            "",
                            ""
                        )))
                }
                WalletRecordType.TYPE_EU_TEST_RESULT_CERTIFICATE-> {
                    subObj = jsonObject.getJSONObject("euTestCertificate")
                    if (subObj.has(name))
                        list.add(SelfAttestedAttributeWithKey(record.id
                                ?: "", jsonObject.getString("sub_type"), SelfAttestedAttribute(
                            subObj.getJSONObject(name).getString("value"),
                            subObj.getJSONObject(name).getString("type"),
                            "",
                            ""
                        )))
                }
                WalletRecordType.TYPE_AADHAR_CARD -> {
                    subObj = jsonObject.getJSONObject("aadharCard")
                    if (subObj.has(name))
                        list.add(SelfAttestedAttributeWithKey(record.id
                                ?: "", jsonObject.getString("sub_type"), SelfAttestedAttribute(
                            subObj.getJSONObject(name).getString("value"),
                            subObj.getJSONObject(name).getString("type"),
                            "",
                            ""
                        )))
                }
            }

        }
        return list
    }
}