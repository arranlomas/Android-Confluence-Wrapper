package com.schiwfty.torrentwrapper.utils

import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.realm.RealmString
import com.schiwfty.torrentwrapper.realm.RealmTorrentFile
import io.realm.RealmList

/**
 * Created by arran on 16/05/2017.
 */
internal fun RealmTorrentFile.mapToModel(): TorrentFile {
    val tf = TorrentFile()
    tf.fileLength = fileLength
    tf.fileDirs = fileDirs?.mapToList()
    tf.torrentHash = torrentHash
    tf.primaryKey = primaryKey
    tf.parentTorrentName = parentTorrentName
    tf.percComplete = percComplete
    return tf
}
internal fun RealmList<RealmString>.mapToList(): List<String>{
    val mutableList = mutableListOf<String>()
    forEach { mutableList.add(it.mapToModel()) }
    return mutableList.toList()
}
internal fun RealmString.mapToModel(): String{
    return value ?: ""
}