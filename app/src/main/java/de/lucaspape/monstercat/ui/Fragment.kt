package de.lucaspape.monstercat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.pages.util.Page

class Fragment() : Fragment() {

    private var page: Page? = null

    constructor(page: Page) : this() {
        this.page = page
    }

    companion object {
        fun newInstance(page: Page): Fragment =
            Fragment(page)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        page?.layout?.let {
            return inflater.inflate(it, container, false)
        }

        return inflater.inflate(R.layout.empty_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        page?.onCreate(view)
    }

    override fun onPause() {
        super.onPause()

        view?.let {
            page?.onPause(it)
        }
    }

    fun onBackPressed() {
        view?.let {
            page?.onBackPressed(it)
        }
    }
}