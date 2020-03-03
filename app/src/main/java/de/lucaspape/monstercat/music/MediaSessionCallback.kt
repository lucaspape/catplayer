package de.lucaspape.monstercat.music

import android.support.v4.media.session.MediaSessionCompat
import de.lucaspape.monstercat.activities.musicPlayer

class MediaSessionCallback : MediaSessionCompat.Callback() {
    override fun onPause() {
        musicPlayer.pause()
    }

    override fun onPlay() {
        MusicPlayer.exoPlayer?.playWhenReady = true
    }

    override fun onSkipToNext() {
        musicPlayer.next()
    }

    override fun onSkipToPrevious() {
        musicPlayer.previous()
    }

    override fun onStop() {
        musicPlayer.stop()
    }

    override fun onSeekTo(pos: Long) {
        MusicPlayer.exoPlayer?.seekTo(pos)
    }

    override fun onFastForward() {
        MusicPlayer.exoPlayer?.seekTo(MusicPlayer.exoPlayer!!.duration)
    }

    override fun onRewind() {
        super.onRewind()
        MusicPlayer.exoPlayer?.seekTo(0)
    }
}