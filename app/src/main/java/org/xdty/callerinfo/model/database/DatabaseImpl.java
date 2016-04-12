package org.xdty.callerinfo.model.database;

import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class DatabaseImpl implements Database {

    private static DatabaseImpl sDatabase;

    private DatabaseImpl() {

    }

    public static DatabaseImpl getInstance() {
        if (sDatabase == null) {
            sDatabase = new DatabaseImpl();
        }
        return sDatabase;
    }

    @Override
    public Observable<List<InCall>> fetchInCalls() {
        return Observable.create(new Observable.OnSubscribe<List<InCall>>() {
            @Override
            public void call(Subscriber<? super List<InCall>> subscriber) {
                subscriber.onNext(InCall.listAll(InCall.class, "time DESC"));
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void clearAllInCalls(List<InCall> inCallList) {
        Observable.from(inCallList).observeOn(Schedulers.io()).subscribe(
                new Action1<InCall>() {
                    @Override
                    public void call(InCall inCall) {
                        inCall.delete();
                    }
                });
    }

    @Override
    public void removeInCall(InCall inCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe(new Action1<InCall>() {
            @Override
            public void call(InCall inCall) {
                inCall.delete();
            }
        });
    }

    @Override
    public Observable<Caller> findCaller(final String number) {
        return Observable.create(new Observable.OnSubscribe<Caller>() {
            @Override
            public void call(Subscriber<? super Caller> subscriber) {
                List<Caller> callers = Caller.find(Caller.class, "number=?", number);
                Caller caller = null;
                if (callers.size() > 0) {
                    caller = callers.get(0);
                }
                subscriber.onNext(caller);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void removeCaller(Caller caller) {
        Observable.just(caller).observeOn(Schedulers.io()).subscribe(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                caller.delete();
            }
        });
    }

    @Override
    public void saveCaller(Caller caller) {
        Observable.just(caller).observeOn(Schedulers.io()).subscribe(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                caller.save();
            }
        });
    }

    @Override
    public void saveInCall(InCall inCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe(new Action1<InCall>() {
            @Override
            public void call(InCall inCall) {
                inCall.save();
            }
        });
    }
}
