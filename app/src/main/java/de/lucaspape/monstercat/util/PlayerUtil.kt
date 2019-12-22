package de.lucaspape.monstercat.util

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.music.audioFocusChangeListener
import de.lucaspape.monstercat.music.contextReference
import de.lucaspape.monstercat.twitch.Stream
import java.io.File

fun getAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()
}

fun requestAudioFocus(context: Context):Int{
    val settings = Settings(context)

    val disableAudioFocus = if (settings.getSetting("disableAudioFocus") != null) {
        settings.getSetting("disableAudioFocus")!!.toBoolean()
    } else {
        false
    }

    return if (disableAudioFocus) {
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    } else {
        val audioManager =
            contextReference?.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(
            audioFocusChangeListener,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
    }
}

fun abandonAudioFocus(context: Context){
    val audioManager =
       context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    audioManager.abandonAudioFocus(audioFocusChangeListener)
}

fun songToMediaSource(context: Context, song: Song): ProgressiveMediaSource {
    if (!File(song.getUrl()).exists()) {

        return ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(
                context, Util.getUserAgent(
                    context, "MonstercatPlayer"
                )
            )
        ).createMediaSource(song.getUrl().toUri())


    } else {
        return ProgressiveMediaSource.Factory(
            DefaultDataSourceFactory(
                context, Util.getUserAgent(
                    context, "MonstercatPlayer"
                )
            )
        ).createMediaSource(Uri.parse("file://" + song.getUrl()))
    }
}

fun streamToMediaSource(context: Context, stream: Stream): HlsMediaSource {
    return HlsMediaSource.Factory(
        DefaultDataSourceFactory(
            context, Util.getUserAgent(
                context, "MonstercatPlayer"
            )
        )
    ).createMediaSource(stream.streamUrl.toUri())
}