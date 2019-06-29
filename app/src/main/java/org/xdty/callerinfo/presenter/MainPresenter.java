package org.xdty.callerinfo.presenter;

import android.annotation.SuppressLint;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.contract.MainContract;
import org.xdty.callerinfo.data.CallerDataSource;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Contact;
import org.xdty.phone.number.RxPhoneNumber;
import org.xdty.phone.number.model.caller.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.functions.Consumer;

@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint("CheckResult")
public class MainPresenter implements MainContract.Presenter, CallerDataSource.OnDataUpdateListener {

    private final List<InCall> mInCallList = new ArrayList<>();
    @Inject
    Setting mSetting;
    @Inject
    Permission mPermission;
    @Inject
    RxPhoneNumber mPhoneNumber;
    @Inject
    Database mDatabase;
    @Inject
    Alarm mAlarm;
    @Inject
    Contact mContact;
    @Inject
    CallerDataSource mCallerDataSource;

    private MainContract.View mView;

    private boolean isInvalidateDataUpdate = false;
    private boolean isWaitDataUpdate = false;

    public MainPresenter(MainContract.View view) {
        mView = view;
        Application.getApplication().getAppComponent().inject(this);
    }

    @Override
    public void result(int requestCode, int resultCode) {

    }

    @Override
    public void loadInCallList() {
        mDatabase.fetchInCalls().subscribe(new Consumer<List<InCall>>() {
            @Override
            public void accept(List<InCall> inCalls) {
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
        mCallerDataSource.loadCallerMap().subscribe(new Consumer<Map<String, Caller>>() {
            @Override
            public void accept(Map<String, Caller> callerMap) {
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
        mDatabase.clearAllInCalls().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
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

        mCallerDataSource.getCaller(number).subscribe(new Consumer<Caller>() {
            @Override
            public void accept(Caller caller) {
                if (caller.getNumber() != null) {
                    mView.showSearchResult(caller);
                } else {
                    mView.showSearchFailed(!caller.isOffline());
                }
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
        mCallerDataSource.setOnDataUpdateListener(this);
        loadCallerMap();

        if (System.currentTimeMillis() - mSetting.lastCheckDataUpdateTime() > 6 * 3600 * 1000) {
            mPhoneNumber.checkUpdate().subscribe(new Consumer<Status>() {
                @Override
                public void accept(Status status) throws Exception {
                    if (status != null && status.count > 0) {
                        mView.notifyUpdateData(status);
                        mSetting.setStatus(status);
                    }
                }
            });
        }
    }

    @Override
    public void clearSearch() {

    }

    @Override
    public void dispatchUpdate(Status status) {
        mView.showUpdateData(status);
        mPhoneNumber.upgradeData().subscribe(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean result) throws Exception {
                mView.updateDataFinished(result);
                if (result) {
                    mSetting.updateLastCheckDataUpdateTime(System.currentTimeMillis());
                }
            }
        });
    }

    @Override
    public Caller getCaller(String number) {
        return mCallerDataSource.getCallerFromCache(number);
    }

    @Override
    public void clearCache() {
        mCallerDataSource.clearCache().subscribe(new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                loadInCallList();
            }
        });
    }

    @Override
    public void itemOnLongClicked(InCall inCall) {
        mView.showBottomSheet(inCall);
    }

    @Override
    public void invalidateDataUpdate(boolean isInvalidate) {
        isInvalidateDataUpdate = isInvalidate;

        if (isWaitDataUpdate) {
            mView.showCallLogs(mInCallList);
            isWaitDataUpdate = false;
        }
    }

    @Override
    public void onDataUpdate(Caller caller) {

        isWaitDataUpdate = isInvalidateDataUpdate;

        if (!isWaitDataUpdate) {
            mView.showCallLogs(mInCallList);
        }

    }
}
