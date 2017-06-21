package com.schiwfty.torrentwrapper.retrofit


import com.schiwfty.torrentwrapper.models.ConfluenceInfo
import com.schiwfty.torrentwrapper.models.FileStatePiece
import com.schiwfty.torrentwrapper.retrofit.ClientAPI
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.ResponseBody
import rx.Observable


/**
 * Created by arran on 4/02/2017.
 */


internal class ConfluenceApi(private val clientAPI: ClientAPI) {

    fun getInfo(hash: String): Observable<ResponseBody> {
        return clientAPI.getInfo(hash)
    }

    fun postTorrent(hash: String, bencode: ByteArray): Observable<ResponseBody> {
        val requestBody = RequestBody.create(MediaType.parse("application/octet-stream"), bencode)
        return clientAPI.postTorrent(hash, requestBody)
    }

    val getStatus: Observable<ConfluenceInfo>
        get() = clientAPI.getStatus()

    fun getFileData(hash: String, path: String): Observable<ResponseBody> {
        return clientAPI.getTorrentDataInfo(hash, path)
    }

    fun getFileState(hash: String, path: String): Observable<List<FileStatePiece>> {
        return clientAPI.getFileState(hash, path)
    }
}