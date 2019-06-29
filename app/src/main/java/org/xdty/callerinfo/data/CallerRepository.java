package org.xdty.callerinfo.data;

import android.annotation.SuppressLint;
import android.util.Log;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.SearchMode;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.model.setting.Setting;
import org.xdty.callerinfo.utils.Alarm;
import org.xdty.callerinfo.utils.Contact;
import org.xdty.phone.number.RxPhoneNumber;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.util.Utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint("CheckResult")
public class CallerRepository implements CallerDataSource {

    private static final String TAG = CallerRepository.class.getSimpleName();

    @Inject
    Database mDatabase;

    @Inject
    RxPhoneNumber mPhoneNumber;

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
        mCallerMap = Collections.synchronizedMap(new HashMap<String, Caller>());
        mErrorCache = Collections.synchronizedMap(new HashMap<String, Long>());
        mLoadingCache = Collections.synchronizedSet(new HashSet<String>());
        Application.getApplication().getAppComponent().inject(this);
    }

    public static String fixNumber(String number) {
        String fixedNumber = number;
        if (number.startsWith("+86")) {
            fixedNumber = number.replace("+86", "");
        }

        if (number.startsWith("86") && number.length() > 9) {
            fixedNumber = number.replaceFirst("^86", "");
        }

        if (number.startsWith("+400")) {
            fixedNumber = number.replace("+", "");
        }

        if (fixedNumber.startsWith("12583")) {
            fixedNumber = fixedNumber.replaceFirst("^12583.", "");
        }

        if (fixedNumber.startsWith("1259023")) {
            fixedNumber = number.replaceFirst("^1259023", "");
        }

        if (fixedNumber.startsWith("1183348")) {
            fixedNumber = number.replaceFirst("^1183348", "");
        }

        return fixedNumber;
    }

    @Override
    public Caller getCallerFromCache(String number) {

        number = fixNumber(number);

        // return empty caller if it's in error cache.
        if (mErrorCache.containsKey(number) &&
                System.currentTimeMillis() - mErrorCache.get(number) < 60 * 1000) {
            return Caller.empty(true);
        }

        return getCallerFromCache(number, true);
    }

    private Caller getCallerFromCache(final String number, boolean fetchIfNotExist) {
        Caller caller = mCallerMap.get(number);
        if (caller == null && number.contains("+86")) {
            caller = mCallerMap.get(number.replace("+86", ""));
        }

        if (caller != null) {
            return caller;
        } else if (fetchIfNotExist) {
            getCaller(number).subscribe(new Consumer<Caller>() {
                @Override
                public void accept(Caller caller) {
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
    public Observable<Caller> getCaller(String numberOrigin, final boolean forceOffline) {

        Log.d(TAG, "getCaller: " + numberOrigin + ", forceOffline: " + forceOffline);

        final String number = fixNumber(numberOrigin);

        return Observable.create(new ObservableOnSubscribe<Caller>() {
            @Override
            public void subscribe(ObservableEmitter<Caller> emitter) throws Exception {

                try {
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
                            emitter.onNext(caller);
                            break;
                        }

                        // load from database
                        caller = mDatabase.findCallerSync(number);

                        if (caller != null) {
                            if (caller.isUpdated()) {
                                cache(caller);
                                emitter.onNext(caller);
                                break;
                            } else {
                                mDatabase.removeCaller(caller);
                            }
                        }

                        // load from phone number library offline data
                        INumber iNumber = Utils.pathGeo(mPhoneNumber.getOfflineNumber(number).toList().blockingGet());

                        if (iNumber != null && iNumber.isValid()) {
                            emitter.onNext(handleResponse(iNumber, false));
                        } else {
                            emitter.onNext(Caller.empty(false));
                        }

                        // stop if the number is special
                        if (iNumber != null && (iNumber.getApiId() == INumber.API_ID_SPECIAL
                                || iNumber.getApiId() == INumber.API_ID_CALLER)) {
                            break;
                        }

                        // stop if only offline is enabled
                        if (mSetting.isOnlyOffline() || forceOffline) {
                            break;
                        }

                        // get online number info
                        INumber iOnlineNumber = Utils.mostCount(mPhoneNumber.getOnlineNumber(number).toList().blockingGet());

                        if (iOnlineNumber != null && iOnlineNumber.isValid()) {
                            if (!iOnlineNumber.hasGeo() && iNumber != null) {
                                iOnlineNumber.patch(iNumber);
                            }
                            emitter.onNext(handleResponse(iOnlineNumber, true));
                        } else {
                            if (iNumber != null) {
                                emitter.onNext(handlePatch(iNumber));
                            } else {
                                emitter.onNext(Caller.empty(true));
                            }
                        }
                    } while (false);
                } catch (Exception e) {
                    Log.e(TAG, "getCaller failed: " + e.getMessage());
                    e.printStackTrace();
                }
                emitter.onComplete();
            }
        }).doOnNext(new Consumer<Caller>() {
            @Override
            public void accept(Caller caller) throws Exception {
                Log.d(TAG, "doOnNext: " + number);
                // add number to error cache
                if (caller.isEmpty()) {
                    mErrorCache.put(number, System.currentTimeMillis());
                } else {
                    mErrorCache.remove(number);
                }
            }
        }).doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                Log.d(TAG, "doOnCompleted: " + number);
                // remove number in loading cache
                mLoadingCache.remove(number);
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Map<String, Caller>> loadCallerMap() {

        return Observable.fromCallable(new Callable<Map<String, Caller>>() {
            @Override
            public Map<String, Caller> call() throws Exception {
                mCallerMap.clear();

                List<Caller> callers = mDatabase.fetchCallersSync();
                for (Caller caller : callers) {
                    String number = caller.getNumber();
                    if (number != null && !number.isEmpty()) {
                        cache(caller);
                    }
                }
                return mCallerMap;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void setOnDataUpdateListener(OnDataUpdateListener listener) {
        mOnDataUpdateListener = listener;
    }

    @Override
    public Observable<Integer> clearCache() {

        mCallerMap.clear();
        mLoadingCache.clear();
        mErrorCache.clear();

        return Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {

                return mDatabase.clearAllCallerSync();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void updateCaller(String number, int type, String typeText) {
        mCallerMap.remove(number);
        MarkedRecord markedRecord = new MarkedRecord();
        markedRecord.setUid(mSetting.getUid());
        markedRecord.setNumber(number);
        markedRecord.setType(type);
        markedRecord.setTypeName(typeText);
        mDatabase.updateMarked(markedRecord);
        mDatabase.updateCaller(markedRecord);
        mAlarm.alarm();

        mOnDataUpdateListener.onDataUpdate(getCallerFromCache(number));
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

            cache(caller);

            return caller;
        }
        return Caller.empty(isOnline);
    }

    private Caller handlePatch(INumber number) {
        Caller caller = Caller.empty(true, number);
        caller.setOffline(false);
        caller.setNumber(number.getNumber());
        caller.setSource(number.getApiId());
        cache(caller);
        return caller;
    }

    private void cache(Caller caller) {
        if (mPermission.canReadContact()) {
            String name = mContact.getName(caller.getNumber());
            caller.setContactName(name);
        }
        mCallerMap.put(caller.getNumber(), caller);
    }

    @Override
    public SearchMode getSearchMode(String number) {
        SearchMode mode = SearchMode.ONLINE;
        if (isIgnoreContact(number)) {
            if (mSetting.isShowingContactOffline()) {
                mode = SearchMode.OFFLINE;
            } else {
                mode = SearchMode.IGNORE;
            }
        }
        return mode;
    }

    @Override
    public boolean isIgnoreContact(String number) {
        return mSetting.isIgnoreKnownContact() && mPermission.canReadContact()
                && (mContact.isExist(number) || mContact.isExist(fixNumber(number)));
    }
}
