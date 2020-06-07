package org.xdty.callerinfo.activity

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import org.xdty.callerinfo.R

class LicensesActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        val webView = findViewById<WebView>(R.id.webView)
        val action = intent.action
        var url = "file:///android_res/raw/licenses.html"
        when (action) {
            ACTION_LICENSE -> {
            }
            ACTION_PRIVACY -> {
                setTitle(R.string.privacy_notice)
                url = "file:///android_res/raw/privacy_notice.html"
            }
            ACTION_FEATURE -> {
                setTitle(R.string.feature_notice)
                url = "file:///android_res/raw/feature_notice.html"
            }
        }
        webView?.loadUrl(url)
    }

    override val layoutId: Int
        get() = R.layout.activity_licenses

    override val titleId: Int
        get() = R.string.license

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        private const val ACTION_LICENSE = "org.xdty.callerinfo.action.VIEW_LICENSES"
        private const val ACTION_PRIVACY = "org.xdty.callerinfo.action.VIEW_PRIVACY"
        private const val ACTION_FEATURE = "org.xdty.callerinfo.action.VIEW_FEATURE"
    }
}