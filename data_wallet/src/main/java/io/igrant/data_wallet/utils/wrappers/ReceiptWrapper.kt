package io.igrant.data_wallet.utils.wrappers

import android.util.Log
import io.igrant.data_wallet.indy.WalletManager
import io.igrant.data_wallet.models.certificateOffer.Attributes
import io.igrant.data_wallet.models.exchange.ExchangeData
import io.igrant.data_wallet.models.receipt.InvoiceLine
import io.igrant.data_wallet.models.receipt.Receipt
import io.igrant.data_wallet.models.wallet.Section

object ReceiptWrapper {

    fun checkCredentialType(attributes: ArrayList<Attributes>): String {
        for (attribute in attributes) {
            Log.d("milna", "checkCredentialType: ${attribute.name} - ${attribute.value}")
            if (attribute.name?.lowercase() == "context" || attribute.name == "@context") {
                when (attribute.value) {
                    "urn:fdc:nsgb:2023:ereceipt:01:1.0" -> {
                        return CredentialTypes.RECEIPT
                    }
                }
            }
        }
        return CredentialTypes.DEFAULT
    }

    fun checkExchangeType(attributes: ArrayList<ExchangeData>): String {
        for (attribute in attributes) {
            Log.d("milna", "checkCredentialType: ${attribute.name} - ${attribute.value}")
            if (attribute.name?.lowercase() == "context" || attribute.name == "@context") {
                when (attribute.data) {
                    "urn:fdc:nsgb:2023:ereceipt:01:1.0" -> {
                        return CredentialTypes.RECEIPT
                    }
                }
            }
        }
        return CredentialTypes.DEFAULT
    }

    fun getAttributesFromReceipt(receipt: Receipt): ArrayList<ArrayList<Map<String, String>>> {
        val mainList: ArrayList<ArrayList<Map<String, String>>> = ArrayList()

        //adding invoice details
        var tempList: ArrayList<Map<String, String>> = ArrayList()
        tempList.add(mapOf("name" to "Item", "quantity" to "Qty", "value" to "Price"))
        for (item in receipt.invoiceLine) {
            tempList.add(
                mapOf(
                    "name" to (item.item?.name ?: ""),
                    "quantity" to (item.invoicedQuantity ?: ""),
                    "value" to "${receipt.documentCurrencyCode} ${String.format("%.2f", (calculateAmountAfterTax(item)))}"
                )
            )
        }
        tempList.add(
            mapOf(
                "name" to "Total",
                "quantity" to "",
                "value" to "${receipt.documentCurrencyCode} ${receipt.legalMonetaryTotal?.chargeTotalAmount.toString()}"
            )
        )
        mainList.add(tempList)

        tempList = ArrayList()
        tempList.add(
            mapOf(
                "name" to "Name",
                "value" to (receipt.accountingCustomerParty?.party?.partyName?.name ?: "")
            )
        )
        tempList.add(
            mapOf(
                "name" to "Issued",
                "value" to (receipt.issueDate ?: "")
            )
        )
        tempList.add(
            mapOf(
                "name" to "Payment Terms",
                "value" to (receipt.paymentTerms?.note ?: "")
            )
        )
        var address =
            "${receipt.accountingCustomerParty?.party?.postaladdress?.streetName}, ${receipt.accountingCustomerParty?.party?.postaladdress?.postalZone}\n${receipt.accountingCustomerParty?.party?.postaladdress?.cityName}, ${receipt.accountingCustomerParty?.party?.postaladdress?.country?.name}"
        tempList.add(
            mapOf(
                "name" to "Address",
                "value" to address
            )
        )
        mainList.add(tempList)
        return mainList
    }

    fun calculateAmountAfterTax(invoice: InvoiceLine): Double {
        val value = (invoice.price?.priceAmount ?: 0.0)
//        + (((invoice.price?.priceAmount
//            ?: 0.0) / 100) * (invoice.item?.classifiedTaxCategory?.percent ?: 0))
        return value
    }

    fun convertReceipt(attributes: ArrayList<Attributes>): Receipt? {
        for (attribute in attributes) {
            if (attribute.name?.lowercase() == "receipt") {
                Log.d("milna", "convertReceipt: ${attribute.value}")
                return WalletManager.getGson.fromJson(attribute.value, Receipt::class.java)
            }
        }
        return null
    }

    fun convertReceiptFromExchange(attributes: ArrayList<ExchangeData>): Receipt? {
        for (attribute in attributes) {
            if (attribute.name?.lowercase() == "receipt") {
                Log.d("milna", "convertReceipt: ${attribute.value}")
                return WalletManager.getGson.fromJson(attribute.data, Receipt::class.java)
            }
        }
        return null
    }

    fun getSections(receipt: Receipt): ArrayList<Section> {
        val sectionList: ArrayList<Section> = ArrayList()
        sectionList.add(Section("Invoice : ${receipt.iD}", CredentialTypes.RECEIPT))
        sectionList.add(Section("Customer details", "customer_details"))
        return sectionList
    }

    fun getShopAddress(receipt: Receipt): String {
        var address =
            "${receipt.accountingSupplierParty?.party?.postaladdress?.streetName}, ${receipt.accountingSupplierParty?.party?.postaladdress?.postalZone}\n${receipt.accountingSupplierParty?.party?.postaladdress?.cityName}, ${receipt.accountingSupplierParty?.party?.postaladdress?.country?.name}"

        if (receipt.accountingSupplierParty?.party?.partyIdentification?.iD != null)
            address =
                "$address\nCompany ID : ${receipt.accountingSupplierParty?.party?.partyIdentification?.iD ?: ""}"
        return address
    }
}