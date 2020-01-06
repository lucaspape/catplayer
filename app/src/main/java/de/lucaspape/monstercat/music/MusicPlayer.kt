package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ExoPlayerFactory
import de.lucaspape.monstercat.background.BackgroundService.Companion.loadContinuousSongListAsyncTask
import java.lang.IndexOutOfBoundsException
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

internal var contextReference: WeakReference<Context>? = null

internal var mediaPlayer: ExoPlayer? = null

internal var currentSong = 0
internal var playList = ArrayList<String>(1)

var shuffle = false
var loop = false
var loopSingle = false

private var sessionCreated = false

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
    context.get()?.let {
        if(!sessionCreated){
            mediaPlayer = ExoPlayerFactory.newSimpleInstance(context.get()!!)


            mediaSession = MediaSessionCompat.fromMediaSession(
                it,
                MediaSession(it, "de.lucaspape.monstercat.music")
            )

            mediaSession?.setCallback(MediaSessionCallback())

            mediaSession?.isActive = true

            sessionCreated = true

        }

        contextReference = context
    }
}

fun addSongList(songList:ArrayList<String>){
    for(songId in songList){
        try {
            playList.add(currentSong + 1, songId)
        } catch (e: IndexOutOfBoundsException) {
            playList.add(songId)
        }
    }

    if (mediaPlayer?.isPlaying == false) {
        playNow(songList[0])
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
            playList.add(currentSong + 1, songId)
        } catch (e: IndexOutOfBoundsException) {
            playList.add(songId)
        }
    }
}

/**
 * Play song now
 */
fun playNow(songId: String) {
    try {
        playList.add(currentSong + 1, songId)
        currentSong++

    } catch (e: IndexOutOfBoundsException) {
        playList.add(songId)
    }

    play()
}

fun clearContinuous() {
    loadContinuousSongListAsyncTask?.cancel(true)

    try{
        playList = ArrayList(playList.subList(0, currentSong))
        currentSong = playList.size
    }catch(e: IndexOutOfBoundsException){
        playList = ArrayList()
        currentSong = 0
    }

}

fun addContinuous(songId: String) {
    playList.add(songId)
}