package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.ConnectivityManager
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.settings.Settings
import java.io.File
import java.io.FileInputStream
import java.lang.IndexOutOfBoundsException

/**
 * Music control methods
 */
internal fun play() {
    try {
        val song = playList[currentSong]

        val settings = Settings(contextReference!!.get()!!)
        val url = song.getUrl()

        mediaPlayer.stop()
        mediaPlayer = MediaPlayer()

        val connectivityManager =
            contextReference!!.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

        if (!File(url).exists()) {
            if (wifi != null && !wifi.isConnected && settings.getSetting("streamOverMobile") != "true") {
                //TODO msg
                println("DONT STREAM")
                return
            } else {
                mediaPlayer.setDataSource(url)
            }
        } else {
            val fis = FileInputStream(File(url))
            mediaPlayer.setDataSource(fis.fd)
        }

        //Prepare the player
        mediaPlayer.prepareAsync()

        //if mediaPlayer is finished preparing
        mediaPlayer.setOnPreparedListener {


            mediaPlayer.start()

            playing = true


            setTitle(song.title)

            startTextAnimation()

            mediaPlayer.setOnCompletionListener {
                next()
            }

            startSeekBarUpdate()

            setCover(song)

            setPlayButtonImage()

            showSongNotification(song.title, song.artist, song.coverUrl, true)

        }
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Stop playback
 */
internal fun stop() {
    playing = false

    setTitle("")

    setPlayButtonImage()

    mediaPlayer.stop()
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

        val title = song.title
        val artist = song.artist
        val coverUrl = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

        mediaPlayer.pause()
        showSongNotification(title, artist, coverUrl, false)

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
    val context = contextReference!!.get()!!
    val settings = Settings(context)
    val primaryResolution = settings.getSetting("primaryCoverResolution")

    try {
        val song = playList[currentSong]

        val title = song.title
        val artist = song.artist
        val coverUrl = context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution

        val length = mediaPlayer.currentPosition

        if (paused) {
            mediaPlayer.seekTo(length)
            mediaPlayer.start()

            paused = false

            val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(NoisyReceiver(), intentFilter)
        } else {
            play()
        }

        showSongNotification(title, artist, coverUrl, true)

        playing = true

        setPlayButtonImage()
    } catch (e: IndexOutOfBoundsException) {

    }
}

/**
 * Next song
 */
fun next() {
    currentSong++
    play()

}

/**
 * Previous song
 */
fun previous() {
    if (currentSong != 0) {
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