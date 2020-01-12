package de.lucaspape.monstercat.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
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
import de.lucaspape.monstercat.fragments.HomeFragment
import de.lucaspape.monstercat.fragments.PlaylistFragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.handlers.async.BackgroundAsync
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.MonstercatPlayer.Companion.mediaPlayer
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.util.*
import java.io.File
import java.lang.ref.WeakReference

val noisyReceiver = MonstercatPlayer.Companion.NoisyReceiver()
var backgroundServiceIntent: Intent? = null

//callback function for back pressed, TODO this is not great
var fragmentBackPressedCallback: () -> Unit = {}

val monstercatPlayer = MonstercatPlayer()

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

                    if (HomeHandler.albumView) {
                        settings.saveSetting(
                            "currentListAlbumViewLastScrollIndex",
                            0.toString()
                        )
                        settings.saveSetting(
                            "currentListAlbumViewTop",
                            0.toString()
                        )
                    } else {
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

        MonstercatPlayer.contextReference = WeakReference(this)
        //create the MusicPlayer.kt mediasession
        monstercatPlayer.createMediaSession()

        val settings = Settings(this)

        if (!loggedIn) {
            Auth().loadLogin(this, {
                //login success
                displayInfo(this, getString(R.string.loginSuccessfulMsg))
            }, {
                //login failed, retrieve new SID

                val sUsername = settings.getSetting("email")
                val sPassword = settings.getSetting("password")

                sUsername?.let { username ->
                    sPassword?.let { password ->
                        //login to monstercat
                        Auth().login(this, username, password, {
                            displayInfo(this, getString(R.string.loginSuccessfulMsg))
                        }, {
                            displayInfo(this, getString(R.string.loginFailedMsg))
                        })
                    }
                }
            })
        }

        //set the correct view
        if (settings.getSetting("albumViewSelected") != null) {
            HomeHandler.albumViewSelected = settings.getSetting("albumView") == true.toString()
        }

        //open the home fragment
        openFragment(HomeFragment.newInstance())

        setContentView(R.layout.activity_main)

        findViewById<BottomNavigationView>(R.id.nav_view).setOnNavigationItemSelectedListener(
            onNavigationItemSelectedListener
        )

        setupMusicPlayer()

        registerButtonListeners()

        //update notification after restart of activity (screen orientation change etc)
        if (mediaPlayer?.isPlaying == true) {
            if (updateLiveInfoAsync?.status != AsyncTask.Status.RUNNING) {
                val currentSong = getCurrentSong()

                currentSong?.let { song ->
                    setCover(song.title, song.version, song.artist, song.coverUrl) { bitmap ->
                        updateNotification(
                            song.title,
                            song.version,
                            song.artist,
                            bitmap
                        )
                    }
                }
            } else {
                setCover(
                    UpdateLiveInfoAsync.previousTitle,
                    UpdateLiveInfoAsync.previousVersion,
                    UpdateLiveInfoAsync.previousArtist,
                    ""
                ) { bitmap ->
                    updateNotification(
                        UpdateLiveInfoAsync.previousTitle,
                        UpdateLiveInfoAsync.previousVersion,
                        UpdateLiveInfoAsync.previousArtist,
                        bitmap
                    )
                }
            }
        }

        if (!BackgroundService.serviceRunning || backgroundServiceIntent == null) {
            backgroundServiceIntent = Intent(this, BackgroundService::class.java)
            startService(backgroundServiceIntent)
        }

        //show privacy policy
        showPrivacyPolicy()
    }

    override fun onDestroy() {
        //if app closed
        hideDownloadNotification(this)

        fragmentBackPressedCallback = {}

        super.onDestroy()
    }

    private fun changeTheme() {
        val settings = Settings(this)

        if (settings.getSetting("darkTheme") != null) {
            if (settings.getSetting("darkTheme")!!.toBoolean()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                BackgroundAsync({
                    downloadFile(
                        "$dataDir/fallback.jpg",
                        getString(R.string.fallbackCoverBlackUrl),
                        cacheDir.toString(),
                        ""
                    ) { _, _ ->
                    }
                }, {}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                BackgroundAsync({
                    downloadFile(
                        "$dataDir/fallback.jpg",
                        getString(R.string.fallbackCoverUrl),
                        cacheDir.toString(),
                        ""
                    ) { _, _ ->
                    }
                }, {}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
            }
        }else{
            if(resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES){
                settings.saveSetting("darkTheme", true.toString())
            }else{
                settings.saveSetting("darkTheme", false.toString())
            }

            changeTheme()
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
        fragmentBackPressedCallback()
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

        textViewReference = WeakReference(textView)
        seekBarReference = WeakReference(seekBar)
        barCoverImageReference = WeakReference(coverBarImageView)
        musicBarReference = WeakReference(musicToolBar)
        playButtonReference = WeakReference(playButton)

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
