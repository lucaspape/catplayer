package de.lucaspape.monstercat.ui.handlers

import android.view.View

interface Handler {
    fun onBackPressed()
    fun onPause()
    fun getLayout(): Int
    fun onCreate(view: View, search:String?)
}