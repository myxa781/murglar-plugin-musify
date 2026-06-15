package com.badmanners.murglar.lib.musify.localization

import com.badmanners.murglar.lib.core.localization.DefaultMessages
import com.badmanners.murglar.lib.core.localization.Messages
import com.badmanners.murglar.lib.core.localization.RussianMessages

interface MusifyMessages : Messages {
    override val playlists: String
    override val youAreNotLoggedIn: String
    override val compilations: String

    val noTags: String
    val topTracks: String
    val topReleases: String
    val soundtracks: String
    val playlists2: String
    val genres: String
    val searchTracks: String
    val searchArtists: String
    val searchAlbums: String
    val discography: String
    val tracksSection: String
}

object MusifyDefaultMessages : DefaultMessages(), MusifyMessages {
    override val serviceName = "Musify"
    override val playlists = "Musify"
    override val youAreNotLoggedIn = "Musify (no auth required)"
    override val compilations = "Compilations"
    override val noTags = "Tags are not supported"
    override val topTracks = "Top Tracks"
    override val topReleases = "Top Releases"
    override val soundtracks = "Soundtracks"
    override val playlists2 = "Playlists"
    override val genres = "Genres"
    override val searchTracks = "Search Tracks"
    override val searchArtists = "Search Artists"
    override val searchAlbums = "Search Albums"
    override val discography = "Discography"
    override val tracksSection = "Tracks"
}

object MusifyRussianMessages : RussianMessages(), MusifyMessages {
    override val serviceName = "Musify"
    override val playlists = "Musify"
    override val youAreNotLoggedIn = "Musify (авторизация не требуется)"
    override val compilations = "Сборники"
    override val noTags = "Теги не поддерживаются"
    override val topTracks = "Топ треков"
    override val topReleases = "Топ релизов"
    override val soundtracks = "Саундтреки"
    override val playlists2 = "Плейлисты"
    override val genres = "Жанры"
    override val searchTracks = "Поиск треков"
    override val searchArtists = "Поиск исполнителей"
    override val searchAlbums = "Поиск альбомов"
    override val discography = "Дискография"
    override val tracksSection = "Треки"
}
