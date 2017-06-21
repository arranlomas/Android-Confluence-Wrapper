package com.schiwfty.confluencewrapper

import android.os.Environment
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log

import com.schiwfty.torrentwrapper.confluence.Confluence
import com.schiwfty.torrentwrapper.confluence.ConfluenceDaemonService

import java.io.File

class SampleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample)
        val directoryPath = Environment.getExternalStorageDirectory().path + File.separator + "wrapper-test"
        Confluence.install(applicationContext, directoryPath)
        Confluence.start(this, R.mipmap.ic_launcher)
                .subscribe {
                    Log.v("confluence state", it.name)
                }
    }
}
