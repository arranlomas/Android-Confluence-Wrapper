package com.schiwfty.torrentwrapper.confluence

import android.Manifest.permission
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.schiwfty.torrentwrapper.confluence.Confluence.stopServiceEvent


/**
 * Created by arran on 17/04/2017.
 */
internal class ConfluenceDaemonService : Service() {
    private val NOTIFICATION_ID = 12345
    var STOP_STRING = "STOP"

    companion object {
        val ARG_SEED = "arg_seed"
        val ARG_SHOW_STOP = "arg_show_stop_action"
        val ARG_NOTIFICATION_ICON_RES = "arg_notification_icon_resource_id"
        val ARG_NOTIFICATION_CHAMNEL_ID = "arg_channel_id"
        val ARG_NOTIFICATION_CHANNEL_NAME = "arg_channel_name"
        val TAG = "DAEMON_SERVICE_TAG"
        var targetIntent: Intent? = null

        fun stopService() {
            stopServiceEvent.onNext(true)
        }

        fun start(context: Context, notificationRes: Int, notificationChannelId: String? = null, notificationChannelName: String? = null, seed: Boolean = false, showStopAction: Boolean = false, targetIntent: Intent? = null) {
            val daemonIntent = Intent(context, ConfluenceDaemonService::class.java)
            ConfluenceDaemonService.Companion.targetIntent = targetIntent
            daemonIntent.putExtra(ARG_NOTIFICATION_ICON_RES, notificationRes)
            daemonIntent.putExtra(ARG_SEED, seed)
            daemonIntent.putExtra(ARG_SHOW_STOP, showStopAction)
            daemonIntent.putExtra(ARG_NOTIFICATION_CHAMNEL_ID, notificationChannelId)
            daemonIntent.putExtra(ARG_NOTIFICATION_CHANNEL_NAME, notificationChannelName)
            daemonIntent.addCategory(ConfluenceDaemonService.TAG)
            if (Build.VERSION.SDK_INT < 26) context.startService(daemonIntent) else context.startForegroundService(daemonIntent)
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        stopServiceEvent.subscribe({ stopService() }, { /*swallow error*/ })
        if (intent != null && intent.action != null && intent.action == STOP_STRING) {
            stopServiceEvent.onNext(true)
        } else if (intent != null) {
            val seed = intent.getBooleanExtra(ARG_SEED, false)
            val showStop = intent.getBooleanExtra(ARG_SHOW_STOP, false)
            val notificationResourceID = intent.getIntExtra(ConfluenceDaemonService.ARG_NOTIFICATION_ICON_RES, -1)
            val channelId = intent.getStringExtra(ARG_NOTIFICATION_CHAMNEL_ID) ?: "confluence_daemon_service_id"
            val channelName = intent.getStringExtra(ARG_NOTIFICATION_CHANNEL_NAME) ?: "Torrent Daemon Service"

            Thread {
                confluencewrapper.Confluencewrapper.androidMain(Confluence.workingDir.absolutePath, seed, ":${Confluence.daemonPort}")
            }.start()

            createChannel(channelId, channelName)
            startForeground(NOTIFICATION_ID, buildNotification(this, channelId, notificationResourceID, showStop, "Daemon running"))
        }

        return super.onStartCommand(intent, flags, startId)
    }

    private fun stopService() {
        stopForeground(true)
    }

    private fun createChannel(channelId: String, channelName: String, importance: Int? = null, visibility: Int? = null) {
        if (Build.VERSION.SDK_INT < 26) return
        val androidChannel = NotificationChannel(channelId, channelName, importance ?: NotificationManager.IMPORTANCE_DEFAULT)
        androidChannel.enableLights(true)
        androidChannel.enableVibration(true)
        androidChannel.lightColor = Color.GREEN
        androidChannel.lockscreenVisibility = visibility ?: Notification.VISIBILITY_PUBLIC
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(androidChannel)
    }

    private fun buildNotification(context: Context, channelId: String, notificationResourceID: Int, showStop: Boolean, contentText: String): Notification {
        val permissionCheck = ContextCompat.checkSelfPermission(context, permission.WRITE_EXTERNAL_STORAGE)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) throw IllegalStateException("Cannot start confluence without have write external storage permissions")

        val builder = NotificationCompat.Builder(context, channelId)

        builder.setOngoing(true)
        builder.setContentText(contentText)

        if (notificationResourceID != -1) {
            builder.setSmallIcon(notificationResourceID)
            if (showStop) {
                val exitIntent = Intent(context, ConfluenceDaemonService::class.java)
                exitIntent.action = STOP_STRING
                val pendingExit = PendingIntent.getService(this, 0, exitIntent, 0)
                builder.addAction(notificationResourceID, STOP_STRING, pendingExit)
            }
        }

        ConfluenceDaemonService.targetIntent?.let {
            val pIntent = PendingIntent.getActivity(this, 0, it, 0)
            builder.setContentIntent(pIntent)
        }
        return builder.build()
    }
}