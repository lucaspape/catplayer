package de.lucaspape.monstercat.core.database.objects

import android.content.Context
import androidx.core.net.toUri
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import de.lucaspape.monstercat.R

data class Stream(
    val id: Int,
    val streamUrl: String,
    val coverUrl: String,
    val name: String
) {
    companion object {
        @JvmStatic
        val TABLE_NAME = "stream"

        @JvmStatic
        val COLUMN_ID = "id"

        @JvmStatic
        val COLUMN_STREAM_URL = "streamUrl"

        @JvmStatic
        val COLUMN_COVER_URL = "coverUrl"

        @JvmStatic
        val COLUMN_NAME = "name"

        @JvmStatic
        val CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    COLUMN_STREAM_URL + " TEXT," +
                    COLUMN_COVER_URL + " TEXT," +
                    COLUMN_NAME + " TEXT" +
                    ")"
    }
    
    fun playbackAllowed(context: Context):Boolean{
        return true
    }

    fun getMediaSource(context: Context, callback: (mediaSource: MediaSource) -> Unit) {
        callback(
            HlsMediaSource.Factory(
                DefaultDataSourceFactory(
                    context, context.getString(R.string.livestreamUserAgent)
                )
            ).createMediaSource(MediaItem.fromUri(streamUrl.toUri()))
        )
    }
}