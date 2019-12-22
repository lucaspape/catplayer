package de.lucaspape.monstercat.util

import android.content.Context
import android.net.ConnectivityManager

fun wifiConnected(context: Context): Boolean? {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

    return wifi?.isConnected
}