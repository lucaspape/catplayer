package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.ExoPlayerFactory
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
    try {
        val song = playList[currentSong]

        val settings = Settings(contextReference!!.get()!!)
        val primaryResolution = settings.getSetting("primaryCoverResolution")

        if(mediaPlayer != null){
            mediaPlayer!!.release()
            mediaPlayer!!.stop()
        }

        mediaPlayer = ExoPlayerFactory.newSimpleInstance(contextReference!!.get()!!)

        val connectivityManager =
            contextReference!!.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if((wifi != null && wifi.isConnected) || settings.getSetting("streamOverMobile") == "true" || File(song.getUrl()).exists()){

            if(File(song.getUrl()).exists()){
                mediaPlayer!!.prepare(ProgressiveMediaSource.Factory(DefaultDataSourceFactory(contextReference!!.get()!!, Util.getUserAgent(
                    contextReference!!.get()!!, "MonstercatPlayer"))).createMediaSource(song.getUrl().toUri()))
            }else{
                mediaPlayer!!.prepare(ProgressiveMediaSource.Factory(DefaultDataSourceFactory(contextReference!!.get()!!, Util.getUserAgent(
                    contextReference!!.get()!!, "MonstercatPlayer"))).createMediaSource(Uri.parse("file://" + song.getUrl())))
            }

            mediaPlayer!!.playWhenReady = true

            setTitle(song.title, song.version, song.artist)

            startTextAnimation()

            setCover(song)

            setPlayButtonImage()

            showSongNotification(
                song.title,
                song.version,
                song.artist,
                contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution,
                true
            )

            playing = true

            startSeekBarUpdate()

            preparing = false
        }

    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Stop playback
 */
internal fun stop() {
    if (mediaPlayer!!.isPlaying) {

        playing = false

        hideTitle()

        setPlayButtonImage()

        mediaPlayer!!.stop()
    }
}

/**
 * Pause playback
 */
fun pause() {
    val context = contextReference!!.get()!!
    val settings = Settings(context)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    try {
        val song = playList[currentSong]

        val coverUrl = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

        mediaPlayer!!.playWhenReady = false
        showSongNotification(song.title, song.artist, song.version, coverUrl, false)

        playing = false
        paused = true

        setPlayButtonImage()
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Resume playback
 */
fun resume() {
    if(!preparing){
        preparing = true

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
                preparing = false

                val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(NoisyReceiver(), intentFilter)
            } else {
                play()
            }

            showSongNotification(song.title, song.version, song.artist, coverUrl, true)

            playing = true

            setPlayButtonImage()
        } catch (e: IndexOutOfBoundsException) {

        }
    }
}

/**
 * Next song
 */
fun next() {
    if(!preparing){
        preparing = true
        currentSong++
        play()
    }
}

/**
 * Previous song
 */
fun previous() {
    if(!preparing){
        preparing = true
        if (currentSong != 0) {
            currentSong--

            play()
        }
    }
}

/**
 * Play song now
 */
fun playNow(song: Song) {
    if(!preparing){
        preparing = true

        try {
            playList.add(currentSong + 1, song)
            currentSong++
        } catch (e: IndexOutOfBoundsException) {
            playList.add(song)
        }

        play()
    }
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
