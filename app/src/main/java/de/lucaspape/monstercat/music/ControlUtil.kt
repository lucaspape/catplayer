package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import androidx.core.net.toUri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.util.Settings
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

        val disableAudioFocus = if (settings.getSetting("disableAudioFocus") != null) {
            settings.getSetting("disableAudioFocus")!!.toBoolean()
        } else {
            false
        }

        val primaryResolution = settings.getSetting("primaryCoverResolution")

        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer!!.stop()
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayerFactory.newSimpleInstance(contextReference!!.get()!!)
        exoPlayer.audioAttributes = audioAttributes

        exoPlayer.addListener(object:Player.EventListener{
            @Override
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                setPlayButtonImage(contextReference!!.get()!!)
            }
        })

        mediaPlayer = exoPlayer

        val requestResult = if (disableAudioFocus) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val audioManager =
                contextReference!!.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
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

                createSongNotification(song.title, song.version, song.artist, contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)

                startSeekBarUpdate()
            }
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

        hideTitle()

        setPlayButtonImage(contextReference!!.get()!!)

        mediaPlayer!!.stop()

        registerNextListener()

        val audioManager =
            contextReference!!.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(audioFocusChangeListener)
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
        createSongNotification(song.title, song.version, song.artist, contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)

        setPlayButtonImage(context)

        registerNextListener()

        val audioManager =
            contextReference!!.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(audioFocusChangeListener)
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

        if (!mediaPlayer!!.isPlaying) {
            val disableAudioFocus = if (settings.getSetting("disableAudioFocus") != null) {
                settings.getSetting("disableAudioFocus")!!.toBoolean()
            } else {
                false
            }

            val requestResult = if (disableAudioFocus) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                val audioManager =
                    contextReference!!.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }

            if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                // mediaPlayer!!.seekTo(length)
                mediaPlayer!!.playWhenReady = true


                val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
                context.registerReceiver(NoisyReceiver(), intentFilter)
            }

            createSongNotification(song.title, song.version, song.artist, contextReference!!.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution)

            setPlayButtonImage(context)

            registerNextListener()
        }

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
    if (mediaPlayer!!.isPlaying) {
        pause()
    } else {
        resume()
    }
}

private fun clearListener() {
    if (mediaPlayer != null) {
        mediaPlayer!!.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            }
        })
    }
}

private fun registerNextListener() {
    if (mediaPlayer != null) {
        mediaPlayer!!.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState) {
                    Player.STATE_ENDED -> next()
                }
            }
        })
    }
}