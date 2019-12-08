package de.lucaspape.monstercat.twitch

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
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
            "https://api.twitch.tv/channels/$channel/access_token", clientId,
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

        val playlistRequest = TwitchRequest(Request.Method.GET,
            "https://usher.ttvnw.net/api/channel/hls/$channel.m3u8?player=$player&token=$token&sig=$sig&allow_audio_only=$allowAudioOnly&allow_source=$allowSource&type=$type&p=$p",
            clientId, Response.Listener { response ->
                val lines = response.lines()
                streamUrl = lines[lines.size - 1]

                updateInfo(context, finished)

            }, Response.ErrorListener {

            })

        val volleyQueue = Volley.newRequestQueue(context)
        volleyQueue.add(playlistRequest)
    }

    fun updateInfo(context: Context, finished: (stream: Stream) -> Unit) {
        val artistTitleRequest =
            StringRequest(Request.Method.GET, context.getString(R.string.liveInfoUrl),
                Response.Listener { artistTitleResponse ->
                    val jsonObject = JSONObject(artistTitleResponse)

                    title = jsonObject.getString("title")
                    artist = jsonObject.getString("artist")

                    titleArtistUpdateUrl = context.getString(R.string.liveInfoUrl)
                    albumCoverUpdateUrl = context.getString(R.string.liveCoverUrl)

                    finished(this)

                },
                Response.ErrorListener { error ->
                    println(error)
                })

        val volleyQueue = Volley.newRequestQueue(context)
        volleyQueue.add(artistTitleRequest)
    }
}