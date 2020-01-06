package de.lucaspape.monstercat.music

import android.support.v4.media.session.MediaSessionCompat
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.mediaPlayer

class MediaSessionCallback : MediaSessionCompat.Callback() {
    override fun onPause() {
        pause()
    }

    override fun onPlay() {
        mediaPlayer?.playWhenReady = true
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
        mediaPlayer?.seekTo(pos)
    }

    override fun onFastForward() {
        mediaPlayer?.seekTo(mediaPlayer!!.duration)
    }

    override fun onRewind() {
        super.onRewind()
        mediaPlayer?.seekTo(0)
    }
}