package de.lucaspape.monstercat.music

import android.content.Context
import android.media.AudioManager
import android.os.AsyncTask
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.background.BackgroundService
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addStreamDownloadSong
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

internal fun playSong(context: Context, song: Song) {
    BackgroundService.streamInfoUpdateAsync?.cancel(true)

    val settings = Settings(context)

    exoPlayer?.release()
    exoPlayer?.stop()

    //new exoplayer
    val newExoPlayer = SimpleExoPlayer.Builder(context).build()
    newExoPlayer.audioAttributes = getAudioAttributes()

    //for play/pause button change and if song ended
    newExoPlayer.addListener(getPlayerListener(context, song))

    exoPlayer = newExoPlayer

    if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true" || File(
                song.getUrl()
            ).exists()
        ) {
            val mediaSource = song.getMediaSource()

            if (mediaSource != null) {
                exoPlayer?.prepare(mediaSource)

                exoPlayer?.playWhenReady = true

                //UI stuff
                setTitle(song.title, song.version, song.artist)

                setCover(context, song.title, song.version, song.artist, song.albumId) {
                    setPlayButtonImage(context)
                    startSeekBarUpdate()
                }
            } else {
                displayInfo(context, context.getString(R.string.songNotPlayableError))
            }
        }
    }
}

fun playStream(stream: Stream) {
    contextReference?.get()?.let { context ->
        BackgroundService.streamInfoUpdateAsync?.cancel(true)

        val settings = Settings(context)

        startPlayerService()

        exoPlayer?.release()
        exoPlayer?.stop()

        //new exoplayer
        val newExoPlayer = SimpleExoPlayer.Builder(context).build()
        newExoPlayer.audioAttributes = getAudioAttributes()

        //for play/pause button change and if song ended
        newExoPlayer.addListener(getStreamPlayerListener(context))

        exoPlayer = newExoPlayer

        //request audio focus
        if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //only play stream if allowed
            if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true") {
                exoPlayer?.prepare(stream.getMediaSource(context))

                exoPlayer?.playWhenReady = true

                //UI stuff
                setTitle(stream.title, stream.version, stream.artist)

                setPlayButtonImage(context)
                startSeekBarUpdate()

                BackgroundService.streamInfoUpdateAsync = StreamInfoUpdateAsync(WeakReference(context), stream)
                BackgroundService.streamInfoUpdateAsync?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
    }
}

internal fun playNext() {
    contextReference?.get()?.let { context ->
        BackgroundService.streamInfoUpdateAsync?.cancel(true)
        startPlayerService()

        val songDatabaseHelper = SongDatabaseHelper(context)
        val song = songDatabaseHelper.getSong(context, nextSong())

        song?.let {
            val settings = Settings(context)
            val downloadStream = settings.getSetting("downloadStream")?.toBoolean()

            if (downloadStream == true) {
                val nextSongId = try {
                    playlist[playlistIndex + 1]
                } catch (e: IndexOutOfBoundsException) {
                    try {
                        songQueue[0]
                    } catch (e: IndexOutOfBoundsException) {
                        ""
                    }
                }

                val nextSong = songDatabaseHelper.getSong(context, nextSongId)

                preDownloadSongStream(context, song, nextSong) { song ->
                    playSong(
                        context,
                        song
                    )
                }

            } else {
                playSong(context, song)
            }
        }
    }
}

internal fun playPrevious() {
    contextReference?.get()?.let { context ->
        BackgroundService.streamInfoUpdateAsync?.cancel(true)
        startPlayerService()

        val songDatabaseHelper = SongDatabaseHelper(context)
        val song = songDatabaseHelper.getSong(context, previousSong())

        song?.let {
            val settings = Settings(context)
            val downloadStream = settings.getSetting("downloadStream")?.toBoolean()

            if (downloadStream == true) {
                val nextSongId = try {
                    playlist[playlistIndex + 1]
                } catch (e: IndexOutOfBoundsException) {
                    try {
                        songQueue[0]
                    } catch (e: IndexOutOfBoundsException) {
                        ""
                    }
                }

                val nextSong = songDatabaseHelper.getSong(context, nextSongId)

                preDownloadSongStream(context, song, nextSong) { song ->
                    playSong(
                        context,
                        song
                    )
                }
            } else {
                playSong(context, song)
            }
        }
    }
}

/**
 * Pre-downloads stream before playback
 */
private fun preDownloadSongStream(
    context: Context,
    song: Song,
    nextSong: Song?,
    currentSongFinished: (song: Song) -> Unit
) {
    if (song.isDownloadable) {
        if (!File(song.downloadLocation).exists() && !File(song.streamDownloadLocation).exists()) {
            addStreamDownloadSong(context, song.songId) {
                currentSongFinished(song)
            }
        } else {
            currentSongFinished(song)
        }
    } else {
        //No permission to download

        currentSongFinished(song)

        displayInfo(
            context,
            context.getString(R.string.errorDownloadNotAllowedFallbackStream)
        )
    }

    try {
        if (nextSong != null) {
            if (!File(nextSong.downloadLocation).exists() && !File(nextSong.streamDownloadLocation).exists()) {
                addStreamDownloadSong(context, nextSong.songId) {}
            }
        }
    } catch (e: IndexOutOfBoundsException) {

    }
}