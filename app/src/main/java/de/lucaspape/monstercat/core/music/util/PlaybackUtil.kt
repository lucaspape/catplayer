package de.lucaspape.monstercat.core.music.util

import android.content.Context
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.updateNotification
import de.lucaspape.monstercat.core.util.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

//songId of song which is prepared in exoPlayer
var exoPlayerSongId = ""

//songId of song which is prepared in nextExoPlayer
var preparedExoPlayerSongId = ""

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

        val song = SongDatabaseHelper(context).getSong(songId)
        val stream = StreamDatabaseHelper(context).getStream(songId)

        if (song != null && stream == null) {
            if (song.playbackAllowed(context)
            ) {
                song.getMediaSource(context, connectSid, cid) { mediaSource ->
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
                songId
            )
        )

        //show title, artist, cover
        SongDatabaseHelper(context).getSong(songId)?.let { song ->
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
    addToPriorityQueue(streamName)
    next(context)
    currentSeekBarUpdateHandlerId = ""
}