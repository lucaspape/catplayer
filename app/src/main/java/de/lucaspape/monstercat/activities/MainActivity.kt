package de.lucaspape.monstercat.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.TypedValue
import android.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.background.BackgroundService
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.background.BackgroundService.Companion.streamInfoUpdateAsync
import de.lucaspape.monstercat.download.hideDownloadNotification
import de.lucaspape.monstercat.fragments.HomeFragment
import de.lucaspape.monstercat.fragments.PlaylistFragment
import de.lucaspape.monstercat.handlers.HomeHandler
import de.lucaspape.monstercat.handlers.async.BackgroundAsync
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.util.*
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

val noisyReceiver = NoisyReceiver()
var backgroundServiceIntent: Intent? = null

//callback function for back pressed, TODO this is not great
var fragmentBackPressedCallback: () -> Unit = {}

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

        //check for internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            displayInfo(this, "No access to internet!")
            println("Internet permission not granted!")
        }

        downloadFallbackCoverImages()

        //adjust theme
        changeTheme()

        val settings = Settings(this)

        settings.getSetting(getString(R.string.crossfadeTimeSetting))?.let {
            crossfade = Integer.parseInt(it)
        }

        if (!loggedIn) {
            Auth().loadLogin(this, {
                //login success
                displayInfo(this, getString(R.string.loginSuccessfulMsg))
            }, {
                //login failed, retrieve new SID

                val sUsername = settings.getSetting(getString(R.string.emailSetting))
                val sPassword = settings.getSetting(getString(R.string.passwordSetting))

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
        if (settings.getSetting(getString(R.string.albumViewSelectedSetting)) != null) {
            HomeHandler.albumViewSelected =
                settings.getSetting(getString(R.string.albumView)) == true.toString()
        }

        val intentExtras = intent.extras


        if (intentExtras != null) {
            if (intentExtras["search"] != null) {
                //open the home fragment
                openFragment(HomeFragment.newInstance(intentExtras["search"] as String))
            }
        } else {
            openFragment(HomeFragment.newInstance())
        }

        setContentView(R.layout.activity_main)

        contextReference = WeakReference(this)

        setupMusicPlayer()

        PlayerSaveState.restoreMusicPlayerState()

        //create the MusicPlayer.kt mediasession
        createMediaSession()

        findViewById<BottomNavigationView>(R.id.nav_view).setOnNavigationItemSelectedListener(
            onNavigationItemSelectedListener
        )

        registerButtonListeners()

        //update notification after restart of activity (screen orientation change etc)
        if (exoPlayer?.isPlaying == true) {
            if (streamInfoUpdateAsync?.status != AsyncTask.Status.RUNNING) {
                val currentSong = getCurrentSong()

                currentSong?.let { song ->
                    setCover(this, song.title, song.version, song.artist, song.albumId) { bitmap ->
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
                    this,
                    StreamInfoUpdateAsync.liveTitle,
                    StreamInfoUpdateAsync.liveVersion,
                    StreamInfoUpdateAsync.liveArtist,
                    StreamInfoUpdateAsync.liveAlbumId
                ) { bitmap ->
                    updateNotification(
                        StreamInfoUpdateAsync.liveTitle,
                        StreamInfoUpdateAsync.liveVersion,
                        StreamInfoUpdateAsync.liveArtist,
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

        PlayerSaveState.saveMusicPlayerState()

        super.onDestroy()
    }

    private fun downloadFallbackCoverImages() {
        val fallbackBlackFile = File("$dataDir/fallback_black.jpg")
        val fallbackBlackFileLow = File("$dataDir/fallback_black_low.jpg")
        val fallbackWhiteFile = File("$dataDir/fallback_white.jpg")
        val fallbackWhiteFileLow = File("$dataDir/fallback_white_low.jpg")

        if (!fallbackBlackFile.exists() || !fallbackBlackFileLow.exists()) {
            BackgroundAsync({
                downloadFile(
                    fallbackBlackFile.absolutePath,
                    getString(R.string.fallbackCoverBlackUrl),
                    cacheDir.toString(),
                    ""
                ) { _, _ ->
                }
            }, {
                FileOutputStream(fallbackBlackFileLow).use { out ->
                    val originalBitmap = BitmapFactory.decodeFile(fallbackBlackFile.absolutePath)
                    originalBitmap?.let {
                        Bitmap.createScaledBitmap(it, 128, 128, false)
                            .compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                }

                changeTheme()
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }

        if (!fallbackWhiteFile.exists() || !fallbackWhiteFileLow.exists()) {
            BackgroundAsync({
                downloadFile(
                    fallbackWhiteFile.absolutePath,
                    getString(R.string.fallbackCoverUrl),
                    cacheDir.toString(),
                    ""
                ) { _, _ ->
                }
            }, {
                FileOutputStream(fallbackWhiteFileLow).use { out ->
                    val originalBitmap = BitmapFactory.decodeFile(fallbackWhiteFile.absolutePath)
                    originalBitmap?.let {
                        Bitmap.createScaledBitmap(it, 128, 128, false)
                            .compress(Bitmap.CompressFormat.JPEG, 100, out)
                    }
                }

                changeTheme()
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private fun changeTheme() {
        val fallbackFile = File("$dataDir/fallback.jpg")
        val fallbackFileLow = File("$dataDir/fallback_low.jpg")

        val fallbackBlackFile = File("$dataDir/fallback_black.jpg")
        val fallbackBlackFileLow = File("$dataDir/fallback_black_low.jpg")
        val fallbackWhiteFile = File("$dataDir/fallback_white.jpg")
        val fallbackWhiteFileLow = File("$dataDir/fallback_white_low.jpg")

        val settings = Settings(this)

        if (settings.getSetting(getString(R.string.darkThemeSetting)) != null) {
            if (settings.getSetting(getString(R.string.darkThemeSetting)) == "true") {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                if (fallbackBlackFile.exists() && fallbackBlackFileLow.exists()) {
                    fallbackBlackFile.copyTo(fallbackFile, true)
                    fallbackBlackFileLow.copyTo(fallbackFileLow, true)
                }
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                if (fallbackWhiteFile.exists() && fallbackWhiteFileLow.exists()) {
                    fallbackWhiteFile.copyTo(fallbackFile, true)
                    fallbackWhiteFileLow.copyTo(fallbackFileLow, true)
                }
            }
        } else {
            if (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                settings.saveSetting(getString(R.string.darkThemeSetting), true.toString())
            } else {
                settings.saveSetting(getString(R.string.darkThemeSetting), false.toString())
            }

            changeTheme()
        }
    }

    private fun showPrivacyPolicy() {
        val settings = Settings(this)

        //for new privacy policy change version number
        if (settings.getSetting(getString(R.string.privacyPolicySetting)) != "1.0") {
            AlertDialog.Builder(this).apply {
                setTitle(getString(R.string.privacyPolicy))
                setPositiveButton(getString(R.string.ok), null)
                setMessage(
                    getString(R.string.reviewPrivacyPolicyMsg, getString(R.string.privacypolicyUrl))
                )
            }.create().run {
                show()

                val textColorTypedValue = TypedValue()
                context.theme.resolveAttribute(R.attr.colorOnSurface, textColorTypedValue, true)

                val positiveButton = getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.setTextColor(textColorTypedValue.data)

                // Now that we've called show(), we can get a reference to the dialog's TextView
                // and modify it as we wish
                findViewById<TextView>(android.R.id.message).apply {
                    textSize = 18f
                    autoLinkMask = Linkify.WEB_URLS
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
            settings.saveSetting(getString(R.string.privacyPolicySetting), "1.0")
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
