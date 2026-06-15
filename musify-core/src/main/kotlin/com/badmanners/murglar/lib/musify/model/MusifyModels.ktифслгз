package com.badmanners.murglar.lib.musify.model

import com.badmanners.murglar.lib.core.model.artist.BaseArtist
import com.badmanners.murglar.lib.core.model.node.NodeType
import com.badmanners.murglar.lib.core.model.track.BaseTrack
import com.badmanners.murglar.lib.core.model.track.source.Source
import com.badmanners.murglar.lib.core.utils.contract.Model

// ── Папка-плейлист (для разделов навигации: жанры, исполнители, альбомы, ...) ──

@Model
class MusifyFolder(
    id: String,
    val title: String,
    smallCoverUrl: String?,
    bigCoverUrl: String?
) : BaseArtist(
    id = id,
    name = title,
    smallCoverUrl = smallCoverUrl,
    bigCoverUrl = bigCoverUrl,
    serviceUrl = "https://musify.club"
)

// ── Исполнитель ───────────────────────────────────────────────────────────────

@Model
class MusifyArtist(
    id: String,
    val artistName: String,
    val slug: String,
    val country: String,
    val genres: List<String>,
    val albumCount: Int,
    val trackCount: Int,
    smallCoverUrl: String?,
    bigCoverUrl: String?
) : BaseArtist(
    id = id,
    name = artistName,
    smallCoverUrl = smallCoverUrl,
    bigCoverUrl = bigCoverUrl,
    serviceUrl = "https://musify.club/artist/$slug-$id"
)

// ── Альбом/релиз ──────────────────────────────────────────────────────────────

@Model
class MusifyAlbum(
    id: String,
    val albumTitle: String,
    val slug: String,
    val artistName: String,
    val year: String,
    val type: String,
    val genres: List<String>,
    val trackCount: Int,
    smallCoverUrl: String?,
    bigCoverUrl: String?
) : BaseArtist(
    id = id,
    name = albumTitle,
    smallCoverUrl = smallCoverUrl,
    bigCoverUrl = bigCoverUrl,
    serviceUrl = "https://musify.club/release/$slug-$id"
)

// ── Плейлист musify.club/theme ────────────────────────────────────────────────

@Model
class MusifyPlaylist(
    id: String,
    val slug: String,
    val playlistTitle: String,
    val trackCount: Int,
    smallCoverUrl: String?,
    bigCoverUrl: String?
) : BaseArtist(
    id = id,
    name = playlistTitle,
    smallCoverUrl = smallCoverUrl,
    bigCoverUrl = bigCoverUrl,
    serviceUrl = "https://musify.club/theme/$slug-$id"
)

// ── Трек ──────────────────────────────────────────────────────────────────────

@Model
class MusifyTrack(
    id: String,
    title: String,
    val slug: String,
    artistIds: List<String>,
    artistNames: List<String>,
    albumId: String?,
    albumName: String?,
    val albumSlug: String?,
    durationMs: Long,
    val bitrate: Int,
    val playCount: Long,
    sources: List<Source>,
    smallCoverUrl: String?,
    bigCoverUrl: String?,
    serviceUrl: String,
    override val nodeType: String = NodeType.TRACK
) : BaseTrack(
    id = id,
    title = title,
    subtitle = null,
    artistIds = artistIds,
    artistNames = artistNames,
    albumId = albumId,
    albumName = albumName,
    albumReleaseDate = null,
    indexInAlbum = null,
    volumeNumber = null,
    durationMs = durationMs,
    genre = null,
    explicit = false,
    gain = null,
    peak = null,
    sources = sources,
    mediaId = "$id:$slug",
    smallCoverUrl = smallCoverUrl,
    bigCoverUrl = bigCoverUrl,
    serviceUrl = serviceUrl
)
