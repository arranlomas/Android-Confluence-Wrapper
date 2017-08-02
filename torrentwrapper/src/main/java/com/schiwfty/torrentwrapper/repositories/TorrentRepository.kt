package com.schiwfty.torrentwrapper.repositories

import android.Manifest
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.util.Log
import com.schiwfty.torrentwrapper.confluence.Confluence
import com.schiwfty.torrentwrapper.confluence.Confluence.torrentInfoStorage
import com.schiwfty.torrentwrapper.models.FileStatePiece
import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.models.TorrentInfo
import com.schiwfty.torrentwrapper.persistence.ITorrentPersistence
import com.schiwfty.torrentwrapper.retrofit.ConfluenceApi
import com.schiwfty.torrentwrapper.utils.*
import com.tbruyelle.rxpermissions.RxPermissions
import okhttp3.ResponseBody
import org.apache.commons.io.IOUtils
import rx.Observable
import rx.subjects.PublishSubject
import java.io.File
import java.io.FileInputStream


/**
 * Created by arran on 29/04/2017.
 */
internal class TorrentRepository(val confluenceApi: ConfluenceApi, val torrentPersistence: ITorrentPersistence) : ITorrentRepository {

    override val torrentInfoDeleteListener: PublishSubject<TorrentInfo> = PublishSubject.create<TorrentInfo>()
    override val torrentFileDeleteListener: PublishSubject<TorrentFile> = PublishSubject.create<TorrentFile>()
    override val torrentFileProgressSource: PublishSubject<List<TorrentFile>> = PublishSubject.create<List<TorrentFile>>()

    private var statusUpdateRunning = true

    private val statusThread = Thread({
        while (statusUpdateRunning) {
            val fileStateObservablesList = mutableListOf<Observable<Pair<TorrentFile, List<FileStatePiece>>>>()
            torrentPersistence.getDownloadFiles().forEach {
                fileStateObservablesList.add(getFileState(it))
            }
            Observable.zip(fileStateObservablesList, { result ->
                result.map { it as Pair<TorrentFile, List<FileStatePiece>> }
                        .toMutableList()
            })
                    .flatMapIterable { it }
                    .map {
                        val (torrentFile, pieces) = it
                        torrentFile.updatePercentage(pieces)
                        torrentPersistence.saveTorrentFile(torrentFile)
                        torrentFile
                    }
                    .toList()
                    .subscribe({
                        torrentFileProgressSource.onNext(it)
                        Log.v("filestateobservableList", "returned: ${it.size}")
                    }, {
                        it.printStackTrace()
                    })
            Thread.sleep(1000)
        }
    })

    init {
        statusThread.start()
        torrentPersistence.torrentFileDeleted = { torrentFile -> torrentFileDeleteListener.onNext(torrentFile) }
    }

    override fun getStatus(): Observable<String> {
        return confluenceApi.getStatus
                .map { it.string() }
                .composeIo()
    }

    override fun downloadTorrentInfo(hash: String): Observable<TorrentInfo?> {
        return confluenceApi.getInfo(hash)
                .composeIo()
                .map {
                    val file: File = File(torrentInfoStorage, "$hash.torrent")
                    val torrentInfo = file.getAsTorrentObject()
                    torrentInfo
                }
    }

    override fun getTorrentInfo(hash: String): Observable<TorrentInfo?> {
        val file: File = File(torrentInfoStorage, "$hash.torrent")
        if (file.isValidTorrentFile()) {
            val torrentInfo = file.getAsTorrentObject()
            return Observable.just(torrentInfo)
        } else return downloadTorrentInfo(hash)
    }

    override fun getAllTorrentsFromStorage(): Observable<List<TorrentInfo>> {
        return Observable.just({
            val torrentList = mutableListOf<TorrentInfo>()
            torrentInfoStorage.walkTopDown().iterator().forEach {
                if (it.isValidTorrentFile()) {
                    val torrentInfo = it.getAsTorrentObject()
                    if (torrentInfo != null) torrentList.add(torrentInfo)
                }
            }
            torrentList.toList()
        }.invoke())
                .composeIo()
    }

    override fun postTorrentFile(hash: String, file: File): Observable<ResponseBody> {
        if (!file.isValidTorrentFile()) throw IllegalStateException("File is not a valid torrent file")
        return Observable.just({
            val inputStream = FileInputStream(file)
            val bytes = IOUtils.toByteArray(inputStream)
            bytes
        }.invoke())
                .flatMap {
                    confluenceApi.postTorrent(hash, it)
                }
                .composeIo()
    }

    override fun getFileState(torrentFile: TorrentFile): Observable<Pair<TorrentFile, List<FileStatePiece>>> {
        val torrentFileObs = Observable.just(torrentFile)
        return Observable.zip(confluenceApi.getFileState(torrentFile.torrentHash, torrentFile.getFullPath()), torrentFileObs, {
            list, torrentFile ->
            Pair(torrentFile, list)
        })
                .composeIo()
    }

    override fun getDownloadingFilesFromPersistence(): Observable<List<TorrentFile>> {
        return Observable.just(torrentPersistence.getDownloadFiles())
                .composeIo()
    }

    override fun deleteTorrentInfoFromStorage(torrentInfo: TorrentInfo): Boolean {
        val file = File(torrentInfoStorage.absolutePath, "${torrentInfo.info_hash}.torrent")
        val deleted = file.delete()
        if (deleted) torrentInfoDeleteListener.onNext(torrentInfo)
        return deleted
    }

    override fun startFileDownloading(torrentFile: TorrentFile, context: Context, wifiOnly: Boolean) {
        torrentPersistence.saveTorrentFile(torrentFile)
        val dm = context.getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(torrentFile.getDownloadableUrl())
        Log.v("requesting download", uri.toString())
        val request = DownloadManager.Request(uri)
        request.setTitle(torrentFile.getFullPath())
                .setDescription("part of torrent: ${torrentFile.parentTorrentName} with hash ${torrentFile.torrentHash}")
        dm.enqueue(request)
    }

    override fun deleteTorrentData(torrentInfo: TorrentInfo): Boolean {
        val file = File(Confluence.workingDir.absolutePath, torrentInfo.name)
        return file.deleteRecursively()
    }

    override fun addTorrentFileToPersistence(torrentFile: TorrentFile) {
        torrentPersistence.saveTorrentFile(torrentFile)
    }

    override fun deleteTorrentFileFromPersistence(torrentFile: TorrentFile) {
        torrentPersistence.removeTorrentDownloadFile(torrentFile)
    }

    override fun deleteTorrentFileData(torrentFile: TorrentFile): Boolean {
        val file = File(Confluence.workingDir.absolutePath, "${torrentFile.parentTorrentName}${File.separator}${torrentFile.getFullPath()}")
        return file.delete()
    }

    override fun getTorrentFileFromPersistence(hash: String, path: String): TorrentFile? {
        return torrentPersistence.getDownloadingFile(hash, path)
    }

    override fun addFileToClient(activity: Activity, file: File): PublishSubject<TorrentInfo> {
        val confluenceFile = File(Confluence.workingDir.absolutePath, file.name)
        if (file.exists() && !confluenceFile.exists()) file.copyTo(confluenceFile)
        Log.v("copying file", "file: ${file.name} working directory: ${Confluence.workingDir.absolutePath}")
        val resultSubject = PublishSubject.create<TorrentInfo>()
        RxPermissions(activity)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .subscribe({
                    if (it) {
                        val (hash, torrentFile) = file.createTorrent(File(Confluence.workingDir, "${file.nameWithoutExtension}.torrent"))
                        val inputStream = FileInputStream(torrentFile)
                        val bytes = IOUtils.toByteArray(inputStream)
//                        val torrentFile = File(Confluence.workingDir, file.name)

                        Log.v("hash", hash)
                        val torrentObject = torrentFile.getAsTorrentObject()
                        confluenceApi.postTorrent(hash, bytes)
                                .composeIo()
                                .map { resultSubject.onNext(torrentObject) }
                                .subscribe({
                                    resultSubject.onCompleted()
                                }, {
                                    resultSubject.onError(it)
                                })
                    } else {
                        resultSubject.onError(IllegalStateException("Permissions required to add a file"))
                        Log.v("Permission Denied", "Permission required to add file to the client")
                    }
                }, {
                    resultSubject.onError(it)
                    it.printStackTrace()
                })
        return resultSubject
    }

    override fun clearPersistence() {
        torrentPersistence.clear()
    }
}