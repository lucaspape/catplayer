package de.lucaspape.monstercat.music.util

import android.support.v4.media.session.MediaSessionCompat
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.stop

class MediaSessionCallback : MediaSessionCompat.Callback() {
    override fun onPause() {
        pause()
    }

    override fun onPlay() {
        exoPlayer?.playWhenReady = true
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
        exoPlayer?.seekTo(pos)
    }

    override fun onFastForward() {
        exoPlayer?.seekTo(exoPlayer!!.duration)
    }

    override fun onRewind() {
        super.onRewind()
        exoPlayer?.seekTo(0)
    }
}