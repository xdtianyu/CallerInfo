package org.xdty.callerinfo.contract

import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall

interface MainBottomContact {
    interface View : BaseView<Presenter> {
        fun init(inCall: InCall, caller: Caller)
        fun updateMark(viewId: Int, caller: Caller)
        fun updateMarkName(name: String?)
    }

    interface Presenter : BasePresenter {
        fun bindData(inCall: InCall?)
        fun canMark(): Boolean
        fun markClicked(viewId: Int)
        fun markCustom(text: String?)
    }
}