package de.lucaspape.monstercat.twitch

import android.content.Context
import androidx.core.net.toUri
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.Volley
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.request.TwitchRequest
import de.lucaspape.monstercat.util.newRequestQueue
import org.json.JSONObject
import kotlin.math.floor

/**
 * Play twitch streams
 */
class Stream(private val clientId: String, private val channel: String) {
    private fun getAccessToken(
        context: Context,
        finished: (token: String, sig: String) -> Unit
    ) {
        val accessTokenRequest = TwitchRequest(Request.Method.GET,
            context.getString(R.string.twitchApiUrl) + "channels/$channel/access_token", clientId,
            Response.Listener { response ->
                val jsonObject = JSONObject(response)

                val token = jsonObject.getString("token")
                val sig = jsonObject.getString("sig")

                finished(token, sig)
            },
            Response.ErrorListener { }
        )

        val volleyQueue = newRequestQueue(context)
        volleyQueue.add(accessTokenRequest)
    }

    private fun getStreamUrl(
        context: Context,
        token: String,
        sig: String,
        videoName:String,
        finished: (streamUrl: String) -> Unit
    ) {
        val player = "twitchweb"
        val allowAudioOnly = "true"
        val allowSource = "true"
        val type = "any"
        val p = floor(Math.random() * 99999) + 1

        val volleyQueue = newRequestQueue(context)

        val playlistRequest = TwitchRequest(Request.Method.GET,
            context.getString(R.string.twitchPlaylistUrl) + "$channel.m3u8?player=$player&token=$token&sig=$sig&allow_audio_only=$allowAudioOnly&allow_source=$allowSource&type=$type&p=$p",
            clientId, Response.Listener { response ->
                val lines = response.lines()

                for(line in lines){
                    if(line.contains("NAME=\"$videoName\"", ignoreCase = true)){
                        val streamUrl = lines[lines.indexOf(line)+2]

                        finished(streamUrl)

                        break
                    }
                }
            }, Response.ErrorListener {

            })

        volleyQueue.add(playlistRequest)
    }

    fun getMediaSource(context: Context, callback: (mediaSource: HlsMediaSource) -> Unit) {
        getAccessToken(context) { token, sig ->
            getStreamUrl(context, token, sig, "audio_only") { streamUrl ->
                callback(
                    HlsMediaSource.Factory(
                        DefaultDataSourceFactory(
                            context, Util.getUserAgent(
                                context, context.getString(R.string.applicationName)
                            )
                        )
                    ).createMediaSource(streamUrl.toUri())
                )
            }
        }
    }
}