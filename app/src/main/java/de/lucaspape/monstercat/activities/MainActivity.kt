package de.lucaspape.monstercat.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.Auth
import de.lucaspape.monstercat.download.DownloadTask
import de.lucaspape.monstercat.fragments.HomeFragment
import de.lucaspape.monstercat.fragments.PlaylistFragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.handlers.async.LoadContinuousSongListAsync
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.util.Settings
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference

var loadContinuousSongListAsyncTask: LoadContinuousSongListAsync? = null
var downloadTask: DownloadTask? = null

/**
 * Main activity
 */
class MainActivity : AppCompatActivity() {

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    openFragment(HomeFragment.newInstance())

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

        val settings = Settings(this)

        if (settings.getSetting("darkTheme") != null) {
            if (settings.getSetting("darkTheme")!!.toBoolean()) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        //check for internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            println("Internet permission not granted!")
        }

        //create the MusicPlayer.kt mediasession
        createMediaSession(WeakReference(this))

        //register receiver which checks if headphones unplugged
        registerReceiver(NoisyReceiver(), IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        //login to monstercat
        Auth().login(this)

        //set the correct view
        if (settings.getSetting("albumViewSelected") != null) {
            HomeHandler.albumViewSelected = settings.getSetting("albumView") == true.toString()
        }

        //start download background task
        downloadTask?.cancel(true)

        downloadTask = DownloadTask(WeakReference(applicationContext))
        downloadTask?.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

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
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(NoisyReceiver())
        } catch (e: IllegalArgumentException) {

        }

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(musicNotificationID)

        super.onDestroy()
    }

    private fun showPrivacyPolicy() {
        val settings = Settings(this)

        //for new privacy policy change version number
        if (settings.getSetting("privacypolicy") != "1.0") {
            val textView = TextView(this)
            val spannableString = SpannableString(
                getString(
                    R.string.reviewPrivacyPolicyMsg, getString(
                        R.string.privacypolicyUrl
                    )
                )
            )

            Linkify.addLinks(spannableString, Linkify.WEB_URLS)
            textView.text = spannableString
            textView.textSize = 18f
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
        //music control buttons
        val playButton = findViewById<ImageButton>(R.id.playButton)

        playButton.setOnClickListener {
            toggleMusic()
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar).setOnClickListener {
            startActivity(Intent(applicationContext, PlayerFullscreenActivity::class.java))
        }
    }
}
