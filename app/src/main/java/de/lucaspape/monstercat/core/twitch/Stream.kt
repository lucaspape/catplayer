package de.lucaspape.monstercat.core.twitch

import android.content.Context
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.R

/**
 * Play twitch streams
 */
class Stream {
    /**
     * Returns hlsMediaSource of twitch stream for ExoPlayer
     */
    fun getMediaSource(context: Context, callback: (mediaSource: HlsMediaSource) -> Unit) {
        val streamUrl = context.getString(R.string.liveStreamUrl)
        
        callback(
            HlsMediaSource.Factory(
                DefaultDataSourceFactory(
                    context, Util.getUserAgent(
                        context, context.getString(R.string.applicationName)
                    )
                )
            ).createMediaSource(MediaItem.fromUri(streamUrl.toUri()))
        )
    }
}