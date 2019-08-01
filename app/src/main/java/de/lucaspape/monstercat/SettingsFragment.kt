package de.lucaspape.monstercat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment

class SettingsFragment: Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        inflater.inflate(R.layout.fragment_settings, container, false)

    companion object {
        fun newInstance(): SettingsFragment = SettingsFragment()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val button = view.findViewById<Button>(R.id.add_account)

        button.setOnClickListener {
            val usernameInput = view.findViewById<EditText>(R.id.usernameInput)
            val passwordInput = view.findViewById<EditText>(R.id.passwordInput)

            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()

            val settings = Settings(view.context)
            settings.saveSetting("email", username)
            settings.saveSetting("password", password)

            Toast.makeText(view.context, "Updated! Please restart app to take effect.", Toast.LENGTH_SHORT)
                .show()
        }
    }
}