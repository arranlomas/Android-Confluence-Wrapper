package com.schiwfty.torrentwrapper.models

import com.google.gson.annotations.SerializedName
import io.realm.annotations.PrimaryKey

/**
 * Created by arran on 30/04/2017.
 */
data class TorrentFile(
        @SerializedName("file_length")
        val fileLength: Long? = null,

        @SerializedName("file_dirs")
        val fileDirs: List<String>? = null,

        @SerializedName("torrent_hash")
        val torrentHash: String,

        @PrimaryKey
        @SerializedName("primary_key")
        val primaryKey: String
) {
    @SerializedName("perc_completed")
    var percComplete: Int = 0

    @SerializedName("parent_torrent_name")
    var parentTorrentName: String = ""
}