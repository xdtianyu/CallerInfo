package org.xdty.callerinfo.data

import io.reactivex.Observable
import org.xdty.callerinfo.model.SearchMode
import org.xdty.callerinfo.model.db.Caller

interface CallerDataSource {
    fun getCallerFromCache(number: String): Caller
    fun getCaller(number: String): Observable<Caller>
    fun getCaller(number: String, forceOffline: Boolean): Observable<Caller>
    fun loadCallerMap(): Observable<Map<String, Caller>>
    fun setOnDataUpdateListener(listener: OnDataUpdateListener)
    fun clearCache(): Observable<Int>
    fun updateCaller(number: String, type: Int, typeText: String)
    fun isIgnoreContact(number: String): Boolean
    fun getSearchMode(number: String): SearchMode

    interface OnDataUpdateListener {
        fun onDataUpdate(caller: Caller)
    }
}