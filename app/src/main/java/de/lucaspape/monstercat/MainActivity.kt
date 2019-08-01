package de.lucaspape.monstercat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.os.AsyncTask
import android.widget.*
import androidx.fragment.app.Fragment
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    companion object{
        @JvmStatic
        var musicPlayer:MusicPlayer? = null
        @JvmStatic
        var sid = ""
        @JvmStatic
        var loggedIn = false
    }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                val homeFragment = HomeFragment.newInstance()
                openFragment(homeFragment)

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                val playlistFragment = PlaylistFragment.newInstance()
                openFragment(playlistFragment)

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                val settingsFragment = SettingsFragment.newInstance()
                openFragment(settingsFragment)

                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
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

        val auth = Auth()
        auth.login(this)

        val homeFragment = HomeFragment.newInstance()
        openFragment(homeFragment)
    }

    override fun onBackPressed() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView.selectedItemId = R.id.navigation_home
    }

    class downloadCoverArray(covers: ArrayList<HashMap<String, Any?>>, simpleAdapter: SimpleAdapter):AsyncTask<Void, Void, String>(){
        val covers = covers
        val simpleAdapter = simpleAdapter

        override fun doInBackground(vararg p0: Void?): String? {
            for(i in covers.indices){
                val primaryResolution = covers[i].get("primaryRes") as String
                val secondaryResolution = covers[i].get("secondaryRes") as String

                val url = URL(covers[i].get("coverUrl") as String + "?image_width=" + primaryResolution)
                val location = covers[i].get("location") as String

                println("Downloading... " + url.toString())

                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val primaryBitmap = BitmapFactory.decodeStream(input)

                FileOutputStream(location + primaryResolution.toString()).use { out ->
                    primaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                val secondaryBitmap = Bitmap.createScaledBitmap(primaryBitmap, secondaryResolution.toInt(), secondaryResolution.toInt(), false)

                FileOutputStream(location + secondaryResolution.toString()).use { out ->
                    secondaryBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }

                println("Finished download!")
            }

            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            simpleAdapter.notifyDataSetChanged()
        }

    }

    class downloadCover(url:String, location:String, simpleAdapter: SimpleAdapter) : AsyncTask<Void, Void, String>() {
        val url = url
        val location = location
        val simpleAdapter = simpleAdapter

        override fun doInBackground(vararg params: Void?): String? {
            try {

                val url = URL(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)

                FileOutputStream(location).use { out ->
                    bitmap!!.compress(Bitmap.CompressFormat.PNG, 100, out)
                }


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
