package io.igrant.data_wallet.utils

class MessageTypes {
    companion object{
        const val OFFER_REQUEST = "offer-credential"
        const val SHARE_REQUEST = "request-presentation"

        const val TYPE_CONNECTION_RESPONSE = "response"
        const val TYPE_PING_RESPONSE = "ping_response"
        const val TYPE_ISSUE_CREDENTIAL = "issue-credential"
        const val TYPE_REQUEST_PRESENTATION_ACK = "ack"
        const val TYPE_DATA_CERTIFICATES = "list-data-certificate-types-response"
        const val TYPE_CONNECTION_INFO_RESPONSE = "organization-info-response"
        const val TYPE_EBSI_CREDENTIAL = "ebsi-credential"
        const val TYPE_RECIEPT = "receipt"
        const val TYPE_NOTIFICATION = "notification"
    }
}