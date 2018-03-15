package com.schiwfty.torrentwrapper.utils

import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

/**
 * Created by arran on 30/04/2017.
 */

fun <T> Observable<T>.composeIo(): Observable<T> = compose<T>({
    it.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
})

fun <T> Observable<T>.retryWhenInvalidTorrent(torrentFile: File): Observable<T> = compose<T>({
    it.composeIoWithRetryXTimesInvalidTorrentOrTimeout(5, torrentFile)
})

fun <T> Observable<T>.composeIoWithRetryXTimesInvalidTorrentOrTimeout(maxRetries: Int, torrentFile: File): Observable<T> = compose<T>({
    var retryCount = 0
    it.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .retryWhen({
                it.flatMap {
                    if (++retryCount < maxRetries && it is InvalidTorrentException) {
                        torrentFile.delete()
                        Observable.timer(0, TimeUnit.MILLISECONDS)
                    } else if (++retryCount < maxRetries && it is SocketTimeoutException) {
                        Observable.timer(0, TimeUnit.MILLISECONDS)
                    } else {
                        Observable.error(it)
                    }
                }
            })
})

class InvalidTorrentException(val throwable: Throwable) : Exception() {
    override val message: String?
        get() = throwable.localizedMessage
}