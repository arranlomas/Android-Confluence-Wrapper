package com.schiwfty.torrentwrapper.realm

import android.support.annotation.NonNull
import com.schiwfty.torrentwrapper.realm.RealmString
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey

/**
 * Created by arran on 16/05/2017.
 */
internal open class RealmTorrentFile(
        @NonNull
        var fileLength: Long = 0,

        @NonNull
        var fileDirs: RealmList<RealmString>? = null,

        @NonNull
        var torrentHash: String = "",

        @NonNull
        @PrimaryKey
        var primaryKey: String = "",

        var percComplete: Int = 0,

        var parentTorrentName: String = ""
) : RealmObject() {

}