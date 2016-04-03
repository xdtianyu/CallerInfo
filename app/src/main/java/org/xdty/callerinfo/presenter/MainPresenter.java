package org.xdty.callerinfo.presenter;

import org.xdty.callerinfo.contract.MainContact;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.setting.Setting;

import java.util.ArrayList;
import java.util.List;

public class MainPresenter implements MainContact.Presenter {

    private MainContact.View mView;
    private Setting mSetting;
    private final List<InCall> mInCallList = new ArrayList<>();

    public MainPresenter(MainContact.View view, Setting setting) {
        mView = view;
        mSetting = setting;
    }

    @Override
    public void result(int requestCode, int resultCode) {

    }

    @Override
    public void loadInCallList() {
        mInCallList.clear();
        mInCallList.addAll(InCall.listAll(InCall.class, "time DESC"));

        mView.showCallLogs(mInCallList);

        if (mInCallList.size() == 0) {
            mView.showNoCallLog(true);
        } else {
            mView.showNoCallLog(false);
        }
    }

    @Override
    public void removeInCallFromList(int position) {
        mInCallList.remove(position);
    }

    @Override
    public void removeInCall(int position) {
        InCall inCall = mInCallList.get(position);
        inCall.delete();
    }

    @Override
    public void clearAll() {
        for (InCall inCall : mInCallList) {
            inCall.delete();
        }
    }

    @Override
    public void search(String number) {

    }

    @Override
    public void checkEula() {
        if (!mSetting.isEulaSet()) {
            mView.showEula();
        }
    }

    @Override
    public void setEula() {
        mSetting.setEula();
    }

    @Override
    public void start() {
        loadInCallList();
    }
}
