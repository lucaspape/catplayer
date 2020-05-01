package de.lucaspape.monstercat.music.util

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.stop

class MediaSessionCallback(private val context: Context) : MediaSessionCompat.Callback() {
    override fun onPause() {
        pause(context)
    }

    override fun onPlay() {
        resume(context)
    }

    override fun onSkipToNext() {
        next(context)
    }

    override fun onSkipToPrevious() {
        previous(context)
    }

    override fun onStop() {
        stop(context)
    }

    override fun onSeekTo(pos: Long) {
        exoPlayer?.seekTo(pos)
    }

    override fun onFastForward() {
        exoPlayer?.duration?.let {
            exoPlayer?.seekTo(it)
        }
    }

    override fun onRewind() {
        super.onRewind()
        exoPlayer?.seekTo(0)
    }
}