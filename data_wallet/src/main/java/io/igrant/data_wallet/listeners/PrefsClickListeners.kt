package io.igrant.data_wallet.listeners

interface PrefsClickListeners {
    fun onAgreementLevelClick(instanceId: String, state: String)
    fun onCompanyLevelClick(ddaInstanceId: String, instanceId: String, state: String)
    fun onInfoClick(instanceId: String)
}