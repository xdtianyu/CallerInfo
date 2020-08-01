package org.xdty.callerinfo.fragment

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.PreferenceScreen
import android.preference.SwitchPreference
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.appbar.AppBarLayout
import org.xdty.callerinfo.R
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.settings.*
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Toasts.show
import org.xdty.callerinfo.utils.Utils.Companion.ignoreBatteryOptimization
import org.xdty.callerinfo.utils.Utils.Companion.isAppInstalled
import org.xdty.callerinfo.utils.Window
import javax.inject.Inject

@Suppress("DEPRECATION")
class SettingsFragment : PreferenceFragment(), PreferenceActions {

    @Inject
    lateinit var mWindow: Window

    @Inject
    lateinit var mSetting: Setting

    @Inject
    lateinit var mAlarm: Alarm

    private var startIntent: Intent? = null
    private lateinit var preferenceBinder: PreferenceBinder
    private lateinit var pluginBinder: PluginBinder

    private fun setStartIntent(intent: Intent) {
        startIntent = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appComponent.inject(this)
        addPreferencesFromResource(R.xml.settings)
        val sharedPrefs = preferenceManager.sharedPreferences
        val preferenceDialogs = PreferenceDialogs(activity, sharedPrefs, mWindow, this)
        pluginBinder = PluginBinder(activity, preferenceDialogs, this)
        val delegate = PreferenceDelegate()
        val clicker = PreferenceClicker(activity, sharedPrefs, preferenceDialogs, pluginBinder, this)
        delegate.actions = this
        delegate.clicker = clicker
        delegate.dialogs = preferenceDialogs
        preferenceBinder = PreferenceBinder(activity, sharedPrefs, preferenceDialogs, pluginBinder, this)
        preferenceBinder.bind()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (startIntent != null && startIntent?.action != null) {
            when (startIntent?.action) {
                PLUGIN_SETTING -> view.postDelayed({ openPreference(getString(R.string.plugin_key)) }, 500)
                else -> {
                }
            }
        }
    }

    private fun openPreference(key: String) {
        val preferenceScreen = preferenceScreen
        val listAdapter = preferenceScreen.rootAdapter
        val itemsCount = listAdapter.count
        var itemNumber = 0
        while (itemNumber < itemsCount) {
            if (listAdapter.getItem(itemNumber) == findPreference(key)) {
                preferenceScreen.onItemClick(null, null, itemNumber, 0)
                break
            }
            ++itemNumber
        }
    }

    override fun onDestroy() {
        if (isAppInstalled(activity, getString(R.string.plugin_package_name))) {
            pluginBinder.unBindPluginService()
        }
        super.onDestroy()
    }

    override fun onPreferenceTreeClick(preferenceScreen: PreferenceScreen,
                                       preference: Preference): Boolean {
        super.onPreferenceTreeClick(preferenceScreen, preference)
        if (preference is PreferenceScreen) {
            setUpNestedScreen(preference)
        }
        return false
    }

    override fun findPreference(key: CharSequence?): Preference? {
        var pref = preferenceBinder.findCachedPreference(key.toString())
        if (pref == null) {
            pref = super.findPreference(key)
        }
        return pref!!
    }

    private fun setUpNestedScreen(preferenceScreen: PreferenceScreen) {
        val dialog = preferenceScreen.dialog
        val listView = dialog.findViewById<ListView>(android.R.id.list)

        val appBarLayout: AppBarLayout
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N || Build.VERSION.RELEASE == "7.0" || Build.VERSION.RELEASE == "N") {
            val root = listView.parent as ViewGroup
            appBarLayout = LayoutInflater.from(activity).inflate(
                    R.layout.settings_toolbar, root, false) as AppBarLayout
            val height: Int
            val tv = TypedValue()
            height = if (activity.theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
                TypedValue.complexToDimensionPixelSize(tv.data,
                        resources.displayMetrics)
            } else {
                appBarLayout.height
            }
            listView.setPadding(0, height, 0, 0)
            root.addView(appBarLayout, 0)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val root = listView.parent as LinearLayout
            appBarLayout = LayoutInflater.from(activity).inflate(
                    R.layout.settings_toolbar, root, false) as AppBarLayout
            root.addView(appBarLayout, 0)
        } else {
            val root = dialog.findViewById<ViewGroup>(android.R.id.content)
            val content = root.getChildAt(0) as ListView
            root.removeAllViews()
            appBarLayout = LayoutInflater.from(activity).inflate(
                    R.layout.settings_toolbar, root, false) as AppBarLayout
            val height: Int
            val tv = TypedValue()
            height = if (activity.theme.resolveAttribute(R.attr.actionBarSize, tv, true)) {
                TypedValue.complexToDimensionPixelSize(tv.data,
                        resources.displayMetrics)
            } else {
                appBarLayout.height
            }
            content.setPadding(dpToPx(16f).toInt(), height, dpToPx(16f).toInt(), 0)
            root.addView(content)
            root.addView(appBarLayout)
        }
        val toolbar: Toolbar = appBarLayout.findViewById(R.id.toolbar)
        toolbar.title = preferenceScreen.title
        toolbar.setNavigationOnClickListener { dialog.dismiss() }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            R.string.ignore_known_contact_key,
            R.string.not_mark_contact_key,
            R.string.display_on_outgoing_key ->
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    setChecked(requestCode, false)
                }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                activity.resources.displayMetrics)
    }

    override fun resetOfflineDataUpgradeWorker() {
        if (!mSetting.isOfflineDataAutoUpgrade) {
            mAlarm.cancelUpgradeWork()
        } else {
            mAlarm.enqueueUpgradeWork()
        }
    }

    override fun onConfirmed(key: Int) {
        when (key) {
            R.string.import_key -> {
                PluginStatus.isCheckStorageExport = false
                pluginBinder.checkStoragePermission()
            }
            R.string.export_key -> {
                PluginStatus.isCheckStorageExport = true
                pluginBinder.checkStoragePermission()
            }
            R.string.ignore_battery_optimizations_key -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                        Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        }
    }

    override fun onConfirmCanceled(key: Int) {
        when (key) {
            R.string.auto_report_key -> (findPreference(
                    getString(R.string.auto_report_key)) as SwitchPreference).isChecked = false
            R.string.enable_marking_key -> (findPreference(
                    getString(R.string.enable_marking_key)) as SwitchPreference).isChecked = false
            R.string.ignore_battery_optimizations_key -> (findPreference(
                    getString(R.string.ignore_battery_optimizations_key)) as SwitchPreference).isChecked = ignoreBatteryOptimization(activity)
        }
    }

    override fun setChecked(key: Int, checked: Boolean) {
        val preference = findPreference(getString(key)) as SwitchPreference
        preference.isChecked = checked
    }

    override fun checkOfflineData() {
        show(activity, R.string.offline_data_checking)
        mAlarm.runUpgradeWorkOnce().observeForever(object : Observer<WorkInfo> {
            override fun onChanged(workInfo: WorkInfo) {
                Log.d(TAG, "onChanged: $workInfo")
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    show(activity, R.string.offline_data_success)
                    preferenceBinder.bindDataVersionPreference()
                } else if (workInfo.state == WorkInfo.State.FAILED) {
                    show(activity, R.string.offline_data_failed)
                }
                WorkManager.getInstance().getWorkInfoByIdLiveData(workInfo.id).removeObserver(this)
            }
        })
    }

    override fun removePreference(parent: Int, child: Int) {
        preferenceBinder.removePreference(parent, child)
    }

    override fun getKeyId(key: String?): Int {
        return preferenceBinder.getKeyId(key!!)
    }

    override fun addPreference(parent: Int, child: Int) {
        preferenceBinder.addPreference(parent, child)
    }

    companion object {
        private val TAG = SettingsFragment::class.java.simpleName
        private const val PLUGIN_SETTING = "org.xdty.callerinfo.action.PLUGIN_SETTING"
        fun newInstance(intent: Intent): SettingsFragment {
            val fragment = SettingsFragment()
            fragment.setStartIntent(intent)
            return fragment
        }
    }
}