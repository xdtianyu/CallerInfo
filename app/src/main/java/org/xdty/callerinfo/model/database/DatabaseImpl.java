package org.xdty.callerinfo.model.database;

import android.util.Log;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

import java.util.List;

import javax.inject.Inject;

import io.requery.Persistable;
import io.requery.sql.EntityDataStore;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

public class DatabaseImpl implements Database {

    @Inject
    EntityDataStore<Persistable> mDataStore;

    private DatabaseImpl() {
        Application.getAppComponent().inject(this);
    }

    public static DatabaseImpl getInstance() {
        return SingletonHelper.INSTANCE;
    }

    @Override
    public Observable<List<InCall>> fetchInCalls() {

        return Observable.create(new Observable.OnSubscribe<List<InCall>>() {
            @Override
            public void call(Subscriber<? super List<InCall>> subscriber) {
                subscriber.onNext(mDataStore.select(InCall.class)
                        .orderBy(InCall.TIME.desc())
                        .get()
                        .toList());
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Caller>> fetchCallers() {
        return Observable.create(new Observable.OnSubscribe<List<Caller>>() {
            @Override
            public void call(Subscriber<? super List<Caller>> subscriber) {
                subscriber.onNext(mDataStore.select(Caller.class).get().toList());
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Void> clearAllInCalls() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                mDataStore.delete(InCall.class).get().value();
                subscriber.onNext(null);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void clearAllInCallSync() {
        mDataStore.delete(InCall.class).get().value();
    }

    @Override
    public void removeInCall(InCall inCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe(new Action1<InCall>() {
            @Override
            public void call(InCall inCall) {
                mDataStore.delete(inCall);
            }
        });
    }

    @Override
    public Observable<Caller> findCaller(final String number) {
        return Observable.create(new Observable.OnSubscribe<Caller>() {
            @Override
            public void call(Subscriber<? super Caller> subscriber) {
                List<Caller> callers = mDataStore.select(Caller.class)
                        .where(Caller.NUMBER.eq(number))
                        .get()
                        .toList();
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
    public Caller findCallerSync(String number) {
        List<Caller> callers = mDataStore.select(Caller.class)
                .where(Caller.NUMBER.eq(number))
                .get()
                .toList();
        Caller caller = null;
        if (callers.size() > 0) {
            caller = callers.get(0);
        }
        return caller;
    }

    @Override
    public void removeCaller(Caller caller) {
        Observable.just(caller).observeOn(Schedulers.io()).subscribe(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                mDataStore.delete(caller);
            }
        });
    }

    @Override
    public void clearAllCallerSync() {
        mDataStore.delete(Caller.class).get().value();
    }

    @Override
    public void updateCaller(Caller caller) {
        Observable.just(caller).observeOn(Schedulers.io()).subscribe(new Action1<Caller>() {
            @Override
            public void call(Caller caller) {
                mDataStore.upsert(caller);
            }
        });
    }

    @Override
    public void saveInCall(InCall inCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe(new Action1<InCall>() {
            @Override
            public void call(InCall inCall) {
                mDataStore.insert(inCall);
            }
        });
    }

    @Override
    public void saveMarked(MarkedRecord markedRecord) {
        Observable.just(markedRecord)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<MarkedRecord>() {
                    @Override
                    public void call(MarkedRecord markedRecord) {
                        mDataStore.insert(markedRecord);
                    }
                });
    }

    @Override
    public void updateMarked(MarkedRecord markedRecord) {
        Observable.just(markedRecord)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<MarkedRecord>() {
                    @Override
                    public void call(MarkedRecord markedRecord) {
                        mDataStore.upsert(markedRecord);
                    }
                });
    }

    @Override
    public void updateCaller(MarkedRecord markedRecord) {
        Observable.just(markedRecord)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<MarkedRecord>() {
                    @Override
                    public void call(MarkedRecord markedRecord) {
                        Caller caller = new Caller();
                        caller.setNumber(markedRecord.getNumber());
                        caller.setName(markedRecord.getTypeName());
                        caller.setLastUpdate(markedRecord.getTime());
                        caller.setType("report");
                        caller.setOffline(false);
                        mDataStore.upsert(caller);
                    }
                });
    }

    @Override
    public Observable<List<MarkedRecord>> fetchMarkedRecords() {
        return Observable.create(new Observable.OnSubscribe<List<MarkedRecord>>() {
            @Override
            public void call(Subscriber<? super List<MarkedRecord>> subscriber) {
                subscriber.onNext(mDataStore.select(MarkedRecord.class).get().toList());
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<MarkedRecord> findMarkedRecord(final String number) {
        return Observable.create(new Observable.OnSubscribe<MarkedRecord>() {
            @Override
            public void call(Subscriber<? super MarkedRecord> subscriber) {
                List<MarkedRecord> records = mDataStore.select(MarkedRecord.class)
                        .where(MarkedRecord.NUMBER.eq(number))
                        .get()
                        .toList();
                MarkedRecord record = null;
                if (records.size() > 0) {
                    record = records.get(0);
                }
                subscriber.onNext(record);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void updateMarkedRecord(String number) {
        Observable.just(number)
                .observeOn(Schedulers.io())
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String number) {
                        List<MarkedRecord> records = mDataStore.select(MarkedRecord.class)
                                .where(MarkedRecord.NUMBER.eq(number))
                                .get()
                                .toList();
                        MarkedRecord record = null;
                        if (records.size() > 0) {
                            record = records.get(0);
                            record.setReported(true);
                            mDataStore.update(record);
                        }

                        if (records.size() > 1) {
                            Log.e("DatabaseImpl", "updateMarkedRecord duplicate number: " + number);
                        }
                    }
                });
    }

    @Override
    public List<Caller> fetchCallersSync() {
        return mDataStore.select(Caller.class).get().toList();
    }

    @Override
    public List<InCall> fetchInCallsSync() {
        return mDataStore.select(InCall.class).orderBy(InCall.TIME.desc()).get().toList();
    }

    @Override
    public List<MarkedRecord> fetchMarkedRecordsSync() {
        return mDataStore.select(MarkedRecord.class).get().toList();
    }

    @Override
    public void addCallers(List<Caller> callers) {
        Observable.from(callers)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<Caller, Observable<Caller>>() {
                    @Override
                    public Observable<Caller> call(Caller caller) {
                        return Observable.just(caller);
                    }
                })
                .subscribe(new Action1<Caller>() {
                    @Override
                    public void call(Caller caller) {
                        mDataStore.insert(caller);
                    }
                });
    }

    @Override
    public void addInCallers(List<InCall> inCalls) {
        Observable.from(inCalls)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<InCall, Observable<InCall>>() {
                    @Override
                    public Observable<InCall> call(InCall inCall) {
                        return Observable.just(inCall);
                    }
                })
                .subscribe(new Action1<InCall>() {
                    @Override
                    public void call(InCall inCall) {
                        mDataStore.insert(inCall);
                    }
                });
    }

    @Override
    public void addMarkedRecords(List<MarkedRecord> markedRecords) {
        Observable.from(markedRecords)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap(new Func1<MarkedRecord, Observable<MarkedRecord>>() {
                    @Override
                    public Observable<MarkedRecord> call(MarkedRecord record) {
                        return Observable.just(record);
                    }
                })
                .subscribe(new Action1<MarkedRecord>() {
                    @Override
                    public void call(MarkedRecord record) {
                        mDataStore.insert(record);
                    }
                });
    }

    @Override
    public void clearAllMarkedRecordSync() {
        mDataStore.delete(MarkedRecord.class).get().value();
    }

    @Override
    public int getInCallCount(String number) {
        long time = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        return mDataStore.count(InCall.class)
                .where(InCall.NUMBER.eq(number))
                .and(InCall.TIME.gt(time))
                .get()
                .value();
    }

    @Override
    public void addInCallersSync(List<InCall> inCalls) {
        for (InCall inCall : inCalls) {
            mDataStore.insert(inCall);
        }
    }

    public void saveMarkedRecord(final INumber number, final String uid) {
        if (number.getType() == Type.REPORT) {
            findMarkedRecord(number.getNumber()).subscribe(new Action1<MarkedRecord>() {
                @Override
                public void call(MarkedRecord record) {
                    if (record == null) {
                        int type = Utils.typeFromString(number.getName());
                        if (type >= 0) {
                            MarkedRecord markedRecord = new MarkedRecord();
                            markedRecord.setNumber(number.getNumber());
                            markedRecord.setUid(uid);
                            markedRecord.setSource(number.getApiId());
                            markedRecord.setType(type);
                            markedRecord.setCount(number.getCount());
                            markedRecord.setTypeName(number.getName());
                            saveMarked(markedRecord);
                        }
                    }
                }
            });
        }
    }

    @Override
    public void removeRecord(MarkedRecord record) {
        Observable.just(record).observeOn(Schedulers.io()).subscribe(new Action1<MarkedRecord>() {
            @Override
            public void call(MarkedRecord record) {
                mDataStore.delete(record);
                removeCaller(findCallerSync(record.getNumber()));
            }
        });
    }

    private static class SingletonHelper {
        private final static DatabaseImpl INSTANCE = new DatabaseImpl();
    }

}
