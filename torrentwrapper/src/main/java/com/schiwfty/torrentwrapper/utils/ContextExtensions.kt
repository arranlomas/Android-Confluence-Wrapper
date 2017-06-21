package com.schiwfty.torrentwrapper.utils

import android.content.Context
import android.content.Intent
import com.schiwfty.torrentwrapper.confluence.ConfluenceDaemonService

/**
 * Created by arran on 26/05/2017.
 */
fun Context.startConfluenceDaemon(){
    val daemonIntent = Intent(this, ConfluenceDaemonService::class.java)
    daemonIntent.addCategory(ConfluenceDaemonService.Companion.TAG)
    this.startService(daemonIntent)
}