package com.schiwfty.confluencewrapper

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.schiwfty.torrentwrapper.confluence.Confluence
import com.schiwfty.torrentwrapper.repositories.ITorrentRepository
import com.schiwfty.torrentwrapper.utils.ParseTorrentResult
import com.schiwfty.torrentwrapper.utils.getFullPath
import com.schiwfty.torrentwrapper.utils.getMagnetLink
import com.schiwfty.torrentwrapper.utils.openFile
import kotlinx.android.synthetic.main.activity_sample.*
import java.io.File

class SampleActivity : AppCompatActivity() {
    lateinit var torrentRepository: ITorrentRepository

    val hashUnderTest = "2eb734e872a0ecf8929633b7aa4825c7f8c354f8"
//    val hashUnderTest = "3ac1e9fb84a728b32ef953075c203e47c25069d3"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).path + File.separator + "wrapper_test"

        Confluence.install(applicationContext, directoryPath, 8081)
        torrentRepository = Confluence.torrentRepository

        torrentRepository.isConnected()
                .subscribe { Log.v("is connected", "$it") }

        val pendingIntent = Intent(this, SampleActivity::class.java)
        Confluence.start(this, R.mipmap.ic_launcher, true, true, pendingIntent)
                .map { Log.v("confluence state", it.name) }
                .flatMap { torrentRepository.isConnected(); }
                .subscribe {
                    Log.v("is connected", "$it")
                }

        torrentRepository.isConnected()
                .subscribe { Log.v("is connected", "$it") }

        get_status.setOnClickListener {
            torrentRepository.getStatus()
                    .subscribe({
                        text_view.text = it
                    }, {
                        text_view.text = it?.localizedMessage
                    })
        }

        download_info.setOnClickListener {
            torrentRepository.downloadTorrentInfo(hashUnderTest)
                    .subscribe({
                        it.unwrapIfSuccess { text_view.text = it.name }
                    }, {
                        text_view.text = it?.localizedMessage
                        it.printStackTrace()
                    })
        }

        get_info.setOnClickListener {
            torrentRepository.downloadTorrentInfo(hashUnderTest)
                    .subscribe({
                        it.unwrapIfSuccess { text_view.text = it.name }
                    }, {
                        text_view.text = it?.localizedMessage
                    })
        }

        start_download.setOnClickListener {
            torrentRepository.downloadTorrentInfo(hashUnderTest)
                    .map { it.unwrapIfSuccess { torrentRepository.startFileDownloading(it.fileList.last(), this, true) } }
                    .subscribe({
                        text_view.text = "download started"
                    }, {
                        text_view.text = it?.localizedMessage
                    })
        }

        open_file.setOnClickListener {
            torrentRepository.downloadTorrentInfo(hashUnderTest)
                    .subscribe({
                        it.unwrapIfSuccess{
                            it.fileList.last().let {
                                it.openFile(this, torrentRepository, {
                                    text_view.text = "no activity to open file"
                                })
                            }
                        }
                    }, {
                        text_view.text = it?.localizedMessage
                    })
        }

        get_torrent_file.setOnClickListener {
            torrentRepository.downloadTorrentInfo(hashUnderTest)
                    .map {
                        it.unwrapIfSuccess ({ torrentRepository.getTorrentFileFromPersistence(it.info_hash, it.fileList.last().getFullPath()) }, { throw IllegalStateException("Could not read torrent")})
                    }
                    .subscribe {
                        it?.primaryKey.let { text_view.text = "primary key = $it" }
                    }
        }

        add_torrent.setOnClickListener {
            val testFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Cloudburst/Test.jpg")
            torrentRepository.addFileToClient(testFile)
                    .subscribe({
                        text_view.text = "Torrent name ${it.name}\nfiles: ${it.fileList}\n magnet: ${it.getMagnetLink()}"
                    }, {
                        text_view.text = it.localizedMessage
                    })

        }

        post_torrent_info.setOnClickListener {
            torrentRepository.postTorrentFile(hashUnderTest, File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Test.torrent"))
                    .subscribe({
                        text_view.text = "$hashUnderTest torrent file created"
                    }, {
                        it.printStackTrace()
                    })
        }

        clear_persistence.setOnClickListener {
            torrentRepository.clearPersistence()
        }

        post_test_torrent.setOnClickListener {
            val testFile = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/Test2.torrent")
            val hash = "1674948e42361aa13868cf5030841f7df7f5b804"
            torrentRepository.postTorrentFile(hash, testFile)
                    .subscribe({
                        text_view.text = "torrent file posted"
                    }, {
                        text_view.text = it.localizedMessage
                    })
        }

        verify_data.setOnClickListener {
            torrentRepository.verifyData(hashUnderTest)
                    .subscribe({
                        text_view.text = "data verified"
                    }, {
                        text_view.text = it.localizedMessage
                    })
        }

        get_all_torrents.setOnClickListener {
            torrentRepository.getAllTorrentsFromStorage()
                    .subscribe({
                        val success = it.filter { it is ParseTorrentResult.Success }
                        val error = it.filter { it is ParseTorrentResult.Error }
                        text_view.text = "success torrents: ${success.size}\nerror torrents: ${error.size}"
                    }, {
                        text_view.text = it.localizedMessage
                    })
        }

        delete_torrent_info.setOnClickListener {
            torrentRepository.getAllTorrentsFromStorage()
                    .flatMapIterable { it }
                    .filter { it is ParseTorrentResult.Success }
                    .map { (it as ParseTorrentResult.Success) }
                    .map { it.torrentInfo }
                    .map { it?.let { torrentRepository.deleteTorrentInfoFromStorage(it) } }
                    .subscribe({
                        text_view.text = "deleted"
                    }, {
                        text_view.text = it.localizedMessage
                    })
        }

        torrentRepository.getAllTorrentsFromStorage()
                .subscribe({
                    text_view.text = "torrents: ${it.size}"
                }, {
                    text_view.text = it.localizedMessage
                })

        stop_service.setOnClickListener {
            Confluence.stop()
            finish()
            Thread {
                Thread.sleep(500)
                val id = android.os.Process.myPid()
                android.os.Process.killProcess(id)
            }.start()
        }
    }
}
