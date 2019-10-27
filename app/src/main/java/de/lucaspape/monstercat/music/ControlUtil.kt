package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.settings.Settings
import java.io.File
import java.lang.IndexOutOfBoundsException

/**
 * Music control methods
 */
internal fun play() {
    clearListener()

    try {
        val song = playList[currentSong]

        val settings = Settings(contextReference!!.get()!!)
        val primaryResolution = settings.getSetting("primaryCoverResolution")

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer!!.stop()
        }

        mediaPlayer = ExoPlayerFactory.newSimpleInstance(contextReference!!.get()!!)

        registerNextListener()

        val connectivityManager =
            contextReference!!.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if ((wifi != null && wifi.isConnected) || settings.getSetting("streamOverMobile") == "true" || File(
                song.getUrl()
            ).exists()
        ) {

            if (!File(song.getUrl()).exists()) {
                mediaPlayer!!.prepare(
                    ProgressiveMediaSource.Factory(
                        DefaultDataSourceFactory(
                            contextReference!!.get()!!, Util.getUserAgent(
                                contextReference!!.get()!!, "MonstercatPlayer"
                            )
                        )
                    ).createMediaSource(song.getUrl().toUri())
                )
            } else {
                mediaPlayer!!.prepare(
                    ProgressiveMediaSource.Factory(
                        DefaultDataSourceFactory(
                            contextReference!!.get()!!, Util.getUserAgent(
                                contextReference!!.get()!!, "MonstercatPlayer"
                            )
                        )
                    ).createMediaSource(Uri.parse("file://" + song.getUrl()))
                )
            }

            mediaPlayer!!.playWhenReady = true

            setTitle(song.title, song.version, song.artist)

            startTextAnimation()

            setCover(song, contextReference!!.get()!!)

            setPlayButtonImage(contextReference!!.get()!!)

            showSongNotification(
                song.title,
                song.version,
                song.artist,
                contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution,
                true
            )

            playing = true

            startSeekBarUpdate()
        }

    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Stop playback
 */
internal fun stop() {
    if (mediaPlayer!!.isPlaying) {

        clearListener()

        playing = false

        hideTitle()

        setPlayButtonImage(contextReference!!.get()!!)

        mediaPlayer!!.stop()

        registerNextListener()
    }
}

/**
 * Pause playback
 */
fun pause() {
    clearListener()

    val context = contextReference!!.get()!!
    val settings = Settings(context)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    try {
        val song = playList[currentSong]

        val coverUrl = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

        mediaPlayer!!.playWhenReady = false
        showSongNotification(song.title, song.version, song.artist, coverUrl, false)

        playing = false
        paused = true

        setPlayButtonImage(context)

        registerNextListener()
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Resume playback
 */
fun resume() {
    clearListener()

    val context = contextReference!!.get()!!
    val settings = Settings(context)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    try {
        val song = playList[currentSong]

        val coverUrl = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

        if (paused) {
            // mediaPlayer!!.seekTo(length)
            mediaPlayer!!.playWhenReady = true

            paused = false

            val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(NoisyReceiver(), intentFilter)
        }

        showSongNotification(song.title, song.version, song.artist, coverUrl, true)

        playing = true

        setPlayButtonImage(context)

        registerNextListener()
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Next song
 */
fun next() {
    clearListener()

    currentSong++
    play()
}

/**
 * Previous song
 */
fun previous() {
    if (currentSong != 0) {
        clearListener()

        currentSong--

        play()
    }
}

/**
 * Play song now
 */
fun playNow(song: Song) {
    try {
        playList.add(currentSong + 1, song)
        currentSong++
    } catch (e: IndexOutOfBoundsException) {
        playList.add(song)
    }

    play()
}

/**
 * Toggle pause/play
 */
fun toggleMusic() {
    if (playing) {
        pause()
    } else {
        resume()
    }
}

private fun clearListener(){
    if(mediaPlayer != null){
        mediaPlayer!!.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            }
        })
    }
}

private fun registerNextListener(){
    if(mediaPlayer != null){
        mediaPlayer!!.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> next()
                }
            }
        })
    }
}