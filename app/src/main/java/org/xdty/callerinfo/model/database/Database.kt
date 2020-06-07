package org.xdty.callerinfo.model.database

import io.reactivex.Observable
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.db.MarkedRecord
import org.xdty.phone.number.model.INumber

interface Database {
    fun fetchInCalls(): Observable<List<InCall>>
    fun fetchCallers(): Observable<List<Caller>>
    fun clearAllInCalls(): Observable<Int>
    fun clearAllInCallSync()
    fun removeInCall(inCall: InCall)
    fun findCaller(number: String): Observable<Caller>
    fun findCallerSync(number: String): Caller?
    fun removeCaller(caller: Caller)
    fun clearAllCallerSync(): Int
    fun updateCaller(caller: Caller)
    fun saveInCall(inCall: InCall)
    fun saveMarked(markedRecord: MarkedRecord)
    fun updateMarked(markedRecord: MarkedRecord)
    fun updateCaller(markedRecord: MarkedRecord)
    fun fetchMarkedRecords(): Observable<List<MarkedRecord>>
    fun findMarkedRecord(number: String): Observable<MarkedRecord>
    fun updateMarkedRecord(number: String)
    fun fetchCallersSync(): List<Caller>
    fun fetchInCallsSync(): List<InCall>
    fun fetchMarkedRecordsSync(): List<MarkedRecord>
    fun addCallers(callers: List<Caller>)
    fun addInCallers(inCalls: List<InCall>)
    fun addMarkedRecords(markedRecords: List<MarkedRecord>)
    fun clearAllMarkedRecordSync()
    fun getInCallCount(number: String): Int
    fun addInCallersSync(inCalls: List<InCall>)
    fun saveMarkedRecord(number: INumber, uid: String)
    fun removeRecord(record: MarkedRecord)
}