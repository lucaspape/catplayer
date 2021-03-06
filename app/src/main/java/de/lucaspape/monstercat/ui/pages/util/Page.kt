package de.lucaspape.monstercat.ui.pages.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class Page : Fragment() {
    abstract val layout: Int

    open fun onBackPressed(view: View):Boolean{
        return true
    }
    
    abstract fun onPause(view: View)
    abstract fun onCreate(view: View)

    abstract val pageName:String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onCreate(view)
    }

    override fun onPause() {
        super.onPause()

        view?.let {
            onPause(it)
        }
    }

    fun onPageBackPressed():Boolean {
        view?.let {
            return onBackPressed(it)
        }
        
        return true
    }
}