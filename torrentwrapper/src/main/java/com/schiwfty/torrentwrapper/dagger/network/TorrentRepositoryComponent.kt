package com.schiwfty.torrentwrapper.dagger.network

import com.schiwfty.torrentwrapper.repositories.ITorrentRepository
import dagger.Component
import javax.inject.Singleton

/**
 * Created by arran on 15/02/2017.
 */
@Singleton
@Component(modules = arrayOf(NetworkModule::class))
interface TorrentRepositoryComponent {
    fun getTorrentRepository(): ITorrentRepository
}


