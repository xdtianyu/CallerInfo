package org.xdty.callerinfo.activity

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.xdty.callerinfo.utils.Utils

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutId)
        setTitle(titleId)
    }

    override fun attachBaseContext(newBase: Context) {
        val context: Context = Utils.changeLang(newBase)
        super.attachBaseContext(context)
    }

    protected abstract val layoutId: Int
    protected abstract val titleId: Int
}