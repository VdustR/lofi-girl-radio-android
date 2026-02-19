package com.vdustr.lofiradio

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.vdustr.lofiradio.data.NewPipeDownloader
import com.vdustr.lofiradio.data.StreamRepository
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import java.util.concurrent.TimeUnit

class LofiRadioApp : Application(), SingletonImageLoader.Factory {

    lateinit var okHttpClient: OkHttpClient
        private set
    lateinit var streamRepository: StreamRepository
        private set

    override fun onCreate() {
        super.onCreate()

        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

        NewPipeDownloader.init(okHttpClient)
        NewPipe.init(NewPipeDownloader.getInstance())

        streamRepository = StreamRepository()
    }

    override fun newImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
            }
            .crossfade(true)
            .build()
    }
}
