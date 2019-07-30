package de.lucaspape.monstercat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.AuthFailureError
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import org.json.JSONObject
import com.android.volley.ParseError
import com.android.volley.toolbox.*
import org.json.JSONArray
import org.json.JSONException
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
            Toast.makeText(view.context, "Set your username and passwort in the settings!", Toast.LENGTH_SHORT)
                .show()
        }else{
            val loginPostParams = JSONObject()
            loginPostParams.put("email", username)
            loginPostParams.put("password", password)

            val loginUrl = "https://connect.monstercat.com/v2/signin"
            var sid = ""

            val loginPostRequest = object: JsonObjectRequest(Request.Method.POST,
                loginUrl, loginPostParams, Response.Listener {response ->
                    val headers = response.getJSONObject("headers")

                    try{
                        //get SID
                        sid = headers.getString("Set-Cookie").substringBefore(';').replace("connect.sid=", "")
                    }catch (e: JSONException){
                        println(headers)
                        println(e)
                    }

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
                    val playlistId = playlistObject.getString("_id")

                    val tracks = playlistObject.getJSONArray("tracks")

                    val playlistHashMap = HashMap<String, Any?>()
                    playlistHashMap.put("playlistName", playlistName)
                    playlistHashMap.put("coverUrl", "")
                    playlistHashMap.put("titles", tracks)
                    playlistHashMap.put("playlistId", playlistId)

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

            playlistView.onItemClickListener = object : AdapterView.OnItemClickListener {
                override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                    val itemValue = playlistView.getItemAtPosition(position) as HashMap<String, Any?>

                    val playlistName = itemValue.get("playlistName")
                    val playlistId = itemValue.get("playlistId")
                    println(playlistName)

                    val playlistTrackUrl = "https://connect.monstercat.com/api/catalog/browse/?playlistId=" + playlistId + "&skip=0&limit=50"
                    val tracks = ArrayList<HashMap<String, Any?>>()

                    val trackRequest = object: StringRequest(Request.Method.GET, playlistTrackUrl, Response.Listener<String>
                    { response ->
                        val jsonObject = JSONObject(response)
                        val jsonArray = jsonObject.getJSONArray("results")

                        for(i in (0 until jsonArray.length())){
                            val playlistObject = jsonArray.getJSONObject(i)

                            val title = playlistObject.getString("title")
                            val version = playlistObject.getString("version")
                            val artist = playlistObject.getString("artistsTitle")
                            val coverUrl = playlistObject.getJSONObject("release").getString("coverUrl")

                            val trackHashMap = HashMap<String, Any?>()
                            trackHashMap.put("title", title)
                            trackHashMap.put("version", version)
                            trackHashMap.put("artist", artist)
                            trackHashMap.put("coverUrl", coverUrl)


                            tracks.add(trackHashMap)

                            val fromTrack = arrayOf("title", "coverUrl")
                            val toTrack = arrayOf(R.id.title, R.id.cover)

                            simpleAdapter = SimpleAdapter(view!!.context, tracks, R.layout.list_single, fromTrack, toTrack.toIntArray())
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
                    val titleQueue = Volley.newRequestQueue(view!!.context)
                    titleQueue.add(trackRequest)

                }

            }
        }
    }

}