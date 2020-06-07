package org.xdty.callerinfo.contract

import android.content.Context
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.phone.number.model.INumber
import org.xdty.phone.number.model.caller.Status

interface MainContract {
    interface View : BaseView<Presenter> {
        fun showNoCallLog(show: Boolean)
        fun showLoading(active: Boolean)
        fun showCallLogs(inCalls: List<InCall>)
        fun showEula()
        fun showSearchResult(number: INumber)
        fun showSearching()
        fun showSearchFailed(isOnline: Boolean)
        fun attachCallerMap(callerMap: Map<String, Caller>)
        val context: Context
        fun notifyUpdateData(status: Status)
        fun showUpdateData(status: Status)
        fun updateDataFinished(result: Boolean)
        fun showBottomSheet(inCall: InCall)
    }

    interface Presenter : BasePresenter {
        fun result(requestCode: Int, resultCode: Int)
        fun loadInCallList()
        fun loadCallerMap()
        fun removeInCallFromList(inCall: InCall)
        fun removeInCall(inCall: InCall)
        fun clearAll()
        fun search(number: String)
        fun checkEula()
        fun setEula()
        fun canDrawOverlays(): Boolean
        fun checkPermission(permission: String): Int
        fun clearSearch()
        fun dispatchUpdate(status: Status)
        fun getCaller(number: String): Caller
        fun clearCache()
        fun itemOnLongClicked(inCall: InCall)
        fun invalidateDataUpdate(isInvalidate: Boolean)
    }
}