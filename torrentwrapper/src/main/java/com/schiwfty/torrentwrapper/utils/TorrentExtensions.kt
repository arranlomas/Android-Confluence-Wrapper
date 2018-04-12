package com.schiwfty.torrentwrapper.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.schiwfty.torrentwrapper.bencoding.TorrentCreator
import com.schiwfty.torrentwrapper.bencoding.TorrentParser
import com.schiwfty.torrentwrapper.confluence.Confluence
import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.models.TorrentInfo
import com.schiwfty.torrentwrapper.repositories.ITorrentRepository
import io.reactivex.Observable
import org.apache.commons.codec.binary.Base32
import org.apache.commons.codec.binary.Hex
import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*
import java.util.regex.Pattern

/**
 * Created by arran on 30/04/2017.
 */
fun File.getAsTorrentObject(): Observable<ParseTorrentResult> {
    if (!isValidTorrentFile()) return Observable.just(ParseTorrentResult.Error(IllegalArgumentException("Not a valid torrent file")))
    return getAsTorrentObject { TorrentParser.parseTorrent(this.absolutePath) }
}

private fun getAsTorrentObject(torrentParseMethod: () -> Observable<ParseTorrentResult>): Observable<ParseTorrentResult> {
    val obs: Observable<ParseTorrentResult>
    try {
        obs = torrentParseMethod.invoke()
    } catch (e: Exception) {
        return Observable.just(ParseTorrentResult.Error(e))
    }
    return obs.map { parseResult ->
        when (parseResult) {
            is ParseTorrentResult.Success -> ParseTorrentResult.Success(parseResult.unwrapIfSuccess { it.mapTorrentFilesToTorrentInfo() })
            is ParseTorrentResult.Error -> parseResult
        }
    }
}

sealed class ParseTorrentResult {
    data class Success(val torrentInfo: TorrentInfo?) : ParseTorrentResult()
    data class Error(val exception: Exception) : ParseTorrentResult()

    fun <T> unwrapIfSuccess(isSuccess: (TorrentInfo) -> T, isError: ((Exception) -> Unit)? = null): T? {
        when (this) {
            is Success -> this.torrentInfo?.let { return isSuccess.invoke(it) }
            is Error -> isError?.invoke(this.exception)
        }
        return null
    }

    fun <T> unwrapIfSuccess(isSuccess: (TorrentInfo) -> T): T? {
        when (this) {
            is Success -> this.torrentInfo?.let { return isSuccess.invoke(it) }
        }
        return null
    }
}

private fun TorrentInfo.mapTorrentFilesToTorrentInfo(): TorrentInfo {
    val torrentInfo = this
    if (torrentInfo.totalSize == 0L && torrentInfo.fileList.size > 1) {
        torrentInfo.fileList.forEach {
            torrentInfo.totalSize += it.fileLength ?: 0
        }
    }
    if (torrentInfo.singleFileTorrent) {
        val paths: LinkedList<String> = LinkedList(listOf(torrentInfo.name))
        val torrentFile = TorrentFile()
        torrentFile.fileLength = torrentInfo.totalSize
        torrentFile.fileDirs = paths
        torrentFile.torrentHash = torrentInfo.info_hash
        torrentFile.primaryKey = "${torrentInfo.info_hash}${paths.concatStrings()}"
        Log.v("generated primaryKey:", "Torrent: ${torrentInfo.name}   File: ${paths.last}     primaryKey: ${torrentInfo.info_hash}${paths.concatStrings()}")
        torrentInfo.fileList = listOf(torrentFile)
    }
    return torrentInfo
}


fun Long.formatBytesAsSize(): String {
    if (this > 0.1 * 1024.0 * 1024.0 * 1024.0) {
        val f = this.toFloat() / 1024f / 1024f / 1024f
        return String.format("%1$.1f GB", f)
    } else if (this > 0.1 * 1024.0 * 1024.0) {
        val f = this.toFloat() / 1024f / 1024f
        return String.format("%1$.1f MB", f)
    } else {
        val f = this / 1024f
        return String.format("%1$.1f kb", f)
    }
}

fun String.findHashFromMagnet(): String? {
    val pattern = Pattern.compile("xt=urn:btih:(.*?)(&|$)")
    val matcher = pattern.matcher(this)
    if (matcher.find()) {
        val initialHash = matcher.group(1)
        if (initialHash.length == 40) return initialHash
        if (initialHash.length == 32) {
            val decoded = Base32().decode(initialHash)
            val hexString = Hex.encodeHex(decoded)
            return String(hexString)
        }
        return null
    } else
        return null
}

fun String.findNameFromMagnet(): String? {
    val pattern = Pattern.compile("dn=(.*?)(&|$)")
    val matcher = pattern.matcher(this)
    if (matcher.find())
        return matcher.group(1)
    else
        return null
}

fun String.findTrackersFromMagnet(): List<String> {
    val trackerList = mutableListOf<String>()
    val pattern = Pattern.compile("tr=(.*?)(&|$)")
    val matcher = pattern.matcher(this)
    while (matcher.find()) {
        trackerList.add(URLDecoder.decode(matcher.group(1), "UTF-8"))
    }
    return trackerList.toList()
}

fun TorrentFile.openFile(context: Context, torrentRepository: ITorrentRepository, notActivityMethod: () -> Unit) {
    torrentRepository.addTorrentFileToPersistence(this)
    val url = Confluence.fullUrl + "/data?ih=" + torrentHash + "&path=" + URLEncoder.encode(getFullPath(), "UTF-8")
    val file = File(getFullPath())
    val map = MimeTypeMap.getSingleton()
    var type: String? = map.getMimeTypeFromExtension(file.extension)

    if (type == null)
        type = "*/*"

    val intent = Intent(Intent.ACTION_VIEW)

    intent.setDataAndType(Uri.parse(url), type)
    try {
        context.startActivity(intent)
    } catch (exception: Exception) {
        notActivityMethod.invoke()
    }

}

fun TorrentFile.getFullPath(): String {
    var path = ""
    fileDirs?.let {
        val dirs = it
        it.forEachIndexed { index, s ->
            if (index == (dirs.size - 1))
                path += s
            else
                path += "$s/"
        }
    }
    return path
}

fun LinkedList<String>.concatStrings(): String {
    var path = ""
    forEachIndexed { index, s ->
        if (index == (size - 1))
            path += s
        else
            path += "$s/"
    }
    return path
}

fun File.isValidTorrentFile(): Boolean {
    if (isDirectory || !exists() || !canRead() || !path.endsWith(".torrent")) return false
    return true
}

fun File.copyToConfluenceWorkingDirectory(): Boolean {
    if (parentFile.absolutePath.equals(Confluence.workingDir)) return true
    val testFile = File(Confluence.workingDir.absolutePath + File.separator + name)
    if (testFile.exists()) return true
    copyTo(File(Confluence.workingDir.absolutePath, name), true)
    return true
}

fun TorrentFile.getShareableDataUrl(): String {
    return "${Confluence.fullUrl}/data?ih=$torrentHash&path=${getFullPath()}"
}

fun TorrentFile.getDownloadableUrl(): String {
    return "http://127.0.0.1:${Confluence.daemonPort}/data?ih=$torrentHash&path=${getFullPath()}"
}

fun TorrentFile.getMimeType(): String {
    val file = File(getFullPath())
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
}

fun TorrentFile.canCast(): Boolean {
    val splitMime = getMimeType().split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
    val fileType = splitMime[0].toLowerCase()
    val format = splitMime[1].toLowerCase()
    if (fileType == "video" || fileType == "audio") {
        return MimeConstants.chromecastSet.contains(format)
    } else return false
}

fun File.createTorrentWithPieces(outputFile: File, announceList: List<String>): String {
    val torrentCreator = TorrentCreator()
    val pieceLength = 512 * 1024
    val pieces = torrentCreator.hashPieces(this, pieceLength)

    val info = HashMap<String, Any>()
    info.put("name", this.name)
    info.put("length", this.length())
    info.put("piece length", pieceLength)
    info.put("pieces", pieces)
    val metainfo = HashMap<String, Any>()
    metainfo.put("announce-list", announceList.toTypedArray())
    metainfo.put("info", info)
    val out = FileOutputStream(outputFile)
    torrentCreator.encodeMap(metainfo, out)
    out.close()
    val infoHash = outputFile.hashMetaInfo()
    return infoHash
}

fun File.createTorrent(announceList: List<String>? = null): String {
    val torrentCreator = TorrentCreator()
    val info = HashMap<String, Any>()
    val metainfo = HashMap<String, Any>()
    announceList?.let {
        metainfo["announce-list"] = it.toTypedArray()
        if (it.isNotEmpty()) metainfo["announce"] = it.first()
    }
    metainfo["info"] = info
    val out = FileOutputStream(this)
    torrentCreator.encodeMap(metainfo, out)
    out.close()
    return this.hashMetaInfo()
}

private fun File.hashMetaInfo(): String {
    val sha1 = MessageDigest.getInstance("SHA-1")
    var input: InputStream? = null

    try {
        input = FileInputStream(this)
        val builder = StringBuilder()
        while (!builder.toString().endsWith("4:info")) {
            builder.append(input.read().toChar()) // It's ASCII anyway.
        }
        val output = ByteArrayOutputStream()
        var data: Int = input.read()
        while (data > -1) {
            output.write(data)
            data = input.read()
        }
        sha1.update(output.toByteArray(), 0, output.size() - 1)
    } finally {
        if (input != null)
            try {
                input.close()
            } catch (ignore: IOException) {
                ignore.printStackTrace()
            }
    }

    val hash = sha1.digest()

    val sb = StringBuffer()

    (0..hash.size - 1).map {
        val hex = Integer.toHexString(hash[it].toInt() and 0xff or 0x100).substring(1, 3)
        sb.append(hex)
    }

    return sb.toString()
}

fun TorrentInfo.getMagnetLink(): String {
    val sb = StringBuilder("magnet:?xt=urn:btih:")
            .append(info_hash)
            .append("&dn=")
            .append(name)
    for (tracker in announceList) {
        sb.append("&tr=")
                .append(tracker)
    }
    return sb.toString()
}

internal val defaultAnnounceList: List<String> = listOf(
        "udp://tracker.coppersurfer.tk:6969/announce",
        "udp://tracker.skyts.net:6969/announce",
        "udp://tracker.safe.moe:6969/announce",
        "udp://tracker.piratepublic.com:1337/announce",
        "udp://tracker.pirateparty.gr:6969/announce",
        "udp://allesanddro.de:1337/announce",
        "udp://9.rarbg.com:2710/announce",
        "udp://p4p.arenabg.com:1337/announce",
        "http://p4p.arenabg.com:1337/announce",
        "udp://tracker.opentrackr.org:1337/announce",
        "http://tracker.opentrackr.org:1337/announce",
        "http://asnet.pw:2710/announce",
        "udp://tracker.internetwarriors.net:1337/announce",
        "udp://public.popcorn-tracker.org:6969/announce",
        "udp://tracker1.wasabii.com.tw:6969/announce",
        "udp://tracker.zer0day.to:1337/announce",
        "udp://tracker.mg64.net:6969/announce",
        "udp://peerfect.org:6969/announce",
        "http://tracker.mg64.net:6881/announce",
        "http://mgtracker.org:6969/announce")