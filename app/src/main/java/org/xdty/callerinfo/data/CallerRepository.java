package org.xdty.callerinfo.data;

import android.util.Log;

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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class CallerRepository implements CallerDataSource {

    private static final String TAG = CallerRepository.class.getSimpleName();

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

    private Set<String> mLoadingCache;

    private Map<String, Long> mErrorCache;

    private OnDataUpdateListener mOnDataUpdateListener;

    public CallerRepository() {
        mCallerMap = new HashMap<>();
        mErrorCache = new HashMap<>();
        mLoadingCache = Collections.synchronizedSet(new HashSet<String>());
        Application.getAppComponent().inject(this);
    }

    @Override
    public Caller getCallerFromCache(String number) {

        // return empty caller if it's in error cache.
        if (mErrorCache.containsKey(number) &&
                System.currentTimeMillis() - mErrorCache.get(number) < 60 * 1000) {
            return Caller.empty(true);
        }

        return getCallerFromCache(number, true);
    }

    private Caller getCallerFromCache(final String number, boolean fetchIfNotExist) {
        Caller caller = mCallerMap.get(number);
        if (caller == null) {
            if (number.contains("+86")) {
                caller = mCallerMap.get(number.replace("+86", ""));
            } else if (number.length > 7 && number.substring(0, 5) == "12583") {
                caller = mCallerMap.get(number.substring(6));
            }
        }

        if (caller != null && caller.isUpdated()) {
            return caller;
        } else if (fetchIfNotExist) {
            getCaller(number).subscribe(new Action1<Caller>() {
                @Override
                public void call(Caller caller) {
                    Log.e(TAG, "call: " + number + "->" + caller.getNumber());
                    if (mOnDataUpdateListener != null) {
                        mOnDataUpdateListener.onDataUpdate(caller);
                    }
                }
            });
        }
        return Caller.empty(false);
    }

    @Override
    public Observable<Caller> getCaller(String number) {
        return getCaller(number, false);
    }

    @Override
    public Observable<Caller> getCaller(final String number, final boolean forceOffline) {

        Log.d(TAG, "getCaller: " + number + ", forceOffline: " + forceOffline);

        return Observable.create(new Observable.OnSubscribe<Caller>() {
            @Override
            public void call(final Subscriber<? super Caller> subscriber) {

                do {
                    // check loading cache
                    if (mLoadingCache.contains(number)) {
                        // return without onCompleted
                        return;
                    }
                    mLoadingCache.add(number);

                    // load from cache
                    Caller caller = getCallerFromCache(number, false);

                    if (caller != null && caller.isUpdated()) {
                        subscriber.onNext(caller);
                        break;
                    }

                    // load from database
                    caller = mDatabase.findCallerSync(number);

                    if (caller != null) {
                        if (caller.isUpdated()) {
                            subscriber.onNext(caller);
                            break;
                        } else {
                            mDatabase.removeCaller(caller);
                        }
                    }

                    // load from phone number library offline data
                    INumber iNumber = mPhoneNumber.getOfflineNumber(number);

                    if (iNumber != null && iNumber.isValid()) {
                        subscriber.onNext(handleResponse(iNumber, false));
                    } else {
                        subscriber.onNext(Caller.empty(false));
                    }

                    // stop if the number is special
                    if (iNumber instanceof SpecialNumber || iNumber instanceof CallerNumber) {
                        break;
                    }

                    // stop if only offline is enabled
                    if (mSetting.isOnlyOffline() || forceOffline) {
                        break;
                    }

                    // get online number info
                    if (number.length > 7 && number.substring(0, 5) == "12583") {
                        iNumber = mPhoneNumber.getNumber(number.substring(6));
                    } else {
                        iNumber = mPhoneNumber.getNumber(number);
                    }

                    if (iNumber != null && iNumber.isValid()) {
                        subscriber.onNext(handleResponse(iNumber, true));
                    } else {
                        subscriber.onNext(Caller.empty(true));
                    }
                } while (false);

                subscriber.onCompleted();
            }
        }).doOnNext(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                Log.d(TAG, "doOnNext: " + number);
                // add number to error cache
                if (caller.isEmpty()) {
                    mErrorCache.put(number, System.currentTimeMillis());
                } else {
                    mErrorCache.remove(number);
                }
            }
        }).doOnCompleted(new Action0() {
            @Override
            public void call() {
                Log.d(TAG, "doOnCompleted: " + number);
                // remove number in loading cache
                mLoadingCache.remove(number);
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

    @Override
    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        mOnDataUpdateListener = listener;
    }

    @Override
    public Observable<Void> clearCache() {

        mCallerMap.clear();
        mLoadingCache.clear();
        mErrorCache.clear();

        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                mDatabase.clearAllCallerSync();
                subscriber.onNext(null);
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

            mCallerMap.put(caller.getNumber(), caller);

            return caller;
        }
        return Caller.empty(isOnline);
    }
}
