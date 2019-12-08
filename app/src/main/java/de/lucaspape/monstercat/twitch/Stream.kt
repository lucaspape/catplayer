package de.lucaspape.monstercat.twitch

import android.content.Context
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.database.Song
import de.lucaspape.monstercat.music.playNow
import de.lucaspape.monstercat.request.TwitchRequest
import org.json.JSONObject
import kotlin.math.floor

class Stream(private val clientId: String) {
    fun playStream(context: Context, channel: String) {
        getAccessToken(context, channel)
    }

    private fun getAccessToken(context: Context, channel: String) {
        val accessTokenRequest = TwitchRequest(Request.Method.GET,
            "https://api.twitch.tv/channels/$channel/access_token", clientId,
            Response.Listener { response ->
                val jsonObject = JSONObject(response)

                getPlaylist(context, channel, jsonObject)
            },
            Response.ErrorListener { }
        )

        val volleyQueue = Volley.newRequestQueue(context)
        volleyQueue.add(accessTokenRequest)
    }

    private fun getPlaylist(context: Context, channel: String, accessToken: JSONObject) {
        val player = "twitchweb"
        val token = accessToken.getString("token")
        val sig = accessToken.getString("sig")
        val allowAudioOnly = "true"
        val allowSource = "true"
        val type = "any"
        val p = floor(Math.random() * 99999) + 1

        val volleyQueue = Volley.newRequestQueue(context)

        val playlistRequest = TwitchRequest(Request.Method.GET,
            "https://usher.ttvnw.net/api/channel/hls/$channel.m3u8?player=$player&token=$token&sig=$sig&allow_audio_only=$allowAudioOnly&allow_source=$allowSource&type=$type&p=$p",
            clientId, Response.Listener { response ->
                val lines = response.lines()

                val artistTitleRequest = StringRequest(Request.Method.GET, context.getString(R.string.liveInfoUrl),
                    Response.Listener { artistTitleResponse ->
                        val jsonObject = JSONObject(artistTitleResponse)

                        val currentTitle = jsonObject.getString("title")
                        val currentArtist = jsonObject.getString("artist")

                        val song = Song(
                            0,
                            "0",
                            currentTitle,
                            "",
                            "",
                            currentArtist,
                            ""
                        )

                        song.streamLocation = lines[lines.size - 1]
                        song.hls = true

                        playNow(song)

                    },
                    Response.ErrorListener { error ->
                        println(error)
                    })

                volleyQueue.add(artistTitleRequest)

            }, Response.ErrorListener {

            })

        volleyQueue.add(playlistRequest)
    }
}