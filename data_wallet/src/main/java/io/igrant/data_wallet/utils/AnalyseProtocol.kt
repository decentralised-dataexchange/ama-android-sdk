package io.igrant.data_wallet.utils

import io.igrant.data_wallet.models.connection.Protocol

object AnalyseProtocol {

    fun checkConnectionType(protocols: ArrayList<Protocol>?): String {
        if (protocols != null)
            for (p in protocols) {
                if (p.pid.contains(
                        "${DidCommPrefixUtils.getType(DidCommPrefixUtils.IGRANT_OPERATOR)}/igrantio-operator",
                        ignoreCase = true
                    )
                ) {
                    return ConnectionTypes.IGRANT_ENABLED_CONNECTION
                }

                if (p.pid.contains(
                        "${DidCommPrefixUtils.getType(DidCommPrefixUtils.PREFIX_1)}/data-controller",
                        ignoreCase = true
                    )
                ) {
                    return ConnectionTypes.V2_CONNECTION
                }
            }

        return ""
    }
}