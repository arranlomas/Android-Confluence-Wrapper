package com.schiwfty.torrentwrapper.utils

import android.content.Context
import android.net.ConnectivityManager
import java.io.IOException
import java.net.NetworkInterface
import java.net.ServerSocket
import java.util.*

/**
 * Created by arran on 27/05/2017.
 */
enum class CONNECTIVITY_STATUS{
    WIFI,
    MOBILE,
    NOT_CONNECTED
}

fun Context.getConnectivityStatus(): CONNECTIVITY_STATUS {
    val cm = this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetwork = cm.activeNetworkInfo
    if (null != activeNetwork) {
        if (activeNetwork.type == ConnectivityManager.TYPE_WIFI) {
            return CONNECTIVITY_STATUS.WIFI
        }
        if (activeNetwork.type == ConnectivityManager.TYPE_MOBILE) {
            return CONNECTIVITY_STATUS.MOBILE
        }
    }
    return CONNECTIVITY_STATUS.NOT_CONNECTED
}

fun getAvailablePort(): Int {
    var s: ServerSocket? = null
    var streamPort = -1
    try {
        s = ServerSocket(0)
        streamPort = s.localPort
        return streamPort
    } catch (e: IOException) {
        e.printStackTrace()
    } finally {
        if (s != null)
            try {
                s.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

    }
    return streamPort
}