package com.schiwfty.torrentwrapper.utils

import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.realm.RealmString
import com.schiwfty.torrentwrapper.realm.RealmTorrentFile
import io.realm.RealmList

/**
 * Created by arran on 16/05/2017.
 */
@JvmName("mapTorrentFileListToRealm")
internal fun List<TorrentFile>.mapToRealm(): RealmList<RealmTorrentFile> {
    val realmList = RealmList<RealmTorrentFile>()
    forEach { realmList.add(it.mapToRealm()) }
    return realmList
}

internal fun TorrentFile.mapToRealm(): RealmTorrentFile {
    val realmTorrentFile = RealmTorrentFile(
            fileLength ?: 0,
            fileDirs?.mapToRealm(),
            torrentHash,
            primaryKey,
            percComplete,
            parentTorrentName
    )
    return realmTorrentFile
}

@JvmName("mapStringListToRealm")
internal fun List<String>.mapToRealm(): RealmList<RealmString> {
    val realmList = RealmList<RealmString>()
    this.forEach { realmList.add(it.mapToRealm()) }
    return realmList
}

internal fun String.mapToRealm(): RealmString {
    val realmString = RealmString(
            this
    )
    return realmString
}