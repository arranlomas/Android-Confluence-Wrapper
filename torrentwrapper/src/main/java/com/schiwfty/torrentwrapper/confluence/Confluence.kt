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
import com.tbruyelle.rxpermissions2.RxPermissions
import com.uphyca.stetho_realm.RealmInspectorModulesProvider
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.subjects.PublishSubject
import io.realm.Realm
import io.realm.RealmConfiguration
import java.io.File

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
    private val subscriptions = CompositeDisposable()
    lateinit var torrentRepository: ITorrentRepository
    lateinit var torrentRepositoryComponent: TorrentRepositoryComponent

    val stopServiceEvent: PublishSubject<Boolean> = PublishSubject.create()

    enum class ConfluenceState {
        STARTED,
        WAITING,
        STOPPED
    }

    fun install(context: Context, workingDirectoryPath: String, daemonPort: Int = 8080) {
        this.workingDir = File(workingDirectoryPath)
        this.daemonPort = daemonPort.toString()

        Realm.init(context)
        Realm.setDefaultConfiguration(RealmConfiguration.Builder()
            .deleteRealmIfMigrationNeeded()
            .build())

        Stetho.initialize(
            Stetho.newInitializerBuilder(context)
                .enableDumpapp(Stetho.defaultDumperPluginsProvider(context))
                .enableWebKitInspector(RealmInspectorModulesProvider.builder(context).build())
                .build())

        val arch = System.getProperty("os.arch")
        Log.v("architecture", arch)

        torrentInfoStorage = File(Confluence.workingDir.absolutePath + File.separator + "torrents")
        torrentRepositoryComponent = DaggerTorrentRepositoryComponent.builder()
            .networkModule(NetworkModule())
            .build()
        torrentRepository = torrentRepositoryComponent.getTorrentRepository()
    }

    fun start(activity: Activity,
        notificationResourceId: Int,
        channelId: String? = null,
        channelName: String? = null,
        seed: Boolean = false,
        showStopAction: Boolean = false,
        targetIntent: Intent? = null,
        onPermissionDenied: (() -> Unit)? = null): PublishSubject<ConfluenceState> {

        RxPermissions(activity)
            .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .flatMap {
                if (it) {
                    workingDir.mkdirs()
                    torrentInfoStorage.mkdirs()
                } else {
                    onPermissionDenied?.invoke()
                }
                torrentRepository.isConnected()

            }
            .subscribe({ connected ->
                if (!connected) {
                    ConfluenceDaemonService.start(activity, notificationResourceId, channelId, channelName, seed, showStopAction, targetIntent)
                    listenForDaemon()
                } else {
                    subscriptions.dispose()
                    startedSubject.onNext(ConfluenceState.STARTED)
                }
            }, {
                it.printStackTrace()
            })
        return startedSubject
    }

    fun stop() {
        ConfluenceDaemonService.stopService()
    }

    private fun listenForDaemon() {
        subscriptions.add(torrentRepository.getStatus()
            .retry()
            .subscribe({
                subscriptions.dispose()
                startedSubject.onNext(ConfluenceState.STARTED)
            }, {
                startedSubject.onNext(ConfluenceState.WAITING)
                it.printStackTrace()
            }))
    }
}