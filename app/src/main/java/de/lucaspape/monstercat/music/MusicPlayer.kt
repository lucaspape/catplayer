package de.lucaspape.monstercat.music

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.session.MediaSession
import android.support.v4.media.session.MediaSessionCompat
import de.lucaspape.monstercat.activities.loadContinuousSongListAsyncTask
import de.lucaspape.monstercat.database.Song
import java.lang.NullPointerException
import java.lang.ref.WeakReference
import kotlin.collections.ArrayList

var contextReference: WeakReference<Context>? = null

var mediaPlayer = MediaPlayer()
var currentSong = 0
var currentContinuousPoint = 0
var playList = ArrayList<Song>(1)
var playing = false
var paused = false

var mediaSession: MediaSessionCompat? = null

/**
 * Checks if headphones unplugged
 * TODO unRegisterReceiver
 */
class NoisyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent!!.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
            pause()
        }
    }
}

/**
 * Create mediaSession and listen for callbacks (pause, play buttons on headphones etc.)
 */
fun createMediaSession(context: WeakReference<Context>) {
    mediaSession = MediaSessionCompat.fromMediaSession(
        context.get()!!,
        MediaSession(context.get()!!, "de.lucaspape.monstercat.music")
    )

    mediaSession!!.setCallback(object : MediaSessionCompat.Callback() {

        override fun onPause() {
            pause()
        }

        override fun onPlay() {
            if (paused) {
                resume()
            } else {
                play()
            }
        }

        override fun onSkipToNext() {
            next()
        }

        override fun onSkipToPrevious() {
            previous()
        }

        override fun onStop() {
            stop()
        }

        override fun onSeekTo(pos: Long) {
            mediaPlayer.seekTo(pos.toInt())
        }

        override fun onFastForward() {
            mediaPlayer.seekTo(mediaPlayer.duration)
        }

        override fun onRewind() {
            super.onRewind()
            mediaPlayer.seekTo(0)
        }

    })

    mediaSession!!.isActive = true
}

/**
 * Play song after
 */
fun addSong(song: Song) {
    playList.add(currentContinuousPoint, song)
    currentContinuousPoint++
}

fun clearContinuous() {
    try {
        loadContinuousSongListAsyncTask!!.cancel(true)
    } catch (e: NullPointerException) {

    }

    playList = ArrayList(playList.subList(0, currentContinuousPoint))
    currentSong = playList.size
    currentContinuousPoint = 0
}

fun addContinuous(song: Song) {
    playList.add(song)
}

