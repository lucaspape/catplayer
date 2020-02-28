package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.os.AsyncTask
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.background.BackgroundService.Companion.streamInfoUpdateAsync
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.download.addStreamDownloadSong
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.stopPlayerService
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference
import kotlin.random.Random

/**
 * Get current song and play it
 */
internal fun play() {
    //start the player service (with its notification)
    streamInfoUpdateAsync?.cancel(true)
    startPlayerService()

    val song = getCurrentSong()

    song?.let {
        MonstercatPlayer.contextReference?.get()?.let { context ->
            val settings = Settings(context)
            val downloadStream = settings.getSetting("downloadStream")?.toBoolean()

            //This downloads the stream before playing it (and it pre-downloads the next stream)
            if (downloadStream == true) {
                preDownloadSongStream(
                    context,
                    song,
                    getSong(MonstercatPlayer.currentSong + 1)
                ) { finishedSong ->
                    playSong(context, finishedSong)
                }
            } else {
                //Stream normally
                playSong(context, song)
            }
        }
    }
}

/**
 * Play specific stream
 */
fun playStream(stream: Stream) {
    MonstercatPlayer.contextReference?.get()?.let { context ->
        streamInfoUpdateAsync?.cancel(true)

        val settings = Settings(context)

        startPlayerService()

        MonstercatPlayer.mediaPlayer?.release()
        MonstercatPlayer.mediaPlayer?.stop()

        //new exoplayer
        val exoPlayer = SimpleExoPlayer.Builder(context).build()
        exoPlayer.audioAttributes = getAudioAttributes()

        //for play/pause button change
        exoPlayer.addListener(getStreamPlayerListener(context))

        MonstercatPlayer.mediaPlayer = exoPlayer

        //request audio focus
        if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //only play stream if allowed
            if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true") {
                MonstercatPlayer.mediaPlayer?.prepare(stream.getMediaSource(context))

                MonstercatPlayer.mediaPlayer?.playWhenReady = true

                //UI stuff
                setTitle(stream.title, stream.version, stream.artist)

                setPlayButtonImage(context)
                startSeekBarUpdate()

                streamInfoUpdateAsync = StreamInfoUpdateAsync(WeakReference(context), stream)
                streamInfoUpdateAsync?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }
    }
}

/**
 * Stop playback
 */
internal fun stop() {
    if (MonstercatPlayer.mediaPlayer?.isPlaying == true) {

        hideTitle()

        setPlayButtonImage(MonstercatPlayer.contextReference?.get()!!)

        MonstercatPlayer.mediaPlayer?.stop()

        MonstercatPlayer.contextReference?.get()?.let { context ->
            abandonAudioFocus(context)
        }
    }

    stopPlayerService()
}

/**
 * Pause playback
 */
fun pause() {
    MonstercatPlayer.contextReference?.get()?.let { context ->
        try {
            MonstercatPlayer.mediaPlayer?.playWhenReady = false

            setPlayButtonImage(context)

            abandonAudioFocus(context)

        } catch (e: IndexOutOfBoundsException) {
        }
    }
}

/**
 * Resume playback
 */
fun resume() {
    MonstercatPlayer.contextReference?.get()?.let { context ->
        try {
            if (MonstercatPlayer.mediaPlayer?.isPlaying == false) {
                if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    MonstercatPlayer.mediaPlayer!!.playWhenReady = true

                    val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                    context.registerReceiver(
                        MonstercatPlayer.Companion.NoisyReceiver(),
                        intentFilter
                    )
                }

                setPlayButtonImage(context)

                startPlayerService()
            }

        } catch (e: IndexOutOfBoundsException) {

        }
    }
}

/**
 * Next song
 */
fun next() {
    if (!MonstercatPlayer.loopSingle) {
        if (MonstercatPlayer.shuffle) {
            MonstercatPlayer.currentSong = Random.nextInt(0, MonstercatPlayer.playlist.size + 1)
        } else {
            MonstercatPlayer.currentSong++
        }
    }

    play()
}

/**
 * Previous song
 */
fun previous() {
    if (MonstercatPlayer.currentSong != 0) {
        MonstercatPlayer.currentSong--

        play()
    }
}

/**
 * Toggle pause/play
 */
fun toggleMusic() {
    if (MonstercatPlayer.mediaPlayer?.isPlaying == true) {
        pause()
    } else {
        resume()
    }
}

/**
 * Play specific song
 */
private fun playSong(context: Context, song: Song) {
    streamInfoUpdateAsync?.cancel(true)
    val settings = Settings(context)

    MonstercatPlayer.mediaPlayer?.release()
    MonstercatPlayer.mediaPlayer?.stop()

    //new exoplayer
    val exoPlayer = SimpleExoPlayer.Builder(context).build()
    exoPlayer.audioAttributes = getAudioAttributes()

    //for play/pause button change and if song ended
    exoPlayer.addListener(getPlayerListener(context, song))

    MonstercatPlayer.mediaPlayer = exoPlayer

    //request audio focus
    if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        //dont stream if not allowed
        if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true" || File(
                song.getUrl()
            ).exists()
        ) {

            val mediaSource = song.getMediaSource()

            if (mediaSource != null) {
                MonstercatPlayer.mediaPlayer?.prepare(mediaSource)

                MonstercatPlayer.mediaPlayer?.playWhenReady = true

                //UI stuff
                setTitle(song.title, song.version, song.artist)

                setCover(context, song.title, song.version, song.artist, song.albumId) {
                    setPlayButtonImage(context)
                    startSeekBarUpdate()
                }
            } else {
                //TODO msg
                displayInfo(context, "You cannot play this song")
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