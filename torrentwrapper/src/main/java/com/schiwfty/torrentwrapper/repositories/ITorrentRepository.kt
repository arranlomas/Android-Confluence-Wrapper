package com.schiwfty.torrentwrapper.repositories

import android.app.Activity
import android.content.Context
import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.models.TorrentInfo
import com.schiwfty.torrentwrapper.models.ConfluenceInfo
import com.schiwfty.torrentwrapper.models.FileStatePiece
import okhttp3.ResponseBody
import rx.Observable
import java.io.File
import rx.subjects.PublishSubject

/**
 * Created by arran on 29/04/2017.
 */
interface ITorrentRepository {
    val torrentFileProgressSource: PublishSubject<List<TorrentFile>>
    val torrentFileDeleteListener: PublishSubject<TorrentFile>
    val torrentInfoDeleteListener: PublishSubject<TorrentInfo>

    //API
    fun getStatus(): Observable<String>

    fun postTorrentFile(hash: String, file: File): Observable<ResponseBody>

    //returns the file state
    fun getFileState(torrentFile: TorrentFile): Observable<Pair<TorrentFile, List<FileStatePiece>>>

    fun downloadTorrentInfo(hash: String): Observable<TorrentInfo?>

    fun getTorrentInfo(hash: String): Observable<TorrentInfo?>

    fun startFileDownloading(torrentFile: TorrentFile, context: Context, wifiOnly: Boolean)

    fun addFileToClient(activity: Activity, file: File): PublishSubject<TorrentInfo>

    //PERSISTENCE
    //adds a torrent file to the realm database
    fun addTorrentFileToPersistence(torrentFile: TorrentFile)

    //returns all the torrent files that are in the confluence/torrent folder that was created by go-confluence
    fun getAllTorrentsFromStorage(): Observable<List<TorrentInfo>>

    //returns all torrent files in persistence
    fun getDownloadingFilesFromPersistence(): Observable<List<TorrentFile>>

    //deletes the torrent file along with any data downloaded that is associated with the torrent
    fun deleteTorrentData(torrentInfo: TorrentInfo): Boolean

    //deletes a downloading torrent file from realm
    fun deleteTorrentFileFromPersistence(torrentFile: TorrentFile)

    //deletes the data relating to a torrent file
    fun deleteTorrentFileData(torrentFile: TorrentFile): Boolean

    //deletes a torrent file from storage
    fun deleteTorrentInfoFromStorage(torrentInfo: TorrentInfo): Boolean

    //gets a downloading file from persistence
    fun getTorrentFileFromPersistence(hash: String, path: String): TorrentFile?

    //clears all files from persistence
    fun clearPersistence()
}