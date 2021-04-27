package de.lucaspape.monstercat.core.database.objects

import android.content.Context
import androidx.core.net.toUri
import com.android.volley.Request
import com.android.volley.toolbox.Volley
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import de.lucaspape.flavor.getYoutubeLivestreamUrl
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.TwitchJsonObjectRequest
import de.lucaspape.monstercat.request.TwitchStringRequest
import org.json.JSONObject

class Stream(
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

    private val channel = streamUrl.replace("https://twitch.tv/", "")
    private val clientId = "kimne78kx3ncx6brgo4mv6wki5h1ko"

    private val twitchApiUrl = "https://gql.twitch.tv/gql"
    private val twitchPlaylistUrl = "https://usher.ttvnw.net/api/channel/hls/"

    private fun getAccessToken(
        context: Context,
        finished: (accessToken: String, signature: String) -> Unit
    ) {
        val postObject = JSONObject()

        postObject.put("operationName", "PlaybackAccessToken")

        val extensionsObject = JSONObject()
        val persistedQueryObject = JSONObject()
        persistedQueryObject.put("version", 1)
        persistedQueryObject.put(
            "sha256Hash",
            "0828119ded1c13477966434e15800ff57ddacf13ba1911c129dc2200705b0712"
        )
        extensionsObject.put("persistedQuery", persistedQueryObject)

        postObject.put("extensions", extensionsObject)

        val variables = JSONObject()
        variables.put("isLive", true)
        variables.put("login", channel)
        variables.put("isVod", false)
        variables.put("vodID", channel)
        variables.put("playerType", "embed")


        postObject.put("variables", variables)

        val accessTokenRequest = TwitchJsonObjectRequest(
            Request.Method.POST,
            twitchApiUrl,
            postObject,
            clientId,
            context.getString(R.string.livestreamUserAgent),
            { response ->
                val accessToken =
                    response.getJSONObject("data").getJSONObject("streamPlaybackAccessToken")
                        .getString("value")
                val signature =
                    response.getJSONObject("data").getJSONObject("streamPlaybackAccessToken")
                        .getString("signature")
                finished(accessToken, signature)
            },
            {
            }
        )

        val volleyQueue = Volley.newRequestQueue(context)
        volleyQueue.add(accessTokenRequest)
    }

    private fun getStreamUrl(
        context: Context,
        token: String,
        sig: String,
        wantResolution: String,
        finished: (streamUrl: String) -> Unit
    ) {
        val volleyQueue = Volley.newRequestQueue(context)

        val playlistRequest = TwitchStringRequest(Request.Method.GET,
            "$twitchPlaylistUrl$channel.m3u8?client_id=$clientId&token=$token&sig=$sig&allow_source=true&allow_audio_only=true",
            context.getString(R.string.livestreamUserAgent), {
                val lines = it.lines()
                
                val resolutions = ArrayList<String>()
                val urls = ArrayList<String>()

                for(i in(4..lines.size step 3)){
                    resolutions.add(lines[i-2].split("NAME=\"")[1].split("\"")[0])
                    urls.add(lines[i])
                }

                for(i in resolutions.indices){
                    if(resolutions[i] == wantResolution){
                        finished(urls[i])
                        
                        break
                    }
                }

            }, {

            })

        volleyQueue.add(playlistRequest)
    }

    suspend fun getMediaSource(context: Context, callback: (mediaSource: MediaSource) -> Unit) {
        if (streamUrl.contains("twitch")) {
            getAccessToken(context) { token, sig ->
                getStreamUrl(context, token, sig, "720p") {
                    callback(
                        HlsMediaSource.Factory(
                            DefaultDataSourceFactory(
                                context, context.getString(R.string.livestreamUserAgent)
                            )
                        ).createMediaSource(MediaItem.fromUri(it.toUri()))
                    )
                }
            }
        } else if (streamUrl.contains("youtube")) {
            getYoutubeLivestreamUrl(context, name) {
                callback(
                    HlsMediaSource.Factory(
                        DefaultDataSourceFactory(
                            context, context.getString(R.string.livestreamUserAgent)
                        )
                    ).createMediaSource(MediaItem.fromUri(it.toUri()))
                )
            }
        }
    }
}