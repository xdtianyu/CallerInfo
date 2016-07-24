package org.xdty.callerinfo.presenter;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.contract.MainContract;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.data.CallerRepository;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Contact;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.caller.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import rx.functions.Action1;

public class MainPresenter implements MainContract.Presenter,
        PhoneNumber.CheckUpdateCallback {

    private final List<InCall> mInCallList = new ArrayList<>();
    @Inject
    Setting mSetting;
    @Inject
    Permission mPermission;
    @Inject
    PhoneNumber mPhoneNumber;
    @Inject
    Database mDatabase;
    @Inject
    Alarm mAlarm;
    @Inject
    Contact mContact;
    @Inject
    CallerDataSource mCallerDataSource;

    private MainContract.View mView;

    public MainPresenter(MainContract.View view) {
        mView = view;
        Application.getAppComponent().inject(this);
    }

    @Override
    public void result(int requestCode, int resultCode) {

    }

    @Override
    public void loadInCallList() {
        mDatabase.fetchInCalls().subscribe(new Action1<List<InCall>>() {
            @Override
            public void call(List<InCall> inCalls) {
                mInCallList.clear();
                mInCallList.addAll(inCalls);

                mView.showCallLogs(mInCallList);

                if (mInCallList.size() == 0) {
                    mView.showNoCallLog(true);
                } else {
                    mView.showNoCallLog(false);
                }
                mView.showLoading(false);
            }
        });
    }

    @Override
    public void loadCallerMap() {
        mCallerDataSource.loadCallerMap().subscribe(new Action1<Map<String, Caller>>() {
            @Override
            public void call(Map<String, Caller> callerMap) {
                mView.attachCallerMap(callerMap);
            }
        });
    }

    @Override
    public void removeInCallFromList(InCall inCall) {
        mInCallList.remove(inCall);
    }

    @Override
    public void removeInCall(InCall inCall) {
        mDatabase.removeInCall(inCall);
    }

    @Override
    public void clearAll() {
        mDatabase.clearAllInCalls().subscribe(new Action1<Void>() {
            @Override
            public void call(Void aVoid) {
                loadInCallList();
            }
        });
    }

    @Override
    public void search(final String number) {
        if (number.isEmpty()) {
            return;
        }

        mView.showSearching();

        mCallerDataSource.getCaller(number).subscribe(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                mView.showSearchResult(caller);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                CallerRepository.CallerThrowable t = (CallerRepository.CallerThrowable) throwable;
                mView.showSearchFailed(t.isOnline());
            }
        });
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
    public boolean canDrawOverlays() {
        return mPermission.canDrawOverlays();
    }

    @Override
    public int checkPermission(String permission) {
        return mPermission.checkPermission(permission);
    }

    @Override
    public void start() {
        mPhoneNumber.setCheckUpdateCallback(this);
        loadCallerMap();

        if (System.currentTimeMillis() - mSetting.lastCheckDataUpdateTime() > 6 * 3600 * 1000) {
            mPhoneNumber.checkUpdate();
        }
    }

    @Override
    public void handleResponse(INumber number, boolean isOnline) {
        if (number != null) {
            if (isOnline && number.isValid()) {
                mDatabase.updateCaller(new Caller(number, !number.isOnline()));
                if (mSetting.isAutoReportEnabled()) {
                    mDatabase.saveMarkedRecord(number, mSetting.getUid());
                    mAlarm.alarm();
                }
            }
            mView.showSearchResult(number);
        }
    }

    @Override
    public void handleResponseFailed(INumber number, boolean isOnline) {
        mView.showSearchFailed(isOnline);
    }

    @Override
    public void clearSearch() {

    }

    @Override
    public void dispatchUpdate(Status status) {
        mView.showUpdateData(status);
        mPhoneNumber.upgradeData();
    }

    @Override
    public void onCheckResult(Status status) {
        if (status != null) {
            mView.notifyUpdateData(status);
            mSetting.setStatus(status);
        }
    }

    @Override
    public void onUpgradeData(boolean result) {
        mView.updateDataFinished(result);
        if (result) {
            mSetting.updateLastCheckDataUpdateTime(System.currentTimeMillis());
        }
    }
}
