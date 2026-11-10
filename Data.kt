package com.vamps.music

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import java.util.Locale

private val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")

internal data class Track(
    val id: Long,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val addedAt: Long,
) {
    fun toMediaItem(): MediaItem {
        val art = if (albumId > 0L) ContentUris.withAppendedId(ALBUM_ART_BASE, albumId) else null
        val meta = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(art)
            .build()
        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(uri)
            .setMediaMetadata(meta)
            .build()
    }
}

/** Snapshot of the real state reported by the player. Nothing here is simulated. */
internal data class NowPlaying(
    val hasItem: Boolean = false,
    val mediaId: String = "",
    val title: String = "",
    val artist: String = "",
    val playing: Boolean = false,
    val buffering: Boolean = false,
    val shuffle: Boolean = false,
    val repeat: Int = Player.REPEAT_MODE_OFF,
    val queueSize: Int = 0,
    val index: Int = 0,
) {
    companion object {
        fun from(p: Player): NowPlaying {
            val item = p.currentMediaItem
            return NowPlaying(
                hasItem = item != null,
                mediaId = item?.mediaId.orEmpty(),
                title = item?.mediaMetadata?.title?.toString().orEmpty(),
                artist = item?.mediaMetadata?.artist?.toString().orEmpty(),
                playing = p.isPlaying,
                buffering = p.playbackState == Player.STATE_BUFFERING,
                shuffle = p.shuffleModeEnabled,
                repeat = p.repeatMode,
                queueSize = p.mediaItemCount,
                index = p.currentMediaItemIndex,
            )
        }
    }
}

internal data class Notice(
    val title: String,
    val body: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null,
)

internal enum class LibMode(val label: String) { ALL("Songs"), LIKED("Favorites"), ALBUMS("Albums"), ARTISTS("Artists") }

internal enum class SearchMode(val label: String) { ALL("All"), SONGS("Songs"), ARTISTS("Artists"), ALBUMS("Albums") }

/** Returns null when the MediaStore query itself fails, so the UI can say so instead of showing an empty library. */
internal fun loadAudio(context: Context): List<Track>? = runCatching {
    val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED,
    )
    val out = ArrayList<Track>()
    context.contentResolver.query(
        collection,
        projection,
        "${MediaStore.Audio.Media.IS_MUSIC} != 0",
        null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC",
    )?.use { c ->
        val iId = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val iTitle = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val iArtist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val iAlbum = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
        val iAlbumId = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val iDuration = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val iAdded = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        while (c.moveToNext()) {
            val id = c.getLong(iId)
            out += Track(
                id = id,
                uri = ContentUris.withAppendedId(collection, id),
                title = c.getString(iTitle)?.takeIf { it.isNotBlank() } ?: "Unknown track",
                artist = c.getString(iArtist)?.takeUnless { it == "<unknown>" || it.isBlank() } ?: "Unknown artist",
                album = c.getString(iAlbum)?.takeUnless { it == "<unknown>" || it.isBlank() } ?: "Unknown album",
                albumId = c.getLong(iAlbumId),
                duration = c.getLong(iDuration),
                addedAt = c.getLong(iAdded),
            )
        }
    }
    out
}.getOrNull()

/** Local, on-device preferences. No account data, no network. */
internal class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("vamps_prefs", Context.MODE_PRIVATE)

    fun likedIds(): Set<Long> =
        (sp.getStringSet("liked", null) ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet()

    fun saveLiked(ids: Set<Long>) {
        sp.edit().putStringSet("liked", ids.map { it.toString() }.toSet()).apply()
    }

    fun recent(): List<Long> =
        (sp.getString("recent", "") ?: "").split(',').mapNotNull { it.toLongOrNull() }

    fun pushRecent(id: Long) {
        val next = (listOf(id) + recent().filter { it != id }).take(30)
        sp.edit().putString("recent", next.joinToString(",")).apply()
    }

    fun clearRecent() {
        sp.edit().remove("recent").apply()
    }

    var eqEnabled: Boolean
        get() = sp.getBoolean("eq_on", false)
        set(value) {
            sp.edit().putBoolean("eq_on", value).apply()
        }

    fun eqLevel(band: Int): Int? = if (sp.contains("eq_$band")) sp.getInt("eq_$band", 0) else null

    fun setEqLevel(band: Int, level: Int) {
        sp.edit().putInt("eq_$band", level).apply()
    }
}

internal fun formatTime(ms: Long): String {
    val s = ms.coerceAtLeast(0L) / 1000L
    return String.format(Locale.getDefault(), "%d:%02d", s / 60L, s % 60L)
}

internal fun openProvider(ctx: Context, pkg: String) {
    val launch = ctx.packageManager.getLaunchIntentForPackage(pkg)
    val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))
    val ok = runCatching { ctx.startActivity(intent) }.isSuccess
    if (!ok) {
        runCatching {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
        }
    }
}

internal fun openAppSettings(ctx: Context) {
    runCatching {
        ctx.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)),
        )
    }
}

internal fun filterTracks(all: List<Track>, rawQuery: String, mode: SearchMode): List<Track> {
    val q = rawQuery.trim()
    if (q.isEmpty()) return emptyList()
    return all.filter { t ->
        when (mode) {
            SearchMode.ALL -> t.title.contains(q, true) || t.artist.contains(q, true) || t.album.contains(q, true)
            SearchMode.SONGS -> t.title.contains(q, true)
            SearchMode.ARTISTS -> t.artist.contains(q, true)
            SearchMode.ALBUMS -> t.album.contains(q, true)
        }
    }
}
