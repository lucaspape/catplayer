package de.lucaspape.monstercat.ui.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.util.TypedValue
import android.widget.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.core.download.*
import de.lucaspape.monstercat.core.music.*
import de.lucaspape.monstercat.core.music.notification.hideLoadingRelatedSongsNotification
import de.lucaspape.monstercat.core.music.notification.showLoadingRelatedNotification
import de.lucaspape.monstercat.core.music.notification.updateNotification
import de.lucaspape.monstercat.core.music.save.PlayerSaveState
import de.lucaspape.monstercat.core.music.util.*
import de.lucaspape.monstercat.core.music.util.setCover
import de.lucaspape.monstercat.ui.*
import de.lucaspape.monstercat.util.*
import de.lucaspape.monstercat.core.util.Settings
import de.lucaspape.monstercat.request.async.checkCustomApiFeatures
import de.lucaspape.monstercat.request.async.loadRelatedTracks
import de.lucaspape.monstercat.ui.pages.*
import de.lucaspape.monstercat.ui.pages.util.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException

var downloadServiceIntent: Intent? = null
var lastOpenPage: String? = null

val genericScope = CoroutineScope(Dispatchers.Default)

/**
 * Main activity
 */
class MainActivity : AppCompatActivity() {
    private var currentPage: Page? = null
        set(value) {
            lastOpenPage = value?.pageName
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFallbackCoverFiles(this)

        checkPermissions()

        downloadFallbackCoverImagesAsync(this) { changeTheme() }

        changeTheme()

        genericScope.launch {
            withContext(Dispatchers.Main) {
                checkCustomApiFeatures(this@MainActivity, {}, {})
            }
        }

        val settings = Settings.getSettings(this)

        if (settings.getString(getString(R.string.appVersionSetting)) != packageManager.getPackageInfo(
                packageName,
                0
            ).versionName
        ) {
            onUpgrade()
        }

        applyPlayerSettings(this)

        login()

        setContentView(R.layout.activity_main)

        registerListeners()

        if (!openFirstPage()) {
            val lastOpenPage = lastOpenPage

            if (lastOpenPage != null) {
                when (lastOpenPage) {
                    HomePage.homePageName -> findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                        R.id.navigation_home
                    PlaylistPage.playlistPageName -> findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                        R.id.navigation_playlist
                    ExplorePage.explorePageName -> findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                        R.id.navigation_explore
                    else -> findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                        R.id.navigation_home
                }
            } else {
                findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                    R.id.navigation_home
            }
        }

        setupPlayer()

        startDownloadService()

        showPrivacyPolicy()
    }

    private fun checkPermissions() {
        //check for internet
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
            != PackageManager.PERMISSION_GRANTED
        ) {
            displayInfo(this, getString(R.string.noInternetAccessError))
        }
    }

    private fun login() {
        val settings = Settings.getSettings(this)

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
    }

    private fun openFirstPage(): Boolean {
        val intentExtras = intent.extras
        val path = intent.data?.path

        //check for extras in intent (open search on startup, open album or playlist on startup using links)
        when {
            intentExtras?.get("search") != null -> {
                openSearch(intentExtras["search"] as String)

                return true
            }
            path != null -> {
                val id = path.substring(path.lastIndexOf("/") + 1, path.length)

                when {
                    path.contains("release") -> openHome(id, false)
                    path.contains("playlist") -> openPlaylist(id, false)
                    else -> openHome(null, false)
                }

                return true
            }
            else -> {
                return false
            }
        }
    }

    private fun onUpgrade() {
        val settings = Settings.getSettings(this)

        try {
            File("$cacheDir/player_state.obj").delete()
        } catch (e: FileNotFoundException) {

        }

        settings.setString(
            getString(R.string.appVersionSetting),
            packageManager.getPackageInfo(packageName, 0).versionName
        )
    }

    private fun setupPlayer() {
        bindPlayerUICallbacks()

        setupMusicPlayer(
            { context, callback, errorCallback ->
                Settings.getSettings(context)
                    .getBoolean(context.getString(R.string.skipMonstercatSongsSetting))?.let {
                        genericScope.launch {
                            val relatedTo = if(history.size > 0){
                                history
                            }else{
                                playlist
                            }

                            loadRelatedTracks(
                                context, relatedTo, playlist, it,
                                finishedCallback = { relatedIdArray ->
                                    relatedIdArray?.let {
                                        callback(relatedIdArray)
                                    }
                                },
                                errorCallback = {
                                    errorCallback()
                                })
                        }
                    }
            },
            { context: Context, msg: String ->
                displayInfo(context, msg)
            },
            Intent(this, MainActivity::class.java)
        )

        //create the MusicPlayer.kt mediasession
        createMediaSession(this)

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
    }

    private fun startDownloadService() {
        //start download service
        if (DownloadService.downloadTask?.active == true) {
            downloadServiceIntent = Intent(this, DownloadService::class.java)
            startService(downloadServiceIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        //if app closed
        hideDownloadNotification(this)

        PlayerSaveState.saveMusicPlayerState(this)

        exoPlayerSongId = ""
        preparedExoPlayerSongId = ""
    }

    override fun onPause() {
        super.onPause()

        PlayerSaveState.saveMusicPlayerState(this)
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
        var value = currentPage?.onPageBackPressed()
        if(value == null){
            value = true
        }

        if(value){
            finish()
        }
    }

    private fun registerListeners() {
        findViewById<ImageButton>(R.id.playButton).setOnClickListener {
            toggleMusic(this)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar).setOnClickListener {
            startActivity(
                Intent(applicationContext, PlayerFullscreenActivity::class.java)
            )
        }

        findViewById<BottomNavigationView>(R.id.nav_view).setOnNavigationItemSelectedListener(
            BottomNavigationView.OnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_explore -> {
                        openExplore(currentPage?.pageName == ExplorePage.explorePageName)

                        return@OnNavigationItemSelectedListener true
                    }
                    R.id.navigation_home -> {
                        openHome(null, currentPage?.pageName == HomePage.homePageName)

                        return@OnNavigationItemSelectedListener true
                    }
                    R.id.navigation_playlist -> {
                        openPlaylist(null, currentPage?.pageName == PlaylistPage.playlistPageName)

                        return@OnNavigationItemSelectedListener true
                    }
                }
                false
            })
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bindPlayerUICallbacks() {
        val titleTextView = findViewById<TextView>(R.id.songCurrentText)
        val seekbar = findViewById<SeekBar>(R.id.seekBar)
        val barCoverImage = findViewById<ImageView>(R.id.barCoverImage)
        val playButton = findViewById<ImageButton>(R.id.playButton)
        val musicBar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar)

        titleChangedCallback = {
            val titleWithArtist = "${de.lucaspape.monstercat.core.music.util.title} - $artist"
            titleTextView.text = titleWithArtist

            musicBar.isVisible = de.lucaspape.monstercat.core.music.util.title.isNotEmpty()
            barCoverImage.isVisible = de.lucaspape.monstercat.core.music.util.title.isNotEmpty()
            playButton.isVisible = de.lucaspape.monstercat.core.music.util.title.isNotEmpty()
        }

        musicBar.isVisible = de.lucaspape.monstercat.core.music.util.title.isNotEmpty()
        barCoverImage.isVisible = de.lucaspape.monstercat.core.music.util.title.isNotEmpty()
        playButton.isVisible = de.lucaspape.monstercat.core.music.util.title.isNotEmpty()

        val titleWithArtist = "${de.lucaspape.monstercat.core.music.util.title} - $artist"
        titleTextView.text = titleWithArtist

        artistChangedCallback = titleChangedCallback

        seekbar.progress = currentPosition

        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    exoPlayer?.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        seekbar.setOnTouchListener { _, _ -> true }

        currentPositionChangedCallback = {
            seekbar.progress = currentPosition
        }

        durationChangedCallback = {
            seekbar.max = duration
        }

        barCoverImage.setImageBitmap(coverBitmap)

        playingChangedCallback = {
            if (visiblePlaying) {
                playButton.setImageURI(pauseButtonDrawable.toUri())

            } else {
                playButton.setImageURI(playButtonDrawable.toUri())
            }
        }
        
        playingChangedCallback()

        coverBitmapChangedCallback = {
            barCoverImage.setImageBitmap(coverBitmap)
        }

        coverDrawableChangedCallback = {
            barCoverImage.setImageDrawable(coverDrawable)
        }

        setTagCallback = { target ->
            barCoverImage.tag = target
        }

        loadingRelatedChangedCallback = {
            if(loadingRelatedSongs){
                showLoadingRelatedNotification(this)
            }else{
                hideLoadingRelatedSongsNotification(this)
            }
        }
    }

    private fun openPage(page: Page) {
        currentPage = page

        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, page)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun openSearch(searchString: String?) {
        openPage(
            SearchPage(
                searchString
            ) {
                findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                    R.id.navigation_home
            }
        )
    }

    private fun openSettings() {
        openPage(
            SettingsPage {
                findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                    R.id.navigation_home
            }

        )
    }

    private fun openExplore(resetPosition: Boolean) {
        openPage(
            ExplorePage(
                { searchString ->
                    openSearch(
                        searchString
                    )
                },
                { openSettings() },
                resetPosition,
                {
                    findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                        R.id.navigation_home
                }
            )
        )
    }

    private fun openHome(albumMcId: String?, resetPosition: Boolean) {
        openPage(
            HomePage(
                { searchString ->
                    openSearch(
                        searchString
                    )
                },
                { openSettings() },
                { openQueue() },
                albumMcId,
                resetPosition

            )
        )
    }

    private fun openPlaylist(playlistId: String?, resetPosition: Boolean) {
        openPage(
            PlaylistPage(
                playlistId,
                resetPosition
            ) {
                findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                    R.id.navigation_home
            }

        )
    }

    private fun openQueue() {
        openPage(
            QueuePage() {
                findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                    R.id.navigation_home
            }
        )
    }
}
