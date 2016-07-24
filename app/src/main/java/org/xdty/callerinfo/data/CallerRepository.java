package org.xdty.callerinfo.data;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.database.Database;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.permission.Permission;
import org.xdty.callerinfo.utils.Contact;
import org.xdty.phone.number.PhoneNumber;
import org.xdty.phone.number.model.INumber;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class CallerRepository implements CallerDataSource, PhoneNumber.Callback {

    @Inject
    Database mDatabase;

    @Inject
    PhoneNumber mPhoneNumber;

    @Inject
    Permission mPermission;

    @Inject
    Contact mContact;

    private Map<String, Caller> mCallerMap;

    public CallerRepository() {
        mCallerMap = new HashMap<>();
        Application.getAppComponent().inject(this);

        mPhoneNumber.addCallback(this);
    }

    @Override
    public void onResponseOffline(INumber number) {

    }

    @Override
    public void onResponse(INumber number) {

    }

    @Override
    public void onResponseFailed(INumber number, boolean isOnline) {

    }

    @Override
    public Observable<Caller> getCaller(String number) {
        Caller caller = mCallerMap.get(number);
        if (caller == null && number.contains("+86")) {
            caller = mCallerMap.get(number.replace("+86", ""));
        }

        if (caller == null) {
            mPhoneNumber.fetch(number);
        }
        return null;
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
}
