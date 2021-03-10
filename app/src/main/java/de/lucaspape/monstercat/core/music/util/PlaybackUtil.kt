package de.lucaspape.monstercat.core.music.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.updateNotification
import de.lucaspape.monstercat.core.util.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.log

//songId of song which is prepared in exoPlayer
var exoPlayerSongId = ""

//songId of song which is prepared in nextExoPlayer
var preparedExoPlayerSongId = ""

private val scope = CoroutineScope(Dispatchers.Default)

fun prepareSong(
    context: Context,
    songId: String,
    callback: () -> Unit,
    notAllowedCallback: () -> Unit
) {
    if (preparedExoPlayerSongId != songId) {
        //new exoplayer
        preparedExoPlayer = SimpleExoPlayer.Builder(context).build()
        preparedExoPlayer?.setAudioAttributes(getAudioAttributes(), false)

        val song = SongDatabaseHelper(context).getSong(context, songId)
        val stream = StreamDatabaseHelper(context).getStream(songId)

        if (song != null && stream == null) {
            if (song.playbackAllowed(context)
            ) {
                song.getMediaSource(connectSid, cid) { mediaSource ->
                    if (mediaSource != null) {
                        preparedExoPlayer?.setMediaSource(mediaSource)
                        preparedExoPlayer?.prepare()
                        preparedExoPlayerSongId = song.songId
                    } else {
                        displayInfo(context, context.getString(R.string.songNotPlayableError))
                    }

                    callback()
                }

            } else {
                notAllowedCallback()
            }
        } else if (song != null && stream != null) {
            if (song.playbackAllowed(context)
            ) {
                scope.launch {
                    stream.getMediaSource(context) { mediaSource ->

                        scope.launch {
                            withContext(Dispatchers.Main) {
                                preparedExoPlayer?.setMediaSource(mediaSource)
                                preparedExoPlayer?.prepare()
                                preparedExoPlayerSongId = stream.name

                                callback()
                            }
                        }
                    }
                }
            } else {
                notAllowedCallback()
            }
        } else {
            displayInfo(context, context.getString(R.string.songNotPlayableError))
        }
    }
}

fun playSong(
    context: Context,
    songId: String,
    showNotification: Boolean,
    playWhenReady: Boolean,
    progress: Long?
) {
    visiblePlaying = playWhenReady

    //preparingDone callback, see below
    val preparingDone = {
        if (exoPlayerSongId != songId || (exoPlayerSongId == songId && preparedExoPlayer?.isPlaying == true)) {
            //prepared player to main player handover
            exoPlayer =
                preparedExoPlayer

            exoPlayerSongId = songId
        }

        val settings = Settings.getSettings(context)

        val disableAudioFocus =
            settings.getBoolean(context.getString(R.string.disableAudioFocusSetting))
        val audioFocus = disableAudioFocus == false || disableAudioFocus == null

        exoPlayer?.setAudioAttributes(getAudioAttributes(), audioFocus)

        //set volume
        exoPlayer?.audioComponent?.volume = volume

        //set progress if not null (for session recovering)
        if (progress != null) {
            exoPlayer?.seekTo(progress)
        } else if (exoPlayer?.isPlaying == false) {
            exoPlayer?.seekTo(0)
        }

        //reset prepared
        preparedExoPlayer = SimpleExoPlayer.Builder(context).build()
        preparedExoPlayerSongId = ""

        //enable playback
        exoPlayer?.playWhenReady = playWhenReady

        //for play/pause button change and if song ended
        exoPlayer?.addListener(
            getPlayerListener(
                context,
                songId,
                true
            )
        )

        //show title, artist, cover
        SongDatabaseHelper(context).getSong(context, songId)?.let { song ->
            //set title/artist
            title = song.shownTitle
            artist = song.artist

            //set cover
            setCover(
                context,
                song.albumId,
                song.artistId
            ) {
                //show notification
                if (showNotification) {
                    updateNotification(
                        context,
                        song.shownTitle,
                        song.artist,
                        it
                    )
                }
            }
        }
    }

    if (exoPlayerSongId != songId) {
        //check if player needs to be prepared
        if (preparedExoPlayerSongId != songId) {
            prepareSong(context, songId, {
                preparingDone()
            }, {
                displayInfo(context, context.getString(R.string.errorPlaybackNotAllowed))
            })
        } else {
            preparingDone()
        }
    } else {
        preparingDone()
    }
}

fun playStream(
    context: Context,
    streamName: String
) {
    prioritySongQueue.add(streamName)
    next(context)
    currentSeekBarUpdateHandlerId = ""
}

private var seekBarUpdateHandler = Handler(Looper.getMainLooper())
var currentSeekBarUpdateHandlerId = ""

fun runSeekBarUpdate(context: Context, prepareNext: Boolean, crossFade: Boolean) {
    val id = UUID.randomUUID().toString()
    currentSeekBarUpdateHandlerId = id

    seekBarUpdateHandler = Handler(Looper.getMainLooper())

    val updateSeekBar = object : Runnable {
        override fun run() {
            exoPlayer?.duration?.toInt()?.let {
                duration = it
            }

            exoPlayer?.currentPosition?.toInt()?.let {
                currentPosition = it
                setPlayerState(it.toLong())
            }

            //add current song to history after 30 seconds
            if(currentPosition > 30*1000){
                history.add(currentSongId)
            }

            if (crossFade) {
                val timeLeft = duration - currentPosition

                if (prepareNext) {
                    if (timeLeft < duration / 2 && exoPlayer?.isPlaying == true) {
                        if (nextSongId != "") {
                            prepareSong(context, nextSongId, {}, {})
                        } else if (playRelatedSongsAfterPlaylistFinished) {
                            loadRelatedSongs(context, playAfter = false)
                        }
                    }
                }

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
                } else if (exoPlayer?.isPlaying == false) {
                    preparedExoPlayer?.playWhenReady = false
                }
            }

            if (id == currentSeekBarUpdateHandlerId) {
                seekBarUpdateHandler.postDelayed(this, 100)
            }
        }
    }

    seekBarUpdateHandler.postDelayed(updateSeekBar, 100)
}