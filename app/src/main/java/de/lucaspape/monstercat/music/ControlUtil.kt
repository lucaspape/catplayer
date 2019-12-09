package de.lucaspape.monstercat.music

import android.content.Context
import android.content.IntentFilter
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import androidx.core.net.toUri
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.activities.downloadTask
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.download.DownloadTask
import de.lucaspape.monstercat.twitch.Stream
import de.lucaspape.monstercat.util.Settings
import java.io.File
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference

var updateLiveInfoAsync: UpdateLiveInfoAsync? = null

/**
 * Music control methods
 */
internal fun play() {
    clearListener()

    try {
        val song = playList[currentSong]

        contextReference?.get()?.let { context ->
            val settings = Settings(context)

            val disableAudioFocus = if (settings.getSetting("disableAudioFocus") != null) {
                settings.getSetting("disableAudioFocus")!!.toBoolean()
            } else {
                false
            }

            val primaryResolution = settings.getSetting("primaryCoverResolution")

            mediaPlayer?.release()
            mediaPlayer?.stop()

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.CONTENT_TYPE_MUSIC)
                .build()

            val exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
            exoPlayer.audioAttributes = audioAttributes

            exoPlayer.addListener(object : Player.EventListener {
                @Override
                override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                    setPlayButtonImage(context)
                }
            })

            mediaPlayer = exoPlayer

            val requestResult = if (disableAudioFocus) {
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                val audioManager =
                    contextReference?.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                audioManager.requestAudioFocus(
                    audioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }

            if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                registerNextListener()

                val connectivityManager =
                    contextReference?.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

                if ((wifi != null && wifi.isConnected) || settings.getSetting("streamOverMobile") == "true" || File(
                        song.getUrl()
                    ).exists()
                ) {

                    if (!File(song.getUrl()).exists()) {
                        mediaPlayer?.prepare(
                            ProgressiveMediaSource.Factory(
                                DefaultDataSourceFactory(
                                    context, Util.getUserAgent(
                                        context, "MonstercatPlayer"
                                    )
                                )
                            ).createMediaSource(song.getUrl().toUri())
                        )

                    } else {
                        mediaPlayer?.prepare(
                            ProgressiveMediaSource.Factory(
                                DefaultDataSourceFactory(
                                    context, Util.getUserAgent(
                                        context, "MonstercatPlayer"
                                    )
                                )
                            ).createMediaSource(Uri.parse("file://" + song.getUrl()))
                        )
                    }

                    mediaPlayer?.playWhenReady = true

                    setTitle(song.title, song.version, song.artist)

                    startTextAnimation()

                    setCover(song, context)

                    setPlayButtonImage(context)

                    createSongNotification(
                        song.title,
                        song.version,
                        song.artist,
                        contextReference?.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution
                    )

                    startSeekBarUpdate()
                }

            }
        }
    } catch (e: IndexOutOfBoundsException) {
    }
}

fun playStream(stream: Stream) {
    clearListener()

    contextReference?.get()?.let { context ->
        val settings = Settings(context)

        val disableAudioFocus = if (settings.getSetting("disableAudioFocus") != null) {
            settings.getSetting("disableAudioFocus")!!.toBoolean()
        } else {
            false
        }

        mediaPlayer?.release()
        mediaPlayer?.stop()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayerFactory.newSimpleInstance(context)
        exoPlayer.audioAttributes = audioAttributes

        exoPlayer.addListener(object : Player.EventListener {
            @Override
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                setPlayButtonImage(context)
            }
        })

        mediaPlayer = exoPlayer

        val requestResult = if (disableAudioFocus) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            val audioManager =
                contextReference?.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        if (requestResult == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            registerNextListener()

            val connectivityManager =
                contextReference?.get()!!.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val wifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)

            if ((wifi != null && wifi.isConnected) || settings.getSetting("streamOverMobile") == "true") {

                mediaPlayer?.prepare(
                    HlsMediaSource.Factory(
                        DefaultDataSourceFactory(
                            context, Util.getUserAgent(
                                context, "MonstercatPlayer"
                            )
                        )
                    ).createMediaSource(stream.streamUrl.toUri())
                )

                mediaPlayer?.playWhenReady = true

                setTitle(stream.title, "", stream.artist)

                startTextAnimation()

                setPlayButtonImage(context)

                startSeekBarUpdate()

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
    if (mediaPlayer?.isPlaying == true) {

        clearListener()

        hideTitle()

        setPlayButtonImage(contextReference?.get()!!)

        mediaPlayer?.stop()

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

    contextReference?.get()?.let { context ->
        val settings = Settings(context)
        val primaryResolution = settings.getSetting("primaryCoverResolution")

        try {
            val song = playList[currentSong]

            mediaPlayer?.playWhenReady = false
            createSongNotification(
                song.title,
                song.version,
                song.artist,
                contextReference?.get()!!.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution
            )

            setPlayButtonImage(context)

            registerNextListener()

            val audioManager =
                contextReference?.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        } catch (e: IndexOutOfBoundsException) {

        }
    }
}

/**
 * Resume playback
 */
fun resume() {
    clearListener()

    contextReference?.get()?.let { context ->
        val settings = Settings(context)
        val primaryResolution = settings.getSetting("primaryCoverResolution")

        try {
            val song = playList[currentSong]

            if (mediaPlayer?.isPlaying == false) {
                val disableAudioFocus = if (settings.getSetting("disableAudioFocus") != null) {
                    settings.getSetting("disableAudioFocus")!!.toBoolean()
                } else {
                    false
                }

                val requestResult = if (disableAudioFocus) {
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                } else {
                    val audioManager =
                        contextReference?.get()!!.getSystemService(Context.AUDIO_SERVICE) as AudioManager
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

                createSongNotification(
                    song.title,
                    song.version,
                    song.artist,
                    context.filesDir.toString() + "/" + song.albumId + ".png" + primaryResolution
                )

                setPlayButtonImage(context)

                registerNextListener()
            }

        } catch (e: IndexOutOfBoundsException) {

        }
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
    if (mediaPlayer?.isPlaying == true) {
        pause()
    } else {
        resume()
    }
}

private fun clearListener() {
    mediaPlayer?.addListener(object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        }
    })
}

private fun registerNextListener() {
    mediaPlayer?.addListener(object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when (playbackState) {
                Player.STATE_ENDED -> next()
            }
        }
    })
}