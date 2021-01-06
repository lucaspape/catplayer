package de.lucaspape.monstercat.ui.pages.util

import android.view.View

interface Page {
    val layout: Int

    fun onBackPressed(view: View)
    fun onPause(view: View)
    fun onCreate(view: View)
}