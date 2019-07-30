package de.lucaspape.monstercat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Exception
import java.lang.reflect.InvocationTargetException

class HomeFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    companion object {
        fun newInstance(): HomeFragment = HomeFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val musicList = view.findViewById<ListView>(R.id.musiclistview)
        val queue = Volley.newRequestQueue(view.context)
        var list = ArrayList<HashMap<String, Any?>>()
        val listFile = File(view.context.cacheDir.toString() + "/" + "songs.list")

        val from = arrayOf("shownTitle", "coverUrl")
        val to = arrayOf(R.id.title, R.id.cover)

        var simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
        musicList.adapter = simpleAdapter

        if (listFile.exists()) {
            try {
                val ois = ObjectInputStream(FileInputStream(listFile))
                list = ois.readObject() as ArrayList<HashMap<String, Any?>>
                ois.close()
            } catch (e: Exception) {
                println(e)
            }

            simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
            simpleAdapter.notifyDataSetChanged()
            musicList.adapter = simpleAdapter
        }

        val swipeRefreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.pullToRefresh)
        swipeRefreshLayout.setOnRefreshListener {
            var requestCount = 0

            val loadMax = 200
            val tempList = Array<HashMap<String, Any?>>(loadMax, {HashMap<String, Any?>()})
            list = ArrayList<HashMap<String, Any?>>()

            //can only load 50 at a time
            for (i in (0 until loadMax / 50)) {
                val url = "https://connect.monstercat.com/api/catalog/browse/?limit=50&skip=" + i * 50

                val stringRequest = StringRequest(
                    Request.Method.GET, url,
                    Response.Listener<String> { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")

                        for (k in (0 until jsonArray.length())) {
                            var id = ""
                            var title = ""
                            var artist = ""
                            var coverUrl = ""
                            var version = ""

                            try {
                                id = jsonArray.getJSONObject(k).getJSONObject("albums").getString("albumId")
                                title = jsonArray.getJSONObject(k).getString("title")
                                artist = jsonArray.getJSONObject(k).getString("artistsTitle")
                                coverUrl = jsonArray.getJSONObject(k).getJSONObject("release").getString("coverUrl")
                                version = jsonArray.getJSONObject(k).getString("version")
                            } catch (e: InvocationTargetException) {

                            }

                            val hashMap = HashMap<String, Any?>()

                            hashMap.put("id", id)
                            hashMap.put("title", title)
                            hashMap.put("artist", artist)
                            hashMap.put(
                                "coverUrl",
                                view.context.cacheDir.toString() + "/" + title + version + artist + ".png"
                            )
                            hashMap.put("version", version)

                            hashMap.put("shownTitle", artist + " " + title + " " + version)


                            if (!File(view.context.cacheDir.toString() + "/" + title + version + artist + ".png").exists()) {
                                MainActivity.downloadCover(
                                    coverUrl + "?image_width=64",
                                    view.context.cacheDir.toString() + "/" + title + version + artist + ".png", simpleAdapter
                                ).execute()
                            }

                            tempList[i*50 + k] = hashMap

                        }

                    },
                    Response.ErrorListener { println("Error!") })

                // Add the request to the RequestQueue.
                queue.add(stringRequest)
                requestCount++

            }

            var finishedRequest = 0
            queue.addRequestFinishedListener<Any> {
                finishedRequest++
                if (finishedRequest == requestCount) {
                    for(i in tempList.indices){
                        list.add(tempList[i])
                    }

                    val oos = ObjectOutputStream(FileOutputStream(listFile))
                    oos.writeObject(list)
                    oos.flush()
                    oos.close()

                    simpleAdapter = SimpleAdapter(view.context, list, R.layout.list_single, from, to.toIntArray())
                    simpleAdapter.notifyDataSetChanged()
                    musicList.adapter = simpleAdapter

                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        //LOGIN

        val settings = Settings()
        val username = settings.getSetting(view.context, "email")
        val password = settings.getSetting(view.context, "password")

        var sid = ""

        if(username != null || password != null){
            val loginPostParams = JSONObject()
            loginPostParams.put("email", username)
            loginPostParams.put("password", password)

            val loginUrl = "https://connect.monstercat.com/v2/signin"


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

            val loginQueue = Volley.newRequestQueue(view.context)
            loginQueue.add(loginPostRequest)
        }

        val musicQueue = Volley.newRequestQueue(view.context)
        val currentSongText = view.findViewById<TextView>(R.id.songCurrent)
        val playButton = view.findViewById<ImageButton>(R.id.playButton)
        val backButton = view.findViewById<ImageButton>(R.id.backbutton)
        val nextButton = view.findViewById<ImageButton>(R.id.nextbutton)
        val seekBar = view.findViewById<SeekBar>(R.id.seekBar)

        val musicPlayer = MusicPlayer(currentSongText, seekBar)
        musicList.onItemClickListener = object : AdapterView.OnItemClickListener {
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val itemValue = musicList.getItemAtPosition(p2) as HashMap<String, Any?>

                val streamHashUrl =
                    "https://connect.monstercat.com/api/catalog/browse/?albumId=" + itemValue.get("id")
                val streamHashRequest = object: StringRequest(
                    Request.Method.GET, streamHashUrl,
                    Response.Listener<String> { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")
                        var streamHash = ""

                        for (i in (0 until jsonArray.length())) {
                            val searchSong = itemValue.get("title") as String + itemValue.get("version") as String
                            println("searchsong: " + searchSong)
                            if (jsonArray.getJSONObject(i).getString("title") + jsonArray.getJSONObject(i).getString(
                                    "version"
                                ) == searchSong
                            ) {
                                if (jsonArray.getJSONObject(i).getBoolean("streamable")) {
                                    streamHash =
                                        jsonArray.getJSONObject(i).getJSONObject("albums").getString("streamHash")
                                } else {
                                    Toast.makeText(view.context, "Song not yet streamable!", Toast.LENGTH_SHORT)
                                        .show()

                                }
                            }
                        }

                        if (streamHash != "") {
                            musicPlayer.addSong(
                                "https://s3.amazonaws.com/data.monstercat.com/blobs/" + streamHash,
                                itemValue.get("artist") as String + " " + itemValue.get("title") as String + " " + itemValue.get(
                                    "version"
                                ) as String
                            )
                            Toast.makeText(
                                view.context,
                                itemValue.get("title") as String + " " + itemValue.get("version") as String + " added to playlist!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    },
                    Response.ErrorListener { println("Error!") }){
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val params = HashMap<String, String>()
                        if(sid != ""){
                            params.put("Cookie", "connect.sid=" + sid)
                        }

                        return params
                    }
                }

                musicQueue.add(streamHashRequest)
            }
        }

        playButton.setOnClickListener {
            musicPlayer.toggleMusic()
        }

        nextButton.setOnClickListener {
            musicPlayer.next()
        }

        backButton.setOnClickListener {
            musicPlayer.previous()
        }

    }


}