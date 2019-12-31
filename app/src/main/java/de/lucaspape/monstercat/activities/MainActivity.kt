package de.lucaspape.monstercat.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.background.BackgroundService
import de.lucaspape.monstercat.background.BackgroundService.Companion.updateLiveInfoAsync
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.hideDownloadNotification
import de.lucaspape.monstercat.util.Auth
import de.lucaspape.monstercat.fragments.HomeFragment
import de.lucaspape.monstercat.fragments.PlaylistFragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.util.Settings
import de.lucaspape.monstercat.util.displayInfo
import java.lang.ref.WeakReference

val noisyReceiver = NoisyReceiver()
var backgroundServiceIntent:Intent? = null

/**
 * Main activity
 */
class MainActivity : AppCompatActivity() {

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    openFragment(HomeFragment.newInstance())

                    val settings = Settings(this)

                    if(HomeHandler.albumView){
                        settings.saveSetting(
                            "currentListAlbumViewLastScrollIndex",
                            0.toString()
                        )
                        settings.saveSetting(
                            "currentListAlbumViewTop",
                            0.toString()
                        )
                    }else{
                        settings.saveSetting(
                            "currentListViewLastScrollIndex",
                            0.toString()
                        )
                        settings.saveSetting(
                            "currentListViewTop",
                            0.toString()
                        )
                    }

                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_dashboard -> {
                    openFragment(PlaylistFragment.newInstance())

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

        //check for internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            displayInfo(this, "No access to internet!")
            println("Internet permission not granted!")
        }

        //adjust theme
        changeTheme()

        //create the MusicPlayer.kt mediasession
        createMediaSession(WeakReference(this))

        //login to monstercat
        Auth().login(this)

        val settings = Settings(this)
        //set the correct view
        if (settings.getSetting("albumViewSelected") != null) {
            HomeHandler.albumViewSelected = settings.getSetting("albumView") == true.toString()
        }

        //show privacy policy
        showPrivacyPolicy()

        //open the home fragment
        openFragment(HomeFragment.newInstance())

        setContentView(R.layout.activity_main)

        findViewById<BottomNavigationView>(R.id.nav_view).setOnNavigationItemSelectedListener(
            onNavigationItemSelectedListener
        )

        setupMusicPlayer()

        registerButtonListeners()

        //update notification after restart of activity (screen orientation change etc)
        if(mediaPlayer?.isPlaying == true){
            if(updateLiveInfoAsync?.status != AsyncTask.Status.RUNNING){
                val currentSong = getCurrentSong()

                currentSong?.let {song ->
                    updateNotification(song.title, song.version, song.artist, filesDir.toString() + "/" + song.albumId + ".png" + settings.getSetting("primaryCoverResolution"))
                }
            }else{
                updateNotification(
                    UpdateLiveInfoAsync.previousTitle,
                    "",
                    UpdateLiveInfoAsync.previousArtist,
                    "$filesDir/live.png"
                )
            }
        }

        if(!BackgroundService.serviceRunning || backgroundServiceIntent == null){
            backgroundServiceIntent = Intent(this, BackgroundService::class.java)
            startService(backgroundServiceIntent)
        }
    }

    override fun onDestroy() {
        //TODO this is slow and not great

        val listView = findViewById<ListView>(R.id.musiclistview)

        val settings = Settings(this)

        if(listView != null) {
            val topChild = listView.getChildAt(0)?.top
            val paddingTop = listView.paddingTop
            val firstVisiblePosition = listView.firstVisiblePosition

            if(topChild != null){
                if(HomeHandler.albumView){
                    settings.saveSetting(
                        "currentListAlbumViewLastScrollIndex",
                        firstVisiblePosition.toString()
                    )
                    settings.saveSetting(
                        "currentListAlbumViewTop",
                        (topChild - paddingTop).toString()
                    )
                }else{
                    settings.saveSetting(
                        "currentListViewLastScrollIndex",
                        firstVisiblePosition.toString()
                    )
                    settings.saveSetting(
                        "currentListViewTop",
                        (topChild - paddingTop).toString()
                    )
                }
            }
        }

        //if app closed
        hideDownloadNotification(this)

        super.onDestroy()
    }

    private fun changeTheme(){
        val settings = Settings(this)

        if (settings.getSetting("darkTheme") != null) {
            if (settings.getSetting("darkTheme")!!.toBoolean()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun showPrivacyPolicy() {
        val settings = Settings(this)

        //for new privacy policy change version number
        if (settings.getSetting("privacypolicy") != "1.0") {
            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.privacyPolicy))
                setPositiveButton(getString(R.string.ok), null)
                setMessage(
                    getString(R.string.reviewPrivacyPolicyMsg, getString(R.string.privacypolicyUrl))
                )
            }.create().run {
                show()
                // Now that we've called show(), we can get a reference to the dialog's TextView
                // and modify it as we wish
                findViewById<TextView>(android.R.id.message).apply {
                    textSize = 18f
                    autoLinkMask = Linkify.WEB_URLS
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
            settings.saveSetting("privacypolicy", "1.0")
        }
    }

    override fun onBackPressed() {
        //open the home fragment
        openFragment(HomeFragment.newInstance())
    }

    /**
     * Set the correct views for the MusicPlayer.kt
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun setupMusicPlayer() {
        val textView = findViewById<TextView>(R.id.songCurrentText)
        val coverBarImageView = findViewById<ImageView>(R.id.barCoverImage)
        val musicToolBar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar)
        val playButton = findViewById<ImageButton>(R.id.playButton)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)

        //setup musicPlayer
        setTextView(textView)
        setSeekBar(seekBar)
        setBarCoverImageView(coverBarImageView)
        setMusicBar(musicToolBar)
        setPlayButton(playButton, this)

        seekBar.setOnTouchListener { _, _ -> true }
    }

    private fun registerButtonListeners() {
        findViewById<ImageButton>(R.id.playButton).setOnClickListener {
            toggleMusic()
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar).setOnClickListener {
            startActivity(Intent(applicationContext, PlayerFullscreenActivity::class.java))
        }
    }
}
