package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import de.lucaspape.monstercat.activities.loadContinuousSongListAsyncTask
import de.lucaspape.monstercat.database.Song
import java.lang.IndexOutOfBoundsException
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

internal var contextReference: WeakReference<Context>? = null

internal var mediaPlayer: ExoPlayer? = null

internal var currentSong = 0
internal var playList = ArrayList<Song>(1)

var mediaSession: MediaSessionCompat? = null

val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener {
    pause()
}

/**
 * Checks if headphones unplugged
 * TODO unRegisterReceiver
 */
class NoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            pause()
        }
    }
}

/**
 * Create mediaSession and listen for callbacks (pause, play buttons on headphones etc.)
 */
fun createMediaSession(context: WeakReference<Context>) {
    mediaPlayer = ExoPlayerFactory.newSimpleInstance(context.get()!!)

    context.get()?.let {
        mediaSession = MediaSessionCompat.fromMediaSession(
            it,
            MediaSession(it, "de.lucaspape.monstercat.music")
        )

        mediaSession?.setCallback(MediaSessionCallback())

        mediaSession?.isActive = true
        contextReference = context
    }
}

/**
 * Play song after
 */
fun addSong(song: Song) {
    if (mediaPlayer?.isPlaying == false) {
        playNow(song)
    } else {
        try {
            playList.add(currentSong + 1, song)
        } catch (e: IndexOutOfBoundsException) {
            playList.add(song)
        }
    }
}

fun clearContinuous() {
    loadContinuousSongListAsyncTask?.cancel(true)

    playList = ArrayList(playList.subList(0, currentSong))
    currentSong = playList.size
}

fun addContinuous(song: Song) {
    playList.add(song)
}

