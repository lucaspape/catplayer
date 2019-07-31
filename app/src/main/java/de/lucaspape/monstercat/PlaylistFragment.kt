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
import java.io.File
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

        val settings = Settings(view.context)

        val username = settings.getSetting("email")
        val password = settings.getSetting("password")

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
                    playlistHashMap.put("type", "playlist")

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

            //TODO implement
            val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)

            //TODO sort
            playlistView.onItemClickListener = object : AdapterView.OnItemClickListener {
                override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, p3: Long) {
                    val itemValue = playlistView.getItemAtPosition(position) as HashMap<String, Any?>

                    val titleQueue = Volley.newRequestQueue(view!!.context)

                    if(itemValue.get("type") == "playlist"){
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
                                var version = playlistObject.getString("version")
                                val artist = playlistObject.getString("artistsTitle")
                                val coverUrl = playlistObject.getJSONObject("release").getString("coverUrl")
                                val id = playlistObject.getString("_id")
                                val streamHash = playlistObject.getJSONObject("albums").getString("streamHash")

                                if(version == "null"){
                                    version = ""
                                }

                                val trackHashMap = HashMap<String, Any?>()
                                trackHashMap.put("title", title)
                                trackHashMap.put("version", version)
                                trackHashMap.put("artist", artist)
                                trackHashMap.put("coverUrl", view.context.cacheDir.toString() + "/" + title + version + artist + ".png")
                                trackHashMap.put("id", id)
                                trackHashMap.put("streamHash", streamHash)
                                trackHashMap.put("shownTitle", artist + " " + title + " " + version)

                                tracks.add(trackHashMap)

                                if (!File(view.context.cacheDir.toString() + "/" + title + version + artist + ".png").exists()) {
                                    MainActivity.downloadCover(
                                        coverUrl + "?image_width=64",
                                        view.context.cacheDir.toString() + "/" + title + version + artist + ".png", simpleAdapter
                                    ).execute()
                                }

                                val fromTrack = arrayOf("shownTitle", "coverUrl")
                                val toTrack = arrayOf(R.id.title, R.id.cover)

                                simpleAdapter = SimpleAdapter(view.context, tracks, R.layout.list_single, fromTrack, toTrack.toIntArray())
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

                        titleQueue.add(trackRequest)
                    }else{
                        //do song things

                        val url = "https://s3.amazonaws.com/data.monstercat.com/blobs/"  + itemValue.get("streamHash")
                        val title = itemValue.get("shownTitle") as String

                        Toast.makeText(
                            view.context,
                            itemValue.get("title") as String + " " + itemValue.get("version") as String + " added to playlist!",
                            Toast.LENGTH_SHORT
                        ).show()

                        MainActivity.musicPlayer!!.addSong(url, title)

                    }

                }
            }


        }
    }

}