package org.xdty.callerinfo.data;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Contact;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.caller.CallerNumber;
import org.xdty.phone.number.model.special.SpecialNumber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class CallerRepository implements CallerDataSource {

    @Inject
    Database mDatabase;

    @Inject
    PhoneNumber mPhoneNumber;

    @Inject
    Permission mPermission;

    @Inject
    Contact mContact;

    @Inject
    Setting mSetting;

    @Inject
    Alarm mAlarm;

    private Map<String, Caller> mCallerMap;

    public CallerRepository() {
        mCallerMap = new HashMap<>();
        Application.getAppComponent().inject(this);
    }

    @Override
    public Observable<Caller> getCaller(final String number) {
        return Observable.create(new Observable.OnSubscribe<Caller>() {
            @Override
            public void call(final Subscriber<? super Caller> subscriber) {

                // load from cache
                Caller caller = mCallerMap.get(number);
                if (caller == null && number.contains("+86")) {
                    caller = mCallerMap.get(number.replace("+86", ""));
                }

                if (caller != null && caller.isUpdated()) {
                    subscriber.onNext(caller);
                    return;
                }

                // load from database
                caller = mDatabase.findCallerSync(number);

                if (caller != null) {
                    if (caller.isUpdated()) {
                        subscriber.onNext(caller);
                        return;
                    } else {
                        mDatabase.removeCaller(caller);
                    }
                }

                // load from phone number library
                INumber iNumber = mPhoneNumber.getOfflineNumber(number);

                if (iNumber != null && iNumber.isValid()) {
                    subscriber.onNext(handleResponse(iNumber, false));
                }

                // stop if the number is special
                if (iNumber instanceof SpecialNumber || iNumber instanceof CallerNumber) {
                    return;
                }

                // stop if only offline is enabled
                if (mSetting.isOnlyOffline()) {
                    return;
                }

                // get online number info
                iNumber = mPhoneNumber.getNumber(number);

                if (iNumber != null && iNumber.isValid()) {
                    subscriber.onNext(handleResponse(iNumber, true));
                } else {
                    subscriber.onError(new CallerThrowable(number, true));
                }
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Map<String, Caller>> loadCallerMap() {

        return Observable.create(new Observable.OnSubscribe<Map<String, Caller>>() {
            @Override
            public void call(final Subscriber<? super Map<String, Caller>> subscriber) {

                List<Caller> callers = mDatabase.fetchCallersSync();
                boolean canReadContact = mPermission.canReadContact();
                for (Caller caller : callers) {
                    String number = caller.getNumber();
                    if (number != null && !number.isEmpty()) {
                        if (canReadContact) {
                            String name = mContact.getName(caller.getNumber());
                            caller.setContactName(name);
                        }
                        mCallerMap.put(caller.getNumber(), caller);
                    }
                }
                subscriber.onNext(mCallerMap);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    private Caller handleResponse(INumber number, boolean isOnline) {
        if (number != null) {
            Caller caller = new Caller(number, !number.isOnline());
            if (isOnline && number.isValid()) {
                mDatabase.updateCaller(caller);
                if (mSetting.isAutoReportEnabled()) {
                    mDatabase.saveMarkedRecord(number, mSetting.getUid());
                    mAlarm.alarm();
                }
            }
            return caller;
        }
        return new Caller();
    }

    public static class CallerThrowable extends Throwable {

        private String mNumber;
        private boolean mIsOnline;

        public CallerThrowable(String number, boolean isOnline) {
            mNumber = number;
            mIsOnline = isOnline;
        }

        public boolean isOnline() {
            return mIsOnline;
        }

        public String getNumber() {
            return mNumber;
        }
    }
}
