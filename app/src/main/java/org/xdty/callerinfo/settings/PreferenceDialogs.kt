package org.xdty.callerinfo.settings

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import org.xdty.callerinfo.R
import org.xdty.callerinfo.settings.dialog.*
import org.xdty.callerinfo.utils.Utils.Companion.mask
import org.xdty.callerinfo.utils.Window

class PreferenceDialogs(private val context: Context, private val sharedPrefs: SharedPreferences, private val window: Window, private val preferenceActions: PreferenceActions) {
    fun showSeekBarDialog(keyId: Int, bundleKey: String?, defaultValue: Int,
                          max: Int, title: Int, textRes: Int) {
        SeekBarDialog(context, sharedPrefs)
                .max(max)
                .defaultValue(defaultValue)
                .seek { progress -> window.sendData(bundleKey, progress, Window.Type.SETTING) }
                .title(title)
                .key(keyId)
                .confirm { }
                .cancel(R.string.cancel, null)
                .dismiss { window.closeWindow() }
                .show()
        window.showTextWindow(textRes, Window.Type.SETTING)
    }

    fun showApiDialog(keyId: Int, title: Int, url: Int) {
        val key = context.getString(keyId)
        EditDialog(context, sharedPrefs)
                .key(key)
                .title(title)
                .confirm { value -> preferenceActions.findPreference(key)?.summary = mask(value!!) }
                .cancel(R.string.cancel, null)
                .help(R.string.fetch) { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(url)))) }
                .show()
    }

    @JvmOverloads
    fun showRadioDialog(keyId: Int, title: Int, listId: Int, defValue: Int,
                        offset: Int = 0) {
        val key = context.getString(keyId)
        RadioDialog(context, sharedPrefs)
                .listId(listId)
                .offset(offset)
                .defaultValue(defValue)
                .check { value ->
                    val preference = preferenceActions.findPreference(key)
                    if (preference != null) {
                        preference.summary = value
                    }
                }
                .key(key)
                .title(title)
                .show()
    }

    fun showTextDialog(title: Int, text: Int) {
        showTextDialog(title, context.getString(text))
    }

    fun showTextDialog(title: Int, text: String?) {
        TextDialog(context, sharedPrefs)
                .title(title)
                .text(text!!)
                .show()
    }

    fun showConfirmDialog(title: Int, text: Int, key: Int) {
        TextDialog(context, sharedPrefs)
                .title(title)
                .text(text)
                .cancel { preferenceActions.onConfirmCanceled(key) }
                .confirm { preferenceActions.onConfirmed(key) }
                .show()
    }

    fun showCustomApiDialog() {
        val key = context.getString(R.string.custom_api_url)
        CustomApiDialog(context, sharedPrefs)
                .title(R.string.custom_api)
                .key(key)
                .confirm { value -> preferenceActions.findPreference(key)?.summary = value }
                .cancel(R.string.cancel, null)
                .help(R.string.document) {
                    context.startActivity(Intent(Intent.ACTION_VIEW,
                            Uri.parse(context.getString(R.string.api_document_url))))
                }
                .show()
    }

    @JvmOverloads
    fun showEditDialog(keyId: Int, title: Int, defaultText: Int, hint: Int,
                       help: Int = 0, helpText: Int = 0) {
        val key = context.getString(keyId)
        EditDialog(context, sharedPrefs)
                .key(key)
                .title(title)
                .hint(hint)
                .defaultText(defaultText)
                .confirm { value -> preferenceActions.findPreference(key)?.summary = value }
                .help(helpText) { showTextDialog(help, helpText) }
                .show()
    }

}