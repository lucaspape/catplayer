package de.lucaspape.monstercat.ui.pages

import android.view.View
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.util.*
import de.lucaspape.monstercat.ui.pages.recycler.SettingsRecyclerPage
import de.lucaspape.monstercat.ui.pages.util.Page
import de.lucaspape.monstercat.ui.pages.util.RecyclerViewPage

/**
 * SettingsActivity
 */
class SettingsPage(
    private val openFilterSettings: () -> Unit,
    private val closeSettings: () -> Unit
) : Page() {
    constructor() : this({}, {})

    companion object {
        @JvmStatic
        val settingsPageName = "settings"
    }

    private var settingsPageObject: RecyclerViewPage? = null

    override fun onCreate(view: View) {
        settingsPageObject = SettingsRecyclerPage(openFilterSettings)
        settingsPageObject?.onCreate(view)

        loggedInStateChangedListeners.add(Listener({
            settingsPageObject?.reload(view)
        }, false))
    }

    override val pageName: String = settingsPageName

    override val layout: Int = R.layout.fragment_settings

    override fun onBackPressed(view: View): Boolean {
        closeSettings()
        return false
    }

    override fun onPause(view: View) {

    }
}