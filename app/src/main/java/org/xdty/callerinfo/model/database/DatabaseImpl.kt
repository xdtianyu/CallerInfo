package org.xdty.callerinfo.model.database

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import io.requery.Persistable
import io.requery.sql.EntityDataStore
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.db.MarkedRecord
import org.xdty.callerinfo.utils.Utils
import org.xdty.phone.number.model.INumber
import org.xdty.phone.number.model.Type
import javax.inject.Inject

@SuppressLint("CheckResult")
class DatabaseImpl private constructor() : Database {
    @Inject
    internal lateinit var mDataStore: EntityDataStore<Persistable>

    init {
        appComponent.inject(this)
    }

    override fun fetchInCalls(): Observable<List<InCall>> {
        return Observable.fromCallable {
            mDataStore.select(InCall::class.java)
                    .orderBy(InCall.TIME.desc())
                    .get()
                    .toList()
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun fetchCallers(): Observable<List<Caller>> {
        return Observable.fromCallable { mDataStore.select(Caller::class.java).get().toList() }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun clearAllInCalls(): Observable<Int> {
        return Observable.fromCallable { mDataStore.delete(InCall::class.java).get().value() }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun clearAllInCallSync() {
        mDataStore.delete(InCall::class.java).get().value()
    }

    override fun removeInCall(inCall: InCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe {
            try {
                mDataStore.delete(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun findCaller(number: String): Observable<Caller> {
        return Observable.fromCallable<Caller> {
            val callers = mDataStore.select(Caller::class.java)
                    .where(Caller.NUMBER.eq(number))
                    .get()
                    .toList()
            var caller: Caller? = null
            if (callers.size > 0) {
                caller = callers[0]
            }
            caller
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun findCallerSync(number: String): Caller? {
        val callers = mDataStore.select(Caller::class.java)
                .where(Caller.NUMBER.eq(number))
                .get()
                .toList()
        var caller: Caller? = null
        if (callers.size > 0) {
            caller = callers[0]
        }
        return caller
    }

    override fun removeCaller(caller: Caller) {
        Observable.just(caller).observeOn(Schedulers.io()).subscribe { mDataStore.delete(it) }
    }

    override fun clearAllCallerSync(): Int {
        return mDataStore.delete(Caller::class.java).get().value()
    }

    override fun updateCaller(caller: Caller) {
        Observable.just(caller).observeOn(Schedulers.io()).subscribe {
            val c = mDataStore.select(Caller::class.java).where(
                    Caller.NUMBER.eq(it.number)).get().firstOr(it)
            if (c !== it) {
                c.callerSource = it.callerSource
                c.callerType = it.callerType
                c.city = it.city
                c.name = it.name
                c.name = it.name
                c.setType(it.type.text)
                c.count = it.count
                c.province = it.province
                c.operators = it.operators
                c.lastUpdate = it.lastUpdate
            }
            mDataStore.upsert(c)
        }
    }

    override fun saveInCall(inCall: InCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe { mDataStore.insert(it) }
    }

    override fun saveMarked(markedRecord: MarkedRecord) {
        updateMarked(markedRecord)
    }

    override fun updateMarked(markedRecord: MarkedRecord) {
        Observable.just(markedRecord)
                .observeOn(Schedulers.io())
                .subscribe {
                    val record = mDataStore.select(MarkedRecord::class.java)
                            .where(MarkedRecord.NUMBER.eq(it.number))
                            .get()
                            .firstOr(it)
                    if (record !== it) {
                        record.count = it.count
                        record.isReported = false
                        record.source = it.source
                        record.time = it.time
                        record.type = it.type
                        record.typeName = it.typeName
                    }
                    mDataStore.upsert(record)
                }
    }

    override fun updateCaller(markedRecord: MarkedRecord) {
        Observable.just(markedRecord)
                .observeOn(Schedulers.io())
                .subscribe {
                    val caller = mDataStore.select(Caller::class.java)
                            .where(
                                    Caller.NUMBER.eq(it.number))
                            .get()
                            .firstOr(Caller())
                    caller.number = it.number
                    caller.name = it.typeName
                    caller.lastUpdate = it.time
                    caller.setType("report")
                    caller.isOffline = false
                    mDataStore.upsert(caller)
                }
    }

    override fun fetchMarkedRecords(): Observable<List<MarkedRecord>> {
        return Observable.fromCallable { mDataStore.select(MarkedRecord::class.java).get().toList() }
                .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun findMarkedRecord(number: String): Observable<MarkedRecord> {
        return Observable.fromCallable<MarkedRecord> {
            val records = mDataStore.select(MarkedRecord::class.java)
                    .where(MarkedRecord.NUMBER.eq(number))
                    .get()
                    .toList()
            var record: MarkedRecord? = null
            if (records.size > 0) {
                record = records[0]
            }
            record
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    override fun updateMarkedRecord(number: String) {
        Observable.just(number)
                .observeOn(Schedulers.io())
                .subscribe {
                    val records = mDataStore.select(MarkedRecord::class.java)
                            .where(MarkedRecord.NUMBER.eq(it))
                            .get()
                            .toList()
                    var record: MarkedRecord? = null
                    if (records.size > 0) {
                        record = records[0]
                        record.isReported = true
                        mDataStore.update(record!!)
                    }
                    if (records.size > 1) {
                        Log.e("DatabaseImpl", "updateMarkedRecord duplicate number: $it")
                    }
                }
    }

    override fun fetchCallersSync(): List<Caller> {
        return mDataStore.select(Caller::class.java).get().toList()
    }

    override fun fetchInCallsSync(): List<InCall> {
        return mDataStore.select(InCall::class.java).orderBy(InCall.TIME.desc()).get().toList()
    }

    override fun fetchMarkedRecordsSync(): List<MarkedRecord> {
        return mDataStore.select(MarkedRecord::class.java).get().toList()
    }

    override fun addCallers(callers: List<Caller>) {
        Observable.fromIterable(callers)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap { caller -> Observable.just(caller) }
                .subscribe { caller -> mDataStore.upsert(caller) }
    }

    override fun addInCallers(inCalls: List<InCall>) {
        Observable.fromIterable(inCalls)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap { inCall -> Observable.just(inCall) }
                .subscribe { inCall -> mDataStore.insert(inCall) }
    }

    override fun addMarkedRecords(markedRecords: List<MarkedRecord>) {
        Observable.fromIterable(markedRecords)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap { record -> Observable.just(record) }
                .subscribe { markedRecord ->
                    val record = mDataStore.select(MarkedRecord::class.java)
                            .where(MarkedRecord.NUMBER.eq(markedRecord.number))
                            .get()
                            .firstOr(markedRecord)
                    if (record !== markedRecord) {
                        record.count = markedRecord.count
                        record.isReported = false
                        record.source = markedRecord.source
                        record.time = markedRecord.time
                        record.type = markedRecord.type
                        record.typeName = markedRecord.typeName
                    }
                    mDataStore.upsert(record)
                }
    }

    override fun clearAllMarkedRecordSync() {
        mDataStore.delete(MarkedRecord::class.java).get().value()
    }

    override fun getInCallCount(number: String): Int {
        val time = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        return mDataStore.count(InCall::class.java)
                .where(InCall.NUMBER.eq(number))
                .and(InCall.TIME.gt(time))
                .get()
                .value()
    }

    override fun addInCallersSync(inCalls: List<InCall>) {
        for (inCall in inCalls) {
            mDataStore.insert(inCall)
        }
    }

    override fun saveMarkedRecord(number: INumber, uid: String) {
        if (number.type == Type.REPORT) {
            findMarkedRecord(number.number).subscribe { record ->
                if (record == null) {
                    val type = Utils.typeFromString(number.name)
                    if (type >= 0) {
                        val markedRecord = MarkedRecord()
                        markedRecord.number = number.number
                        markedRecord.uid = uid
                        markedRecord.source = number.apiId
                        markedRecord.type = type
                        markedRecord.count = number.count
                        markedRecord.typeName = number.name
                        saveMarked(markedRecord)
                    }
                }
            }
        }
    }

    override fun removeRecord(record: MarkedRecord) {
        Observable.just(record).observeOn(Schedulers.io()).subscribe {
            mDataStore.delete(it)
            removeCaller(findCallerSync(it.number)!!)
        }
    }


    companion object {
        val instance = DatabaseImpl()
    }
}