package de.lucaspape.monstercat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import android.widget.SimpleAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import android.widget.AdapterView.OnItemClickListener
import android.os.StrictMode
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.AsyncTask
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED) {
            println("Internet permission not granted!")
        }

        val queue = Volley.newRequestQueue(this)

        val musicList = findViewById<ListView>(R.id.musiclistview)
        val list = ArrayList<HashMap<String,Any?>>()



        val loadMax = 100

        for(i in (0 until loadMax / 50)){
            val url = "https://connect.monstercat.com/api/catalog/browse/?limit=50&skip=" + i

            val stringRequest = StringRequest(
                Request.Method.GET, url,
                Response.Listener<String> { response ->
                    val json = JSONObject(response)
                    val jsonArray = json.getJSONArray("results")

                    val from = arrayOf("shownLabel", "coverUrl")
                    val to = arrayOf(R.id.txt, R.id.img)

                    val simpleAdapter = SimpleAdapter(baseContext, list, R.layout.list_single, from, to.toIntArray())

                    musicList.adapter = simpleAdapter

                    for(k in (0 until jsonArray.length())){
                        val id = jsonArray.getJSONObject(k).getJSONObject("albums").getString("albumId")
                        val title = jsonArray.getJSONObject(k).getString("title")
                        val artist = jsonArray.getJSONObject(k).getString("artistsTitle")
                        val coverUrl = jsonArray.getJSONObject(k).getJSONObject("release").getString("coverUrl")
                        val version = jsonArray.getJSONObject(k).getString("version")

                        val hashMap = HashMap<String,Any?>()

                        if(!File(this.cacheDir.toString() + "/"  + title + version + ".png").exists()){
                            downloadCover(coverUrl + "?image_width=64", this.cacheDir.toString() + "/"  + title + version + ".png", simpleAdapter).execute()
                        }

                        hashMap.put("id", id)
                        hashMap.put("title", title)
                        hashMap.put("artist", artist)
                        hashMap.put("coverUrl", this.cacheDir.toString() + "/"  + title + version + ".png")
                        hashMap.put("version", version)
                        //hashMap.put("coverBitmap", R.drawable.ic_home_black_24dp)


                        //drawableFromUrl(coverUrl + "?image_width=64")
                        hashMap.put("shownLabel", title + " " + artist + " " + version)

                        list.add(hashMap)

                        simpleAdapter.notifyDataSetChanged()

                    }


                },
                Response.ErrorListener { println("Error!") })

            // Add the request to the RequestQueue.
            queue.add(stringRequest)
        }


        val musicPlayer = MusicPlayer()

        musicList.onItemClickListener = object: OnItemClickListener{
            override fun onItemClick(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val itemValue = musicList.getItemAtPosition(p2) as HashMap<String, Any?>

                val streamHashUrl = "https://connect.monstercat.com/api/catalog/browse/?albumId=" + itemValue.get("id")
                val streamHashRequest = StringRequest(
                    Request.Method.GET, streamHashUrl,
                    Response.Listener<String> { response ->
                        val json = JSONObject(response)
                        val jsonArray = json.getJSONArray("results")
                        var streamHash = ""

                        for(i in (0 until jsonArray.length())){
                            val searchSong = itemValue.get("title") as String + itemValue.get("version") as String
                            if(jsonArray.getJSONObject(i).getString("title") + jsonArray.getJSONObject(i).getString("version") == searchSong){

                                streamHash = jsonArray.getJSONObject(i).getJSONObject("albums").getString("streamHash")
                            }
                        }

                        if(streamHash != ""){
                            musicPlayer.addSong("https://s3.amazonaws.com/data.monstercat.com/blobs/" + streamHash)
                        }

                    },
                    Response.ErrorListener { println("Error!") })

                queue.add(streamHashRequest)
            }
        }



    }

    class downloadCover(url:String, location:String, simpleAdapter: SimpleAdapter) : AsyncTask<Void, Void, String>() {
        val url = url
        val location = location
        var simpleAdapter = simpleAdapter

        override fun doInBackground(vararg params: Void?): String? {
            try {

                val url = URL(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)

                FileOutputStream(location).use({ out ->
                    bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out) // bmp is your Bitmap instance
                    // PNG is a lossless format, the compression factor (100) is ignored
                })




            } catch (e: IOException) {
                // Log exception
                return null
            }

            return null
        }

        override fun onPreExecute() {
            super.onPreExecute()
            // ...
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            simpleAdapter.notifyDataSetChanged()
        }
    }

}
