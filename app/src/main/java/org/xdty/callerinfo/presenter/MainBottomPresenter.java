package org.xdty.callerinfo.presenter;

import android.view.View;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.contract.MainBottomContact;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.setting.Setting;

import javax.inject.Inject;

public class MainBottomPresenter implements MainBottomContact.Presenter {

    @Inject
    Setting mSetting;
    @Inject
    CallerDataSource mCallerDataSource;

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
    public void markClicked(View view) {
        mView.updateMark(view, mCaller);
    }
}
