package com.schiwfty.torrentwrapper.models

import android.util.Log
import com.google.gson.annotations.SerializedName
import io.realm.annotations.PrimaryKey

/**
 * Created by arran on 30/04/2017.
 */
class TorrentFile {

    @SerializedName("file_length")
    var fileLength: Long? = null

    @SerializedName("file_dirs")
    var fileDirs: List<String>? = null

    @SerializedName("torrent_hash")
    var torrentHash: String = ""

    @PrimaryKey
    @SerializedName("primary_key")
    var primaryKey: String = ""

    @SerializedName("perc_completed")
    var percComplete: Int = 0

    @SerializedName("parent_torrent_name")
    var parentTorrentName: String = ""

    fun updatePercentage(pieces: List<FileStatePiece>){
        var totalFileSize: Long = 0
        var totalCompletedSize: Long = 0
        pieces.forEach {
            totalFileSize += it.bytes
            if (it.complete) {
                totalCompletedSize += it.bytes
            }
        }
        val percCompleted = (totalCompletedSize.toDouble() / totalFileSize.toDouble()) * 100.0
        percComplete = Math.round(percCompleted).toInt()
        Log.v("$primaryKey perc", "UPDATED: $percCompleted%")
    }
}