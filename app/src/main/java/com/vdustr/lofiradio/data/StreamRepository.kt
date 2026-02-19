package com.vdustr.lofiradio.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.channel.tabs.ChannelTabs
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import org.schabi.newpipe.extractor.stream.StreamType

class StreamRepository {

    @Volatile
    private var cachedStreams: List<LofiStream> = emptyList()

    /**
     * When true, the last fetchLiveStreams returned cached data due to a network error.
     */
    @Volatile
    var isUsingCache: Boolean = false
        private set

    suspend fun fetchLiveStreams(): List<LofiStream> = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube

            val tabExtractor = service.getChannelTabExtractorFromId(
                LOFI_GIRL_CHANNEL_ID,
                ChannelTabs.LIVESTREAMS
            )
            tabExtractor.fetchPage()

            val streams = tabExtractor.initialPage.items
                .filterIsInstance<StreamInfoItem>()
                .filter { it.streamType == StreamType.LIVE_STREAM }
                .mapNotNull { item ->
                    val videoId = extractVideoId(item.url)
                    if (videoId.isEmpty()) return@mapNotNull null
                    LofiStream(
                        title = item.name,
                        videoId = videoId,
                        thumbnailUrl = item.thumbnails.firstOrNull()?.url,
                        viewerCount = item.viewCount
                    )
                }

            cachedStreams = streams
            isUsingCache = false
            streams
        } catch (e: Exception) {
            val cached = cachedStreams
            if (cached.isNotEmpty()) {
                isUsingCache = true
                cached
            } else {
                throw e
            }
        }
    }

    suspend fun getHlsUrl(videoId: String): String = withContext(Dispatchers.IO) {
        require(videoId.isNotEmpty()) { "videoId must not be empty" }
        withTimeout(30_000L) {
            val service = ServiceList.YouTube
            val videoUrl = "https://www.youtube.com/watch?v=$videoId"
            val extractor = service.getStreamExtractor(
                service.streamLHFactory.fromUrl(videoUrl)
            )
            extractor.fetchPage()

            val hlsUrl = extractor.hlsUrl
            if (hlsUrl.isNullOrEmpty()) {
                throw IllegalStateException("No HLS URL available for video $videoId")
            }
            hlsUrl
        }
    }

    private fun extractVideoId(url: String): String {
        return url.substringAfter("v=", "")
            .substringBefore("&")
            .ifEmpty {
                url.substringAfterLast("/").substringBefore("?")
            }
    }

    companion object {
        const val LOFI_GIRL_CHANNEL_ID = "UCSJ4gkVC6NrvII8umztf0Ow"
    }
}
