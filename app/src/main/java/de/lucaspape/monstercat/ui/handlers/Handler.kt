package de.lucaspape.monstercat.ui.handlers

import android.view.View

interface Handler {
    val layout:Int

    fun onBackPressed(view: View)
    fun onPause(view: View)
    fun onCreate(view: View)
}