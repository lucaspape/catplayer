package de.lucaspape.monstercat.ui.handlers

import android.view.View

abstract class Handler(onSearch:(searchString:String?) -> Unit) {
    abstract val layout:Int

    abstract fun onBackPressed(view: View)
    abstract fun onPause(view: View)
    abstract fun onCreate(view: View)
}