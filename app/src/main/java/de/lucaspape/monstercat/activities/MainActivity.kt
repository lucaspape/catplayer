package de.lucaspape.monstercat.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.AsyncTask
import android.os.Bundle
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
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
import de.lucaspape.monstercat.handlers.async.LoadContinuousSongListAsync
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.settings.Settings
import java.lang.ref.WeakReference

var loadContinuousSongListAsyncTask: LoadContinuousSongListAsync? = null

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
        setContentView(R.layout.activity_main)

        findViewById<BottomNavigationView>(R.id.nav_view).setOnNavigationItemSelectedListener(
            onNavigationItemSelectedListener
        )

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
        val settings = Settings(this)
        if (settings.getSetting("albumViewSelected") != null) {
            HomeHandler.albumViewSelected = settings.getSetting("albumView") == true.toString()
        }

        //open the home fragment
        openFragment(HomeFragment.newInstance())

        //start download background task
        DownloadTask(WeakReference(applicationContext)).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        //show privacy policy
        showPrivacyPolicy()

        setupMusicPlayer()
        registerButtonListeners()
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
    fun setupMusicPlayer() {
        val textview1 = findViewById<TextView>(R.id.songCurrent1)
        val textview2 = findViewById<TextView>(R.id.songCurrent2)
        val coverBarImageView = findViewById<ImageView>(R.id.barCoverImage)
        val musicToolBar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar)
        val playButton = findViewById<ImageButton>(R.id.playButton)
        val seekBar = findViewById<SeekBar>(R.id.seekBar)

        val weakReference = WeakReference(applicationContext)

        //setup musicPlayer

        contextReference = (weakReference)
        setTextView(textview1, textview2)
        setSeekBar(seekBar)
        setBarCoverImageView(coverBarImageView)
        setMusicBar(musicToolBar)
        setPlayButton(playButton)
    }

    fun registerButtonListeners(){
        //music control buttons
        val playButton = findViewById<ImageButton>(R.id.playButton)
        val backButton = findViewById<ImageButton>(R.id.backbutton)
        val nextButton = findViewById<ImageButton>(R.id.nextbutton)

        playButton.setOnClickListener {
            toggleMusic()
        }

        nextButton.setOnClickListener {
            next()
        }

        backButton.setOnClickListener {
            previous()
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar).setOnClickListener {
            startActivity(Intent(applicationContext, PlayerFullscreenActivity::class.java))
        }
    }
}
