package com.vdustr.lofiradio.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as NPRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException

/**
 * OkHttp-based Downloader implementation for NewPipe Extractor.
 * Uses a browser User-Agent to avoid bot detection.
 */
class NewPipeDownloader private constructor(
    private val client: OkHttpClient
) : Downloader() {

    override fun execute(request: NPRequest): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBody = dataToSend?.toRequestBody()

        val requestBuilder = Request.Builder()
            .url(url)
            .method(httpMethod, requestBody)
            .header("User-Agent", USER_AGENT)

        headers.forEach { (name, values) ->
            requestBuilder.removeHeader(name)
            values.forEach { value ->
                requestBuilder.addHeader(name, value)
            }
        }

        val response = client.newCall(requestBuilder.build()).execute()

        return response.use { resp ->
            // Handle rate limiting
            if (resp.code == 429) {
                throw ReCaptchaException("reCaptcha Challenge requested", url)
            }

            Response(
                resp.code,
                resp.message,
                resp.headers.toMultimap(),
                resp.body?.string(),
                resp.request.url.toString()
            )
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        @Volatile
        private var instance: NewPipeDownloader? = null

        fun init(client: OkHttpClient) {
            instance = NewPipeDownloader(client)
        }

        fun getInstance(): NewPipeDownloader {
            return instance ?: throw IllegalStateException(
                "NewPipeDownloader not initialized. Call init() first."
            )
        }
    }
}
