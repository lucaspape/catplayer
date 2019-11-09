package de.lucaspape.monstercat.music

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
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

            val notificationServiceIntent = Intent(contextReference!!.get()!!, MusicNotificationService::class.java)
            notificationServiceIntent.putExtra("title", song.title)
            notificationServiceIntent.putExtra("version", song.version)
            notificationServiceIntent.putExtra("artist", song.artist)
            notificationServiceIntent.putExtra("coverLocation", contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)
            notificationServiceIntent.putExtra("playing", true.toString())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                contextReference!!.get()!!.startForegroundService(notificationServiceIntent)
            }else{
                contextReference!!.get()!!.startService(notificationServiceIntent)
            }

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

        mediaPlayer!!.playWhenReady = false

        val notificationServiceIntent = Intent(contextReference!!.get()!!, MusicNotificationService::class.java)
        notificationServiceIntent.putExtra("title", song.title)
        notificationServiceIntent.putExtra("version", song.version)
        notificationServiceIntent.putExtra("artist", song.artist)
        notificationServiceIntent.putExtra("coverLocation", contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)
        notificationServiceIntent.putExtra("playing", false.toString())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            contextReference!!.get()!!.startForegroundService(notificationServiceIntent)
        }else{
            contextReference!!.get()!!.startService(notificationServiceIntent)
        }

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

        if (paused) {
            // mediaPlayer!!.seekTo(length)
            mediaPlayer!!.playWhenReady = true

            paused = false

            val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(NoisyReceiver(), intentFilter)
        }

        val notificationServiceIntent = Intent(contextReference!!.get()!!, MusicNotificationService::class.java)
        notificationServiceIntent.putExtra("title", song.title)
        notificationServiceIntent.putExtra("version", song.version)
        notificationServiceIntent.putExtra("artist", song.artist)
        notificationServiceIntent.putExtra("coverLocation", contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)
        notificationServiceIntent.putExtra("playing", true.toString())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            contextReference!!.get()!!.startForegroundService(notificationServiceIntent)
        }else{
            contextReference!!.get()!!.startService(notificationServiceIntent)
        }

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