package org.xdty.callerinfo.data

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.Observable
import io.reactivex.ObservableOnSubscribe
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.data.CallerDataSource.OnDataUpdateListener
import org.xdty.callerinfo.model.SearchMode
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.MarkedRecord
import org.xdty.callerinfo.model.permission.Permission
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Contact
import org.xdty.phone.number.RxPhoneNumber
import org.xdty.phone.number.model.INumber
import org.xdty.phone.number.util.Utils
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

//TODO: refactor
@SuppressLint("CheckResult")
class CallerRepository : CallerDataSource {
    @Inject
    internal lateinit var mDatabase: Database
    @Inject
    internal lateinit var mPhoneNumber: RxPhoneNumber
    @Inject
    internal lateinit var mPermission: Permission
    @Inject
    internal lateinit var mContact: Contact
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mAlarm: Alarm

    private val mCallerMap: MutableMap<String, Caller> = Collections.synchronizedMap(HashMap())
    private val mLoadingCache: MutableSet<String> = Collections.synchronizedSet(HashSet())
    private val mErrorCache: MutableMap<String, Long> = Collections.synchronizedMap(HashMap())
    private lateinit var mOnDataUpdateListener: OnDataUpdateListener

    init {
        appComponent.inject(this)
    }

    override fun getCallerFromCache(number: String): Caller {
        var number = number
        number = fixNumber(number)
        // return empty caller if it's in error cache.
        return if (mErrorCache.containsKey(number) &&
                System.currentTimeMillis() - mErrorCache[number]!! < 60 * 1000) {
            Caller.empty(true)
        } else getCallerFromCache(number, true)
    }

    private fun getCallerFromCache(number: String, fetchIfNotExist: Boolean): Caller {
        var caller = mCallerMap[number]
        if (caller == null && number.contains("+86")) {
            caller = mCallerMap[number.replace("+86", "")]
        }
        if (caller != null) {
            return caller
        } else if (fetchIfNotExist) {
            getCaller(number).subscribe { caller ->
                Log.e(TAG, "call: " + number + "->" + caller.number)
                mOnDataUpdateListener.onDataUpdate(caller)
            }
        }
        return Caller.empty(false)
    }

    override fun getCaller(number: String): Observable<Caller> {
        return getCaller(number, false)
    }

    override fun getCaller(numberOrigin: String, forceOffline: Boolean): Observable<Caller> {
        Log.d(TAG, "getCaller: $numberOrigin, forceOffline: $forceOffline")
        val number = fixNumber(numberOrigin)
        return Observable.create<Caller>(ObservableOnSubscribe { emitter ->
            try {
                do { // check loading cache
                    if (mLoadingCache.contains(number)) { // return without onCompleted
                        return@ObservableOnSubscribe
                    }
                    mLoadingCache.add(number)
                    // load from cache
                    var caller: Caller? = getCallerFromCache(number, false)
                    if (caller != null && caller.isUpdated) {
                        emitter.onNext(caller)
                        break
                    }
                    // load from database
                    caller = mDatabase.findCallerSync(number)
                    if (caller != null) {
                        if (caller.isUpdated) {
                            cache(caller)
                            emitter.onNext(caller)
                            break
                        } else {
                            mDatabase.removeCaller(caller)
                        }
                    }
                    // load from phone number library offline data
                    val iNumber = Utils.pathGeo(mPhoneNumber.getOfflineNumber(number).toList().blockingGet())
                    if (iNumber != null && iNumber.isValid) {
                        emitter.onNext(handleResponse(iNumber, false))
                    } else {
                        emitter.onNext(Caller.empty(false))
                    }
                    // stop if the number is special
                    if (iNumber != null && (iNumber.apiId == INumber.API_ID_SPECIAL
                                    || iNumber.apiId == INumber.API_ID_CALLER)) {
                        break
                    }
                    // stop if only offline is enabled
                    if (mSetting.isOnlyOffline || forceOffline) {
                        break
                    }
                    // get online number info
                    val iOnlineNumber = Utils.mostCount(mPhoneNumber.getOnlineNumber(number).toList().blockingGet())
                    if (iOnlineNumber != null && iOnlineNumber.isValid) {
                        if (!iOnlineNumber.hasGeo() && iNumber != null) {
                            iOnlineNumber.patch(iNumber)
                        }
                        emitter.onNext(handleResponse(iOnlineNumber, true))
                    } else {
                        if (iNumber != null) {
                            emitter.onNext(handlePatch(iNumber))
                        } else {
                            emitter.onNext(Caller.empty(true))
                        }
                    }
                } while (false)
            } catch (e: Exception) {
                Log.e(TAG, "getCaller failed: " + e.message)
                e.printStackTrace()
            }
            emitter.onComplete()
        }).doOnNext { caller ->
            Log.d(TAG, "doOnNext: $number")
            // add number to error cache
            if (caller.isEmpty) {
                mErrorCache[number] = System.currentTimeMillis()
            } else {
                mErrorCache.remove(number)
            }
        }.doOnComplete {
            Log.d(TAG, "doOnCompleted: $number")
            // remove number in loading cache
            mLoadingCache.remove(number)
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun loadCallerMap(): Observable<Map<String, Caller>> {
        return Observable.fromCallable<Map<String, Caller>> {
            mCallerMap.clear()
            val callers = mDatabase.fetchCallersSync()
            for (caller in callers) {
                val number = caller.number
                if (number != null && !number.isEmpty()) {
                    cache(caller)
                }
            }
            mCallerMap
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun setOnDataUpdateListener(listener: OnDataUpdateListener) {
        mOnDataUpdateListener = listener
    }

    override fun clearCache(): Observable<Int> {
        mCallerMap.clear()
        mLoadingCache.clear()
        mErrorCache.clear()
        return Observable.fromCallable { mDatabase.clearAllCallerSync() }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun updateCaller(number: String, type: Int, typeText: String) {
        mCallerMap.remove(number)
        val markedRecord = MarkedRecord()
        markedRecord.uid = mSetting.uid
        markedRecord.number = number
        markedRecord.type = type
        markedRecord.typeName = typeText
        mDatabase.updateMarked(markedRecord)
        mDatabase.updateCaller(markedRecord)
        mAlarm.alarm()
        mOnDataUpdateListener.onDataUpdate(getCallerFromCache(number))
    }

    private fun handleResponse(number: INumber?, isOnline: Boolean): Caller {
        if (number != null) {
            val caller = Caller(number, !number.isOnline)
            if (isOnline && number.isValid) {
                mDatabase.updateCaller(caller)
                if (mSetting.isAutoReportEnabled) {
                    mDatabase.saveMarkedRecord(number, mSetting.uid)
                    mAlarm.alarm()
                }
            }
            cache(caller)
            return caller
        }
        return Caller.empty(isOnline)
    }

    private fun handlePatch(number: INumber): Caller {
        val caller = Caller.empty(true, number)
        caller.isOffline = false
        caller.number = number.number
        caller.setSource(number.apiId)
        cache(caller)
        return caller
    }

    private fun cache(caller: Caller) {
        if (mPermission.canReadContact()) {
            val name = mContact.getName(caller.number)
            caller.contactName = name
        }
        mCallerMap[caller.number] = caller
    }

    override fun getSearchMode(number: String): SearchMode {
        var mode = SearchMode.ONLINE
        if (isIgnoreContact(number)) {
            mode = if (mSetting.isShowingContactOffline) {
                SearchMode.OFFLINE
            } else {
                SearchMode.IGNORE
            }
        }
        return mode
    }

    override fun isIgnoreContact(number: String): Boolean {
        return (mSetting.isIgnoreKnownContact && mPermission.canReadContact()
                && (mContact.isExist(number) || mContact.isExist(fixNumber(number))))
    }

    companion object {
        private val TAG = CallerRepository::class.java.simpleName
        fun fixNumber(number: String): String {
            var fixedNumber = number
            if (number.startsWith("+86")) {
                fixedNumber = number.replace("+86", "")
            }
            if (number.startsWith("86") && number.length > 9) {
                fixedNumber = number.replaceFirst("^86".toRegex(), "")
            }
            if (number.startsWith("+400")) {
                fixedNumber = number.replace("+", "")
            }
            if (fixedNumber.startsWith("12583")) {
                fixedNumber = fixedNumber.replaceFirst("^12583.".toRegex(), "")
            }
            if (fixedNumber.startsWith("1259023")) {
                fixedNumber = number.replaceFirst("^1259023".toRegex(), "")
            }
            if (fixedNumber.startsWith("1183348")) {
                fixedNumber = number.replaceFirst("^1183348".toRegex(), "")
            }
            return fixedNumber
        }
    }
}