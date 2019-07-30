package de.lucaspape.monstercat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.SimpleAdapter
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import org.json.JSONObject
import com.android.volley.ParseError
import com.android.volley.toolbox.*
import java.io.UnsupportedEncodingException

class PlaylistFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_playlist, container, false)

    companion object {
        fun newInstance(): PlaylistFragment = PlaylistFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val playlistView = view.findViewById<ListView>(R.id.listview)
        val list = ArrayList<HashMap<String, Any?>>()

        val from = arrayOf("playlistName", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)

        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
        playlistView.adapter = simpleAdapter

        val settings = Settings()

        val username = settings.getSetting(view.context, "email")
        val password = settings.getSetting(view.context, "password")

        if(username == null || password == null){
            println("password not set")
        }else{
            val loginPostParams = JSONObject()
            loginPostParams.put("email", username)
            loginPostParams.put("password", password)

            val loginUrl = "https://connect.monstercat.com/v2/signin"
            var sid = ""

            val loginPostRequest = object: JsonObjectRequest(Request.Method.POST,
                loginUrl, loginPostParams, Response.Listener {response ->
                    val headers = response.getJSONObject("headers")

                    //get SID
                    sid = headers.getString("Set-Cookie").substringBefore(';').replace("connect.sid=", "")

                }, Response.ErrorListener {error ->

                })
            {
                override fun parseNetworkResponse(response: NetworkResponse?): Response<JSONObject> {
                    try {
                        val jsonResponse = JSONObject()
                        jsonResponse.put("headers", JSONObject(response!!.headers as Map<*, *>))

                        return Response.success(
                            jsonResponse,
                            HttpHeaderParser.parseCacheHeaders(response)
                        )
                    } catch (e: UnsupportedEncodingException) {

                        return Response.error<JSONObject>(ParseError(e))
                    }
                }
            }

            val queue = Volley.newRequestQueue(view.context)
            queue.add(loginPostRequest)

            val playlistUrl = "https://connect.monstercat.com/v2/self/playlists"

            val playlistRequest = object: StringRequest(Request.Method.GET, playlistUrl, Response.Listener<String>
            { response ->
                val jsonObject = JSONObject(response)
                val jsonArray = jsonObject.getJSONArray("results")

                for(i in (0 until jsonArray.length())){
                    val playlistObject = jsonArray.getJSONObject(i)
                    val playlistName = playlistObject.getString("name")

                    val tracks = playlistObject.getJSONArray("tracks")

                    val playlistHashMap = HashMap<String, Any?>()
                    playlistHashMap.put("playlistName", playlistName)
                    playlistHashMap.put("coverUrl", "")
                    playlistHashMap.put("titles", tracks)

                    list.add(playlistHashMap)

                    simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
                    playlistView.adapter = simpleAdapter
                }
            }, Response.ErrorListener {error ->

            }
            ){
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val params = HashMap<String, String>()
                    params.put("Cookie", "connect.sid=" + sid)

                    return params
                }
            }

            var done = false

            queue.addRequestFinishedListener<Any> {
                //request playlist
                if(!done){
                    queue.add(playlistRequest)
                    done = true
                }
            }

            val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        }
    }

}