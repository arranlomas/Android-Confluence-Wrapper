package com.schiwfty.torrentwrapper.repositories

import android.content.Context
import com.schiwfty.torrentwrapper.models.FileStatePiece
import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.models.TorrentInfo
import com.schiwfty.torrentwrapper.utils.ParseTorrentResult
import com.schiwfty.torrentwrapper.utils.defaultAnnounceList
import okhttp3.ResponseBody
import rx.Observable
import rx.subjects.PublishSubject
import java.io.File

/**
 * Created by arran on 29/04/2017.
 */
interface ITorrentRepository {
    val torrentFileProgressSource: PublishSubject<List<TorrentFile>>
    val torrentFileDeleteListener: PublishSubject<TorrentFile>
    val torrentInfoDeleteListener: PublishSubject<TorrentInfo>

    //API
    fun getStatus(): Observable<String>

    fun isConnected(): Observable<Boolean>

    fun postTorrentFile(hash: String, file: File): Observable<ResponseBody>

    fun verifyData(hash: String): Observable<Boolean>

    //returns the file state
    fun getFileState(torrentFile: TorrentFile): Observable<Pair<TorrentFile, List<FileStatePiece>>>

    fun downloadTorrentInfo(hash: String, deleteErroneousTorrents: Boolean = false): Observable<ParseTorrentResult>

    fun startFileDownloading(torrentFile: TorrentFile, context: Context, wifiOnly: Boolean)

    fun addFileToClient(file: File, announceList: Array<String> = defaultAnnounceList): PublishSubject<TorrentInfo>

    //PERSISTENCE
    //adds a torrent file to the realm database
    fun addTorrentFileToPersistence(torrentFile: TorrentFile)

    //returns all the torrent files that are in the confluence/torrent folder that was created by go-confluence
    fun getAllTorrentsFromStorage(deleteErroneousTorrents: Boolean = false): Observable<List<ParseTorrentResult>>

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