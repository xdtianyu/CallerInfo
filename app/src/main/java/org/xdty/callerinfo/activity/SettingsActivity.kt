package org.xdty.callerinfo.activity

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import org.xdty.callerinfo.R
import org.xdty.callerinfo.fragment.SettingsFragment
import org.xdty.callerinfo.utils.Utils

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.action_settings)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(android.R.id.content, SettingsFragment.newInstance(intent))
                    .commit()
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val context: Context = Utils.changeLang(newBase)
        super.attachBaseContext(context)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            finish()
        }
        return true
    }
}