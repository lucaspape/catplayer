package de.lucaspape.monstercat

import android.Manifest
import android.app.AlertDialog
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.auth.Auth
import de.lucaspape.monstercat.download.DownloadHandler
import de.lucaspape.monstercat.download.DownloadTask
import de.lucaspape.monstercat.fragments.HomeFragment
import de.lucaspape.monstercat.fragments.PlaylistFragment
import de.lucaspape.monstercat.fragments.SettingsFragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.music.NoisyReceiver
import de.lucaspape.monstercat.music.contextReference
import de.lucaspape.monstercat.music.createMediaSession
import de.lucaspape.monstercat.settings.Settings
import java.lang.ref.WeakReference

class MainActivity : AppCompatActivity() {

    companion object{
        @JvmStatic
        var downloadHandler:DownloadHandler? = null
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

        downloadHandler = DownloadHandler()

        val weakReference = WeakReference(applicationContext)
        DownloadTask(weakReference).execute()

        //for new privacy policy change version number
        if(settings.getSetting("privacypolicy") != "1.0"){
            val textView = TextView(this)
            val spannableString = SpannableString(getString(R.string.reviewPrivacyPolicyMsg, getString(R.string.privacypolicyUrl)))

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

    override fun onBackPressed() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.nav_view)
        bottomNavigationView.selectedItemId = R.id.navigation_home

        HomeHandler.albumView = HomeHandler.albumViewSelected
    }
}
