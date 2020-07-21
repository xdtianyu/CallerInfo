package org.xdty.callerinfo.settings.dialog

import android.content.Context
import android.content.DialogInterface
import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.app.AlertDialog
import org.xdty.callerinfo.R

abstract class SettingsDialog(protected var context: Context, protected var sharedPrefs: SharedPreferences) {

    protected var builder: AlertDialog.Builder = AlertDialog.Builder(context)

    protected lateinit var key: String
    protected lateinit var layout: View

    protected var hint = 0
    protected var defaultText = 0
    private var help = 0

    private var listener: Listener? = null

    fun key(keyId: Int): SettingsDialog {
        key = context.getString(keyId)
        return this
    }

    fun key(key: String): SettingsDialog {
        this.key = key
        return this
    }

    fun title(titleId: Int): SettingsDialog {
        builder.setTitle(context.getString(titleId))
        return this
    }

    fun hint(hint: Int): SettingsDialog {
        this.hint = hint
        return this
    }

    fun defaultText(defaultText: Int): SettingsDialog {
        this.defaultText = defaultText
        return this
    }

    fun help(help: Int): SettingsDialog {
        this.help = help
        return this
    }

    fun listen(listener: Listener?) {
        this.listener = listener
    }

    fun show() {
        bindViews()
        builder.setPositiveButton(R.string.ok) { _: DialogInterface?, _: Int -> onConfirm() }
        builder.setNegativeButton(R.string.cancel) { _: DialogInterface?, _: Int -> onCancel() }
        if (help != 0) {
            builder.setNeutralButton(help) { _: DialogInterface?, _: Int -> onHelp() }
        }
        builder.setCancelable(true)
        builder.show()
    }

    protected abstract fun bindViews()
    protected abstract fun onConfirm()
    protected fun onConfirm(value: String?) {
        if (listener != null) {
            listener!!.onConfirm(value)
        }
    }

    protected fun onCancel() {
        if (listener != null) {
            listener!!.onCancel()
        }
    }

    protected fun onHelp() {
        if (listener != null) {
            listener!!.onHelp()
        }
    }

    open class Listener {
        open fun onConfirm(value: String?) {}
        fun onCancel() {}
        open fun onHelp() {}
    }

}