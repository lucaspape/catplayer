package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import de.lucaspape.monstercat.background.BackgroundService.Companion.loadContinuousSongListAsyncTask
import java.lang.ref.WeakReference

class MonstercatPlayer {
    companion object {
        @JvmStatic
        var contextReference: WeakReference<Context>? = null

        @JvmStatic
        var playlist = ArrayList<String>()
            internal set

        @JvmStatic
        var randomAlreadyPlayed = ArrayList<String?>()

        @JvmStatic
        var currentSong = 0
            internal set

        @JvmStatic
        var shuffle = false
        @JvmStatic
        var loop = false
        @JvmStatic
        var loopSingle = false

        @JvmStatic
        private var sessionCreated = false
        @JvmStatic
        var mediaSession: MediaSessionCompat? = null
            internal set
        @JvmStatic
        internal var mediaPlayer: ExoPlayer? = null

        @JvmStatic
        val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
            pause()
        }

        class NoisyReceiver : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                    pause()
                }
            }
        }
    }

    /**
     * Create mediaSession and listen for callbacks (pause, play buttons on headphones etc.)
     */
    fun createMediaSession() {
        contextReference?.get()?.let {
            if (!sessionCreated) {
                mediaPlayer = SimpleExoPlayer.Builder(it).build()


                mediaSession = MediaSessionCompat.fromMediaSession(
                    it,
                    MediaSession(it, "de.lucaspape.monstercat.music")
                )

                mediaSession?.setCallback(MediaSessionCallback())

                mediaSession?.isActive = true

                sessionCreated = true

            }
        }
    }

    /**
     * Play song after
     */
    fun addSong(songId: String) {
        if (mediaPlayer?.isPlaying == false) {
            playNow(songId)
        } else {
            try {
                playlist.add(currentSong + 1, songId)
            } catch (e: IndexOutOfBoundsException) {
                playlist.add(songId)
            }
        }
    }

    /**
     * Play song now
     */
    fun playNow(songId: String) {
        try {
            playlist.add(currentSong + 1, songId)
            currentSong++

        } catch (e: IndexOutOfBoundsException) {
            playlist.add(songId)
        }

        play()
    }

    fun clearContinuous() {
        loadContinuousSongListAsyncTask?.cancel(true)

        try {
            playlist = ArrayList(playlist.subList(0, currentSong))
            currentSong = playlist.size
        } catch (e: IndexOutOfBoundsException) {
            playlist = ArrayList()
            currentSong = 0
        }

    }

    fun addContinuous(songId: String) {
        playlist.add(songId)
    }
}

