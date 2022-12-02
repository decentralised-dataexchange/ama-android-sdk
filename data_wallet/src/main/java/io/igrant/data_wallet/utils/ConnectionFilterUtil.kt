package io.igrant.data_wallet.utils

import android.content.Context
import io.igrant.data_wallet.R
import io.igrant.data_wallet.models.ConnectionFilter

object ConnectionFilterUtil {

    //connection
    const val CONNECTION_FILTER_ALL: Int = 0
    const val CONNECTION_FILTER_ORGANIZATION: Int = 1
    const val CONNECTION_FILTER_PEOPLE: Int = 2
    const val CONNECTION_FILTER_DEVICES: Int = 3

    //History
    const val HISTORY_FILTER_ALL: Int = 0
    const val HISTORY_FILTER_ACTIVE: Int = 1
    const val HISTORY_FILTER_PASSIVE: Int = 2

    //History
    const val THIRD_PARTY_FILTER_ALL: Int = 0

    fun getFilterList(
        context: Context,
        filterType: Int?,
        extraList: ArrayList<String>? = ArrayList()
    ): ArrayList<ConnectionFilter> {
        return when (filterType) {
            FilterType.CONNECTION -> {
                getConnectionFilterList(context)
            }
            FilterType.MY_SHARED_HISTORY -> {
                getHistoryFilterList(context)
            }
            FilterType.THIRD_PARTY_DATA_SHARING -> {
                getThirdPartyFilterList(context, extraList)
            }
            else -> {
                ArrayList()
            }
        }
    }

    fun getThirdPartyFilterList(
        context: Context,
        extraList: ArrayList<String>?
    ): ArrayList<ConnectionFilter> {
        val list: ArrayList<ConnectionFilter> = ArrayList()

        var connectionFilter = ConnectionFilter(
            id = THIRD_PARTY_FILTER_ALL,
            isEnabled = true,
            isSelected = true,
            name = context.resources.getString(R.string.third_party_data_sharing_all_sectors),
            logo = null
        )
        list.add(connectionFilter)

        extraList?.forEachIndexed { index, s ->
            connectionFilter = ConnectionFilter(
                id = index + 1,
                isEnabled = true,
                isSelected = false,
                name = s,
                logo = null
            )
            list.add(connectionFilter)
        }
        return list
    }

    fun getConnectionFilterList(context: Context): ArrayList<ConnectionFilter> {
        val list: ArrayList<ConnectionFilter> = ArrayList()

        var connectionFilter = ConnectionFilter(
            id = CONNECTION_FILTER_ALL,
            isEnabled = true,
            isSelected = true,
            name = context.resources.getString(R.string.welcome_connection_all),
            logo = null
        )
        list.add(connectionFilter)

        connectionFilter = ConnectionFilter(
            id = CONNECTION_FILTER_ORGANIZATION,
            isEnabled = true,
            isSelected = false,
            name = context.resources.getString(R.string.connection_organisations),
            logo = R.drawable.ic_office_building
        )
        list.add(connectionFilter)

        connectionFilter = ConnectionFilter(
            id = CONNECTION_FILTER_PEOPLE,
            isEnabled = false,
            isSelected = false,
            name = context.resources.getString(R.string.welcome_people),
            logo = R.drawable.ic_people
        )
        list.add(connectionFilter)

        connectionFilter = ConnectionFilter(
            id = CONNECTION_FILTER_DEVICES,
            isEnabled = false,
            isSelected = false,
            name = context.resources.getString(R.string.welcome_devices),
            logo = R.drawable.ic_devices
        )
        list.add(connectionFilter)


        return list
    }


    fun getHistoryFilterList(context: Context): ArrayList<ConnectionFilter> {
        val list: ArrayList<ConnectionFilter> = ArrayList()

        var connectionFilter = ConnectionFilter(
            id = HISTORY_FILTER_ALL,
            isEnabled = true,
            isSelected = true,
            name = context.resources.getString(R.string.my_shared_history_all_history),
            logo = null
        )
        list.add(connectionFilter)

        connectionFilter = ConnectionFilter(
            id = HISTORY_FILTER_ACTIVE,
            isEnabled = true,
            isSelected = false,
            name =
            context.resources.getString(R.string.my_shared_history_active_data_sharing),
            logo = null
        )
        list.add(connectionFilter)

        connectionFilter = ConnectionFilter(
            id = HISTORY_FILTER_PASSIVE,
            isEnabled = true,
            isSelected = false,
            name =
            context.resources.getString(R.string.my_shared_history_passive_data_sharing),
            logo = null
        )
        list.add(connectionFilter)

        return list
    }
}