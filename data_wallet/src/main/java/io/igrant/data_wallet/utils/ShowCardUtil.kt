package io.igrant.data_wallet.utils

import android.content.Context
import android.content.Intent
import io.igrant.data_wallet.activity.CertificateDetailActivity
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.wallet.WalletModel

object ShowCardUtil {

    fun showCard(exchangeData: ExchangeData, context: Context) {
        val credentialList = SearchUtils.searchWallet(
            WalletRecordType.WALLET,
            "{ \"credential_id\":\"${exchangeData.recordId}\"}"
        )
        if (credentialList.records?.size ?: 0 > 0) {
            if (exchangeData.type == 0) {

                val intent = Intent(context, CertificateDetailActivity::class.java)
                intent.putExtra(
                    CertificateDetailActivity.EXTRA_WALLET_DETAIL,
                    credentialList.records?.get(0)?.value
                )
                intent.putExtra(CertificateDetailActivity.EXTRA_FROM, "exchange")
                context.startActivity(intent)

            } else {
                val certificate = WalletManager.getGson.fromJson(
                    credentialList.records?.get(0)?.value,
                    WalletModel::class.java
                )
//                when (exchangeData.selfAttestedSubType) {
//                    WalletRecordType.TYPE_PASSPORT, WalletRecordType.TYPE_SINGAPORE_PASSPORT -> {
//                        val intent = Intent(context, PassportDetailActivity::class.java)
//                        intent.putExtra(
//                            PassportDetailActivity.EXTRA_PASSPORT_DETAIL,
//                            certificate.passportDetails
//                        )
//                        intent.putExtra(
//                            PassportDetailActivity.EXTRA_CREDENTIAL_ID,
//                            certificate.credentialId
//                        )
//                        intent.putExtra(
//                            PassportDetailActivity.EXTRA_IS_FROM,
//                            "exchange"
//                        )
//                        intent.putExtra(PassportDetailActivity.EXTRA_IS_FROM_VIEW, true)
//                        context.startActivity(intent)
//                    }
//
//                    WalletRecordType.TYPE_PASS_BOARDING_PASS -> {
//                        val intent = Intent(context, PassActivity::class.java)
//
//                        intent.putExtra(
//                            PassActivity.EXTRA_PASS_STRING,
//                            certificate.data ?: ""
//                        )
//                        intent.putExtra(
//                            PassActivity.EXTRA_IS_VIEW,
//                            true
//                        )
//                        intent.putExtra(
//                            PassActivity.EXTRA_LOGO,
//                            certificate.logo?.value ?: ""
//                        )
//                        intent.putExtra(
//                            PassActivity.EXTRA_ICON,
//                            certificate.icon?.value ?: ""
//                        )
//
//                        context.startActivity(intent)
//                    }
//
//                    WalletRecordType.TYPE_INDIAN_VACCINATION_CERTIFICATE -> {
//                        val intent =
//                            Intent(context, IndianVaccinationCertificateDetailActivity::class.java)
//                        intent.putExtra(
//                            IndianVaccinationCertificateDetailActivity.EXTRA_IN_COVID_DETAIL,
//                            certificate.indianCertificate
//                        )
//                        intent.putExtra(
//                            IndianVaccinationCertificateDetailActivity.EXTRA_IN_COVID_CREDENTIAL_ID,
//                            certificate.credentialId
//                        )
//                        intent.putExtra(
//                            IndianVaccinationCertificateDetailActivity.EXTRA_IS_FROM,
//                            "exchange"
//                        )
//                        intent.putExtra(
//                            IndianVaccinationCertificateDetailActivity.EXTRA_IN_COVID_IS_FROM_VIEW,
//                            true
//                        )
//                        context.startActivity(intent)
//                    }
//
//                    WalletRecordType.TYPE_PHILIPPINES_VACCINATION_CERTIFICATE -> {
//                        val intent = Intent(
//                            context,
//                            PhilippinesVaccinationCertificateDetailActivity::class.java
//                        )
//                        intent.putExtra(
//                            PhilippinesVaccinationCertificateDetailActivity.EXTRA_IN_COVID_DETAIL,
//                            certificate.philippinesCovidCertificate
//                        )
//                        intent.putExtra(
//                            PhilippinesVaccinationCertificateDetailActivity.EXTRA_IN_COVID_CREDENTIAL_ID,
//                            certificate.credentialId
//                        )
//                        intent.putExtra(
//                            PhilippinesVaccinationCertificateDetailActivity.EXTRA_IS_FROM,
//                            "exchange"
//                        )
//                        intent.putExtra(
//                            PhilippinesVaccinationCertificateDetailActivity.EXTRA_IN_COVID_IS_FROM_VIEW,
//                            true
//                        )
//                        context.startActivity(intent)
//                    }
//
//                    WalletRecordType.TYPE_AADHAR_CARD -> {
//                        val intent = Intent(context, IndianAadharDetailActivity::class.java)
//                        intent.putExtra(
//                            IndianAadharDetailActivity.EXTRA_IN_AADHAR_DETAIL,
//                            certificate.aadharCard
//                        )
//                        intent.putExtra(
//                            IndianAadharDetailActivity.EXTRA_IN_AADHAR_CREDENTIAL_ID,
//                            certificate.credentialId
//                        )
//                        intent.putExtra(
//                            IndianAadharDetailActivity.EXTRA_IS_FROM,
//                            "exchange"
//                        )
//                        intent.putExtra(
//                            IndianAadharDetailActivity.EXTRA_IN_AADHAR_IS_FROM_VIEW,
//                            true
//                        )
//                        context.startActivity(intent)
//                    }
//
//                    WalletRecordType.TYPE_EU_VACCINATION_CERTIFICATE, WalletRecordType.TYPE_UK_VACCINATION_CERTIFICATE -> {
//                        val intent =
//                            Intent(context, EUVaccinationCertificateDetailActivity::class.java)
//                        intent.putExtra(
//                            EUVaccinationCertificateDetailActivity.EXTRA_IN_COVID_DETAIL,
//                            certificate.euCertificate
//                        )
//                        intent.putExtra(
//                            EUVaccinationCertificateDetailActivity.EXTRA_IN_COVID_COUNTRY,
//                            certificate.subType
//                        )
//                        intent.putExtra(
//                            EUVaccinationCertificateDetailActivity.EXTRA_IN_COVID_CREDENTIAL_ID,
//                            certificate.credentialId
//                        )
//                        intent.putExtra(
//                            EUVaccinationCertificateDetailActivity.EXTRA_IS_FROM,
//                            "exchange"
//                        )
//                        intent.putExtra(
//                            EUVaccinationCertificateDetailActivity.EXTRA_IN_COVID_IS_FROM_VIEW,
//                            true
//                        )
//                        context.startActivity(intent)
//                    }
//                }
            }
        }
    }
}