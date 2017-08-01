package com.schiwfty.torrentwrapper.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import com.schiwfty.torrentwrapper.bencoding.TorrentCreator
import com.schiwfty.torrentwrapper.bencoding.TorrentParser
import com.schiwfty.torrentwrapper.confluence.Confluence
import com.schiwfty.torrentwrapper.confluence.Confluence.announceList
import com.schiwfty.torrentwrapper.models.TorrentFile
import com.schiwfty.torrentwrapper.models.TorrentInfo
import com.schiwfty.torrentwrapper.repositories.ITorrentRepository
import java.io.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.*
import java.util.regex.Pattern


/**
 * Created by arran on 30/04/2017.
 */
fun File.getAsTorrentObject(): TorrentInfo? {
    if (!isValidTorrentFile()) return null
    val torrentInfo = TorrentParser.parseTorrent(this.absolutePath)
    if (torrentInfo?.totalSize == 0L && torrentInfo.fileList.size > 1) {
        torrentInfo.fileList.forEach {
            torrentInfo.totalSize += it.fileLength ?: 0
        }
    }
    if (torrentInfo?.singleFileTorrent ?: false) {
        if (torrentInfo?.totalSize != null) {
            val paths: LinkedList<String> = LinkedList(listOf(torrentInfo.name))
            val torrentFile = TorrentFile()
            torrentFile.fileLength = torrentInfo.totalSize
            torrentFile.fileDirs = paths
            torrentFile.torrentHash = torrentInfo.info_hash
            torrentFile.primaryKey = "${torrentInfo.info_hash}${paths.concatStrings()}"
            Log.v("generated primaryKey:", "Torrent: ${torrentInfo.name}   File: ${paths.last}     primaryKey: ${torrentInfo.info_hash}${paths.concatStrings()}")
            torrentInfo.fileList = listOf(torrentFile)
        }
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
    if (matcher.find())
        return matcher.group(1)
    else
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
    if (isDirectory) return false
    if (!exists()) return false
    if (!canRead()) return false
    if (!path.endsWith(".torrent")) return false
    return true
}

fun File.copyToTorrentDirectory(): Boolean {
    if (!isValidTorrentFile()) return false
    if (parentFile.absolutePath.equals(Confluence.torrentInfoStorage)) return true
    val testFile = File(Confluence.torrentInfoStorage.absolutePath + File.separator + name)
    if (testFile.exists()) return true
    val result = copyTo(File(Confluence.torrentInfoStorage.absolutePath, name), true)
    return result.isValidTorrentFile()
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

fun File.createTorrent(outputFile: File): Pair<String, File> {
    val torrentCreator = TorrentCreator()
    val pieceLength = 512 * 1024
    val pieces = torrentCreator.hashPieces(this, pieceLength)

    val info = HashMap<String, Any>()
    info.put("name", this.name)
    info.put("length", this.length())
    info.put("piece length", pieceLength)
    info.put("pieces", pieces)
    val metainfo = HashMap<String, Any>()
    metainfo.put("announce-list", announceList)
    metainfo.put("info", info)
    val out = FileOutputStream(outputFile)
    torrentCreator.encodeMap(metainfo, out)
    out.close()
    val infoHash = outputFile.hashMetaInfo()
    return Pair(infoHash, outputFile)
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

fun String.getTorrentFileWithTrackers(outputFile: File):  Pair<String, File>{
    val torrentCreator = TorrentCreator()

    val info = HashMap<String, Any>()
    info.put("name", this)
    val metainfo = HashMap<String, Any>()
    metainfo.put("announce", Confluence.announceList.first())
    metainfo.put("announce-list", Confluence.announceList)
    metainfo.put("info", info)
    val out = FileOutputStream(outputFile)
    torrentCreator.encodeMap(metainfo, out)
    out.close()
    val infoHash = outputFile.hashMetaInfo()
    return Pair(infoHash, outputFile)
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