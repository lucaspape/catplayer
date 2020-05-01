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
import de.lucaspape.monstercat.ui.*
import de.lucaspape.monstercat.ui.handlers.*
import de.lucaspape.monstercat.util.*
import de.lucaspape.util.Settings
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.ref.WeakReference

val noisyReceiver = NoisyReceiver()
var downloadServiceIntent: Intent? = null

/**
 * Main activity
 */
class MainActivity : AppCompatActivity() {
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
                    openFragment(
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                            HomeHandler({ searchString ->
                                search(
                                    searchString
                                )
                            }, { openSettings() }, null)
                        )
                    )

                    return@OnNavigationItemSelectedListener true
                }
                R.id.navigation_dashboard -> {
                    openFragment(
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                            PlaylistHandler(null)
                        )
                    )

                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    private fun search(searchString: String?) {
        openFragment(
            de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                SearchHandler(
                    searchString
                ) {
                    openFragment(
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                            HomeHandler({ searchString ->
                                search(
                                    searchString
                                )
                            }, { openSettings() }, null)
                        )
                    )
                }
            )
        )
    }

    private fun openSettings() {
        openFragment(
            de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                SettingsHandler {
                    openFragment(
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                            HomeHandler({ searchString ->
                                search(
                                    searchString
                                )
                            }, { openSettings() }, null)
                        )
                    )
                }
            )
        )
    }

    private fun openFragment(fragment: Fragment) {
        currentFragment = fragment

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //set files for fallback covers
        setFallbackCoverFiles(this)

        //check for internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            displayInfo(this, getString(R.string.noInternetAccessError))
        }

        //download fallback covers
        downloadFallbackCoverImages()

        //adjust theme
        changeTheme()

        val settings = Settings.getSettings(this)

        if (settings.getString(getString(R.string.customApiSupportsV1Setting)) == null) {
            checkCustomApiFeaturesAsync(this, {}, {})
        }

        //check for app version, if changed reset player state
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

        applyPlayerSettings(this)

        //login
        if (!loggedIn) {
            Auth().checkLogin(this, {
                //login success
                println(getString(R.string.loginSuccessfulMsg))
            }, {
                //login failed, retrieve new SID

                val sUsername = settings.getString(getString(R.string.emailSetting))
                val sPassword = settings.getString(getString(R.string.passwordSetting))

                sUsername?.let { username ->
                    sPassword?.let { password ->
                        //login to monstercat
                        Auth().login(this, username, password, {
                            println(getString(R.string.loginSuccessfulMsg))
                        }, {
                            displayInfo(this, getString(R.string.loginFailedMsg))
                        })
                    }
                }
            })
        }

        val intentExtras = intent.extras
        val path = intent.data?.path

        //check for extras in intent (open search on startup, open album or playlist on startup using links)
        when {
            intentExtras?.get("search") != null -> {
                search(intentExtras["search"] as String)
            }
            path != null -> {
                val id = path.substring(path.lastIndexOf("/") + 1, path.length)

                when {
                    path.contains("release") -> openFragment(
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                            HomeHandler({ searchString ->
                                search(
                                    searchString
                                )
                            }, { openSettings() }, id)
                        )
                    )
                    path.contains("playlist") -> openFragment(
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                            PlaylistHandler(id)
                        )
                    )
                    else -> openFragment(
                        de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                            HomeHandler({ searchString ->
                                search(
                                    searchString
                                )
                            }, { openSettings() }, null)
                        )
                    )
                }
            }
            else -> {
                openFragment(
                    de.lucaspape.monstercat.ui.fragments.Fragment.newInstance(
                        HomeHandler({ searchString ->
                            search(
                                searchString
                            )
                        }, { openSettings() }, null)
                    )
                )
            }
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
            setCover(
                this,
                currentSongId
            ) { bitmap ->
                updateNotification(
                    this,
                    currentSongId,
                    bitmap
                )
            }
        }

        //start download service
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

        exoPlayerSongId = ""
        preparedExoPlayerSongId = ""
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
        val settings = Settings.getSettings(this)

        val darkMode = settings.getBoolean(getString(R.string.darkThemeSetting))

        if (darkMode != null) {
            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                if (fallbackBlackFile.exists() && fallbackBlackFileLow.exists()) {
                    fallbackBlackFile.copyTo(fallbackFile, true)
                    fallbackBlackFileLow.copyTo(fallbackFileLow, true)
                }

                switchDrawablesToWhite()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

                if (fallbackWhiteFile.exists() && fallbackWhiteFileLow.exists()) {
                    fallbackWhiteFile.copyTo(fallbackFile, true)
                    fallbackWhiteFileLow.copyTo(fallbackFileLow, true)
                }

                switchDrawablesToBlack()
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
        val settings = Settings.getSettings(this)

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
