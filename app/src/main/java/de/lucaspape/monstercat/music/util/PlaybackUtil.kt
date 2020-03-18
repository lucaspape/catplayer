package de.lucaspape.monstercat.music.util

import android.content.Context
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Handler
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
import java.util.*

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
    SongDatabaseHelper(context).getSong(
        context,
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
        title = "${song.title} ${song.version}"
        artist = song.artist

        setCover(
            context,
            song.albumId,
            song.artistId
        ) {
            setPlayButtonImage(context)
            runSeekBarUpdate(context, true)

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
                title = "${stream.title}, ${stream.version}"

                setPlayButtonImage(context)
                runSeekBarUpdate(context, false)

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

private var seekBarUpdateHandler = Handler()
private var currentSeekBarUpdateHandlerId = ""

internal fun runSeekBarUpdate(context: Context, crossFade: Boolean) {
    val id = UUID.randomUUID().toString()
    currentSeekBarUpdateHandlerId = id

    seekBarUpdateHandler = Handler()

    val updateSeekBar = object : Runnable {
        override fun run() {
            exoPlayer?.duration?.toInt()?.let {
                duration = it
            }

            exoPlayer?.currentPosition?.toInt()?.let {
                currentPosition = it
                setPlayerState(it.toLong())
            }

            if (crossFade) {
                val timeLeft = duration - currentPosition

                if (timeLeft < crossfade && exoPlayer?.isPlaying == true) {
                    if (timeLeft >= 1) {
                        val nextVolume: Float = (crossfade.toFloat() - timeLeft) / crossfade
                        nextExoPlayer?.audioComponent?.volume = nextVolume

                        val currentVolume = 1 - nextVolume
                        exoPlayer?.audioComponent?.volume = currentVolume
                    }

                    nextExoPlayer?.playWhenReady = true
                } else if (timeLeft < duration / 2 && exoPlayer?.isPlaying == true) {
                    prepareNextSong(
                        context
                    )
                }
            }

            if (id == currentSeekBarUpdateHandlerId) {
                seekBarUpdateHandler.postDelayed(this, 0)
            }
        }
    }

    seekBarUpdateHandler.postDelayed(updateSeekBar, 0)
}