package de.lucaspape.monstercat.core.music.util

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.startPlayerService
import de.lucaspape.monstercat.core.music.notification.updateNotification
import de.lucaspape.monstercat.request.StreamInfoUpdateAsync
import de.lucaspape.util.Settings
import java.lang.ref.WeakReference
import java.util.*

fun getAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()
}

fun requestAudioFocus(context: Context): Int {
    val settings = Settings.getSettings(context)

    return if (settings.getBoolean(context.getString(R.string.disableAudioFocusSetting)) == true) {
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener(AudioFocusChangeListener.getAudioFocusChangeListener(
                    WeakReference(context)
                ))
                .build()

            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                AudioFocusChangeListener.getAudioFocusChangeListener(WeakReference(context)),
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
    }
}

fun abandonAudioFocus(context: Context) {
    val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    @Suppress("DEPRECATION")
    audioManager.abandonAudioFocus(AudioFocusChangeListener.audioFocusChangeListener)
}

var currentListenerId = ""

fun getStreamPlayerListener(context: Context): Player.EventListener {
    val id = UUID.randomUUID().toString()
    currentListenerId = id

    return object : Player.EventListener {
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (currentListenerId == id) {
                exoPlayer?.isPlaying?.let { isPlaying ->
                    playing = isPlaying
                }

                setCover(
                    context,
                    StreamInfoUpdateAsync.liveSongId
                ) { bitmap ->
                    updateNotification(
                        context,
                        StreamInfoUpdateAsync.liveSongId,
                        bitmap
                    )
                }
            }
        }
    }
}

fun getPlayerListener(context: Context, songId: String): Player.EventListener {
    val id = UUID.randomUUID().toString()
    currentListenerId = id

    return object : Player.EventListener {
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (currentListenerId == id) {
                exoPlayer?.isPlaying?.let { isPlaying ->
                    playing = isPlaying
                }

                startPlayerService(context, songId)

                exoPlayer?.duration?.let { duration ->
                    exoPlayer?.currentPosition?.let { currentPosition ->
                        val timeLeft = duration - currentPosition

                        if (timeLeft < crossfade && preparedExoPlayerSongId == nextSongId) {
                            preparedExoPlayer?.playWhenReady = playWhenReady
                        }
                    }
                }

                if (playbackState == Player.STATE_ENDED) {
                    currentListenerId = ""
                    next(context)
                } else {
                    setCover(
                        context,
                        songId
                    ) {
                        updateNotification(context, songId, it)
                    }

                    runSeekBarUpdate(context, prepareNext = true, crossFade = true)
                }
            }
        }
    }
}