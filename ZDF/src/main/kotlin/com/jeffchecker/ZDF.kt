package com.jeffchecker

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.AppUtils.parseJson
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.newExtractorLink
import java.net.URLEncoder

open class MediathekViewChannelProvider(
    private val channel: String,
    displayName: String,
    siteUrl: String,
    private val homeTitle: String
) : MainAPI() {
    override var name = displayName
    override var mainUrl = siteUrl
    override var lang = "de"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val apiUrl = "https://mediathekviewweb.de/api/query"
    private val pageSize = 30

    private suspend fun queryChannel(search: String? = null, page: Int = 1): List<MvwEntry> {
        val queries = mutableListOf<Map<String, Any>>(
            mapOf(
                "fields" to listOf("channel"),
                "query" to channel
            )
        )

        if (!search.isNullOrBlank()) {
            queries += mapOf(
                "fields" to listOf("topic", "title"),
                "query" to search
            )
        }

        val payload = mapOf(
            "queries" to queries,
            "sortBy" to "timestamp",
            "sortOrder" to "desc",
            "future" to false,
            "offset" to ((page - 1).coerceAtLeast(0) * pageSize),
            "size" to pageSize
        )

        val encoded = URLEncoder.encode(payload.toJson(), Charsets.UTF_8.name())
        val response = app.get("$apiUrl?query=$encoded").parsed<MvwResponse>()
        return response.result?.results.orEmpty()
    }

    private fun MvwEntry.toSearchResponse(): SearchResponse {
        return newMovieSearchResponse(
            name = title,
            url = this.toJson(),
            type = TvType.Movie
        ) {
            year = timestamp.takeIf { it > 0 }?.let {
                java.util.Calendar.getInstance().apply {
                    timeInMillis = it * 1000
                }.get(java.util.Calendar.YEAR)
            }
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val entries = queryChannel(page = page)
        return newHomePageResponse(
            HomePageList(
                name = homeTitle,
                list = entries.map { it.toSearchResponse() },
                isHorizontalImages = false
            ),
            hasNext = entries.size == pageSize
        )
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        return queryChannel(search = query).map { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val entry = parseJson<MvwEntry>(url)
        return newMovieLoadResponse(
            name = entry.title,
            url = url,
            dataUrl = entry.toJson(),
            type = TvType.Movie
        ) {
            plot = entry.description.ifBlank { null }
            duration = entry.duration.takeIf { it > 0 }?.div(60)?.toInt()
            tags = listOf(entry.channel, entry.topic).filter { it.isNotBlank() }
            year = entry.timestamp.takeIf { it > 0 }?.let {
                java.util.Calendar.getInstance().apply {
                    timeInMillis = it * 1000
                }.get(java.util.Calendar.YEAR)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val entry = parseJson<MvwEntry>(data)
        val sourceName = entry.channel.ifBlank { channel }
        val streams = listOf(
            "$sourceName HD" to entry.urlVideoHd,
            "$sourceName SD" to entry.urlVideo,
            "$sourceName LQ" to entry.urlVideoLow
        ).filter { it.second.isNotBlank() }.distinctBy { it.second }

        streams.forEach { (label, streamUrl) ->
            callback.invoke(
                newExtractorLink(
                    source = sourceName,
                    name = label,
                    url = streamUrl
                )
            )
        }

        if (entry.urlSubtitle.isNotBlank()) {
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = "Deutsch",
                    url = entry.urlSubtitle
                )
            )
        }

        return streams.isNotEmpty()
    }

    data class MvwResponse(
        val result: MvwResult? = null,
        val err: List<String>? = null
    )

    data class MvwResult(
        val results: List<MvwEntry> = emptyList()
    )

    data class MvwEntry(
        val id: String = "",
        val channel: String = "",
        val topic: String = "",
        val title: String = "",
        val description: String = "",
        val timestamp: Long = 0,
        val duration: Long = 0,
        val size: Long = 0,
        @JsonProperty("url_website")
        val urlWebsite: String = "",
        @JsonProperty("url_subtitle")
        val urlSubtitle: String = "",
        @JsonProperty("url_video")
        val urlVideo: String = "",
        @JsonProperty("url_video_low")
        val urlVideoLow: String = "",
        @JsonProperty("url_video_hd")
        val urlVideoHd: String = ""
    )
}

class ZDF : MediathekViewChannelProvider(
    channel = "ZDF",
    displayName = "ZDF Mediathek",
    siteUrl = "https://www.zdf.de",
    homeTitle = "Neu in der ZDF Mediathek"
)

class ZDFneo : MediathekViewChannelProvider(
    channel = "ZDFneo",
    displayName = "ZDFneo",
    siteUrl = "https://www.zdf.de",
    homeTitle = "Neu bei ZDFneo"
)

class ZDFinfo : MediathekViewChannelProvider(
    channel = "ZDFinfo",
    displayName = "ZDFinfo",
    siteUrl = "https://www.zdf.de",
    homeTitle = "Neu bei ZDFinfo"
)

class DreiSat : MediathekViewChannelProvider(
    channel = "3sat",
    displayName = "3sat Mediathek",
    siteUrl = "https://www.3sat.de",
    homeTitle = "Neu in der 3sat Mediathek"
)

class KiKA : MediathekViewChannelProvider(
    channel = "KIKA",
    displayName = "KiKA Mediathek",
    siteUrl = "https://www.kika.de",
    homeTitle = "Neu in der KiKA Mediathek"
)

class Phoenix : MediathekViewChannelProvider(
    channel = "PHOENIX",
    displayName = "phoenix Mediathek",
    siteUrl = "https://www.phoenix.de",
    homeTitle = "Neu in der phoenix Mediathek"
)
