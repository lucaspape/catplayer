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
import de.lucaspape.util.Settings
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.log

//songId of song which is prepared in exoPlayer
var exoPlayerSongId = ""

//songId of song which is prepared in nextExoPlayer
var preparedExoPlayerSongId = ""

internal fun prepareSong(context: Context, songId: String, callback: () -> Unit) {
    if (preparedExoPlayerSongId != songId) {
        //new exoplayer
        preparedExoPlayer = SimpleExoPlayer.Builder(context).build()
        preparedExoPlayer?.audioAttributes =
            getAudioAttributes()

        SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
            if (song.playbackAllowed(context)
            ) {
                val mediaSource = song.getMediaSource()

                if (mediaSource != null) {
                    preparedExoPlayer?.prepare(mediaSource)
                    preparedExoPlayerSongId = song.songId
                } else {
                    displayInfo(context, context.getString(R.string.songNotPlayableError))
                }
            }

            callback()
            return
        }

        contextReference?.let { weakReference ->
            AddTrackToDbAsync(weakReference, songId, finishedCallback = { _, song ->
                if (song.playbackAllowed(context)
                ) {
                    val mediaSource = song.getMediaSource()

                    if (mediaSource != null) {
                        preparedExoPlayer?.prepare(mediaSource)
                        preparedExoPlayerSongId = song.songId
                    } else {
                        displayInfo(context, context.getString(R.string.songNotPlayableError))
                    }
                }

                callback()

            }, errorCallback = {
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        }
    }
}

internal fun playSong(
    context: Context,
    songId: String,
    showNotification: Boolean,
    requestAudioFocus: Boolean,
    playWhenReady: Boolean,
    progress: Long?
) {
    //cancel stream info updater if running
    streamInfoUpdateAsync?.cancel(true)

    //request audio focus if enabled
    val audioFocus = if (requestAudioFocus) {
        requestAudioFocus(context)
    } else {
        AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    if (audioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        //preparingDone callback, see below
        val preparingDone = {
            //prepared player to main player handover
            exoPlayer?.playWhenReady = false
            exoPlayer?.stop(true)
            exoPlayer = null

            exoPlayer =
                preparedExoPlayer

            exoPlayerSongId = songId

            //reset prepared
            preparedExoPlayer?.playWhenReady = false
            preparedExoPlayer = null
            preparedExoPlayerSongId = ""

            //set volume
            exoPlayer?.audioComponent?.volume = volume

            //set progress if not null (for session recovering)
            if (progress != null) {
                exoPlayer?.seekTo(progress)
            }

            //enable playback
            exoPlayer?.playWhenReady = playWhenReady

            //for play/pause button change and if song ended
            exoPlayer?.addListener(
                getPlayerListener(
                    context,
                    songId
                )
            )

            //show title, artist, cover
            SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
                //set title/artist
                title = "${song.title} ${song.version}"
                artist = song.artist

                //start seekbar updater
                runSeekBarUpdate(context, prepareNext = true, crossFade = true)

                //set cover
                setCover(
                    context,
                    song.albumId,
                    song.artistId
                ) {
                    //show notification
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
        
        //check if player needs to be prepared
        if (preparedExoPlayerSongId != songId) {
            prepareSong(context, songId) {
                preparingDone()
            }
        } else {
            preparingDone()
        }
    }
}

fun playStream(stream: Stream) {
    contextReference?.get()?.let { context ->
        streamInfoUpdateAsync?.cancel(true)

        val settings = Settings.getSettings(context)

        startPlayerService("")

        exoPlayer?.playWhenReady = false
        exoPlayer?.release()
        exoPlayer?.stop()
        exoPlayer = null

        //new exoplayer
        exoPlayer = SimpleExoPlayer.Builder(context).build()
        exoPlayer?.audioAttributes =
            getAudioAttributes()

        //for play/pause button change and if song ended
        exoPlayer?.addListener(
            getStreamPlayerListener(
                context
            )
        )

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

internal fun runSeekBarUpdate(context: Context, prepareNext: Boolean, crossFade: Boolean) {
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

            if (prepareNext) {
                if (timeLeft < duration / 2 && exoPlayer?.isPlaying == true) {
                    prepareSong(context, nextSongId) {}
                }
            }

            if (crossFade) {
                if (timeLeft < crossfade && exoPlayer?.isPlaying == true && nextSongId == preparedExoPlayerSongId) {
                    if (timeLeft >= 1) {
                        val crossVolume = 1 - log(
                            100 - ((crossfade.toFloat() - timeLeft) / crossfade * 100),
                            100.toFloat()
                        )

                        val higherVolume = crossVolume * volume
                        val lowerVolume = volume - higherVolume

                        if (higherVolume > 0.toFloat() && higherVolume.isFinite()) {
                            preparedExoPlayer?.audioComponent?.volume = higherVolume
                        }

                        if (lowerVolume > 0.toFloat() && lowerVolume.isFinite()) {
                            exoPlayer?.audioComponent?.volume = lowerVolume
                        }
                    }

                    preparedExoPlayer?.playWhenReady = true
                }
            }

            if (id == currentSeekBarUpdateHandlerId) {
                seekBarUpdateHandler.postDelayed(this, 100)
            }
        }
    }

    seekBarUpdateHandler.postDelayed(updateSeekBar, 100)
}