package com.schiwfty.torrentwrapper.confluence

import android.Manifest.permission
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat


/**
 * Created by arran on 17/04/2017.
 */
class ConfluenceDaemonService : Service() {
    private val NOTIFICATION_ID = 12345
    var STOP_STRING = "STOP"


    companion object {
        val ARG_NOTIFICATION_ICON_RES = "arg_notification_icon_resource_id"
        val TAG = "DAEMON_SERVICE_TAG"
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            confluencewrapper.Confluencewrapper.androidMain(Confluence.workingDir.absolutePath, true, ":${Confluence.daemonPort}")
        }.start()
        val notificationResourceID = intent?.getIntExtra(ARG_NOTIFICATION_ICON_RES, -1) ?: -1
        val permissionCheck = ContextCompat.checkSelfPermission(this,
                permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) throw IllegalStateException("Cannot start confluence without have write external storage permissions")

        var action: String? = null
        if (intent != null) {
            action = intent.action
        }
        if (intent != null && action != null && action == STOP_STRING) {
            stopForeground(true)
        } else if (intent != null) {
            val exitIntent = Intent(this, ConfluenceDaemonService::class.java)
            exitIntent.action = STOP_STRING

            val pendingExit = PendingIntent.getService(this, 0, exitIntent, 0)

            val builder = NotificationCompat.Builder(this)
                    .setOngoing(true)
                    .setContentText("Daemon running")

            if (notificationResourceID != -1) {
                builder.setSmallIcon(notificationResourceID)
                        .addAction(notificationResourceID, STOP_STRING, pendingExit)

            }
//            //TODO add this back in so clicking a notification opens an activity but tke the activity as an argument
//            val targetIntent = Intent(this, MainActivity::class.java)
//            val pIntent = PendingIntent.getActivity(this, 0, targetIntent, 0)
//                    builder.setContentIntent(pIntent)

            startForeground(NOTIFICATION_ID, builder.build())
        }

        return super.onStartCommand(intent, flags, startId)
    }

}