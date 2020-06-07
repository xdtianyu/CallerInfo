package org.xdty.callerinfo.presenter

import android.util.Log
import org.xdty.callerinfo.application.Application.Companion.appComponent
import org.xdty.callerinfo.contract.MainBottomContact.Presenter
import org.xdty.callerinfo.contract.MainBottomContact.View
import org.xdty.callerinfo.data.CallerDataSource
import org.xdty.callerinfo.model.MarkType
import org.xdty.callerinfo.model.database.Database
import org.xdty.callerinfo.model.db.Caller
import org.xdty.callerinfo.model.db.InCall
import org.xdty.callerinfo.model.setting.Setting
import org.xdty.callerinfo.utils.Alarm
import org.xdty.callerinfo.utils.Utils
import javax.inject.Inject

class MainBottomPresenter(private val mView: View) : Presenter {
    @Inject
    internal lateinit var mSetting: Setting
    @Inject
    internal lateinit var mCallerDataSource: CallerDataSource
    @Inject
    internal lateinit var mDatabase: Database
    @Inject
    internal lateinit var mAlarm: Alarm

    private var mInCall: InCall? = null
    private var mCaller: Caller? = null
    override fun start() {
        if (mInCall != null) {
            mView.init(mInCall!!, mCaller!!)
        } else {
            Log.e(TAG, "mInCall is null")
        }
    }

    override fun bindData(inCall: InCall?) {
        mInCall = inCall
        mCaller = mCallerDataSource.getCallerFromCache(inCall!!.number)
    }

    override fun canMark(): Boolean {
        return mCaller!!.isMark || mCaller!!.canMark() && mInCall!!.duration > 0
    }

    override fun markClicked(viewId: Int) {
        val type: MarkType = MarkType.fromResourceId(viewId)
        if (type != MarkType.CUSTOM) {
            val typeText = Utils.typeFromId(type.toInt())
            mCallerDataSource.updateCaller(mInCall!!.number, type.toInt(), typeText)
            mView.updateMarkName(typeText)
        }
        mView.updateMark(viewId, mCaller!!)
    }

    override fun markCustom(text: String?) {
        mCallerDataSource.updateCaller(mInCall!!.number, MarkType.CUSTOM.toInt(),
                text!!)
        mView.updateMarkName(text)
    }

    companion object {
        private val TAG = MainBottomPresenter::class.java.simpleName
    }

    init {
        appComponent.inject(this)
    }
}