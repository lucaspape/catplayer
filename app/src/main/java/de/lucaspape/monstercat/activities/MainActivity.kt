package de.lucaspape.monstercat.activities

import android.Manifest
import android.app.AlertDialog
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.auth.Auth
import de.lucaspape.monstercat.download.DownloadTask
import de.lucaspape.monstercat.fragments.HomeFragment
import de.lucaspape.monstercat.fragments.PlaylistFragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.music.NoisyReceiver
import de.lucaspape.monstercat.music.createMediaSession
import de.lucaspape.monstercat.settings.Settings
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

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

        createMediaSession(WeakReference(this))

        val intentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(NoisyReceiver(), intentFilter)

        val settings = Settings(this)

        val auth = Auth()
        auth.login(this)

        if(settings.getSetting("albumViewSelected") != null){
            HomeHandler.albumViewSelected = settings.getSetting("albumView") == true.toString()
        }

        val homeFragment = HomeFragment.newInstance()
        openFragment(homeFragment)

        val weakReference = WeakReference(applicationContext)
        DownloadTask(weakReference).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        //for new privacy policy change version number
        if(settings.getSetting("privacypolicy") != "1.0"){
            val textView = TextView(this)
            val spannableString = SpannableString(getString(
                R.string.reviewPrivacyPolicyMsg, getString(
                    R.string.privacypolicyUrl
                )))

            Linkify.addLinks(spannableString, Linkify.WEB_URLS)
            textView.text = spannableString
            textView.movementMethod = LinkMovementMethod.getInstance()

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setTitle(getString(R.string.privacyPolicy))
            alertDialogBuilder.setCancelable(true)
            alertDialogBuilder.setPositiveButton(getString(R.string.ok), null)
            alertDialogBuilder.setView(textView)
            alertDialogBuilder.show()
            settings.saveSetting("privacypolicy", "1.0")
        }
    }
}