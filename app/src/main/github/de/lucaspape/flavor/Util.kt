package de.lucaspape.flavor

import android.view.View
import com.github.kiulian.downloader.YoutubeDownloader
import de.lucaspape.monstercat.core.database.helper.StreamDatabaseHelper
import de.lucaspape.monstercat.core.music.util.playStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun playYoutubeLivestream(view: View, streamName: String){
    withContext(Dispatchers.Default){
        val streamDatabaseHelper = StreamDatabaseHelper(view.context)
        val stream = streamDatabaseHelper.getStream(streamName)

        stream?.let {
            val videoId = stream.streamUrl.replace("https://www.youtube.com/watch?v=", "")

            val youtubeDownloader = YoutubeDownloader()
            val video = youtubeDownloader.getVideo(videoId)

            val url = video.details().liveUrl()

            streamDatabaseHelper.deleteStream(streamName)
            streamDatabaseHelper.insertStream(url, "", streamName)

            withContext(Dispatchers.Main){
                playStream(view.context, streamName)
            }
        }
    }
}