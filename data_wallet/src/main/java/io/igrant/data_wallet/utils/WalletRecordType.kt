package io.igrant.data_wallet.utils

class WalletRecordType {
    companion object{
        const val MEDIATOR_CONNECTION ="mediator_connection"
        const val MEDIATOR_CONNECTION_INVITATION ="mediator_connection_invitation"

        const val REGISTRY_CONNECTION = "registry_connection"
        const val REGISTRY_CONNECTION_INVITATION ="registry_connection_invitation"
        const val REGISTRY_ANCHOR = "registry_anchor"

        const val CONNECTION ="connection"
        const val CONNECTION_INVITATION ="connection_invitation"

        const val MEDIATOR_DID_DOC ="mediator_did_doc"
        const val MEDIATOR_DID_KEY ="mediator_did_key"

        const val REGISTRY_DID_DOC ="registry_did_doc"
        const val REGISTRY_DID_KEY ="registry_did_key"

        const val DID_DOC ="did_doc"
        const val DID_KEY ="did_key"

        const val CREDENTIAL_EXCHANGE_V10 = "credential_exchange_v10"

        const val MESSAGE_RECORDS = "inbox_messages"
        const val TEMP_MESSAGE_RECORDS = "temp_inbox_messages"

        const val DATA_HISTORY = "data_history"

        const val PRESENTATION_EXCHANGE_V10 = "presentation_exchange_v10"

        const val WALLET ="wallet"

        const val SELF_ATTESTED_CREDENTIALS ="self_attested_credentials"

        //Main Certificate types
        const val CERTIFICATE_TYPE_PROFILE = "profile"
        const val CERTIFICATE_TYPE_ID_CARDS = "id_cards"
        const val CERTIFICATE_TYPE_SELF_ATTESTED = "self_attested"
        const val CERTIFICATE_TYPE_CREDENTIALS = "attested_credentials"
        const val CERTIFICATE_TYPE_EBSI_CREDENTIAL = "ebsi_credentials"

        //self Attested credentials
        const val TYPE_PASSPORT = "0"
        const val TYPE_INDIAN_VACCINATION_CERTIFICATE = "1"
        const val TYPE_EU_VACCINATION_CERTIFICATE = "2"
        const val TYPE_UK_VACCINATION_CERTIFICATE = "3"
        const val TYPE_AADHAR_CARD = "4"
        const val TYPE_SINGAPORE_PASSPORT = "5"
        const val TYPE_MY_VACCINATION_CERTIFICATE = "6"
        const val TYPE_EU_TEST_RESULT_CERTIFICATE = "7"
        const val TYPE_PHILIPPINES_VACCINATION_CERTIFICATE = "8"
        const val TYPE_QRCODE = "9"
        const val TYPE_PROFILE = "10"

        const val TYPE_PASS_BOARDING_PASS = "pass_boarding_pass"
        //cardTypes
//        const val TYPE_INDIAN_COVID_CERTIFICATE = "indian_covid_certificate"
//        const val TYPE_EU_COVID_CERTIFICATE = "eu_covid_certificate"
//        const val TYPE_UK_COVID_CERTIFICATE = "uk_covid_certificate"
//        const val TYPE_AADHAR_CARD_CERTIFICATE = "aadhar_card_certificate"


        //ebsi
        const val EBSI_CONNECTION = "ebsi_connection"
        const val EBSI_NATURAL_PERSON_CONNECTION = "ebsi_connection_natural_person"
        const val TYPE_EBSI_VERIFIABLE_ID = "ebsi_verifiable_id"
        const val TYPE_EBSI_DIPLOMA = "ebsi_diploma"
        const val TYPE_EBSI_STUDENT_ID = "ebsi_student_id"
    }
}