package de.lucaspape.monstercat.music.util

import android.content.Context
import android.media.AudioManager
import android.os.AsyncTask
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.objects.Song
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.contextReference
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import java.lang.ref.WeakReference

private var preparedSong = ""

internal fun prepareSong(context: Context, song: Song) {
    if (preparedSong != song.songId) {
        //new exoplayer
        val newExoPlayer = SimpleExoPlayer.Builder(context).build()
        newExoPlayer.audioAttributes =
            getAudioAttributes()

        nextExoPlayer = newExoPlayer

        if (song.playbackAllowed(context)
        ) {
            val mediaSource = song.getMediaSource()

            if (mediaSource != null) {
                nextExoPlayer?.prepare(mediaSource)
                preparedSong = song.songId
            } else {
                displayInfo(context, context.getString(R.string.songNotPlayableError))
            }
        }
    }
}

internal fun prepareNextSong(context: Context) {
    SongDatabaseHelper(context).getSong(context,
        getNextSong()
    )?.let { song ->
        prepareSong(context, song)
    }
}

internal fun playSong(
    context: Context,
    song: Song,
    showNotification: Boolean,
    requestAudioFocus: Boolean,
    playWhenReady: Boolean,
    progress: Long?
) {
    streamInfoUpdateAsync?.cancel(true)

    val audioFocus = if (requestAudioFocus) {
        requestAudioFocus(context)
    } else {
        null
    }

    if (audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED || !requestAudioFocus) {
        exoPlayer?.release()
        exoPlayer?.stop()

        if (preparedSong != song.songId) {
            prepareSong(context, song)
        }

        exoPlayer =
            nextExoPlayer

        preparedSong = ""

        exoPlayer?.audioComponent?.volume = 1.0f

        if (progress != null) {
            exoPlayer?.seekTo(progress)
        }

        //for play/pause button change and if song ended
        exoPlayer?.addListener(
            getPlayerListener(
                context,
                song
            )
        )

        exoPlayer?.playWhenReady = playWhenReady

        if (playWhenReady) {
            listenerEnabled = true
        }

        //UI stuff
        setTitle(
            song.title,
            song.version,
            song.artist
        )

        setCover(
            context,
            song.title,
            song.version,
            song.artist,
            song.artistId,
            song.albumId
        ) {
            setPlayButtonImage(context)
            startSeekBarUpdate(true)

            if (showNotification) {
                updateNotification(
                    song.title,
                    song.version,
                    song.artist,
                    it
                )
            }
        }
    }
}

fun playStream(stream: Stream) {
    contextReference?.get()?.let { context ->
        streamInfoUpdateAsync?.cancel(true)

        val settings = Settings(context)

        startPlayerService("")

        exoPlayer?.release()
        exoPlayer?.stop()

        //new exoplayer
        val newExoPlayer = SimpleExoPlayer.Builder(context).build()
        newExoPlayer.audioAttributes =
            getAudioAttributes()

        //for play/pause button change and if song ended
        newExoPlayer.addListener(
            getStreamPlayerListener(
                context
            )
        )

        exoPlayer = newExoPlayer

        //request audio focus
        if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //only play stream if allowed
            if (wifiConnected(context) == true || settings.getBoolean(context.getString(R.string.streamOverMobileSetting)) == true) {
                exoPlayer?.prepare(stream.getMediaSource(context))

                exoPlayer?.playWhenReady = true

                //UI stuff
                setTitle(
                    stream.title,
                    stream.version,
                    stream.artist
                )

                setPlayButtonImage(context)
                startSeekBarUpdate(false)

                streamInfoUpdateAsync =
                    StreamInfoUpdateAsync(
                        WeakReference(context),
                        stream
                    )
                streamInfoUpdateAsync?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
    }
}