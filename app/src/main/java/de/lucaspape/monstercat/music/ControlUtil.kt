package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.os.AsyncTask
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import de.lucaspape.monstercat.background.BackgroundService.Companion.updateLiveInfoAsync
import de.lucaspape.monstercat.background.BackgroundService.Companion.waitForDownloadTask
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.database.helper.SongDatabaseHelper
import de.lucaspape.monstercat.download.addDownloadSong
import de.lucaspape.monstercat.music.notification.startPlayerService
import de.lucaspape.monstercat.music.notification.stopPlayerService
import de.lucaspape.monstercat.music.notification.updateNotification
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
    updateLiveInfoAsync?.cancel(true)

    //start the player service (with its notification)
    startPlayerService()

    val song = getCurrentSong()

    song?.let {
        MonstercatPlayer.contextReference?.get()?.let { context ->
            val settings = Settings(context)
            val downloadStream = settings.getSetting("downloadStream")?.toBoolean()

            //This downloads the stream before playing it (and it pre-downloads the next stream)
            if (downloadStream == true) {
                if (song.isDownloadable) {
                    if (!File(song.downloadLocation).exists() && !File(song.streamDownloadLocation).exists()) {
                        addDownloadSong(
                            song.streamLocation,
                            song.streamDownloadLocation,
                            song.shownTitle
                        )
                    }

                    waitForDownloadTask?.cancel(true)

                    waitForDownloadTask = object : AsyncTask<Void, Void, String>() {
                        override fun doInBackground(vararg params: Void?): String? {
                            if (!File(song.downloadLocation).exists()) {
                                while (!File(song.streamDownloadLocation).exists()) {
                                    Thread.sleep(100)
                                }
                            }

                            return null
                        }

                        override fun onPostExecute(result: String?) {
                            playSong(context, song)

                            try {
                                val nextSong = getSong(MonstercatPlayer.currentSong + 1)

                                if(nextSong != null){
                                    if (!File(nextSong.downloadLocation).exists() && !File(nextSong.streamDownloadLocation).exists()) {
                                        addDownloadSong(
                                            nextSong.streamLocation,
                                            nextSong.streamDownloadLocation,
                                            nextSong.shownTitle
                                        )
                                    }
                                }

                            } catch (e: IndexOutOfBoundsException) {

                            }
                        }

                    }

                    waitForDownloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                } else {
                    //No permission to download

                    playSong(context, song)
                    displayInfo(
                        context,
                        context.getString(R.string.errorDownloadNotAllowedFallbackStream)
                    )
                }

            } else {
                //Stream normally

                playSong(context, song)
            }

        }
    }
}

/**
 * Play specific song
 */
private fun playSong(context: Context, song: Song) {
    val settings = Settings(context)

    val primaryResolution = settings.getSetting("primaryCoverResolution")

    MonstercatPlayer.mediaPlayer?.release()
    MonstercatPlayer.mediaPlayer?.stop()

    //new exoplayer
    val exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
    exoPlayer.audioAttributes = getAudioAttributes()

    //for play/pause button change
    exoPlayer.addListener(object : Player.EventListener {
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            setCover(song, context)
            setPlayButtonImage(context)
            startSeekBarUpdate()

            updateNotification(
                song.title,
                song.version,
                song.artist,
                MonstercatPlayer.contextReference?.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution
            )
        }
    })

    MonstercatPlayer.mediaPlayer = exoPlayer

    //request audio focus
    if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        //if song ended play next
        registerNextListener()

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
                startTextAnimation()
                setCover(song, context)
                setPlayButtonImage(context)
                startSeekBarUpdate()
            } else {
                //TODO msg
                displayInfo(context, "You cannot play this song")
            }
        }
    }
}

/**
 * Play specific stream
 */
fun playStream(stream: Stream) {
    MonstercatPlayer.contextReference?.get()?.let { context ->
        val settings = Settings(context)

        startPlayerService()

        MonstercatPlayer.mediaPlayer?.release()
        MonstercatPlayer.mediaPlayer?.stop()

        //new exoplayer
        val exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        exoPlayer.audioAttributes = getAudioAttributes()

        //for play/pause button change
        exoPlayer.addListener(object : Player.EventListener {
            @Override
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                setPlayButtonImage(context)

                updateNotification(
                    UpdateLiveInfoAsync.previousTitle,
                    "",
                    UpdateLiveInfoAsync.previousArtist,
                    context.filesDir.toString() + "/live.png"
                )
            }
        })

        MonstercatPlayer.mediaPlayer = exoPlayer

        //request audio focus
        if (requestAudioFocus(context) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            //only play stream if allowed
            if (wifiConnected(context) == true || settings.getSetting("streamOverMobile") == "true") {
                MonstercatPlayer.mediaPlayer?.prepare(stream.getMediaSource(context))

                MonstercatPlayer.mediaPlayer?.playWhenReady = true

                //UI stuff
                setTitle(stream.title, "", stream.artist)
                startTextAnimation()
                setPlayButtonImage(context)
                startSeekBarUpdate()

                //this will update the title and artist of the stream in the background (from an api)
                updateLiveInfoAsync?.cancel(true)

                updateLiveInfoAsync = UpdateLiveInfoAsync(WeakReference(context), stream)
                updateLiveInfoAsync?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
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

        registerNextListener()

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

            registerNextListener()

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
                    context.registerReceiver(MonstercatPlayer.Companion.NoisyReceiver(), intentFilter)
                }

                setPlayButtonImage(context)

                registerNextListener()

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
        if(MonstercatPlayer.shuffle){
            MonstercatPlayer.currentSong = Random.nextInt(0,MonstercatPlayer.playlist.size + 1)
        }else{
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
 * Returns current song from playlist
 */
fun getCurrentSong(): Song? {
    try {
        return getSong(MonstercatPlayer.currentSong)
    } catch (e: IndexOutOfBoundsException) {
        return if(MonstercatPlayer.loop){
            MonstercatPlayer.currentSong = 0

            return try{
                getSong(MonstercatPlayer.currentSong)
            }catch (e: IndexOutOfBoundsException){
                null
            }
        }else{
            null
        }
    }
}

fun getSong(index:Int):Song?{
    val songId = MonstercatPlayer.playlist[index]

    MonstercatPlayer.contextReference?.get()?.let {context ->
        val songDatabaseHelper = SongDatabaseHelper(context)

        return songDatabaseHelper.getSong(context, songId)
    }

    return null
}

private fun registerNextListener() {
    var nextRun = false

    MonstercatPlayer.mediaPlayer?.addListener(object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if(playbackState == Player.STATE_ENDED){
                if(!nextRun){
                    nextRun = true
                    next()
                }
            }
        }
    })
}