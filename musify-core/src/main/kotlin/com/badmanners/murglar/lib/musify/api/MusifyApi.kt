package com.badmanners.murglar.lib.musify.api

import com.badmanners.murglar.lib.core.network.NetworkMiddleware
import com.badmanners.murglar.lib.core.network.NetworkRequest
import com.badmanners.murglar.lib.core.network.ResponseConverters
import com.badmanners.murglar.lib.core.model.track.source.Bitrate
import com.badmanners.murglar.lib.core.model.track.source.Container
import com.badmanners.murglar.lib.core.model.track.source.Extension
import com.badmanners.murglar.lib.core.model.track.source.Source

// ── Модели ──────────────────────────────────────────────────────────────────

data class MusifyTrackRaw(
    val id: String,
    val slug: String,
    val title: String,
    val artistNames: List<String>,
    val artistIds: List<String>,
    val albumId: String,
    val albumName: String,
    val albumSlug: String,
    val year: String,
    val coverUrl: String,
    val durationSec: Int,
    val bitrate: Int,
    val playCount: Long,
    val trackPageUrl: String,
    val genre: String
) {
    val source: Source get() = Source(
        id        = id,
        url       = "https://musify.club/api/track/$id/stream-url",
        tag       = "${bitrate}kbps",
        extension = Extension.MP3,
        container = Container.PROGRESSIVE,
        bitrate   = if (bitrate >= 320) Bitrate.B_320 else if (bitrate >= 256) Bitrate.B_256 else Bitrate.B_128
    )
}

data class MusifyArtistRaw(
    val id: String,
    val slug: String,
    val name: String,
    val country: String,
    val genres: List<String>,
    val albumCount: Int,
    val trackCount: Int,
    val coverUrl: String
)

data class MusifyAlbumRaw(
    val id: String,
    val slug: String,
    val title: String,
    val artistName: String,
    val year: String,
    val type: String,
    val genres: List<String>,
    val coverUrl: String,
    val trackCount: Int
)

data class MusifyPlaylistRaw(
    val id: String,
    val slug: String,
    val title: String,
    val coverUrl: String,
    val trackCount: Int
)

data class MusifyGenre(
    val id: String,
    val slug: String,
    val name: String
)

// ── API ──────────────────────────────────────────────────────────────────────

class MusifyApi(
    private val network: NetworkMiddleware,
    private val logger: com.badmanners.murglar.lib.core.log.LoggerMiddleware? = null
) {

    companion object {
        const val BASE = "https://musify.club"
        const val BASE_EN = "https://musify.club/en"

        private val UA = "Mozilla/5.0 (Linux; Android 11; Pixel 5) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        // stream-url API
        private val RE_JSON_URL = Regex(""""url"\s*:\s*"([^"]+)"""")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Stream URL (без авторизации)
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getStreamUrl(trackId: String): String {
        val json = getJson("$BASE/api/track/$trackId/stream-url")
        return RE_JSON_URL.find(json)?.groupValues?.get(1)
            ?: error("No url in stream-url response for track $trackId")
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Публичные методы
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun getTopTracks(p: Int=0) = parseTracks(get("$BASE_EN/hits${pageParam(p)}"))
    suspend fun getTopReleases()    = parseAlbums(get("$BASE_EN/top"))
    suspend fun getAlbums(p: Int=0) = parseAlbums(get("$BASE_EN/albums${pageParam(p)}"))
    suspend fun getCompilations(p: Int=0) = parseAlbums(get("$BASE_EN/compilations${pageParam(p)}"))
    suspend fun getSoundtracks(p: Int=0)  = parseAlbums(get("$BASE_EN/soundtracks${pageParam(p)}"))
    suspend fun getArtists(p: Int=0)      = parseArtists(get("$BASE_EN/artist${pageParam(p)}"))
    suspend fun getPlaylists(p: Int=0)    = parsePlaylists(get("$BASE_EN/theme${pageParam(p)}"))

    suspend fun getArtistTracks(slug: String, id: String, p: Int=0) =
        parseTracks(get("$BASE_EN/artist/$slug-$id/tracks${pageParam(p)}"))

    suspend fun getArtistAlbums(slug: String, id: String, p: Int=0) =
        parseAlbums(get("$BASE_EN/artist/$slug-$id/discography${pageParam(p)}"))

    suspend fun getReleaseTracks(slug: String, id: String) =
        parseTracks(get("$BASE_EN/release/$slug-$id"))

    suspend fun getGenreTracks(slug: String, id: String, p: Int=0) =
        parseTracks(get("$BASE_EN/genre/$slug-$id${pageParam(p)}"))

    suspend fun getGenreAlbums(slug: String, id: String, p: Int=0) =
        parseAlbums(get("$BASE_EN/genre/$slug-$id${pageParam(p)}"))

    suspend fun getPlaylistTracks(slug: String, id: String, p: Int=0) =
        parseTracks(get("$BASE_EN/theme/$slug-$id${pageParam(p)}"))

    suspend fun getGenres(): List<MusifyGenre> {
        // /en/genre/{slug}-{id}  => название берётся из текста ссылки
        val html = get("$BASE_EN/genre")
        return parseLinksWithText(html, """/en/genre/([\w%-]+-(\d+))""")
            .map { (slug, id, name) -> MusifyGenre(id, slug, name) }
            .distinctBy { it.id }
    }

    suspend fun searchTracks(query: String, p: Int=0): List<MusifyTrackRaw> {
        val q = enc(query)
        val url = "$BASE_EN/search?searchText=$q${if (p > 0) "&page=${p+1}" else ""}"
        return parseTracks(get(url))
    }

    suspend fun searchArtists(query: String) =
        parseArtists(get("$BASE_EN/search?searchText=${enc(query)}&type=artists"))

    suspend fun searchAlbums(query: String) =
        parseAlbums(get("$BASE_EN/search?searchText=${enc(query)}&type=releases"))

    // ─────────────────────────────────────────────────────────────────────────
    // ПАРСЕР ТРЕКОВ
    //
    // Реальная структура одного трека в markdown (из HTML):
    //
    //   ![](https://41s.musify.club/img/.../cover.jpg)
    //   [Shape Of You](https://musify.club/en/track/ed-sheeran-shape-of-you-7201831)
    //   [Ed Sheeran](https://musify.club/en/artist/ed-sheeran-94793)
    //   5.9M
    //   03:54 320 Кб/с
    //   [/track/dl/7201831/ed-sheeran-shape-of-you.mp3](/track/dl/7201831/...)
    //
    // Стратегия: режем по dl-ссылкам, контекст ПЕРЕД каждой dl — это карточка трека.
    // ─────────────────────────────────────────────────────────────────────────

    fun parseTracks(html: String): List<MusifyTrackRaw> {
        val result = mutableListOf<MusifyTrackRaw>()

        // Все /track/dl/{id}/{filename} — id числовой, filename может содержать %XX
        val dlRe   = Regex("""/track/dl/(\d+)/([^)\s"'<]+\.mp3)""")
        val dlAll  = dlRe.findAll(html).toList()
        if (dlAll.isEmpty()) return result

        // CDN-картинка (все варианты musify CDN)
        val imgRe    = Regex("""https?://\d+[ts]*\.musify\.club/img/[^\s)"']+""")

        // Ссылка на страницу трека: /en/track/{slug-with-id}
        // id — последнее число в slug
        val trackRe  = Regex("""/en/track/([\w%.-]+-(\d+))(?:[)\s"'<]|$)""")

        // Ссылки на артиста: /en/artist/{slug}-{id}
        val artistRe = Regex("""/en/artist/([\w%.-]+-(\d+))(?:[)\s"'<]|$)""")

        // Длительность: MM:SS (после может идти пробел или HTML-тег '<')
        val durRe    = Regex("""(\d{1,2}):(\d{2})(?=[\s<])""")

        // Битрейт: RU-страница помечает "Кб/с", EN — "Kbps"
        val brRe     = Regex("""(\d{2,3})\s*(?:Кб/с|[Kk]bps)""")

        for ((idx, dl) in dlAll.withIndex()) {
            val trackId = dl.groupValues[1]

            // Контекст ДО этой dl: от предыдущей dl (или начала) до текущей
            val blockStart = if (idx > 0) dlAll[idx - 1].range.last + 1 else 0
            val blockEnd   = dl.range.first
            val before     = html.substring(blockStart, blockEnd)

            // Хвост после dl (для битрейта/длительности если они после — на этом сайте они ДО, но на всякий)
            val afterEnd = if (idx + 1 < dlAll.size) dlAll[idx + 1].range.first
                           else minOf(html.length, dl.range.last + 300)
            val ctx = before + html.substring(dl.range.last + 1, afterEnd)

            // ── Обложка: последний img в before (ближайший к dl)
            val cover = imgRe.findAll(before).lastOrNull()?.value ?: ""

            // ── Страница трека + название
            // Рантайм get() возвращает СЫРОЙ HTML, поэтому основной формат —
            // HTML-якорь: <a ... href=".../en/track/slug-id" ...>Название</a>
            // (href может быть как абсолютным, так и корне-относительным /en/track/...)
            // Markdown-формат [Название](url) и имя файла из dl — fallback'и.
            val htmlLinkRe = Regex("""<a\b[^>]*href="[^"]*/en/track/([\w%.-]+-(\d+))[^"]*"[^>]*>([^<]+)</a>""")
            val mdLinkRe   = Regex("""\[([^\]]+)]\(https://musify\.club/en/track/([\w%.-]+-(\d+))[^)]*\)""")

            // Предпочитаем якорь, чей id совпадает с trackId; иначе — последний в блоке
            val htmlMatch = htmlLinkRe.findAll(before).lastOrNull { it.groupValues[2] == trackId }
                ?: htmlLinkRe.findAll(before).lastOrNull()
            val mdMatch   = mdLinkRe.findAll(before).lastOrNull { it.groupValues[3] == trackId }
                ?: mdLinkRe.findAll(before).lastOrNull()

            val title: String
            val trackSlug: String

            when {
                htmlMatch != null -> {
                    trackSlug = htmlMatch.groupValues[1]
                    title     = htmlMatch.groupValues[3].htmlDecode()
                }
                mdMatch != null -> {
                    trackSlug = mdMatch.groupValues[2]
                    title     = mdMatch.groupValues[1].htmlDecode()
                }
                else -> {
                    // Fallback: slug из любой /en/track-ссылки + название из имени dl-файла
                    val m2 = trackRe.findAll(before).lastOrNull { it.groupValues[2] == trackId }
                    trackSlug = m2?.groupValues?.get(1) ?: trackId
                    val fname = dl.groupValues[2].removeSuffix(".mp3")
                    title = (try { java.net.URLDecoder.decode(fname, "UTF-8") }
                             catch (_: Exception) { fname })
                        .replace("-", " ").trim()
                        .replaceFirstChar { it.uppercase() }
                }
            }

            // ── Артисты
            // Основной формат (сырой HTML): <a ... href=".../en/artist/slug-id" ...>Имя</a>
            // Markdown [Имя](.../en/artist/slug-id) и имя из slug — fallback'и.
            val htmlArtistRe = Regex("""<a\b[^>]*href="[^"]*/en/artist/([\w%.-]+-(\d+))[^"]*"[^>]*>([^<]+)</a>""")
            val mdArtistRe   = Regex("""\[([^\]]+)]\(https://musify\.club/en/artist/([\w%.-]+-(\d+))[^)]*\)""")
            val artistNames  = mutableListOf<String>()
            val artistIds    = mutableListOf<String>()

            // 1) HTML-якоря
            for (am in htmlArtistRe.findAll(before)) {
                val aId   = am.groupValues[2]
                val aName = am.groupValues[3].htmlDecode()
                if (aId !in artistIds && aName.isNotBlank()) { artistIds += aId; artistNames += aName }
            }
            // 2) Markdown-fallback
            if (artistIds.isEmpty()) {
                for (am in mdArtistRe.findAll(before)) {
                    val aId   = am.groupValues[3]
                    val aName = am.groupValues[1].htmlDecode()
                    if (aId !in artistIds && aName.isNotBlank()) { artistIds += aId; artistNames += aName }
                }
            }
            // 3) Fallback: имя восстанавливаем из slug ссылки на артиста
            if (artistIds.isEmpty()) {
                for (am in artistRe.findAll(before)) {
                    val aId   = am.groupValues[2]
                    val aSlug = am.groupValues[1]
                    if (aId !in artistIds) {
                        artistIds += aId
                        artistNames += aSlug.substringBeforeLast("-$aId")
                            .replace('-', ' ').replaceFirstChar { it.uppercase() }
                    }
                }
            }

            // ── Длительность и битрейт (обычно в before, т.к. стоят до dl-ссылки)
            val durMatch    = durRe.find(ctx)
            val durationSec = if (durMatch != null)
                durMatch.groupValues[1].toInt() * 60 + durMatch.groupValues[2].toInt()
            else 0

            val bitrate = brRe.find(ctx)?.groupValues?.get(1)?.toIntOrNull() ?: 320

            // ── Год (если есть)
            val year = Regex("""\b(19|20)\d{2}\b""").find(before)?.value ?: ""

            // slug для пути узла — без хвостового -id (id хранится отдельно).
            // URL страницы трека канонический: /track/{slug}-{id}.
            val cleanSlug = trackSlug.removeSuffix("-$trackId")

            result += MusifyTrackRaw(
                id          = trackId,
                slug        = cleanSlug,
                title       = title,
                artistNames = artistNames.ifEmpty { listOf("Unknown") },
                artistIds   = artistIds.ifEmpty  { listOf("0") },
                albumId     = "",
                albumName   = "",
                albumSlug   = "",
                year        = year,
                coverUrl    = cover,
                durationSec = durationSec,
                bitrate     = bitrate,
                playCount   = 0L,
                trackPageUrl = "$BASE_EN/track/$cleanSlug-$trackId",
                genre        = ""
            )
        }
        logger?.i("MusifyApi", "parseTracks -> ${result.size} tracks")
        return result
    }
    // ─────────────────────────────────────────────────────────────────────────
    // ПАРСЕР АЛЬБОМОВ/РЕЛИЗОВ
    //
    // Рантайм get() отдаёт СЫРОЙ HTML. Карточка релиза содержит:
    //   <a href=".../en/release/slug-year-id"><img src="...cdn.../cover.jpg"></a>  (обложка)
    //   <a href=".../en/artist/slug-id">Artist</a>
    //   <a href=".../en/release/slug-year-id">Title</a>                            (заголовок)
    //   <a href=".../en/albums|compilations|soundtracks/YYYY">YYYY</a>
    //   <a href=".../en/genre/slug">Genre</a> ...
    // Парсим по проверенным URL-паттернам (одинаковы в HTML и markdown).
    // ─────────────────────────────────────────────────────────────────────────

    fun parseAlbums(html: String): List<MusifyAlbumRaw> {
        val result = mutableListOf<MusifyAlbumRaw>()

        val imgRe = Regex("""https?://\d+[ts]*\.musify\.club/img/[^\s)"']+""")

        // Все ссылки на релиз с текстом (HTML-якорь). Текст обложки = вложенный <img> -> пусто.
        data class Rel(val pos: Int, val id: String, val slug: String, val text: String)
        val rels = mutableListOf<Rel>()

        val htmlRelRe = Regex(
            """<a\b[^>]*href="[^"]*/en/release/([\w%.,'-]+-(\d+))[^"]*"[^>]*>(.*?)</a>""",
            RegexOption.DOT_MATCHES_ALL
        )
        for (m in htmlRelRe.findAll(html)) {
            val text = m.groupValues[3].replace(Regex("<[^>]+>"), "").trim()
            rels += Rel(m.range.first, m.groupValues[2], m.groupValues[1], text)
        }
        // Fallback: markdown (image-link обложки + текстовая ссылка-заголовок)
        if (rels.isEmpty()) {
            val mdImg = Regex("""\[!\[[^\]]*]\([^)]*\)]\(https://musify\.club/en/release/([\w%.,'-]+-(\d+))[^)]*\)""")
            val mdTxt = Regex("""\[([^\]]+)]\(https://musify\.club/en/release/([\w%.,'-]+-(\d+))[^)]*\)""")
            for (m in mdImg.findAll(html)) rels += Rel(m.range.first, m.groupValues[2], m.groupValues[1], "")
            for (m in mdTxt.findAll(html)) rels += Rel(m.range.first, m.groupValues[3], m.groupValues[2], m.groupValues[1])
            rels.sortBy { it.pos }
        }
        if (rels.isEmpty()) {
            logger?.i("MusifyApi", "parseAlbums -> 0 (no release links)")
            return result
        }

        // Уникальные id в порядке появления; первое вхождение = начало карточки
        val firstPos = LinkedHashMap<String, Int>()
        val slugById = HashMap<String, String>()
        for (r in rels) if (r.id !in firstPos) { firstPos[r.id] = r.pos; slugById[r.id] = r.slug.removeSuffix("-${r.id}") }
        val ids = firstPos.keys.toList()

        val htmlArtistRe = Regex("""<a\b[^>]*href="[^"]*/en/artist/([\w%.-]+-(\d+))[^"]*"[^>]*>([^<]+)</a>""")
        val mdArtistRe   = Regex("""\[([^\]]+)]\(https://musify\.club/en/artist/([\w%.-]+-(\d+))[^)]*\)""")
        val htmlGenreRe  = Regex("""<a\b[^>]*href="[^"]*/en/genre/[^"]*"[^>]*>([^<]+)</a>""")
        val mdGenreRe    = Regex("""\[([^\]]+)]\(https://musify\.club/en/genre/[^)]+\)""")
        val typeYearRe   = Regex("""/en/(albums|compilations|soundtracks)/(\d{4})""")

        for ((idx, id) in ids.withIndex()) {
            val start = firstPos[id]!!
            val end   = if (idx + 1 < ids.size) firstPos[ids[idx + 1]]!! else minOf(html.length, start + 1500)
            val block = html.substring(start, end)
            val slug  = slugById[id]!!

            // Название — релиз-ссылка с непустым текстом внутри блока
            val title = rels.filter { it.pos in start until end && it.id == id && it.text.isNotBlank() }
                .maxByOrNull { it.text.length }?.text?.htmlDecode()
                ?: slug.substringBeforeLast("-$id").replace('-', ' ').replaceFirstChar { it.uppercase() }

            val cover = imgRe.find(block)?.value ?: ""

            val artists = LinkedHashMap<String, String>()
            for (am in htmlArtistRe.findAll(block)) artists.putIfAbsent(am.groupValues[2], am.groupValues[3].htmlDecode())
            if (artists.isEmpty()) for (am in mdArtistRe.findAll(block)) artists.putIfAbsent(am.groupValues[3], am.groupValues[1].htmlDecode())
            val artistName = artists.values.joinToString(", ")

            val ty   = typeYearRe.find(block)
            val year = ty?.groupValues?.get(2) ?: ""
            val type = when (ty?.groupValues?.get(1)) {
                "compilations" -> "Сборник"
                "soundtracks"  -> "Саундтрек"
                else           -> "Альбом"
            }

            val genres = LinkedHashSet<String>()
            for (g in htmlGenreRe.findAll(block)) genres += g.groupValues[1].htmlDecode()
            if (genres.isEmpty()) for (g in mdGenreRe.findAll(block)) genres += g.groupValues[1].htmlDecode()

            result += MusifyAlbumRaw(
                id = id, slug = slug, title = title,
                artistName = artistName, year = year, type = type,
                genres = genres.toList(), coverUrl = cover, trackCount = 0
            )
        }
        logger?.i("MusifyApi", "parseAlbums -> ${result.size} albums")
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ПАРСЕР КАРТОЧЕК (артисты/плейлисты) — сырой HTML, с markdown-fallback
    //
    // Карточка: <a href=".../en/{seg}/slug-id"><img src="...cdn..."></a>
    //           <a href=".../en/{seg}/slug-id">Name</a>
    // ─────────────────────────────────────────────────────────────────────────

    private data class CardRaw(val id: String, val slug: String, val name: String, val cover: String)

    private fun parseCards(html: String, seg: String): List<CardRaw> {
        val out = mutableListOf<CardRaw>()
        val imgRe = Regex("""https?://\d+[ts]*\.musify\.club/img/[^\s)"']+""")

        data class Ref(val pos: Int, val id: String, val slug: String, val text: String)
        val refs = mutableListOf<Ref>()

        val htmlRe = Regex(
            """<a\b[^>]*href="[^"]*/en/$seg/([\w%.-]+-(\d+))[^"]*"[^>]*>(.*?)</a>""",
            RegexOption.DOT_MATCHES_ALL
        )
        for (m in htmlRe.findAll(html)) {
            val text = m.groupValues[3].replace(Regex("<[^>]+>"), "").trim()
            refs += Ref(m.range.first, m.groupValues[2], m.groupValues[1], text)
        }
        if (refs.isEmpty()) {
            val mdImg = Regex("""\[!\[[^\]]*]\([^)]*\)]\(https://musify\.club/en/$seg/([\w%.-]+-(\d+))[^)]*\)""")
            val mdTxt = Regex("""\[([^\]]+)]\(https://musify\.club/en/$seg/([\w%.-]+-(\d+))[^)]*\)""")
            for (m in mdImg.findAll(html)) refs += Ref(m.range.first, m.groupValues[2], m.groupValues[1], "")
            for (m in mdTxt.findAll(html)) refs += Ref(m.range.first, m.groupValues[3], m.groupValues[2], m.groupValues[1])
            refs.sortBy { it.pos }
        }
        if (refs.isEmpty()) return out

        val firstPos = LinkedHashMap<String, Int>()
        val slugById = HashMap<String, String>()
        for (r in refs) if (r.id !in firstPos) { firstPos[r.id] = r.pos; slugById[r.id] = r.slug.removeSuffix("-${r.id}") }
        val ids = firstPos.keys.toList()

        for ((idx, id) in ids.withIndex()) {
            val start = firstPos[id]!!
            val end   = if (idx + 1 < ids.size) firstPos[ids[idx + 1]]!! else minOf(html.length, start + 600)
            val slug  = slugById[id]!!
            val name  = refs.filter { it.pos in start until end && it.id == id && it.text.isNotBlank() }
                .maxByOrNull { it.text.length }?.text?.htmlDecode()
                ?: slug.substringBeforeLast("-$id").replace('-', ' ').replaceFirstChar { it.uppercase() }
            val cover = imgRe.find(html.substring(start, end))?.value ?: ""
            if (name.length < 2) continue
            out += CardRaw(id, slug, name, cover)
        }
        return out
    }

    fun parseArtists(html: String): List<MusifyArtistRaw> {
        val result = parseCards(html, "artist").map {
            MusifyArtistRaw(
                id = it.id, slug = it.slug, name = it.name,
                country = "", genres = emptyList(),
                albumCount = 0, trackCount = 0, coverUrl = it.cover
            )
        }
        logger?.i("MusifyApi", "parseArtists -> ${result.size} artists")
        return result
    }

    fun parsePlaylists(html: String): List<MusifyPlaylistRaw> {
        val result = parseCards(html, "theme").map {
            MusifyPlaylistRaw(it.id, it.slug, it.name, it.cover, 0)
        }
        logger?.i("MusifyApi", "parsePlaylists -> ${result.size} playlists")
        return result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP
    // ─────────────────────────────────────────────────────────────────────────

    suspend fun get(url: String): String {
        logger?.i("MusifyApi", "GET $url")
        val req = NetworkRequest.Builder(url, "GET")
            .addHeader("User-Agent", UA)
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "en-US,en;q=0.9,ru;q=0.8")
            .addHeader("Referer", BASE)
            .build()
        val body = network.execute(req, ResponseConverters.asString()).result
        logger?.i("MusifyApi", "GET $url -> ${body.length} chars")
        return body
    }

    private suspend fun getJson(url: String): String {
        val req = NetworkRequest.Builder(url, "GET")
            .addHeader("User-Agent", UA)
            .addHeader("Accept", "application/json, */*")
            .addHeader("Referer", BASE)
            .build()
        return network.execute(req, ResponseConverters.asString()).result
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Утилиты
    // ─────────────────────────────────────────────────────────────────────────

    // Пагинация: /en/hits/page2, /en/albums/page3 и т.д.
    private fun pageParam(page: Int) = if (page > 0) "/page${page + 1}" else ""

    private fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

    /** Парсит ссылки вида /en/{type}/{slug}-{id} с текстом из markdown */
    private fun parseLinksWithText(html: String, pattern: String): List<Triple<String, String, String>> {
        val result = mutableListOf<Triple<String, String, String>>()
        val seen   = mutableSetOf<String>()
        val re     = Regex("""\[([^\]]+)]\(https://musify\.club$pattern[^\)]*\)""")
        for (m in re.findAll(html)) {
            val name = m.groupValues[1].htmlDecode().trim()
            val slug = m.groupValues[2]
            val id   = m.groupValues[3]
            if (id !in seen && name.isNotBlank()) {
                seen += id
                result += Triple(slug, id, name)
            }
        }
        return result
    }

    private fun String.htmlDecode(): String {
        // 1) Сначала &amp; -> & (на случай двойного кодирования вида &amp;#xNN;)
        var s = this.replace("&amp;", "&")
        // 2) Шестнадцатеричные числовые ссылки: &#xHHHH; / &#XHHHH;
        //    (musify так кодирует кириллицу и спецсимволы, напр. Ø = &#xD8;)
        s = Regex("&#[xX]([0-9A-Fa-f]+);").replace(s) { m ->
            runCatching { String(Character.toChars(m.groupValues[1].toInt(16))) }
                .getOrDefault(m.value)
        }
        // 3) Десятичные числовые ссылки: &#NNNN;
        s = Regex("&#(\\d+);").replace(s) { m ->
            runCatching { String(Character.toChars(m.groupValues[1].toInt())) }
                .getOrDefault(m.value)
        }
        // 4) Именованные сущности
        s = s.replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&nbsp;", " ")
        return s.trim()
    }
}
