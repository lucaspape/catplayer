package de.lucaspape.monstercat.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.R
import de.lucaspape.monstercat.ui.handlers.Handler

class Fragment() : Fragment() {

    private var search: String? = null
    private var handler:Handler? = null

    constructor(handler: Handler, search: String) : this(handler) {
        if (search != "") {
            this.search = search
        }
    }

    constructor(handler:Handler): this(){
        this.handler = handler
    }

    companion object {
        fun newInstance(handler: Handler, search: String): Fragment =
            Fragment(handler, search)

        fun newInstance(handler: Handler): Fragment =
            Fragment(handler)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        handler?.layout?.let {
            return inflater.inflate(it, container, false)
        }

        return inflater.inflate(R.layout.empty_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handler?.onCreate(view, search)
    }

    override fun onPause() {
        super.onPause()

        view?.let {
            handler?.onPause(it)
        }
    }

    fun onBackPressed(){
        view?.let {
            handler?.onBackPressed(it)
        }
    }
}