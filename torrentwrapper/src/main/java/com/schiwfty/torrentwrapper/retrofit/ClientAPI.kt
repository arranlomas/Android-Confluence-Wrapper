package com.schiwfty.torrentwrapper.retrofit

/**
 * Created by arran on 4/02/2017.
 */


import com.schiwfty.torrentwrapper.models.FileStatePiece
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.http.*
import io.reactivex.Observable


internal interface ClientAPI {

    @POST("/metainfo")
    fun postTorrent(@Query("ih") info_hash: String, @Body bencodedBody: RequestBody): Observable<ResponseBody>

    @GET("/info")
    fun getInfo(@Query("ih") info_hash: String): Observable<ResponseBody>

    @GET("/status")
    fun getStatus(): Observable<ResponseBody>

    @GET("/data")
    @Streaming
    fun getTorrentDataInfo(@Query("ih") info_hash: String, @Query("path") file_path: String): Observable<ResponseBody>

    @GET("/fileState")
    fun getFileState(@Query("ih") info_hash: String, @Query("path") file_path: String): Observable<List<FileStatePiece>>


    @GET("/verifyData")
    fun verifyData(@Query("ih") info_hash: String): Observable<Any>
}
