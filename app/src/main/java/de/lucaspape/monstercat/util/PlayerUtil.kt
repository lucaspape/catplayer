package de.lucaspape.monstercat.util

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.music.setCover
import de.lucaspape.monstercat.music.setPlayButtonImage
import de.lucaspape.monstercat.music.startSeekBarUpdate

fun getAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()
}

fun requestAudioFocus(context: Context): Int {
    val settings = Settings(context)

    val disableAudioFocus = if (settings.getSetting("disableAudioFocus") != null) {
        settings.getSetting("disableAudioFocus")!!.toBoolean()
    } else {
        false
    }

    return if (disableAudioFocus) {
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

fun getStreamPlayerListener(context:Context): Player.EventListener{
    return object: Player.EventListener{
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            setPlayButtonImage(context)

            setCover(
                context,
                StreamInfoUpdateAsync.liveTitle,
                StreamInfoUpdateAsync.liveVersion,
                StreamInfoUpdateAsync.liveArtist,
                StreamInfoUpdateAsync.liveAlbumId
            ) { bitmap ->
                updateNotification(
                    StreamInfoUpdateAsync.liveTitle,
                    StreamInfoUpdateAsync.liveVersion,
                    StreamInfoUpdateAsync.liveArtist,
                    bitmap
                )
            }
        }
    }
}

fun getPlayerListener(context:Context, song: Song): Player.EventListener{
    return object: Player.EventListener{
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                next()
            }else{
                setCover(context, song.title, song.version, song.artist, song.albumId) {
                    updateNotification(
                        song.title,
                        song.version,
                        song.artist,
                        it
                    )
                }

                setPlayButtonImage(context)
                startSeekBarUpdate()
            }
        }
    }
}