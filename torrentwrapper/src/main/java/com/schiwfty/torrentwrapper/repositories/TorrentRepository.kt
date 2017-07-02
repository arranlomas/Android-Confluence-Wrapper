package com.schiwfty.torrentwrapper.repositories

import android.app.DownloadManager
import android.content.Context
import android.content.Context.DOWNLOAD_SERVICE
import android.net.Uri
import android.util.Log
import com.schiwfty.torrentwrapper.confluence.Confluence
import com.schiwfty.torrentwrapper.confluence.Confluence.torrentInfoStorage
import com.schiwfty.torrentwrapper.models.ConfluenceInfo
import com.schiwfty.torrentwrapper.models.FileStatePiece
import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.models.TorrentInfo
import com.schiwfty.torrentwrapper.persistence.ITorrentPersistence
import com.schiwfty.torrentwrapper.retrofit.ConfluenceApi
import com.schiwfty.torrentwrapper.utils.*
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
    override val torrentFileProgressSource: PublishSubject<Boolean> = PublishSubject.create<Boolean>()

    private var statusUpdateRunning = true

    private val statusThread = Thread({
        while (statusUpdateRunning) {
            val files = torrentPersistence.getDownloadFiles()
            var percentagesCompleted = 0
            files.forEach {
                //TODO - improve to just add a list of files that were updated to the publish subject
                getFileState(it)
                        .subscribe({
                            val (torrentFile, pieces) = it
                            var totalFileSize: Long = 0
                            var totalCompletedSize: Long = 0
                            pieces.forEach {
                                totalFileSize += it.bytes
                                if (it.complete) {
                                    totalCompletedSize += it.bytes
                                }
                            }
                            val percCompleted = (totalCompletedSize.toDouble() / totalFileSize.toDouble()) * 100.0
                            torrentFile.percComplete = Math.round(percCompleted).toInt()
                            torrentPersistence.saveTorrentFile(torrentFile)
                            percentagesCompleted++
                            if (percentagesCompleted == files.size) torrentFileProgressSource.onNext(true)
                            Log.v("HASH", "${torrentFile.torrentHash} PATH: ${torrentFile.getFullPath()} PERC: $percCompleted")
                            Log.v("-----------", "-----------------------")
                        }, {
                            it.printStackTrace()
                        })
            }
            Thread.sleep(2000)
        }
    })

    init {
        statusThread.start()
        torrentPersistence.torrentFileDeleted =  { torrentFile -> torrentFileDeleteListener.onNext(torrentFile) }
    }

    override fun getStatus(): Observable<ConfluenceInfo> {
        return confluenceApi.getStatus
                .composeIo()
                .map { it }
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
            file.copyToTorrentDirectory()
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

    override fun addTorrentToClient(file: File) {

    }
}