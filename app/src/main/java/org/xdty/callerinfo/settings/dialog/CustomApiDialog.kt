package org.xdty.callerinfo.settings.dialog

import android.content.Context
import android.content.SharedPreferences
import android.view.View
import android.widget.EditText
import org.xdty.callerinfo.R

class CustomApiDialog(context: Context, sharedPrefs: SharedPreferences) : SettingsDialog(context, sharedPrefs) {

    private lateinit var apiUri: EditText
    private lateinit var apiKey: EditText

    private lateinit var customApiKey: String

    override fun bindViews() {
        val layout = View.inflate(context, R.layout.dialog_custom_api, null)
        builder.setView(layout)

        apiUri = layout.findViewById(R.id.api_uri)
        apiKey = layout.findViewById(R.id.api_key)

        customApiKey = context.getString(R.string.custom_api_key)

        apiUri.setText(sharedPrefs.getString(key, ""))
        apiKey.setText(sharedPrefs.getString(customApiKey, ""))
    }

    override fun onConfirm() {
        val value: String = apiUri.text.toString()
        val key: String = apiKey.text.toString()
        val editor = sharedPrefs.edit()
        editor.putString(key, value)
        editor.putString(customApiKey, key)
        editor.apply()

        super.onConfirm(value)
    }
}