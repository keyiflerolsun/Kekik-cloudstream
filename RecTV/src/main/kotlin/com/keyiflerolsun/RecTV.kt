// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.
// ! https://github.com/Amiqo09/Diziyou-Cloudstream

package com.keyiflerolsun

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

class RecTV : MainAPI() {
    override var mainUrl              = "https://m.prectv7.sbs"
    override var name                 = "RecTV"
    override val hasMainPage          = true
    override var lang                 = "tr"
    override val hasQuickSearch       = false
    override val hasChromecastSupport = true
    override val hasDownloadSupport   = true
    override val supportedTypes       = setOf(TvType.Movie, TvType.Live, TvType.TvSeries)

    val swKey = "4F5A9C3D9A86FA54EACEDDD635185/c3c5bd17-e37b-4b94-a944-8a3688a30452"

    override val mainPage = mainPageOf(
        "${mainUrl}/api/channel/by/filtres/0/0/SAYFA/${swKey}/"      to "Canlı",
        "${mainUrl}/api/movie/by/filtres/0/created/SAYFA/${swKey}/"  to "Son Filmler",
        "${mainUrl}/api/serie/by/filtres/0/created/SAYFA/${swKey}/"  to "Son Diziler",
        "${mainUrl}/api/movie/by/filtres/14/created/SAYFA/${swKey}/" to "Aile",
        "${mainUrl}/api/movie/by/filtres/1/created/SAYFA/${swKey}/"  to "Aksiyon",
        "${mainUrl}/api/movie/by/filtres/13/created/SAYFA/${swKey}/" to "Animasyon",
        "${mainUrl}/api/movie/by/filtres/19/created/SAYFA/${swKey}/" to "Belgesel",
        "${mainUrl}/api/movie/by/filtres/4/created/SAYFA/${swKey}/"  to "Bilim Kurgu",
        "${mainUrl}/api/movie/by/filtres/2/created/SAYFA/${swKey}/"  to "Dram",
        "${mainUrl}/api/movie/by/filtres/10/created/SAYFA/${swKey}/" to "Fantastik",
        "${mainUrl}/api/movie/by/filtres/3/created/SAYFA/${swKey}/"  to "Komedi",
        "${mainUrl}/api/movie/by/filtres/8/created/SAYFA/${swKey}/"  to "Korku",
        "${mainUrl}/api/movie/by/filtres/17/created/SAYFA/${swKey}/" to "Macera",
        "${mainUrl}/api/movie/by/filtres/5/created/SAYFA/${swKey}/"  to "Romantik"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val page = page - 1

        val url  = request.data.replace("SAYFA", "${page}")
        val home = app.get(url, headers=mapOf("user-agent" to "okhttp/4.12.0"))

        val movies = AppUtils.tryParseJson<List<RecItem>>(home.text)!!.mapNotNull { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)

            if (item.label != "CANLI" && item.label != "Canlı") {
                newMovieSearchResponse(item.title, "${toDict}", TvType.Movie) { this.posterUrl = item.image }
            } else {
                LiveSearchResponse(
                    name      = item.title,
                    url       = "${toDict}",
                    apiName   = this@RecTV.name,
                    type      = TvType.Live,
                    posterUrl = item.image
                )
            }
        }

        return newHomePageResponse(request.name, movies)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val home    = app.get("${mainUrl}/api/search/${query}/${swKey}/")
        val veriler = AppUtils.tryParseJson<RecSearch>(home.text)

        val sonuclar = mutableListOf<SearchResponse>()

        veriler?.channels?.forEach { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)

            sonuclar.add(newMovieSearchResponse(item.title, "${toDict}", TvType.Movie) { this.posterUrl = item.image })
        }

        veriler?.posters?.forEach { item ->
            val toDict = jacksonObjectMapper().writeValueAsString(item)

            sonuclar.add(newMovieSearchResponse(item.title, "${toDict}", TvType.Movie) { this.posterUrl = item.image })
        }

        return sonuclar
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val veri = AppUtils.tryParseJson<RecItem>(url) ?: return null

        if (veri.type == "serie") {
            val diziReq  = app.get("${mainUrl}/api/season/by/serie/${veri.id}/${swKey}/")
            val sezonlar = AppUtils.tryParseJson<List<RecDizi>>(diziReq.text) ?: return null

            val episodeList = mutableListOf<Episode>()

            for (sezon in sezonlar) {
                for (bolum in sezon.episodes) {
                    episodeList.add(Episode(
                        data        = bolum.sources.first().url,
                        name        = bolum.title,
                        season      = sezon.title.substringBefore(".S").toIntOrNull() ?: 1,
                        episode     = bolum.title.substringAfter("Bölüm ").toIntOrNull() ?: 1,
                        description = sezon.title.substringAfter(".S "),
                        posterUrl   = veri.image
                    ))
                }
            }

            return newTvSeriesLoadResponse(veri.title, url, TvType.TvSeries, episodeList) {
                this.posterUrl = veri.image
                this.plot      = veri.description
                this.year      = veri.year
                this.tags      = veri.genres?.map { it.title }
                this.rating    = "${veri.rating}".toRatingInt()
            }
        }

        if (veri.label != "CANLI" && veri.label != "Canlı") {
            return newMovieLoadResponse(veri.title, url, TvType.Movie, url) {
                this.posterUrl = veri.image
                this.plot      = veri.description
                this.year      = veri.year
                this.tags      = veri.genres?.map { it.title }
                this.rating    = "${veri.rating}".toRatingInt()
            }
        } else {
            return LiveStreamLoadResponse(
                name      = veri.title,
                url       = url,
                apiName   = this.name,
                dataUrl   = url,
                posterUrl = veri.image,
                plot      = veri.description,
                tags      = veri.genres?.map { it.title },
            )
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        if (data.startsWith("http")) {
            Log.d("RCTV", "data » ${data}")
            callback.invoke(
                ExtractorLink(
                    source  = "${this.name}",
                    name    = "${this.name}",
                    url     = data,
                    headers = mapOf(
                        "User-Agent"      to "googleusercontent",
                        "origin"          to "https://twitter.com",
                        "Accept-Encoding" to "gzip",
                    ),
                    referer = "https://twitter.com/",
                    quality = Qualities.Unknown.value,
                    type    = INFER_TYPE
                )
            )
            return true
        }

        val veri = AppUtils.tryParseJson<RecItem>(data) ?: return false

        veri.sources.forEach { source ->
            Log.d("RCTV", "source » ${source}")
            callback.invoke(
                ExtractorLink(
                    source  = "${this.name} - ${source.type}",
                    name    = "${this.name} - ${source.type}",
                    url     = source.url,
                    headers = mapOf(
                        "User-Agent"      to "googleusercontent",
                        "origin"          to "https://twitter.com",
                        "Accept-Encoding" to "gzip",
                    ),
                    referer = "https://twitter.com/",
                    quality = Qualities.Unknown.value,
                    type    = if (source.type == "mp4") ExtractorLinkType.VIDEO else ExtractorLinkType.M3U8
                )
            )
        }

        return true
    }
}