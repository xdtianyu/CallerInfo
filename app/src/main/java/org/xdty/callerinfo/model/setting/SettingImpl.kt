package org.xdty.callerinfo.model.setting

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import android.preference.PreferenceManager
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import org.xdty.callerinfo.BuildConfig
import org.xdty.callerinfo.R
import org.xdty.callerinfo.model.Status
import org.xdty.callerinfo.utils.Utils
import java.util.*
import kotlin.collections.ArrayList

class SettingImpl private constructor() : Setting {
    private val mPrefs: SharedPreferences
    private val mWindowPrefs: SharedPreferences
    private var isOutgoing = false

    override val screenWidth: Int
        get() = Resources.getSystem().displayMetrics.widthPixels

    override val screenHeight: Int
        get() = Resources.getSystem().displayMetrics.heightPixels

    override val statusBarHeight: Int
        get() {
            var result = 0
            val resourceId = sContext!!.resources
                    .getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                result = sContext!!.resources.getDimensionPixelSize(resourceId)
            }
            return result
        }

    override val isEulaSet: Boolean
        get() = mPrefs.getBoolean("eula", false)

    override val windowHeight: Int
        get() = mPrefs.getInt(getString(R.string.window_height_key), defaultHeight)

    override val defaultHeight: Int
        get() = screenHeight / 8

    override val isShowCloseAnim: Boolean
        get() = mPrefs.getBoolean(getString(R.string.window_close_anim_key), true)

    override val isTransBackOnly: Boolean
        get() = mPrefs.getBoolean(getString(R.string.window_trans_back_only_key), true)

    override val isEnableTextColor: Boolean
        get() = mPrefs.getBoolean(getString(R.string.window_text_color_key), false)

    override val textPadding: Int
        get() = mPrefs.getInt(getString(R.string.window_text_padding_key), 0)

    override val textAlignment: Int
        get() = mPrefs.getInt(getString(R.string.window_text_alignment_key), 1)

    override val textSize: Int
        get() = mPrefs.getInt(getString(R.string.window_text_size_key), 20)

    override val windowTransparent: Int
        get() = mPrefs.getInt(getString(R.string.window_transparent_key), 80)

    override val isDisableMove: Boolean
        get() = mPrefs.getBoolean(getString(R.string.disable_move_key), false)

    override val isAutoReportEnabled: Boolean
        get() = mPrefs.getBoolean(getString(R.string.auto_report_key), false)

    override val isMarkingEnabled: Boolean
        get() = mPrefs.getBoolean(getString(R.string.enable_marking_key), false)

    override fun addPaddingMark(number: String) {
        val key = getString(R.string.padding_mark_numbers_key)
        val list = paddingMarks
        if (list.contains(number)) {
            return
        }
        list.add(number)
        val paddingNumbers = gson.toJson(list)
        mPrefs.edit().putString(key, paddingNumbers).apply()
    }

    override fun removePaddingMark(number: String) {
        val key = getString(R.string.padding_mark_numbers_key)
        val list = paddingMarks
        if (!list.contains(number)) {
            return
        }
        list.remove(number)
        val paddingNumbers = gson.toJson(list)
        mPrefs.edit().putString(key, paddingNumbers).apply()
    }

    override val paddingMarks: ArrayList<String>
        get() {
            val key = getString(R.string.padding_mark_numbers_key)
            return if (mPrefs.contains(key)) {
                val paddingNumbers = mPrefs.getString(key, null)
                ArrayList(
                        Arrays.asList(*gson.fromJson(paddingNumbers, Array<String>::class.java)))
            } else {
                ArrayList()
            }
        }

    override val uid: String
        get() {
            val key = getString(R.string.uid_key)
            return if (mPrefs.contains(key)) {
                mPrefs.getString(key, "")!!
            } else {
                val uid = Utils.getDeviceId(sContext!!)
                mPrefs.edit().putString(key, uid).apply()
                uid
            }
        }

    override fun updateLastScheduleTime() {
        updateLastScheduleTime(System.currentTimeMillis())
    }

    override fun updateLastScheduleTime(timestamp: Long) {
        mPrefs.edit()
                .putLong(getString(R.string.last_schedule_time_key), timestamp)
                .apply()
    }

    override fun lastScheduleTime(): Long {
        return mPrefs.getLong(getString(R.string.last_schedule_time_key), 0)
    }

    override fun lastCheckDataUpdateTime(): Long {
        return mPrefs.getLong(getString(R.string.last_check_data_update_time_key), 0)
    }

    override fun updateLastCheckDataUpdateTime(timestamp: Long) {
        mPrefs.edit()
                .putLong(getString(R.string.last_check_data_update_time_key), timestamp)
                .apply()
    }

    override var status: Status
        get() = Status(
                mPrefs.getInt(getString(R.string.offline_status_version_key), 0),
                mPrefs.getInt(getString(R.string.offline_status_count_key), 0),
                mPrefs.getInt(getString(R.string.offline_status_new_count_key), 0),
                mPrefs.getLong(getString(R.string.offline_status_timestamp_key), 0),
                "",
                ""
        )
        set(status) {
            mPrefs.edit().putInt(getString(R.string.offline_status_version_key), status.version).apply()
            mPrefs.edit().putInt(getString(R.string.offline_status_count_key), status.count).apply()
            mPrefs.edit().putInt(getString(R.string.offline_status_new_count_key), status.newCount).apply()
            mPrefs.edit().putLong(getString(R.string.offline_status_timestamp_key), status.timestamp).apply()
        }

    override val isNotMarkContact: Boolean
        get() = mPrefs.getBoolean(getString(R.string.not_mark_contact_key), false)

    override val isDisableOutGoingHangup: Boolean
        get() = mPrefs.getBoolean(getString(R.string.disable_outgoing_blacklist_key), false)

    override val isTemporaryDisableHangup: Boolean
        get() = mPrefs.getBoolean(getString(R.string.temporary_disable_blacklist_key), false)

    override val repeatedCountIndex: Int
        get() = mPrefs.getInt(getString(R.string.repeated_incoming_count_key), 1)

    override fun clear() {
        mPrefs.edit().clear().apply()
        mWindowPrefs.edit().clear().apply()
    }

    override val normalColor: Int
        get() = mPrefs.getInt("color_normal", ContextCompat.getColor(sContext!!, R.color.blue_light))

    override val poiColor: Int
        get() = mPrefs.getInt("color_poi", ContextCompat.getColor(sContext!!, R.color.orange_dark))

    override val reportColor: Int
        get() = mPrefs.getInt("color_report", ContextCompat.getColor(sContext!!, R.color.red_light))

    override val isOnlyOffline: Boolean
        get() = mPrefs.getBoolean(sContext!!.getString(R.string.only_offline_key), false)

    override fun fix() { // fix api type because baidu api is dead
        val type = mPrefs.getInt(getString(R.string.api_type_key), 1)
        if (type == 0) {
            mPrefs.edit().remove(getString(R.string.api_type_key)).apply()
        }
    }

    override fun setOutgoing(isOutgoing: Boolean) {
        this.isOutgoing = isOutgoing
    }

    override val isOutgoingPositionEnabled: Boolean
        get() = mPrefs.getBoolean(getString(R.string.outgoing_window_position_key), false)

    override val isAddingRingOnceCallLog: Boolean
        get() = mPrefs.getBoolean(getString(R.string.ring_once_and_auto_hangup_key), false)

    override val isOfflineDataAutoUpgrade: Boolean
        get() = mPrefs.getBoolean(getString(R.string.offline_data_auto_upgrade_key), true)

    override val isHidingWhenTouch: Boolean
        get() = mPrefs.getBoolean(getString(R.string.hide_when_touch_key), false)

    override fun setEula() {
        val editor = mPrefs.edit()
        editor.putBoolean("eula", true)
        editor.putInt("eula_version", 1)
        editor.putInt("version", BuildConfig.VERSION_CODE)
        editor.apply()
    }

    override val ignoreRegex: String
        get() = mPrefs.getString(getString(R.string.ignore_regex_key), "")!!.replace("*",
                "[0-9]").replace(" ", "|")

    override val isHidingOffHook: Boolean
        get() = mPrefs.getBoolean(getString(R.string.hide_when_off_hook_key), false)

    override val isShowingOnOutgoing: Boolean
        get() = mPrefs.getBoolean(getString(R.string.display_on_outgoing_key), false)

    override val isIgnoreKnownContact: Boolean
        get() = mPrefs.getBoolean(getString(R.string.ignore_known_contact_key), false)

    override val isShowingContactOffline: Boolean
        get() = mPrefs.getBoolean(getString(R.string.contact_offline_key), false)

    override val isAutoHangup: Boolean
        get() = mPrefs.getBoolean(getString(R.string.auto_hangup_key), false)

    override val isAddingCallLog: Boolean
        get() = mPrefs.getBoolean(getString(R.string.add_call_log_key), false)

    override val isCatchCrash: Boolean
        get() = mPrefs.getBoolean(getString(R.string.catch_crash_key), false)

    override val isForceChinese: Boolean
        get() = mPrefs.getBoolean(getString(R.string.force_chinese_key), false)

    override val keywords: String
        get() {
            val keywordKey = getString(R.string.hangup_keyword_key)
            val keywordDefault = getString(R.string.hangup_keyword_default)
            var keywords = mPrefs.getString(keywordKey, keywordDefault)!!.trim { it <= ' ' }
            if (keywords.isEmpty()) {
                keywords = keywordDefault
            }
            return keywords
        }

    override val geoKeyword: String
        get() = mPrefs.getString(getString(R.string.hangup_geo_keyword_key), "")!!.trim { it <= ' ' }

    override val numberKeyword: String
        get() = mPrefs.getString(getString(R.string.hangup_number_keyword_key), "")!!.trim { it <= ' ' }

    override val windowX: Int
        get() {
            val prefix = if (isOutgoing && isOutgoingPositionEnabled) "out_" else ""
            return mWindowPrefs.getInt(prefix + "x", -1)
        }

    override val windowY: Int
        get() {
            val prefix = if (isOutgoing && isOutgoingPositionEnabled) "out_" else ""
            return mWindowPrefs.getInt(prefix + "y", -1)
        }

    override fun setWindow(x: Int, y: Int) {
        val prefix = if (isOutgoing && isOutgoingPositionEnabled) "out_" else ""
        val editor = mWindowPrefs.edit()
        editor.putInt(prefix + "x", x)
        editor.putInt(prefix + "y", y)
        editor.apply()
    }

    private fun getString(@StringRes resId: Int): String {
        return sContext!!.getString(resId)
    }

    private object SingletonHelper {
        val INSTANCE = SettingImpl()
    }

    companion object {
        private val gson = Gson()
        private var sContext: Context? = null
        fun init(context: Context) {
            sContext = context.applicationContext
        }

        val instance: Setting
            get() {
                checkNotNull(sContext) { "Setting is not initialized!" }
                return SingletonHelper.INSTANCE
            }
    }

    init {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(sContext)
        mWindowPrefs = sContext!!.getSharedPreferences("window", Context.MODE_PRIVATE)
    }
}