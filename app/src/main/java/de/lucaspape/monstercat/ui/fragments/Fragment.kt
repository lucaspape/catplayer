package de.lucaspape.monstercat.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import de.lucaspape.monstercat.ui.handlers.Handler

class Fragment(private val handler:Handler) : Fragment() {

    private var search: String? = null

    constructor(handler: Handler, search: String) : this(handler) {
        if (search != "") {
            this.search = search
        }
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
        return inflater.inflate(handler.getLayout(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handler.onCreate(view, search)
    }

    override fun onPause() {
        super.onPause()

        view?.let {
            handler.onPause(it)
        }
    }

    fun onBackPressed(){
        view?.let {
            handler.onBackPressed(it)
        }
    }
}