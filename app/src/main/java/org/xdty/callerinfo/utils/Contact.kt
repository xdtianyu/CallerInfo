package org.xdty.callerinfo.utils

import android.annotation.SuppressLint
import android.provider.ContactsContract
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.application.Application.Companion.application
import org.xdty.callerinfo.model.permission.Permission
import javax.inject.Inject

@SuppressLint("CheckResult")
class Contact private constructor() {
    @Inject
    internal lateinit var mPermission: Permission

    private val mContactMap: MutableMap<String, String> = HashMap()
    private var lastUpdateTime: Long = 0
    fun isExist(number: String?): Boolean {
        loadContactCache()
        return mContactMap.containsKey(number)
    }

    fun getName(number: String): String {
        return if (mContactMap.containsKey(number)) {
            mContactMap[number]!!
        } else ""
    }

    private fun loadContactCache() {
        if (mPermission.canReadContact() && System.currentTimeMillis() - lastUpdateTime
                > Constants.CONTACT_CACHE_INTERVAL) {
            loadContactMap().subscribe { map ->
                mContactMap.clear()
                mContactMap.putAll(map!!)
                lastUpdateTime = System.currentTimeMillis()
            }
        }
    }

    private fun loadContactMap(): Observable<Map<String, String>> {
        return Observable.create<Map<String, String>> { emitter ->
            val contactsMap: MutableMap<String, String> = HashMap()
            val cursor = application
                    .contentResolver
                    .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                            null)
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    val name = cursor.getString(cursor.getColumnIndex(
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    var number = cursor.getString(
                            cursor.getColumnIndex(
                                    ContactsContract.CommonDataKinds.Phone.NUMBER))
                    if (number != null) {
                        number = number.replace("[^\\d]".toRegex(), "")
                        contactsMap[number] = name
                    }
                }
                cursor.close()
            }
            emitter.onNext(contactsMap)
            emitter.onComplete()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    companion object {
        val instance = Contact()
    }

    init {
        appComponent.inject(this)
        loadContactCache()
    }
}