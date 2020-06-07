package org.xdty.callerinfo.exporter

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.GsonBuilder
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.xdty.callerinfo.BuildConfig
import org.xdty.callerinfo.R
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.database.DatabaseImpl.Companion.instance
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.db.MarkedRecord

class Exporter(context: Context) {
    private val mPrefs: SharedPreferences
    private val mWindowPrefs: SharedPreferences
    private val mDatabase: Database
    private val uidKey: String
    fun export(): Observable<String> {
        return Observable.create<String> { emitter ->
            emitter.onNext(exportSync())
            emitter.onComplete()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun exportSync(): String {
        val data = Data()
        data.setting = mPrefs.all
        data.window = mWindowPrefs.all
        data.timestamp = System.currentTimeMillis()
        data.versionCode = BuildConfig.VERSION_CODE
        data.versionName = BuildConfig.VERSION_NAME
        data.callers = mDatabase.fetchCallersSync()
        data.inCalls = mDatabase.fetchInCallsSync()
        data.markedRecords = mDatabase.fetchMarkedRecordsSync()
        return gson.toJson(data)
    }

    fun fromString(s: String?): Observable<String> {
        return Observable.create<String> { emitter ->
            emitter.onNext(fromStringSync(s)!!)
            emitter.onComplete()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    fun fromStringSync(s: String?): String? {
        var res: String? = null
        try {
            val data = gson.fromJson(s, Data::class.java)
            for ((key, value1) in data.setting!!) {
                val value = value1!!
                addPref(mPrefs, key, value)
            }
            for ((key, value1) in data.window!!) {
                val value = value1!!
                addPref(mWindowPrefs, key, value)
            }
            mDatabase.addCallers(data.callers!!)
            mDatabase.addInCallers(data.inCalls!!)
            mDatabase.addMarkedRecords(data.markedRecords!!)
        } catch (e: Exception) {
            e.printStackTrace()
            res = e.message
        }
        return res
    }

    private fun addPref(prefs: SharedPreferences, key: String, value: Any) { // ignore uid
        if (key == uidKey) {
            return
        }
        if (value is String) {
            prefs.edit().putString(key, value).apply()
        } else if (value is Int) {
            prefs.edit().putInt(key, value).apply()
        } else if (value is Long) {
            prefs.edit().putLong(key, value).apply()
        } else if (value is Float) {
            prefs.edit().putFloat(key, value).apply()
        } else if (value is Boolean) {
            prefs.edit().putBoolean(key, value).apply()
        }
    }

    class Data {
        internal var setting: Map<String, *>? = null
        internal var window: Map<String, *>? = null
        internal var callers: List<Caller>? = null
        internal var inCalls: List<InCall>? = null
        internal var markedRecords: List<MarkedRecord>? = null
        internal var versionName: String? = null
        internal var versionCode = 0
        internal var timestamp: Long = 0
    }

    companion object {
        private val gson = GsonBuilder().setPrettyPrinting().create()
    }

    init {
        val context = context.applicationContext
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        mWindowPrefs = context.getSharedPreferences("window", Context.MODE_PRIVATE)
        mDatabase = instance
        uidKey = context.getString(R.string.uid_key)
    }
}