package com.vamps.music

import android.content.Intent
import android.media.audiofx.Equalizer
import android.os.Handler
import android.os.Looper
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var player: ExoPlayer? = null
    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private val sleepRunnable = Runnable {
        player?.pause()
        sleepEndsAtMs = 0L
    }

    companion object {
        @Volatile var instance: PlaybackService? = null
        @Volatile var equalizer: Equalizer? = null
        @Volatile var sleepEndsAtMs: Long = 0L
        @Volatile var stopAfterTrack: Boolean = false

        /** minutes > 0: pause after that many minutes; minutes < 0: pause at the end of the current track; 0: cancel. */
        fun setSleep(minutes: Int): Boolean {
            val service = instance ?: return false
            service.applySleep(minutes)
            return true
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        val attrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(attrs, true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exo.setHandleAudioBecomingNoisy(true)
        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                attachEqualizer(audioSessionId)
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM && stopAfterTrack) {
                    exo.pauseAtEndOfMediaItems = false
                    stopAfterTrack = false
                }
            }
        })
        player = exo
        mediaSession = MediaSession.Builder(this, exo).build()
        attachEqualizer(exo.audioSessionId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = mediaSession?.player
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    private fun applySleep(minutes: Int) {
        handler.removeCallbacks(sleepRunnable)
        player?.pauseAtEndOfMediaItems = false
        stopAfterTrack = false
        sleepEndsAtMs = 0L
        when {
            minutes > 0 -> {
                val ms = minutes * 60_000L
                sleepEndsAtMs = System.currentTimeMillis() + ms
                handler.postDelayed(sleepRunnable, ms)
            }
            minutes < 0 -> {
                stopAfterTrack = true
                player?.pauseAtEndOfMediaItems = true
            }
        }
    }

    private fun attachEqualizer(sessionId: Int) {
        runCatching { equalizer?.release() }
        equalizer = null
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return
        equalizer = runCatching {
            val eq = Equalizer(0, sessionId)
            val prefs = Prefs(this)
            val range = eq.bandLevelRange
            for (band in 0 until eq.numberOfBands.toInt()) {
                val saved = prefs.eqLevel(band) ?: continue
                eq.setBandLevel(band.toShort(), saved.coerceIn(range[0].toInt(), range[1].toInt()).toShort())
            }
            eq.setEnabled(prefs.eqEnabled)
            eq
        }.getOrNull()
    }

    override fun onDestroy() {
        handler.removeCallbacks(sleepRunnable)
        sleepEndsAtMs = 0L
        stopAfterTrack = false
        runCatching { equalizer?.release() }
        equalizer = null
        instance = null
        mediaSession?.release()
        mediaSession = null
        player?.release()
        player = null
        super.onDestroy()
    }
}
