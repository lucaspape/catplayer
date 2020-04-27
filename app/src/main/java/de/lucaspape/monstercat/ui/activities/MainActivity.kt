package de.lucaspape.monstercat.ui.activities

import android.Manifest
import android.annotation.SuppressLint
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.download.DownloadService
import de.lucaspape.monstercat.download.hideDownloadNotification
import de.lucaspape.util.BackgroundAsync
import de.lucaspape.monstercat.music.*
import de.lucaspape.monstercat.music.notification.updateNotification
import de.lucaspape.monstercat.music.save.PlayerSaveState
import de.lucaspape.monstercat.music.util.*
import de.lucaspape.monstercat.music.util.setCover
import de.lucaspape.monstercat.ui.handlers.HomeHandler
import de.lucaspape.monstercat.ui.handlers.PlaylistHandler
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.Settings
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.ref.WeakReference

val noisyReceiver = NoisyReceiver()
var downloadServiceIntent: Intent? = null

var offlineDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_24dp"
var downloadDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_file_download_24dp"
var addToQueueDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_play_24"
var shareDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_share_24dp"
var openInAppDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_open_in_new_24dp"
var addToPlaylistDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_24"
var deleteDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_delete_outline_24"
var editDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_edit_24"
var playlistPublicDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_public_24"
var playlistPrivateDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_block_24"

var playButtonDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_play_arrow_24dp"
var pauseButtonDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_pause_24dp"

var createPlaylistDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_add_24dp"
var addPlaylistDrawable = "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_24"

/**
 * Main activity
 */
class MainActivity : AppCompatActivity() {
    private var fallbackFile = File("")
    private var fallbackFileLow = File("")

    private var fallbackBlackFile = File("")
    private var fallbackBlackFileLow = File("")
    private var fallbackWhiteFile = File("")
    private var fallbackWhiteFileLow = File("")

    private var currentFragment: Fragment? = null

    //callback function for back pressed
    var fragmentBackPressedCallback: () -> Unit = {
        val fragment = currentFragment

        if (fragment is de.lucaspape.monstercat.ui.fragments.Fragment) {
            fragment.onBackPressed()
        }
    }

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    currentFragment =
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(HomeHandler())
                    currentFragment?.let {
                        openFragment(it)
                    }

                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_dashboard -> {
                    currentFragment =
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(PlaylistHandler())
                    currentFragment?.let {
                        openFragment(it)
                    }

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

        fallbackFile = File("$dataDir/fallback.webp")
        fallbackFileLow = File("$dataDir/fallback_low.webp")

        fallbackBlackFile = File("$dataDir/fallback_black.webp")
        fallbackBlackFileLow = File("$dataDir/fallback_black_low.webp")
        fallbackWhiteFile = File("$dataDir/fallback_white.webp")
        fallbackWhiteFileLow = File("$dataDir/fallback_white_low.webp")

        //check for internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            displayInfo(this, getString(R.string.noInternetAccessError))
        }

        downloadFallbackCoverImages()

        //adjust theme
        changeTheme()

        val settings = Settings(this)

        if (settings.getString(getString(R.string.appVersionSetting)) != packageManager.getPackageInfo(
                packageName,
                0
            ).versionName
        ) {
            try {
                File("$cacheDir/player_state.obj").delete()
            } catch (e: FileNotFoundException) {

            }

            settings.setString(
                getString(R.string.appVersionSetting),
                packageManager.getPackageInfo(packageName, 0).versionName
            )
        }

        settings.getInt(getString(R.string.crossfadeTimeSetting))?.let {
            crossfade = it
        }

        settings.getFloat(getString(R.string.volumeSetting))?.let {
            volume = it
        }

        settings.getBoolean(getString(R.string.playRelatedSetting))?.let {
            playRelatedSongsAfterPlaylistFinished = it
        }

        if (!loggedIn) {
            Auth().checkLogin(this, {
                //login success
                displayInfo(this, getString(R.string.loginSuccessfulMsg))
            }, {
                //login failed, retrieve new SID

                val sUsername = settings.getString(getString(R.string.emailSetting))
                val sPassword = settings.getString(getString(R.string.passwordSetting))

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

        val intentExtras = intent.extras

        if (intentExtras != null) {
            if (intentExtras["search"] != null) {
                //open the home fragment
                currentFragment = de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                    HomeHandler(),
                    intentExtras["search"] as String
                )
                currentFragment?.let {
                    openFragment(it)
                }
            }
        } else {
            openFragment(de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(HomeHandler()))
        }

        setContentView(R.layout.activity_main)

        contextReference = WeakReference(this)

        setupMusicPlayer()

        //create the MusicPlayer.kt mediasession
        createMediaSession()

        findViewById<BottomNavigationView>(R.id.nav_view).setOnNavigationItemSelectedListener(
            onNavigationItemSelectedListener
        )

        registerButtonListeners()

        //update notification after restart of activity (screen orientation change etc)
        if (exoPlayer?.isPlaying == true) {
            val currentSong = getCurrentSongId()

            setCover(
                this,
                currentSong
            ) { bitmap ->
                updateNotification(
                    this,
                    currentSong,
                    bitmap
                )
            }
        }

        if (DownloadService.downloadTask?.status != AsyncTask.Status.RUNNING) {
            downloadServiceIntent = Intent(this, DownloadService::class.java)
            startService(downloadServiceIntent)
        }

        //show privacy policy
        showPrivacyPolicy()
    }

    override fun onDestroy() {
        super.onDestroy()

        //if app closed
        hideDownloadNotification(this)

        fragmentBackPressedCallback = {}

        PlayerSaveState.saveMusicPlayerState(this)

        currentSong = ""
        preparedSong = ""
    }

    override fun onPause() {
        super.onPause()

        PlayerSaveState.saveMusicPlayerState(this)
    }

    private fun downloadFallbackCoverImages() {
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
                            .compress(Bitmap.CompressFormat.WEBP, 100, out)
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
                            .compress(Bitmap.CompressFormat.WEBP, 100, out)
                    }
                }

                changeTheme()
            }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
        }
    }

    private fun changeTheme() {
        val settings = Settings(this)

        val darkMode = settings.getBoolean(getString(R.string.darkThemeSetting))

        if (darkMode != null) {
            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                if (fallbackBlackFile.exists() && fallbackBlackFileLow.exists()) {
                    fallbackBlackFile.copyTo(fallbackFile, true)
                    fallbackBlackFileLow.copyTo(fallbackFileLow, true)
                }

                offlineDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_white_24dp"
                downloadDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_file_download_white_24dp"
                addToQueueDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_play_white_24"
                shareDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_share_white_24dp"
                openInAppDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_open_in_new_white_24dp"
                addToPlaylistDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_white_24"
                deleteDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_delete_outline_white_24"
                editDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_edit_white_24"
                playlistPublicDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_public_white_24"
                playlistPrivateDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_block_white_24"

                playButtonDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_play_arrow_white_24dp"
                pauseButtonDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_pause_white_24dp"

                createPlaylistDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_add_white_24dp"
                addPlaylistDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_white_24"
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                if (fallbackWhiteFile.exists() && fallbackWhiteFileLow.exists()) {
                    fallbackWhiteFile.copyTo(fallbackFile, true)
                    fallbackWhiteFileLow.copyTo(fallbackFileLow, true)
                }

                offlineDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_offline_pin_black_24dp"
                downloadDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_file_download_black_24dp"
                addToQueueDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_play_black_24"
                shareDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_share_black_24dp"
                openInAppDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_open_in_new_black_24dp"
                addToPlaylistDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_black_24"
                deleteDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_delete_outline_black_24"
                editDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_edit_black_24"
                playlistPublicDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_public_black_24"
                playlistPrivateDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_block_black_24"
                playButtonDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_play_arrow_black_24dp"
                pauseButtonDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_pause_black_24dp"

                createPlaylistDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_add_black_24dp"
                addPlaylistDrawable =
                    "android.resource://de.lucaspape.monstercat/drawable/ic_playlist_add_black_24"
            }
        } else {
            if (resources.configuration.uiMode.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES) {
                settings.setBoolean(getString(R.string.darkThemeSetting), true)
            } else {
                settings.setBoolean(getString(R.string.darkThemeSetting), false)
            }

            changeTheme()
        }
    }

    private fun showPrivacyPolicy() {
        val settings = Settings(this)

        //for new privacy policy change version number
        if (settings.getString(getString(R.string.privacyPolicySetting)) != "1.1") {
            MaterialAlertDialogBuilder(this).apply {
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
                findViewById<TextView>(android.R.id.message)?.apply {
                    textSize = 18f
                    autoLinkMask = Linkify.WEB_URLS
                    movementMethod = LinkMovementMethod.getInstance()
                }
            }
            settings.setString(getString(R.string.privacyPolicySetting), "1.1")
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
        musicBarReference = WeakReference(musicToolBar)
        seekBarReference = WeakReference(seekBar)
        barCoverImageReference = WeakReference(coverBarImageView)
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
