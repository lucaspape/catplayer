package de.lucaspape.monstercat.core.music.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.startPlayerService
import de.lucaspape.monstercat.core.music.notification.updateNotification
import de.lucaspape.monstercat.util.Listener
import java.util.*

fun getAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()
}

var currentListenerId = ""

var playerPositionChangedListeners = ArrayList<Listener>()

private fun runPositionChangedListeners(context: Context, songId: String) {
    val stream = StreamDatabaseHelper(context).getStream(songId)

    if(stream == null){
        try {
            val iterator = playerPositionChangedListeners.iterator()

            while (iterator.hasNext()) {
                val listener = iterator.next()

                listener.run()

                playerPositionChangedListeners.removeIf { it.removeOnCalled && it.listenerId == listener.listenerId }
            }
        } catch (e: ConcurrentModificationException) {

        }
    }else{
        duration = 0
        currentPosition = 0
    }
}

fun getPlayerListener(context: Context, songId: String): Player.EventListener {
    val id = UUID.randomUUID().toString()
    currentListenerId = id

    runPlayerLoop(context, songId)

    return object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (currentListenerId == id) {
                visiblePlaying = playWhenReady

                startPlayerService(context, songId)

                if (playbackState == Player.STATE_ENDED) {
                    currentListenerId = ""
                    next(context)
                } else {
                    setCover(
                        context,
                        songId
                    ) {
                        updateNotification(context, songId, it)
                    }
                }
            }
        }
    }
}

private var seekBarUpdateHandler = Handler(Looper.getMainLooper())
var currentSeekBarUpdateHandlerId = ""

var loadedLyricsId = ""
var loadingLyrics = false

private fun runPlayerLoop(context: Context, songId: String) {
    val id = UUID.randomUUID().toString()
    currentSeekBarUpdateHandlerId = id

    seekBarUpdateHandler = Handler(Looper.getMainLooper())

    val updateSeekBar = object : Runnable {
        override fun run() {
            runPositionChangedListeners(context, songId)

            if (id == currentSeekBarUpdateHandlerId) {
                seekBarUpdateHandler.postDelayed(this, 100)
            }
        }
    }

    seekBarUpdateHandler.postDelayed(updateSeekBar, 100)
}

