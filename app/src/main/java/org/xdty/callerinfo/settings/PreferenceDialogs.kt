package org.xdty.callerinfo.settings

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import org.xdty.callerinfo.R
import org.xdty.callerinfo.settings.dialog.*
import org.xdty.callerinfo.settings.dialog.RadioDialog.CheckedListener
import org.xdty.callerinfo.settings.dialog.SeekBarDialog.SeekListener
import org.xdty.callerinfo.settings.dialog.SettingsDialog.*
import org.xdty.callerinfo.utils.Utils.Companion.mask
import org.xdty.callerinfo.utils.Window

class PreferenceDialogs(private val context: Context, private val sharedPrefs: SharedPreferences, private val window: Window, private val preferenceActions: PreferenceActions) {
    fun showSeekBarDialog(keyId: Int, bundleKey: String?, defaultValue: Int,
                          max: Int, title: Int, textRes: Int) {
        SeekBarDialog(context, sharedPrefs)
                .max(max)
                .defaultValue(defaultValue)
                .seek(object : SeekListener() {
                    override fun onSeek(progress: Int) {
                        window.sendData(bundleKey, progress, Window.Type.SETTING)
                    }
                })
                .title(title)
                .key(keyId)
                .confirm(ConfirmListener())
                .cancel(R.string.cancel, null)
                .dismiss(DialogInterface.OnDismissListener { window.closeWindow() })
                .show()
        window.showTextWindow(textRes, Window.Type.SETTING)
    }

    fun showApiDialog(keyId: Int, title: Int, url: Int) {
        val key = context.getString(keyId)
        EditDialog(context, sharedPrefs)
                .key(key)
                .title(title)
                .confirm(object : ConfirmListener() {
                    override fun onConfirm(value: String?) {
                        preferenceActions.findPreference(key).summary = mask(value!!)
                    }
                })
                .cancel(R.string.cancel, null)
                .help(R.string.fetch, object : HelpListener() {
                    override fun onHelp() {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(url))))
                    }
                })
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
                .check(object : CheckedListener() {
                    override fun onChecked(value: String) {
                        val preference = preferenceActions.findPreference(key)
                        if (preference != null) {
                            preference.summary = value
                        }
                    }
                })
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
                .cancel(object : CancelListener() {
                    override fun onCancel() {
                        preferenceActions.onConfirmCanceled(key)
                    }
                })
                .confirm(object : ConfirmListener() {
                    override fun onConfirm(value: String?) {
                        preferenceActions.onConfirmed(key)
                    }
                })
                .show()
    }

    fun showCustomApiDialog() {
        val key = context.getString(R.string.custom_api_url)
        CustomApiDialog(context, sharedPrefs)
                .title(R.string.custom_api)
                .key(key)
                .confirm(object : ConfirmListener() {
                    override fun onConfirm(value: String?) {
                        preferenceActions.findPreference(key).summary = value
                    }
                })
                .cancel(R.string.cancel, null)
                .help(R.string.document, object : HelpListener() {
                    override fun onHelp() {
                        context.startActivity(Intent(Intent.ACTION_VIEW,
                                Uri.parse(context.getString(R.string.api_document_url))))
                    }
                })
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
                .confirm(object : ConfirmListener() {
                    override fun onConfirm(value: String?) {
                        preferenceActions.findPreference(key).summary = value
                    }
                })
                .help(helpText, object : HelpListener() {
                    override fun onHelp() {
                        showTextDialog(help, helpText)
                    }
                })
                .show()
    }

}