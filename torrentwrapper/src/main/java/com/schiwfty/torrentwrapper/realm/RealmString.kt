package com.schiwfty.torrentwrapper.realm

import io.realm.RealmObject
import io.realm.annotations.PrimaryKey


/**
 * Created by arran on 16/05/2017.
 */
internal open class RealmString(
        @PrimaryKey
        var value: String? = null
) : RealmObject()