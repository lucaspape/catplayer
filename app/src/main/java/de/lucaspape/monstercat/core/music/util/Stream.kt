package de.lucaspape.monstercat.core.music.util

import android.content.Context
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.getAuthorizedRequestQueue
import de.lucaspape.monstercat.request.newLoadLivestreamUrlRequest

class Stream {
    private fun getStreamUrl(
        context: Context,
        callback: (liveStreamUrl: String) -> Unit,
        errorCallback: () -> Unit
    ) {
        val requestQueue = getAuthorizedRequestQueue(context, context.getString(R.string.customApiBaseUrlSetting))
        
        val loadLiveStreamRequest = newLoadLivestreamUrlRequest(context, {
            callback(it.getString("monstercat"))
        }, {
            errorCallback()
        })
        
        if(loadLiveStreamRequest != null){
            requestQueue.add(loadLiveStreamRequest)
        }else{
            errorCallback()
        }
    }

    /**
     * Returns hlsMediaSource of twitch stream for ExoPlayer
     */
    fun getMediaSource(context: Context, callback: (mediaSource: HlsMediaSource) -> Unit) {
        getStreamUrl(context, {
            callback(HlsMediaSource.Factory(
                DefaultDataSourceFactory(
                    context, context.getString(R.string.livestreamUserAgent)
                )
            ).createMediaSource(MediaItem.fromUri(it.toUri())))
        }, {})
    }
}