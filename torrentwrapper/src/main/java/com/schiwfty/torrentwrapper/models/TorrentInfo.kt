package com.schiwfty.torrentwrapper.models

import com.google.gson.annotations.SerializedName
import java.util.*
import com.google.firebase.database.Exclude


/**
 * Created by arran on 30/04/2017.
 */
class TorrentInfo {

    var name: String = ""

    @SerializedName("announce")
    var announce: String = ""

    @SerializedName("piece_length")
    var pieceLength: Long = 0

    @SerializedName("pieces_blob")
    @get:Exclude var piecesBlob: ByteArray? = null

    @SerializedName("pieces")
    @get:Exclude var pieces: List<String> = emptyList()

    @SerializedName("single_file_torrent")
    var singleFileTorrent: Boolean = false

    @SerializedName("total_size")
    var totalSize: Long = 0

    @SerializedName("file_list")
    var fileList: List<TorrentFile> = emptyList()

    @SerializedName("comment")
    var comment: String = ""

    @SerializedName("created_by")
    var createdBy: String = ""

    @SerializedName("creation_date")
    var creationDate: Date? = null

    @SerializedName("announce_list")
    var announceList: List<String> = emptyList()

    @SerializedName("info_hash")
    var info_hash: String = ""
}