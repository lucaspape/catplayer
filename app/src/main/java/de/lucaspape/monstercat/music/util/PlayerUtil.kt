package de.lucaspape.monstercat.music.util

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.util.Settings

fun getAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()
}

fun requestAudioFocus(context: Context): Int {
    val settings = Settings(context)

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
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()

            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            val audioManager =
                context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
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
    audioManager.abandonAudioFocus(audioFocusChangeListener)
}

fun getStreamPlayerListener(context: Context): Player.EventListener {
    return object : Player.EventListener {
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
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

fun getPlayerListener(context: Context, songId: String): Player.EventListener {
    return object : Player.EventListener {
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (listenerEnabled) {
                exoPlayer?.isPlaying?.let { isPlaying ->
                    playing = isPlaying
                }

                startPlayerService(songId)

                exoPlayer?.duration?.let { duration ->
                    exoPlayer?.currentPosition?.let { currentPosition ->
                        val timeLeft = duration - currentPosition

                        if (timeLeft < crossfade) {
                            nextExoPlayer?.playWhenReady = playWhenReady
                        }
                    }
                }

                if (playbackState == Player.STATE_ENDED) {
                    next()
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