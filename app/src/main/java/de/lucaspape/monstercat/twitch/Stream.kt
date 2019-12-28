package de.lucaspape.monstercat.twitch

import android.content.Context
import androidx.core.net.toUri
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.TwitchRequest
import org.json.JSONObject
import kotlin.math.floor

class Stream(private val clientId: String) {

    var streamUrl = ""
    var titleArtistUpdateUrl = ""
    var albumCoverUpdateUrl = ""
    var artist = ""
    var title = ""

    fun getStreamInfo(context: Context, channel: String, finished: (stream: Stream) -> Unit) {
        getAccessToken(context, channel, finished)
    }

    private fun getAccessToken(
        context: Context,
        channel: String,
        finished: (stream: Stream) -> Unit
    ) {
        val accessTokenRequest = TwitchRequest(Request.Method.GET,
            context.getString(R.string.twitchApiUrl) + "channels/$channel/access_token", clientId,
            Response.Listener { response ->
                val jsonObject = JSONObject(response)

                getPlaylist(context, channel, jsonObject, finished)
            },
            Response.ErrorListener { }
        )

        val volleyQueue = Volley.newRequestQueue(context)
        volleyQueue.add(accessTokenRequest)
    }

    private fun getPlaylist(
        context: Context,
        channel: String,
        accessToken: JSONObject,
        finished: (stream: Stream) -> Unit
    ) {
        val player = "twitchweb"
        val token = accessToken.getString("token")
        val sig = accessToken.getString("sig")
        val allowAudioOnly = "true"
        val allowSource = "true"
        val type = "any"
        val p = floor(Math.random() * 99999) + 1

        val volleyQueue = Volley.newRequestQueue(context)

        val playlistRequest = TwitchRequest(Request.Method.GET,
            context.getString(R.string.twitchPlaylistUrl) + "$channel.m3u8?player=$player&token=$token&sig=$sig&allow_audio_only=$allowAudioOnly&allow_source=$allowSource&type=$type&p=$p",
            clientId, Response.Listener { response ->
                val lines = response.lines()
                streamUrl = lines[lines.size - 1]

                updateInfo(context, Volley.newRequestQueue(context), finished)

            }, Response.ErrorListener {

            })

        volleyQueue.add(playlistRequest)
    }

    fun updateInfo(
        context: Context,
        volleyQueue: RequestQueue,
        finished: (stream: Stream) -> Unit
    ) {
        val artistTitleRequest =
            StringRequest(Request.Method.GET,
                context.getString(R.string.liveInfoUrl) + "?key=" + context.getString(R.string.lucaspapeApiKey),
                Response.Listener { artistTitleResponse ->
                    val jsonObject = JSONObject(artistTitleResponse)

                    title = jsonObject.getString("title")
                    artist = jsonObject.getString("artist")

                    titleArtistUpdateUrl =
                        context.getString(R.string.liveInfoUrl) + "?key=" + context.getString(R.string.lucaspapeApiKey)
                    albumCoverUpdateUrl =
                        context.getString(R.string.liveCoverUrl) + "?key=" + context.getString(R.string.lucaspapeApiKey)

                },
                Response.ErrorListener { error ->
                    println(error)
                })

        volleyQueue.addRequestFinishedListener<Any> {
            finished(this)
        }

        volleyQueue.add(artistTitleRequest)
    }

    fun getMediaSource(context: Context): HlsMediaSource {
        return HlsMediaSource.Factory(
            DefaultDataSourceFactory(
                context, Util.getUserAgent(
                    context, context.getString(R.string.applicationName)
                )
            )
        ).createMediaSource(streamUrl.toUri())
    }
}