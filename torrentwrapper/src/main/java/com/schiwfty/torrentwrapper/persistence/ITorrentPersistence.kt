package com.schiwfty.torrentwrapper.persistence

import com.schiwfty.torrentwrapper.models.TorrentFile

/**
 * Created by arran on 19/05/2017.
 */
internal interface ITorrentPersistence {
    var torrentFileDeleted: (TorrentFile) -> Unit

    fun getDownloadFiles(): List<TorrentFile>

    fun getDownloadingFile(hash: String, path: String): TorrentFile?

    fun removeTorrentDownloadFile(torrentFile: TorrentFile)

    fun saveTorrentFile(torrentFile: TorrentFile)

    fun clear()
}