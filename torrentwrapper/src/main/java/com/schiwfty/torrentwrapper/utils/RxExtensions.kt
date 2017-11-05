package com.schiwfty.torrentwrapper.utils

import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
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
    it.retryWhen(RetryWhenInvalidTorrentOrTimeout(5, torrentFile))
})


private class RetryWhenInvalidTorrentOrTimeout(val maxRetries: Int, val torrentFile: File)
    : Func1<Observable<out Throwable>, Observable<*>> {

    internal var retryCount = 0

    override fun call(attempts: Observable<out Throwable>): Observable<*> {
        return attempts.flatMap({
            if (++retryCount < maxRetries && it is InvalidTorrentException) {
                torrentFile.delete()
                Observable.timer(0, TimeUnit.MILLISECONDS)
            } else if (++retryCount < maxRetries && it is SocketTimeoutException){
                Observable.timer(0, TimeUnit.MILLISECONDS)
            }
            else {
                Observable.error(it as Throwable)
            }
        })
    }
}


class InvalidTorrentException(val throwable: Throwable): Exception(){
    override val message: String?
        get() = throwable.localizedMessage
}