// ! Bu araç @keyiflerolsun tarafından | @KekikAkademi için yazılmıştır.

package com.keyiflerolsun

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson

class HQPorner : MainAPI() {
    override var mainUrl              = "https://m.hqporner.com"
    override var name                 = "HQPorner"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "${mainUrl}/top/month"               to "Month TOP",
        "${mainUrl}/top/week"                to "Week TOP",
        "${mainUrl}/category/1080p-porn"     to "1080p",
        "${mainUrl}/category/4k-porn"        to "4K",
        "${mainUrl}/category/60fps-porn"     to "60FPS",
        "${mainUrl}/category/amateur"        to "Amateur",
        "${mainUrl}/category/teen-porn"      to "Teen",
        "${mainUrl}/category/babe"           to "Babe",
        "${mainUrl}/category/pov"            to "POV",
        "${mainUrl}/category/orgasm"         to "Orgasm",
        "${mainUrl}/category/porn-massage"   to "Sex Massage",
        "${mainUrl}/category/threesome"      to "Threesome",
        "${mainUrl}/category/group-sex"      to "Group Sex",
        "${mainUrl}/category/lesbian"        to "Lesbian",
        "${mainUrl}/category/milf"           to "Milf",
        "${mainUrl}/category/mature"         to "Mature",
        "${mainUrl}/category/long-hair"      to "Long Hair",
        "${mainUrl}/category/big-tits"       to "Big Tits",
        "${mainUrl}/category/small-tits"     to "Small tits",
        "${mainUrl}/category/squeezing-tits" to "Squeezing Tits",
        "${mainUrl}/category/big-ass"        to "Big Ass",
        "${mainUrl}/category/latina"         to "Latina",
        "${mainUrl}/category/russian"        to "Russian",
        "${mainUrl}/category/blonde"         to "Blonde",
        "${mainUrl}/category/redhead"        to "Redhead",
        "${mainUrl}/category/blowjob"        to "Blowjob",
        "${mainUrl}/category/brunette"       to "Brunette",
        "${mainUrl}/category/undressing"     to "Undressing",
        "${mainUrl}/category/cumshot"        to "Cumshot",
        "${mainUrl}/category/outdoor"        to "Outdoor",
        "${mainUrl}/category/deepthroat"     to "Deepthroat",
        "${mainUrl}/category/handjob"        to "Handjob",
        "${mainUrl}/category/pussy-licking"  to "Pussy Licking",
        "${mainUrl}/category/moaning"        to "Moaning",
        "${mainUrl}/category/vintage"        to "Vintage",
        "${mainUrl}/category/tattooed"       to "Tattooed",
        "${mainUrl}/category/beach-porn"     to "Beach"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}/${page}").document
        val home     = document.select("div.box.page-content div.row section").mapNotNull { it.toMainPageResult() }

        return newHomePageResponse(
            list    = HomePageList(
                name               = request.name,
                list               = home,
                isHorizontalImages = true
            ),
            hasNext = true
        )
    }

    private fun Element.toMainPageResult(): SearchResponse? {
        val lowerCaseTitle = this.selectFirst("h3 a")?.text() ?:"No Title"
        val title          = lowerCaseTitle.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        val href           = fixUrlNull(this.selectFirst("h3 a")?.attr("href")) ?: return null
        val posterUrl      = fixUrlNull(this.selectFirst("img")?.attr("src"))

        return newMovieSearchResponse(title, LoadUrl(href, posterUrl).toJson(), TvType.NSFW) { this.posterUrl = posterUrl }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val searchResponse = mutableListOf<SearchResponse>()

        for (i in 1..5) {
            val document = app.get("${mainUrl}/?q=${query.replace(" ", "+")}&p=${i}").document

            val results = document.select("div.box.page-content div.row section").mapNotNull { it.toMainPageResult() }

            searchResponse.addAll(results)

            if (results.isEmpty()) break
        }

        return searchResponse
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    private fun convertTimeToMinutes(timeText: String): Int {
        if (timeText == "") {
            return 0
        }

        val regex = Regex("""(\d+)m (\d+)s""")
        val matchResult = regex.find(timeText)
        return if (matchResult != null) {
            val minutes = matchResult.groupValues[1].toInt()
            val seconds = matchResult.groupValues[2].toInt()
            minutes + seconds / 60
        } else {
            0
        }
    }

    override suspend fun load(url: String): LoadResponse? {
        val loadData = tryParseJson<LoadUrl>(url) ?: return null
        val document = app.get(loadData.href).document

        val lowerCaseTitle  = document.selectFirst("h1.main-h1")?.text() ?: "No Title"
        val title           = lowerCaseTitle.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        val poster          = loadData.posterUrl
        val description     = title
        val tags            = document.select("p a[href*='/category']").map { it.text() }
        val duration        = convertTimeToMinutes(document.selectFirst("li.fa-clock-o")?.text()?.trim() ?: "")
        val recommendations = document.select("div.row div.row section").mapNotNull { it.toMainPageResult() }
        val actors          = document.select("li a[href*='/actress']").map { Actor(it.text()) }

        if (actors.isEmpty() && duration == 0 && tags.isEmpty()) {
            return null
        }

        return newMovieLoadResponse(title, url, TvType.NSFW, loadData.href) {
            this.posterUrl       = poster
            this.plot            = description
            this.tags            = tags
            this.duration        = duration
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(data: String, isCasting: Boolean, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit): Boolean {
        Log.d("HPRN", "data » ${data}")
        val document = app.get(data).document

        val rawURL = Regex("""url: '/blocks/altplayer\.php\?i=//(.*?)',""").find(document.toString())?.groupValues?.get(1) ?: return false
        val vidURL = "https://${rawURL}"
        Log.d("HPRN", "vidURL » ${vidURL}")

        loadExtractor(vidURL, "${mainUrl}/", subtitleCallback, callback)

        return true
    }
}

data class LoadUrl(
    val href: String,
    val posterUrl: String?
)