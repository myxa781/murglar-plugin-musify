package com.badmanners.murglar.lib.musify.node
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

import com.badmanners.murglar.lib.core.model.node.Node
import com.badmanners.murglar.lib.core.model.node.NodeParameters.PagingType.ENDLESSLY_PAGEABLE
import com.badmanners.murglar.lib.core.model.node.NodeParameters.PagingType.NON_PAGEABLE
import com.badmanners.murglar.lib.core.model.node.NodeType.ARTIST
import com.badmanners.murglar.lib.core.model.node.NodeType.TRACK
import com.badmanners.murglar.lib.core.model.node.Path
import com.badmanners.murglar.lib.core.model.track.source.Bitrate
import com.badmanners.murglar.lib.core.model.track.source.Container
import com.badmanners.murglar.lib.core.model.track.source.Extension
import com.badmanners.murglar.lib.core.model.track.source.Source
import com.badmanners.murglar.lib.core.node.BaseNodeResolver
import com.badmanners.murglar.lib.core.node.LikeConfig
import com.badmanners.murglar.lib.core.node.MappedEntity
import com.badmanners.murglar.lib.core.node.Root
import com.badmanners.murglar.lib.core.node.Search
import com.badmanners.murglar.lib.core.node.Track
import com.badmanners.murglar.lib.core.model.node.Node.Companion.to
import com.badmanners.murglar.lib.musify.MusifyMurglar
import com.badmanners.murglar.lib.musify.api.MusifyAlbumRaw
import com.badmanners.murglar.lib.musify.api.MusifyApi
import com.badmanners.murglar.lib.musify.api.MusifyArtistRaw
import com.badmanners.murglar.lib.musify.api.MusifyPlaylistRaw
import com.badmanners.murglar.lib.musify.api.MusifyTrackRaw
import com.badmanners.murglar.lib.musify.localization.MusifyMessages
import com.badmanners.murglar.lib.musify.model.MusifyAlbum
import com.badmanners.murglar.lib.musify.model.MusifyArtist
import com.badmanners.murglar.lib.musify.model.MusifyFolder
import com.badmanners.murglar.lib.musify.model.MusifyPlaylist
import com.badmanners.murglar.lib.musify.model.MusifyTrack

class MusifyNodeResolver(
    murglar: MusifyMurglar,
    messages: MusifyMessages
) : BaseNodeResolver<MusifyMurglar, MusifyMessages>(murglar, messages) {

    override val configurations = listOf(

        Root(
            pattern = "topTracks",
            name = messages::topTracks,
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = false,
            isOwn = false,
            contentNodeType = TRACK,
            nodeContentSupplier = { p, page, _ ->
                murglar.api.getTopTracks(page ?: 0).map { it.toTrackNode(p) }
            }
        ),

        Root(
            pattern = "topReleases",
            name = messages::topReleases,
            paging = NON_PAGEABLE,
            hasSubdirectories = true,
            isOwn = false,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, _, _ ->
                murglar.api.getTopReleases().map { it.toAlbumNode(p) }
            }
        ),

        Root(
            pattern = "allAlbums",
            name = messages::albums,
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = true,
            isOwn = false,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, page, _ ->
                murglar.api.getAlbums(page ?: 0).map { it.toAlbumNode(p) }
            }
        ),

        Root(
            pattern = "allCompilations",
            name = messages::compilations,
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = true,
            isOwn = false,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, page, _ ->
                murglar.api.getCompilations(page ?: 0).map { it.toAlbumNode(p) }
            }
        ),

        Root(
            pattern = "allSoundtracks",
            name = messages::soundtracks,
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = true,
            isOwn = false,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, page, _ ->
                murglar.api.getSoundtracks(page ?: 0).map { it.toAlbumNode(p) }
            }
        ),

        Root(
            pattern = "allArtists",
            name = messages::artists,
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = true,
            isOwn = false,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, page, _ ->
                murglar.api.getArtists(page ?: 0).map { it.toArtistNode(p) }
            }
        ),

        Root(
            pattern = "allGenres",
            name = messages::genres,
            paging = NON_PAGEABLE,
            hasSubdirectories = true,
            isOwn = false,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, _, _ ->
                murglar.api.getGenres().map { genre ->
                    makeFolder("genre-${genre.id}-${genre.slug}", genre.name).convertFolder(p)
                }
            }
        ),

        Root(
            pattern = "allPlaylists",
            name = messages::playlists2,
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = true,
            isOwn = false,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, page, _ ->
                murglar.api.getPlaylists(page ?: 0).map { it.toPlaylistNode(p) }
            }
        ),

        MappedEntity(
            pattern = "*/folder-<folderId>",
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = true,
            type = ARTIST,
            contentNodeType = ARTIST,
            relatedPaths = { emptyList() },
            like = null,
            nodeSupplier = ::getFolderNode,
            nodeContentSupplier = ::getFolderContent
        ),

        MappedEntity(
            pattern = "*/artist-<artistId:\\d+>-<artistSlug>",
            paging = NON_PAGEABLE,
            hasSubdirectories = true,
            type = ARTIST,
            contentNodeType = ARTIST,
            relatedPaths = { emptyList() },
            like = null,
            nodeSupplier = ::getArtistNode,
            nodeContentSupplier = ::getArtistSubfolders
        ),

        MappedEntity(
            pattern = "*/release-<releaseId:\\d+>-<releaseSlug>",
            paging = NON_PAGEABLE,
            hasSubdirectories = false,
            type = ARTIST,
            contentNodeType = TRACK,
            relatedPaths = { emptyList() },
            like = null,
            nodeSupplier = ::getReleaseNode,
            nodeContentSupplier = ::getReleaseTracks
        ),

        MappedEntity(
            pattern = "*/playlist-<playlistId:\\d+>-<playlistSlug>",
            paging = ENDLESSLY_PAGEABLE,
            hasSubdirectories = false,
            type = ARTIST,
            contentNodeType = TRACK,
            relatedPaths = { emptyList() },
            like = null,
            nodeSupplier = ::getPlaylistNode,
            nodeContentSupplier = ::getPlaylistTracks
        ),

        Search(
            pattern = "searchTracks",
            name = messages::searchTracks,
            hasSubdirectories = false,
            contentNodeType = TRACK,
            nodeContentSupplier = { p, _, params ->
                val q = params.getQuery().ifEmpty { return@Search emptyList() }
                murglar.api.searchTracks(q).map { it.toTrackNode(p) }
            }
        ),

        Search(
            pattern = "searchArtists",
            name = messages::searchArtists,
            hasSubdirectories = true,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, _, params ->
                val q = params.getQuery().ifEmpty { return@Search emptyList() }
                murglar.api.searchArtists(q).map { it.toArtistNode(p) }
            }
        ),

        Search(
            pattern = "searchAlbums",
            name = messages::searchAlbums,
            hasSubdirectories = true,
            contentNodeType = ARTIST,
            nodeContentSupplier = { p, _, params ->
                val q = params.getQuery().ifEmpty { return@Search emptyList() }
                murglar.api.searchAlbums(q).map { it.toAlbumNode(p) }
            }
        ),

        Track(
            pattern = "*/track-<trackId:\\d+>-<trackSlug>",
            relatedPaths = { emptyList() },
            nodeSupplier = ::getTrackNode
        )
    )

    override suspend fun getTracksByMediaIds(mediaIds: List<String>): List<MusifyTrack> =
        murglar.getTracksByMediaIds(mediaIds)

    // ── Папки ────────────────────────────────────────────────────────────────

    private suspend fun getFolderNode(p: Path, params: Map<String, String>): Node {
        val id = params["folderId"]!!
        return makeFolder(id, decodeFolderTitle(id)).convertFolder(p)
    }

    private suspend fun getFolderContent(p: Path, page: Int?, params: Map<String, String>): List<Node> {
        val id = params["folderId"]!!
        return when {
            id.startsWith("artist_tracks_") -> {
                val rest       = id.removePrefix("artist_tracks_")
                val artistId   = rest.substringBefore("_")
                val artistSlug = rest.substringAfter("_")
                murglar.api.getArtistTracks(artistSlug, artistId, page ?: 0).map { it.toTrackNode(p) }
            }
            id.startsWith("artist_disco_") -> {
                val rest       = id.removePrefix("artist_disco_")
                val artistId   = rest.substringBefore("_")
                val artistSlug = rest.substringAfter("_")
                murglar.api.getArtistAlbums(artistSlug, artistId, page ?: 0).map { it.toAlbumNode(p) }
            }
            id.startsWith("genre_tracks_") -> {
                val rest      = id.removePrefix("genre_tracks_")
                val genreId   = rest.substringBefore("_")
                val genreSlug = rest.substringAfter("_")
                murglar.api.getGenreTracks(genreSlug, genreId, page ?: 0).map { it.toTrackNode(p) }
            }
            id.startsWith("genre-") -> {
                if ((page ?: 0) > 0) return emptyList()
                val parts     = id.removePrefix("genre-").split("-", limit = 2)
                val genreId   = parts[0]
                val genreSlug = if (parts.size > 1) parts[1] else genreId
                val name      = genreSlug.humanize()
                listOf(
                    makeFolder("genre_tracks_${genreId}_${genreSlug}", "${messages.tracksSection}: $name").convertFolder(p)
                )
            }
            else -> emptyList()
        }
    }

    // ── Исполнитель ──────────────────────────────────────────────────────────

    // ЗАМЕНИТЬ НА:
private suspend fun getArtistNode(p: Path, params: Map<String, String>): Node {
    val id   = params["artistId"]!!
    val slug = params["artistSlug"] ?: id
    return makeArtistStub(id, slug).convertArtist(p)
}

    private suspend fun getArtistSubfolders(p: Path, page: Int?, params: Map<String, String>): List<Node> {
        if ((page ?: 0) > 0) return emptyList()
        val id   = params["artistId"]!!
        val slug = params["artistSlug"] ?: id
        return listOf(
            makeFolder("artist_tracks_${id}_${slug}", messages.tracksSection).convertFolder(p),
            makeFolder("artist_disco_${id}_${slug}",  messages.discography).convertFolder(p)
        )
    }

    // ── Релиз ────────────────────────────────────────────────────────────────

private suspend fun getReleaseNode(p: Path, params: Map<String, String>): Node {
    val id   = params["releaseId"]!!
    val slug = params["releaseSlug"] ?: id
    return makeAlbumStub(id, slug).convertAlbum(p)
}

    private suspend fun getReleaseTracks(p: Path, page: Int?, params: Map<String, String>): List<Node> {
        if ((page ?: 0) > 0) return emptyList()
        val id   = params["releaseId"]!!
        val slug = params["releaseSlug"] ?: id
        return murglar.api.getReleaseTracks(slug, id).map { it.toTrackNode(p) }
    }

    // ── Плейлист ─────────────────────────────────────────────────────────────

    private suspend fun getPlaylistNode(p: Path, params: Map<String, String>): Node {
        val id   = params["playlistId"]!!
        val slug = params["playlistSlug"] ?: id
        return MusifyPlaylist(id, slug, slug.humanize(), 0, null, null).convertPlaylist(p)
    }

    private suspend fun getPlaylistTracks(p: Path, page: Int?, params: Map<String, String>): List<Node> {
        val id   = params["playlistId"]!!
        val slug = params["playlistSlug"] ?: id
        return murglar.api.getPlaylistTracks(slug, id, page ?: 0).map { it.toTrackNode(p) }
    }

    // ── Трек ─────────────────────────────────────────────────────────────────

    private suspend fun getTrackNode(p: Path, params: Map<String, String>): Node {
        val id   = params["trackId"]!!
        val slug = params["trackSlug"] ?: id
        val html = murglar.api.get("${MusifyApi.BASE}/track/$slug-$id")
        val raw  = murglar.api.parseTracks(html).firstOrNull { it.id == id }
            ?: murglar.api.parseTracks(html).firstOrNull()
            ?: error("Track $id not found")
        return raw.toTrackNode(p)
    }


    // ── Конвертеры ───────────────────────────────────────────────────────────

    private fun MusifyTrackRaw.toTrackNode(p: Path): Node {
        val track = MusifyTrack(
            id = id,
            title = title,
            slug = slug,
            artistIds = artistIds,
            artistNames = artistNames,
            albumId = albumId.ifEmpty { null },
            albumName = albumName.ifEmpty { null },
            albumSlug = albumSlug.ifEmpty { null },
            durationMs = durationSec * 1000L,
            bitrate = bitrate,
            playCount = playCount,
            sources = listOf(source),
            smallCoverUrl = fixImageUrl(coverUrl),
            bigCoverUrl = fixImageUrl(coverUrl),
            serviceUrl = trackPageUrl
        )
        return track.convertTrack(p)
    }

    private fun MusifyArtistRaw.toModel() = MusifyArtist(
        id = id, artistName = name, slug = slug, country = country,
        genres = genres, albumCount = albumCount, trackCount = trackCount,
        smallCoverUrl = fixImageUrl(coverUrl), bigCoverUrl = fixImageUrl(coverUrl)
    )

    private fun MusifyArtistRaw.toArtistNode(p: Path): Node = toModel().convertArtist(p)

    private fun MusifyAlbumRaw.toModel() = MusifyAlbum(
        id = id, albumTitle = title, slug = slug, artistName = artistName,
        year = year, type = type, genres = genres, trackCount = trackCount,
        smallCoverUrl = fixImageUrl(coverUrl), bigCoverUrl = fixImageUrl(coverUrl)
    )

    private fun MusifyAlbumRaw.toAlbumNode(p: Path): Node = toModel().convertAlbum(p)

    private fun MusifyPlaylistRaw.toPlaylistNode(p: Path): Node =
        MusifyPlaylist(id, slug, title, trackCount,
            fixImageUrl(coverUrl), fixImageUrl(coverUrl)
        ).convertPlaylist(p)
		
    // ── Path helpers ─────────────────────────────────────────────────────────

    private fun folderPath(p: Path, f: MusifyFolder)      = p.child("folder-${f.id}")
    private fun artistPath(p: Path, a: MusifyArtist)      = p.child("artist-${a.id}-${a.slug}")
    private fun albumPath(p: Path, a: MusifyAlbum)        = p.child("release-${a.id}-${a.slug}")
    private fun playlistPath(p: Path, pl: MusifyPlaylist) = p.child("playlist-${pl.id}-${pl.slug}")
    private fun trackPath(p: Path, t: MusifyTrack)        = p.child("track-${t.id}-${t.slug}")

    private fun MusifyFolder.convertFolder(p: Path)     = convert(::folderPath, p)
    private fun MusifyArtist.convertArtist(p: Path)     = convert(::artistPath, p)
    private fun MusifyAlbum.convertAlbum(p: Path)       = convert(::albumPath, p)
    private fun MusifyPlaylist.convertPlaylist(p: Path) = convert(::playlistPath, p)
    private fun MusifyTrack.convertTrack(p: Path)       = convert(::trackPath, p)

    // ── Stubs / helpers ──────────────────────────────────────────────────────

    private fun makeFolder(id: String, title: String, cover: String? = null) =
        MusifyFolder(id = id, title = title, smallCoverUrl = cover, bigCoverUrl = cover)

    private fun makeArtistStub(id: String, slug: String) = MusifyArtist(
        id = id, artistName = slug.humanize(), slug = slug,
        country = "", genres = emptyList(), albumCount = 0, trackCount = 0,
        smallCoverUrl = null, bigCoverUrl = null
    )

    private fun makeAlbumStub(id: String, slug: String) = MusifyAlbum(
        id = id, albumTitle = slug.humanize(), slug = slug,
        artistName = "", year = "", type = "Альбом", genres = emptyList(), trackCount = 0,
        smallCoverUrl = null, bigCoverUrl = null
    )

  private fun String.humanize(): String {
    return try {
        URLDecoder.decode(this, StandardCharsets.UTF_8.name())
            .replace('-', ' ')
            .replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        this.replace('-', ' ').replaceFirstChar { it.uppercase() }
    }
}


private fun fixImageUrl(url: String?): String? {
    if (url.isNullOrEmpty()) return url
    // Регулярное выражение удаляет суффиксы размера типа _200x200, 
    // чтобы получить оригинальное качество изображения
    return url.replace(Regex("_\\d+x\\d+"), "")
}


private fun decodeFolderTitle(id: String): String {
    val decodedId = try {
        URLDecoder.decode(id, StandardCharsets.UTF_8.name())
    } catch (e: Exception) {
        id
    }
    
    return when {
        decodedId.startsWith("genre-")         -> decodedId.removePrefix("genre-").substringAfter("-").humanize()
        decodedId.startsWith("artist_tracks_") -> messages.tracksSection
        decodedId.startsWith("artist_disco_")  -> messages.discography
        decodedId.startsWith("genre_tracks_")  -> messages.tracksSection
        else                                   -> decodedId.humanize()
    }
}
}