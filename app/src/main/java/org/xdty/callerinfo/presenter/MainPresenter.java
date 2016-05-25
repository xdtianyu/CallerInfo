package org.xdty.callerinfo.presenter;

import android.support.annotation.NonNull;

import org.xdty.callerinfo.contract.MainContract;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.database.DatabaseImpl;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.AlarmUtils;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.functions.Action1;

public class MainPresenter implements MainContract.Presenter, PhoneNumber.Callback {

    private final List<InCall> mInCallList = new ArrayList<>();
    private final Map<Integer, InCall> mPaddingRemoveMap = new HashMap<>();
    private MainContract.View mView;
    private Setting mSetting;
    private Permission mPermission;
    private PhoneNumber mPhoneNumber;
    private Database mDatabase;

    public MainPresenter(MainContract.View view, Setting setting, Permission permission) {
        mView = view;
        mSetting = setting;
        mPermission = permission;
        mPhoneNumber = PhoneNumber.getInstance();
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
        final Map<String, Caller> callerMap = new HashMap<>();
        final boolean canReadContact = mPermission.canReadContact();
        mDatabase.fetchCallers().subscribe(new Action1<List<Caller>>() {
            @Override
            public void call(List<Caller> callers) {

                for (Caller caller : callers) {
                    String number = caller.getNumber();
                    if (number != null && !number.isEmpty()) {
                        if (canReadContact) {
                            String name = Utils.getContactName(mView.getContext(),
                                    caller.getNumber());
                            caller.setContactName(name);
                        }
                        callerMap.put(caller.getNumber(), caller);
                    }
                }
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
        mDatabase.clearAllInCalls(mInCallList).subscribe(new Action1<Void>() {
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

        mDatabase.findCaller(number).subscribe(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                if (caller != null) {
                    if (caller.isUpdated()) {
                        mView.showSearchResult(caller);
                        return;
                    } else {
                        mDatabase.removeCaller(caller);
                    }
                }
                mView.showSearching();
                mPhoneNumber.fetch(number);
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
    public void requestDrawOverlays(int requestCode) {
        mPermission.requestDrawOverlays(requestCode);
    }

    @Override
    public int checkPermission(String permission) {
        return mPermission.checkPermission(permission);
    }

    @Override
    public void requestPermissions(@NonNull String[] permissions, int requestCode) {
        mPermission.requestPermissions(permissions, requestCode);
    }

    @Override
    public void start() {
        mPhoneNumber.addCallback(this);
        mDatabase = DatabaseImpl.getInstance();
        loadCallerMap();
    }

    @Override
    public void handleResponse(INumber number, boolean isOnline) {
        if (number != null) {
            if (isOnline && number.isValid()) {
                mDatabase.updateCaller(new Caller(number, !number.isOnline()));
                MarkedRecord.trySave(number, mSetting, mDatabase);
                AlarmUtils.alarm();
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
        mPhoneNumber.removeCallback(this);
    }

    @Override
    public void onResponseOffline(INumber number) {
        handleResponse(number, false);
    }

    @Override
    public void onResponse(INumber number) {
        handleResponse(number, true);
    }

    @Override
    public void onResponseFailed(INumber number, boolean isOnline) {
        handleResponseFailed(number, isOnline);
    }
}
