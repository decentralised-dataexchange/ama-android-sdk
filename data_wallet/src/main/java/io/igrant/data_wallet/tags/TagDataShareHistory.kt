package io.igrant.data_wallet.tags

data class TagDataShareHistory(
    var type: String? = null,
    var id: String? = null,
    var orgId: String? = null,
    var industryScope: String? = null,
    // values - true/false
    var isActive: String? = null,
    var connectionDid: String? = null,
    // to map with the instance id
    var contextId: String? = null,
    //Passive == true or Active == false
    var thirdParty: String? = null
)
