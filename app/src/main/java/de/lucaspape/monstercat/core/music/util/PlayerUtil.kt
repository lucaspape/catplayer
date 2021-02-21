package de.lucaspape.monstercat.core.music.util

import android.content.Context
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.audio.AudioAttributes
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.startPlayerService
import de.lucaspape.monstercat.core.music.notification.updateNotification
import java.util.*

fun getAudioAttributes(): AudioAttributes {
    return AudioAttributes.Builder()
        .setUsage(C.USAGE_MEDIA)
        .setContentType(C.CONTENT_TYPE_MUSIC)
        .build()
}

var currentListenerId = ""

fun getPlayerListener(context: Context, songId: String, crossFade:Boolean): Player.EventListener {
    val id = UUID.randomUUID().toString()
    currentListenerId = id

    return object : Player.EventListener {
        @Override
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (currentListenerId == id) {
                exoPlayer?.isPlaying?.let { isPlaying ->
                    playing = isPlaying
                }

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
                    
                    val stream = StreamDatabaseHelper(context).getStream(songId)

                    if(stream == null){
                        runSeekBarUpdate(context, prepareNext = true, crossFade)
                    }else{
                        duration = 0
                        currentPosition = 0
                    }
                }
            }
        }
    }
}