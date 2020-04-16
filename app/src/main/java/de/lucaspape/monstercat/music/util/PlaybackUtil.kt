package de.lucaspape.monstercat.music.util

import android.content.Context
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Handler
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.contextReference
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.request.async.AddTrackToDbAsync
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import java.lang.ref.WeakReference
import java.util.*

var currentSong = ""
var preparedSong = ""

internal fun prepareSong(context: Context, songId: String) {
    if (preparedSong != songId) {
        //new exoplayer
        val newExoPlayer = SimpleExoPlayer.Builder(context).build()
        newExoPlayer.audioAttributes =
            getAudioAttributes()

        nextExoPlayer = newExoPlayer

        SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
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

            return
        }

        val syncObject = Object()

        contextReference?.let { weakReference ->
            AddTrackToDbAsync(weakReference, songId, finishedCallback = {_, song ->
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

                synchronized(syncObject){
                    syncObject.notify()
                }
            }, errorCallback = {
                synchronized(syncObject){
                    syncObject.notify()
                }
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

            synchronized(syncObject){
                syncObject.wait()
            }
        }
    }
}

internal fun playSong(
    context: Context,
    songId:String,
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
        if(currentSong != songId){
            exoPlayer?.release()
            exoPlayer?.stop()

            if (preparedSong != songId) {
                prepareSong(context, songId)
            }

            exoPlayer =
                nextExoPlayer

            preparedSong = ""

            exoPlayer?.audioComponent?.volume = 1.0f
        }

        if (progress != null) {
            exoPlayer?.seekTo(progress)
        }

        exoPlayer?.playWhenReady = playWhenReady

        //for play/pause button change and if song ended
        exoPlayer?.addListener(
            getPlayerListener(
                context,
                songId
            )
        )

        currentSong = songId

        SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
            //UI stuff
            title = "${song.title} ${song.version}"
            artist = song.artist

            setCover(
                context,
                song.albumId,
                song.artistId
            ) {
                runSeekBarUpdate(context, prepareNext = true, crossFade = true)

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
                streamInfoUpdateAsync =
                    StreamInfoUpdateAsync(
                        WeakReference(context)
                    )
                streamInfoUpdateAsync?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

                stream.getMediaSource(context) { mediaSource ->
                    exoPlayer?.prepare(mediaSource)

                    exoPlayer?.playWhenReady = true

                    currentSeekBarUpdateHandlerId = ""
                    
                    duration = 0
                    currentPosition = 0
                    setPlayerState(0.toLong())
                }
            }
        }
    }
}

private var seekBarUpdateHandler = Handler()
private var currentSeekBarUpdateHandlerId = ""

internal fun runSeekBarUpdate(context: Context, prepareNext:Boolean, crossFade: Boolean) {
    val id = UUID.randomUUID().toString()
    currentSeekBarUpdateHandlerId = id

    seekBarUpdateHandler = Handler()

    val updateSeekBar = object : Runnable {
        override fun run() {
            exoPlayer?.isPlaying?.let { isPlaying ->
                playing = isPlaying
            }

            exoPlayer?.duration?.toInt()?.let {
                duration = it
            }

            exoPlayer?.currentPosition?.toInt()?.let {
                currentPosition = it
                setPlayerState(it.toLong())
            }

            val timeLeft = duration - currentPosition

            if(prepareNext){
                if (timeLeft < duration / 2 && exoPlayer?.isPlaying == true) {
                    prepareSong(context, getNextSongId())
                }
            }

            if (crossFade) {
                if (timeLeft < crossfade && exoPlayer?.isPlaying == true) {
                    if (timeLeft >= 1) {
                        val nextVolume: Float = (crossfade.toFloat() - timeLeft) / crossfade
                        nextExoPlayer?.audioComponent?.volume = nextVolume

                        val currentVolume = 1 - nextVolume
                        exoPlayer?.audioComponent?.volume = currentVolume
                    }

                    nextExoPlayer?.playWhenReady = true
                }
            }

            if (id == currentSeekBarUpdateHandlerId) {
                seekBarUpdateHandler.postDelayed(this, 100)
            }
        }
    }

    seekBarUpdateHandler.postDelayed(updateSeekBar, 100)
}