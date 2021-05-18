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
import androidx.viewpager.widget.ViewPager
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
import de.lucaspape.monstercat.ui.activities.util.CoverImageViewPagerAdapter
import de.lucaspape.monstercat.ui.activities.util.SongTitleViewPagerAdapter
import de.lucaspape.monstercat.ui.pages.*
import de.lucaspape.monstercat.ui.pages.util.Page
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

var downloadServiceIntent: Intent? = null
var lastOpenPage: String? = null

val genericScope = CoroutineScope(Dispatchers.Default)

fun login(context: Context) {
    val settings = Settings.getSettings(context)

    //login
    if (!loggedIn) {
        val sUsername = settings.getString(context.getString(R.string.emailSetting))
        val sPassword = settings.getString(context.getString(R.string.passwordSetting))

        if ((sUsername != null && sPassword != null) && (sUsername != "" && sPassword != "")) {
            Auth().checkLogin(context, {
                //login success
                println(context.getString(R.string.loginSuccessfulMsg))

                //create the MusicPlayer.kt mediasession
                createMediaSession(context, true)
            }, {
                //login failed, retrieve new SID

                //login to monstercat
                Auth().login(context, sUsername, sPassword, {
                    println(context.getString(R.string.loginSuccessfulMsg))

                    //create the MusicPlayer.kt mediasession
                    createMediaSession(context, true)
                }, {
                    displayInfo(context, context.getString(R.string.loginFailedMsg))

                    //create the MusicPlayer.kt mediasession
                    createMediaSession(context, true)
                })

            })
        } else {
            createMediaSession(context, true)
        }
    }
}

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

        checkPermissions()

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

        login(this)

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

        PlayerSaveState.delete(this)

        settings.setString(
            getString(R.string.appVersionSetting),
            packageManager.getPackageInfo(packageName, 0).versionName
        )
    }

    private fun setupPlayer() {
        bindPlayerUICallbacks()

        setupMusicPlayer(
            this,
            { context, callback, errorCallback ->
                genericScope.launch {
                    val relatedTo = if (history.size > 0) {
                        history
                    } else {
                        playlist
                    }

                    loadRelatedTracks(
                        context, relatedTo, playlist,
                        finishedCallback = { relatedIdArray ->
                            relatedIdArray?.let {
                                callback(relatedIdArray)
                            }
                        },
                        errorCallback = {
                            errorCallback()
                        })
                }
            },
            { context: Context, msg: String ->
                displayInfo(context, msg)
            },
            Intent(this, MainActivity::class.java)
        )

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

        PlayerSaveState.save(this)

        exoPlayerSongId = ""
        preparedExoPlayerSongId = ""
    }

    override fun onPause() {
        super.onPause()

        PlayerSaveState.save(this)
    }

    private fun changeTheme() {
        val settings = Settings.getSettings(this)

        val darkMode = settings.getBoolean(getString(R.string.darkThemeSetting))

        if (darkMode != null) {
            if (darkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

                switchDrawablesToWhite()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

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

        if (value == null) {
            value = true
        }

        if (value) {
            finish()
        }
    }

    private fun registerListeners() {
        findViewById<ImageButton>(R.id.playButton)?.setOnClickListener {
            toggleMusic(this)
        }

        findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar)?.setOnClickListener {
            openFullscreenActivity()
        }

        findViewById<ImageView>(R.id.fullscreenAlbumImage)?.setOnClickListener {
            openFullscreenActivity()
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

        val playButton = findViewById<ImageButton>(R.id.fullScreenPlay)
        val backButton = findViewById<ImageButton>(R.id.fullscreenPrev)
        val nextButton = findViewById<ImageButton>(R.id.fullscreenNext)
        val shuffleButton = findViewById<ImageButton>(R.id.fullscreenShuffle)
        val loopButton = findViewById<ImageButton>(R.id.fullscreenLoop)

        if (shuffle) {
            shuffleButton?.setImageResource(R.drawable.ic_shuffle_green_24dp)
        } else {
            shuffleButton?.setImageResource(R.drawable.ic_shuffle_24dp)
        }

        when {
            loop -> {
                loopButton?.setImageResource(R.drawable.ic_repeat_green_24dp)
            }
            loopSingle -> {
                loopButton?.setImageResource(R.drawable.ic_repeat_one_green_24dp)
            }
            else -> {
                loopButton?.setImageResource(R.drawable.ic_repeat_24dp)
            }
        }

        playButton?.setOnClickListener {
            toggleMusic(this)
        }

        nextButton?.setOnClickListener {
            next(this)
        }

        backButton?.setOnClickListener {
            previous(this)
        }

        shuffleButton?.setOnClickListener {
            if (shuffle) {
                shuffle = false
                shuffleButton.setImageResource(R.drawable.ic_shuffle_24dp)
            } else {
                shuffle = true
                shuffleButton.setImageResource(R.drawable.ic_shuffle_green_24dp)
            }
        }

        loopButton?.setOnClickListener {
            when {
                loop -> {
                    loop = false

                    loopSingle = true

                    loopButton.setImageResource(R.drawable.ic_repeat_one_green_24dp)
                }
                loopSingle -> {
                    loopSingle = false
                    loopButton.setImageResource(R.drawable.ic_repeat_24dp)
                }
                else -> {
                    loop = true
                    loopButton.setImageResource(R.drawable.ic_repeat_green_24dp)
                }
            }
        }

        val titleTextView = findViewById<TextView>(R.id.fullscreenTitle)
        val artistTextView = findViewById<TextView>(R.id.fullscreenArtist)

        titleTextView?.setOnClickListener {
            openSearch(titleTextView.text.toString())
        }

        artistTextView?.setOnClickListener {
            openSearch(artistTextView.text.toString())
        }
    }

    var lastPageSelect: Long = 0

    @SuppressLint("ClickableViewAccessibility")
    fun bindPlayerUICallbacks() {
        val viewPager = findViewById<ViewPager>(R.id.currentSongTextViewPager)
        val seekbar = findViewById<SeekBar>(R.id.seekBar)
        val barCoverImage = findViewById<ImageView>(R.id.barCoverImage)
        val playButton = findViewById<ImageButton>(R.id.playButton)
        val musicBar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.musicBar)

        val titleTextViewFullscreen = findViewById<TextView>(R.id.fullscreenTitle)
        val artistTextViewFullscreen = findViewById<TextView>(R.id.fullscreenArtist)
        val seekbarFullscreen = findViewById<SeekBar>(R.id.fullscreenSeekBar)
        // val barCoverImageFullscreen = findViewById<ImageView>(R.id.fullscreenAlbumImage)
        val fullscreenViewPager = findViewById<ViewPager>(R.id.fullscreenViewPager)
        val playButtonFullscreen = findViewById<ImageButton>(R.id.fullScreenPlay)

        val songTimePassedFullscreen = findViewById<TextView>(R.id.songTimePassed)
        val songTimeMaxFullscreen = findViewById<TextView>(R.id.songTimeMax)

        titleChangedCallback = {
            viewPager?.adapter = SongTitleViewPagerAdapter(this) {
                openFullscreenActivity()
            }

            viewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {

                }

                override fun onPageSelected(position: Int) {
                    if (System.currentTimeMillis() - lastPageSelect > 200) {
                        lastPageSelect = System.currentTimeMillis()

                        when (position) {
                            0 -> {
                                previous(this@MainActivity)
                            }

                            2 -> {
                                next(this@MainActivity)
                            }

                        }
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {

                }

            })

            viewPager?.currentItem = 1

            fullscreenViewPager?.adapter = CoverImageViewPagerAdapter(this) {
                openFullscreenActivity()
            }

            fullscreenViewPager?.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {

                }

                override fun onPageSelected(position: Int) {
                    if (System.currentTimeMillis() - lastPageSelect > 200) {
                        lastPageSelect = System.currentTimeMillis()

                        when (position) {
                            0 -> {
                                previous(this@MainActivity)
                            }

                            2 -> {
                                next(this@MainActivity)
                            }

                        }
                    }
                }

                override fun onPageScrollStateChanged(state: Int) {

                }

            })

            fullscreenViewPager?.currentItem = 1

            titleTextViewFullscreen?.text = de.lucaspape.monstercat.core.music.util.title
            artistTextViewFullscreen?.text = artist

            val visible = de.lucaspape.monstercat.core.music.util.title.isNotEmpty()

            musicBar?.isVisible = visible
            barCoverImage?.isVisible = visible
            playButton?.isVisible = visible
            viewPager?.isVisible = visible
        }

        artistChangedCallback = titleChangedCallback

        seekbar?.progress = currentPosition.toInt()
        seekbarFullscreen?.progress = currentPosition.toInt()

        seekbarFullscreen?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser)
                    exoPlayer?.seekTo(progress.toLong())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })

        seekbar?.setOnTouchListener { _, _ -> true }

        currentPositionChangedCallback = {
            seekbar?.progress = currentPosition.toInt()
            seekbarFullscreen?.progress = currentPosition.toInt()

            val minutes = currentPosition / 60000
            val seconds = (currentPosition % 60000) / 1000

            val text = if (seconds < 10) {
                "$minutes:0$seconds"
            } else {
                "$minutes:$seconds"
            }

            songTimePassedFullscreen?.text = text
        }

        durationChangedCallback = {
            seekbar?.max = duration.toInt()
            seekbarFullscreen?.max = duration.toInt()

            val minutes = duration / 60000
            val seconds = (duration % 60000) / 1000

            val text = if (seconds < 10) {
                "$minutes:0$seconds"
            } else {
                "$minutes:$seconds"
            }

            songTimeMaxFullscreen?.text = text
        }

        barCoverImage?.setImageBitmap(coverBitmap)

        playingChangedCallback = {
            if (visiblePlaying) {
                playButton?.setImageURI(pauseButtonDrawable.toUri())
                playButtonFullscreen?.setImageURI(pauseButtonDrawable.toUri())

            } else {
                playButton?.setImageURI(playButtonDrawable.toUri())
                playButtonFullscreen?.setImageURI(playButtonDrawable.toUri())
            }
        }

        coverBitmapChangedCallback = {
            barCoverImage?.setImageBitmap(coverBitmap)
        }

        coverDrawableChangedCallback = {
            barCoverImage?.setImageDrawable(coverDrawable)
        }

        setTagCallback = { target ->
            barCoverImage?.tag = target
        }

        loadingRelatedChangedCallback = {
            if (loadingRelatedSongs) {
                showLoadingRelatedNotification(this)
            } else {
                hideLoadingRelatedSongsNotification(this)
            }
        }

        refreshUI()
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
            SettingsPage({ openFilterSettings() }) {
                findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                    R.id.navigation_home
            }

        )
    }

    private fun openFilterSettings() {
        openPage(FilterPage {
            openSettings()
        })
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
            QueuePage {
                findViewById<BottomNavigationView>(R.id.nav_view).selectedItemId =
                    R.id.navigation_home
            }
        )
    }

    private fun openFullscreenActivity(){
        startActivity(
            Intent(applicationContext, PlayerFullscreenActivity::class.java)
        )

        finish()
    }
}