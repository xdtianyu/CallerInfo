package org.xdty.callerinfo.presenter;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.contract.MainBottomContact;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Utils;

import javax.inject.Inject;

public class MainBottomPresenter implements MainBottomContact.Presenter {

    @Inject
    Setting mSetting;
    @Inject
    CallerDataSource mCallerDataSource;
    @Inject
    Database mDatabase;
    @Inject
    Alarm mAlarm;

    private InCall mInCall;
    private Caller mCaller;

    private MainBottomContact.View mView;

    public MainBottomPresenter(MainBottomContact.View view) {
        mView = view;
        Application.getAppComponent().inject(this);
    }

    @Override
    public void start() {
        mView.init(mInCall, mCaller);
    }

    @Override
    public void bindData(InCall inCall) {
        mInCall = inCall;
        mCaller = mCallerDataSource.getCallerFromCache(inCall.getNumber());
    }

    @Override
    public boolean canMark() {
        return mCaller.isMark() || mCaller.canMark() && mInCall.getDuration() > 0;
    }

    @Override
    public void markClicked(int viewId) {
        MarkedRecord.MarkType type = MarkedRecord.MarkType.fromResourceId(viewId);

        if (type != MarkedRecord.MarkType.CUSTOM) {
            String typeText = Utils.typeFromId(type.toInt());
            mCallerDataSource.updateCaller(mInCall.getNumber(), type.toInt(), typeText);
            mView.updateMarkName(typeText);
        }

        mView.updateMark(viewId, mCaller);
    }

    @Override
    public void markCustom(String text) {
        mCallerDataSource.updateCaller(mInCall.getNumber(), MarkedRecord.MarkType.CUSTOM.toInt(),
                text);
        mView.updateMarkName(text);
    }
}
