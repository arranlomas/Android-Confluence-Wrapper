package com.schiwfty.confluencewrapper

import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import com.schiwfty.torrentwrapper.confluence.Confluence
import com.schiwfty.torrentwrapper.models.TorrentInfo
import com.schiwfty.torrentwrapper.repositories.ITorrentRepository
import com.schiwfty.torrentwrapper.utils.generateTorrentFile
import com.schiwfty.torrentwrapper.utils.getFullPath
import com.schiwfty.torrentwrapper.utils.getMagnetLink
import com.schiwfty.torrentwrapper.utils.openFile
import kotlinx.android.synthetic.main.activity_sample.*
import java.io.File

class SampleActivity : AppCompatActivity() {
    lateinit var torrentRepository: ITorrentRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + File.separator + "wrapper-test"
        Confluence.install(applicationContext, directoryPath)
        Confluence.start(this, R.mipmap.ic_launcher)
                .subscribe {
                    Log.v("confluence state", it.name)
                    when (it) {
                        Confluence.ConfluenceState.STARTED -> buttons.visibility = View.VISIBLE
                        else -> buttons.visibility = View.GONE
                    }
                }

        torrentRepository = Confluence.torrentRepository

        get_status.setOnClickListener {
            torrentRepository.getStatus()
                    .subscribe {
                        text_view.text = it.toString()
                    }
        }

        download_info.setOnClickListener {
            torrentRepository.downloadTorrentInfo("b99f93d2df9472910941c4a315718fb0d1eff191")
                    .subscribe {
                        text_view.text = it?.name

                    }
        }

        get_info.setOnClickListener {
            torrentRepository.getTorrentInfo("b99f93d2df9472910941c4a315718fb0d1eff191")
                    .subscribe {
                        text_view.text = it?.name

                    }
        }

        start_download.setOnClickListener {
            torrentRepository.downloadTorrentInfo("b99f93d2df9472910941c4a315718fb0d1eff191")
                    .map { it?.fileList?.last()?.let { torrentRepository.startFileDownloading(it, this, true) } }
                    .subscribe {
                        text_view.text = "download started"

                    }
        }

        open_file.setOnClickListener {
            torrentRepository.getTorrentInfo("b99f93d2df9472910941c4a315718fb0d1eff191")
                    .subscribe {
                        it?.fileList?.last()?.let {
                            it.openFile(this, torrentRepository, {
                                text_view.text = "no activity to open file"
                            })
                        }
                    }
        }

        get_torrent_file.setOnClickListener {
            torrentRepository.getTorrentInfo("b99f93d2df9472910941c4a315718fb0d1eff191")
                    .map { torrentRepository.getTorrentFileFromPersistence(it!!.info_hash, it.fileList.last().getFullPath()) }
                    .subscribe {
                        it?.primaryKey.let { text_view.text = "primary key = $it" }
                    }
        }

        add_torrent.setOnClickListener {
            val testFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Cloudburst/Test.jpg")
            torrentRepository.addFileToClient(this, testFile)
                    .subscribe({
                        text_view.text = "Torrent name ${it.name}\nfiles: ${it.fileList}\n magnet: ${it.getMagnetLink()}"
                    }, {
                        text_view.text = it.localizedMessage
                    })

        }

        post_torrent_info.setOnClickListener {
            val torrentInfo = TorrentInfo()
            torrentInfo.name = "arran test torrent"
            torrentInfo.announce = "test announce"
            torrentInfo.announceList = listOf("test_announce.com/announce")
            torrentInfo.singleFileTorrent = true
            torrentInfo.totalSize = 1000
            torrentInfo.createdBy = "android Confluence wrapper"
            val outputFile = File(Confluence.workingDir.absolutePath, "${torrentInfo.name}.torrent")
            val (hash, file) = torrentInfo.generateTorrentFile(outputFile)
            torrentRepository.postTorrentFile(hash, file.absoluteFile)
                    .subscribe({
                        text_view.text = "$hash torrent file created ${file.absolutePath}"
                    }, {
                        it.printStackTrace()
                    })
        }
    }
}
