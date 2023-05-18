package io.igrant.data_wallet.models.receipt

import com.google.gson.annotations.SerializedName


data class Receipt(

    @SerializedName("@context") var context: String? = null,
    @SerializedName("@type") var type: String? = null,
    @SerializedName("customizationID") var customizationID: String? = null,
    @SerializedName("profileID") var profileID: String? = null,
    @SerializedName("iD") var iD: String? = null,
    @SerializedName("issueDate") var issueDate: String? = null,
    @SerializedName("invoiceTypeCode") var invoiceTypeCode: String? = null,
    @SerializedName("documentCurrencyCode") var documentCurrencyCode: String? = null,
    @SerializedName("buyerReference") var buyerReference: String? = null,
    @SerializedName("accountingSupplierParty") var accountingSupplierParty: AccountingSupplierParty? = AccountingSupplierParty(),
    @SerializedName("accountingCustomerParty") var accountingCustomerParty: AccountingCustomerParty? = AccountingCustomerParty(),
    @SerializedName("paymentMeans") var paymentMeans: PaymentMeans? = PaymentMeans(),
    @SerializedName("paymentTerms") var paymentTerms: PaymentTerms? = PaymentTerms(),
    @SerializedName("taxTotal") var taxTotal: TaxTotal? = TaxTotal(),
    @SerializedName("legalMonetaryTotal") var legalMonetaryTotal: LegalMonetaryTotal? = LegalMonetaryTotal(),
    @SerializedName("invoiceLine") var invoiceLine: ArrayList<InvoiceLine> = arrayListOf()

)