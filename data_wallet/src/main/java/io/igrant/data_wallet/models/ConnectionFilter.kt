package io.igrant.data_wallet.models

data class ConnectionFilter (
    var id: Int? = null,
    var name: String? = null,
    var logo: Int? = null,
    var isSelected: Boolean = false,
    var isEnabled: Boolean = true
)