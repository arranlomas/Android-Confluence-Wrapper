package com.schiwfty.torrentwrapper.confluence

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.facebook.stetho.Stetho
import com.schiwfty.torrentwrapper.dagger.network.DaggerTorrentRepositoryComponent
import com.schiwfty.torrentwrapper.dagger.network.NetworkModule
import com.schiwfty.torrentwrapper.dagger.network.TorrentRepositoryComponent
import com.schiwfty.torrentwrapper.repositories.ITorrentRepository
import com.tbruyelle.rxpermissions.RxPermissions
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import io.realm.Realm
import rx.subjects.PublishSubject
import rx.subscriptions.CompositeSubscription
import java.io.File
import java.io.IOException
import java.net.ServerSocket


/**
 * Created by arran on 11/04/2017.
 */
object Confluence {
    lateinit var fullUrl: String
    lateinit var localhostIP: String
    lateinit var daemonPort: String

    lateinit var workingDir: File
    lateinit var torrentInfoStorage: File
    val startedSubject = PublishSubject.create<ConfluenceState>()!!
    val subscriptions = CompositeSubscription()
    lateinit var torrentRepository: ITorrentRepository
    lateinit var torrentRepositoryComponent: TorrentRepositoryComponent

    enum class ConfluenceState {
        STARTED,
        WAITING,
        STOPPED
    }

    fun install(context: Context, workingDirectoryPath: String) {
        Realm.init(context)
        Stetho.initialize(
                Stetho.newInitializerBuilder(context)
                        .enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
                        .enableWebKitInspector(RealmInspectorModulesProvider.builder(context).build())
                        .build())

        val arch = System.getProperty("os.arch")
        Log.v("architecture", arch)

        workingDir = File(workingDirectoryPath)
        torrentInfoStorage = File(com.schiwfty.torrentwrapper.confluence.Confluence.workingDir.absolutePath + java.io.File.separator + "torrents")
        torrentRepositoryComponent = DaggerTorrentRepositoryComponent.builder()
                .networkModule(NetworkModule())
                .build()
        torrentRepository = torrentRepositoryComponent.getTorrentRepository()
    }
    private fun getAvailablePort(): Int {
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

    fun start(activity: Activity, notificationResourceId: Int): PublishSubject<ConfluenceState> {
        return start(activity, notificationResourceId, {})
    }

    fun start(activity: Activity, notificationResourceId: Int, onPermissionDenied: () -> Unit): PublishSubject<ConfluenceState> {
        RxPermissions(activity)
                .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({
                    if (it != null && it) {
                        workingDir.mkdirs()
                        torrentInfoStorage.mkdirs()
                        val daemonIntent = Intent(activity, ConfluenceDaemonService::class.java)
                        daemonIntent.putExtra(ConfluenceDaemonService.ARG_NOTIFICATION_ICON_RES, notificationResourceId)
                        daemonIntent.addCategory(ConfluenceDaemonService.TAG)
                        activity.startService(daemonIntent)
                    } else {
                        subscriptions.unsubscribe()
                        startedSubject.onNext(ConfluenceState.STOPPED)
                        onPermissionDenied.invoke()
                    }
                }, {
                    it.printStackTrace()
                })
        listenForDaemon()
        return startedSubject
    }

    private fun listenForDaemon() {
        subscriptions.add(torrentRepository.getStatus()
                .retry()
                .subscribe({
                    subscriptions.unsubscribe()
                    startedSubject.onNext(ConfluenceState.STARTED)
                }, {
                    startedSubject.onNext(ConfluenceState.WAITING)
                    it.printStackTrace()
                }))

    }
}