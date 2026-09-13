package com.example

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

class KankiAnimeProvider : MainAPIProvider() {
    override var mainUrl = "https://animecix.net"
    override var name = "KankiAnimeSistemi"
    override val supportedTypes = setOf(TvType.Anime)
    
    private val apiUrl = "https://animecix.net"

    override suspend fun search(query: String): List<SearchResponse> {
        val link = "$apiUrl/search?query=$query"
        val response = app.get(link).parsed<AnimecixSearchRoot>()
        
        return response.data?.map { anime ->
            newAnimeSearchResponse(anime.name ?: "", "$mainUrl/anime/${anime.id}") {
                this.posterUrl = anime.poster
            }
        } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val animeId = url.substringAfterLast("/")
        val animeDetails = app.get("$apiUrl/titles/$animeId").parsed<AnimecixDetailRoot>()
        val episodesList = app.get("$apiUrl/titles/$animeId/seasons/1").parsed<AnimecixEpisodeRoot>()

        val episodes = episodesList.data?.map { ep ->
            Episode(
                data = "$apiUrl/videos/${ep.id}",
                name = ep.name,
                episode = ep.episodeNumber
            )
        } ?: emptyList()

        return newAnimeLoadResponse(animeDetails.data?.name ?: "", url, TvType.Anime) {
            this.posterUrl = animeDetails.data?.poster
            this.plot = animeDetails.data?.description
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val videoData = app.get(data).parsed<AnimecixVideoRoot>()
        val videoUrl = videoData.data?.videoUrl ?: return false

        callback.invoke(
            ExtractorLink(
                name = "Kanki Player",
                source = "AnimeciX",
                url = videoUrl,
                referer = mainUrl,
                quality = Qualities.P1080.value,
                isM3u8 = videoUrl.contains(".m3u8")
            )
        )
        return true
    }
}

data class AnimecixSearchRoot(val data: List<AnimecixSearchData>?)
data class AnimecixSearchData(val id: Int?, val name: String?, val poster: String?)
data class AnimecixDetailRoot(val data: AnimecixDetailData?)
data class AnimecixDetailData(val name: String?, val poster: String?, val description: String?)
data class AnimecixEpisodeRoot(val data: List<AnimecixEpisodeData>?)
data class AnimecixEpisodeData(val id: Int?, val name: String?, val episodeNumber: Int?)
data class AnimecixVideoRoot(val data: AnimecixVideoData?)
data class AnimecixVideoData(@JsonProperty("video_url") val videoUrl: String?)
