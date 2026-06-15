package com.badmanners.murglar.lib.musify

import com.badmanners.murglar.lib.core.localization.DefaultMessages
import com.badmanners.murglar.lib.core.localization.MessageException
import com.badmanners.murglar.lib.core.localization.RussianMessages
import com.badmanners.murglar.lib.core.log.LoggerMiddleware
import com.badmanners.murglar.lib.core.model.node.Node
import com.badmanners.murglar.lib.core.model.track.source.Bitrate
import com.badmanners.murglar.lib.core.model.track.source.Container
import com.badmanners.murglar.lib.core.model.track.source.Extension
import com.badmanners.murglar.lib.core.model.track.source.Source
import com.badmanners.murglar.lib.core.network.NetworkMiddleware
import com.badmanners.murglar.lib.core.notification.NotificationMiddleware
import com.badmanners.murglar.lib.core.preference.Preference
import com.badmanners.murglar.lib.core.preference.PreferenceMiddleware
import com.badmanners.murglar.lib.core.service.BaseMurglar
import com.badmanners.murglar.lib.musify.api.MusifyApi
import com.badmanners.murglar.lib.musify.api.MusifyTrackRaw
import com.badmanners.murglar.lib.musify.localization.MusifyDefaultMessages
import com.badmanners.murglar.lib.musify.localization.MusifyMessages
import com.badmanners.murglar.lib.musify.localization.MusifyRussianMessages
import com.badmanners.murglar.lib.musify.login.MusifyLoginResolver
import com.badmanners.murglar.lib.musify.model.MusifyTrack
import com.badmanners.murglar.lib.musify.node.MusifyNodeResolver

class MusifyMurglar(
    id: String,
    preferences: PreferenceMiddleware,
    network: NetworkMiddleware,
    notifications: NotificationMiddleware,
    logger: LoggerMiddleware
) : BaseMurglar<MusifyTrack, MusifyMessages>(id, MESSAGES, preferences, network, notifications, logger) {

    companion object {
        val MESSAGES = mapOf(
            DefaultMessages.DEFAULT to MusifyDefaultMessages,
            RussianMessages.RUSSIAN to MusifyRussianMessages
        )
    }

    val api = MusifyApi(network, logger)

    override val possibleFormats = listOf(
        Extension.MP3 to Bitrate.B_320,
        Extension.MP3 to Bitrate.B_128
    )

    override val loginResolver = MusifyLoginResolver(messages)
    override val nodeResolver  = MusifyNodeResolver(this, messages)
    override val murglarPreferences: List<Preference> get() = emptyList()

    /**
     * Получаем реальный URL потока через /api/track/{id}/stream-url.
     * Source.url хранит https://musify.club/api/track/{id}/stream-url как маркер.
     * Здесь мы его резолвим в реальную ссылку на mp3 (с временным токеном).
     */
    override suspend fun resolveSourceForUrl(track: MusifyTrack, source: Source): Source {
        val streamUrl = api.getStreamUrl(track.id)
        return source.copy(url = streamUrl)
    }

    override suspend fun getTags(track: MusifyTrack, parent: Node?) =
        throw MessageException(messages.noTags)

    override suspend fun getTracksByMediaIds(mediaIds: List<String>): List<MusifyTrack> =
        mediaIds.mapNotNull { mediaId ->
            val parts     = mediaId.split(":", limit = 2)
            val trackId   = parts[0]
            val trackSlug = if (parts.size > 1) parts[1] else trackId
            try {
                val html   = api.get("${MusifyApi.BASE}/track/$trackSlug-$trackId")
                val tracks = api.parseTracks(html)
                tracks.firstOrNull { it.id == trackId }?.toMusifyTrack()
                    ?: tracks.firstOrNull()?.toMusifyTrack()
            } catch (_: Exception) { null }
        }

    fun MusifyTrackRaw.toMusifyTrack(): MusifyTrack = MusifyTrack(
        id            = id,
        title         = title,
        slug          = slug,
        artistIds     = artistIds,
        artistNames   = artistNames,
        albumId       = albumId.ifEmpty { null },
        albumName     = albumName.ifEmpty { null },
        albumSlug     = albumSlug.ifEmpty { null },
        durationMs    = durationSec * 1000L,
        bitrate       = bitrate,
        playCount     = playCount,
        sources       = listOf(source),
        smallCoverUrl = coverUrl.ifEmpty { null },
        bigCoverUrl   = coverUrl.ifEmpty { null },
        serviceUrl    = trackPageUrl
    )
}
