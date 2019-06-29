package org.xdty.callerinfo.model.database;

import android.annotation.SuppressLint;
import android.util.Log;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.callerinfo.utils.Utils;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.Type;

import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.requery.Persistable;
import io.requery.sql.EntityDataStore;

@SuppressLint("CheckResult")
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DatabaseImpl implements Database {

    @Inject
    EntityDataStore<Persistable> mDataStore;

    private DatabaseImpl() {
        Application.getApplication().getAppComponent().inject(this);
    }

    public static DatabaseImpl getInstance() {
        return SingletonHelper.INSTANCE;
    }

    @Override
    public Observable<List<InCall>> fetchInCalls() {

        return Observable.create(new ObservableOnSubscribe<List<InCall>>() {
            @Override
            public void subscribe(ObservableEmitter<List<InCall>> emitter) throws Exception {
                emitter.onNext(mDataStore.select(InCall.class)
                        .orderBy(InCall.TIME.desc())
                        .get()
                        .toList());
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<List<Caller>> fetchCallers() {
        return Observable.create(new ObservableOnSubscribe<List<Caller>>() {
            @Override
            public void subscribe(ObservableEmitter<List<Caller>> emitter) throws Exception {
                emitter.onNext(mDataStore.select(Caller.class).get().toList());
                emitter.onComplete();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<Integer> clearAllInCalls() {
        return Observable.fromCallable(new Callable<Integer>() {
            @Override
            public Integer call() {
                return mDataStore.delete(InCall.class).get().value();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void clearAllInCallSync() {
        mDataStore.delete(InCall.class).get().value();
    }

    @Override
    public void removeInCall(InCall inCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe(new Consumer<InCall>() {
            @Override
            public void accept(InCall inCall) {
                try {
                    mDataStore.delete(inCall);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public Observable<Caller> findCaller(final String number) {
        return Observable.create(new ObservableOnSubscribe<Caller>() {
            @Override
            public void subscribe(ObservableEmitter<Caller> emitter) throws Exception {
                List<Caller> callers = mDataStore.select(Caller.class)
                        .where(Caller.NUMBER.eq(number))
                        .get()
                        .toList();
                Caller caller = null;
                if (callers.size() > 0) {
                    caller = callers.get(0);
                }
                emitter.onNext(caller);
                emitter.onComplete();
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
        Observable.just(caller).observeOn(Schedulers.io()).subscribe(new Consumer<Caller>() {
            @Override
            public void accept(Caller caller) {
                mDataStore.delete(caller);
            }
        });
    }

    @Override
    public int clearAllCallerSync() {
        return mDataStore.delete(Caller.class).get().value();
    }

    @Override
    public void updateCaller(Caller caller) {
        Observable.just(caller).observeOn(Schedulers.io()).subscribe(new Consumer<Caller>() {
            @Override
            public void accept(Caller caller) {
                Caller c = mDataStore.select(Caller.class).where(
                        Caller.NUMBER.eq(caller.getNumber())).get().firstOr(caller);
                if (c != caller) {
                    c.setCallerSource(caller.getCallerSource());
                    c.setCallerType(caller.getCallerType());
                    c.setCity(caller.getCity());
                    c.setName(caller.getName());
                    c.setName(caller.getName());
                    c.setType(caller.getType().getText());
                    c.setCount(caller.getCount());
                    c.setProvince(caller.getProvince());
                    c.setOperators(caller.getOperators());
                    c.setLastUpdate(caller.getLastUpdate());
                }
                mDataStore.upsert(c);
            }
        });
    }

    @Override
    public void saveInCall(InCall inCall) {
        Observable.just(inCall).observeOn(Schedulers.io()).subscribe(new Consumer<InCall>() {
            @Override
            public void accept(InCall inCall) {
                mDataStore.insert(inCall);
            }
        });
    }

    @Override
    public void saveMarked(MarkedRecord markedRecord) {
        updateMarked(markedRecord);
    }

    @Override
    public void updateMarked(MarkedRecord markedRecord) {
        Observable.just(markedRecord)
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<MarkedRecord>() {
                    @Override
                    public void accept(MarkedRecord markedRecord) {
                        MarkedRecord record = mDataStore.select(MarkedRecord.class)
                                .where(MarkedRecord.NUMBER.eq(markedRecord.getNumber()))
                                .get()
                                .firstOr(markedRecord);

                        if (record != markedRecord) {
                            record.setCount(markedRecord.getCount());
                            record.setReported(false);
                            record.setSource(markedRecord.getSource());
                            record.setTime(markedRecord.getTime());
                            record.setType(markedRecord.getType());
                            record.setTypeName(markedRecord.getTypeName());
                        }

                        mDataStore.upsert(record);
                    }
                });
    }

    @Override
    public void updateCaller(MarkedRecord markedRecord) {
        Observable.just(markedRecord)
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<MarkedRecord>() {
                    @Override
                    public void accept(MarkedRecord markedRecord) {
                        Caller caller = mDataStore.select(Caller.class)
                                .where(
                                        Caller.NUMBER.eq(markedRecord.getNumber()))
                                .get()
                                .firstOr(new Caller());
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
        return Observable.fromCallable(new Callable<List<MarkedRecord>>() {
            @Override
            public List<MarkedRecord> call() throws Exception {
                return mDataStore.select(MarkedRecord.class).get().toList();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Observable<MarkedRecord> findMarkedRecord(final String number) {
        return Observable.fromCallable(new Callable<MarkedRecord>() {
            @Override
            public MarkedRecord call() throws Exception {
                List<MarkedRecord> records = mDataStore.select(MarkedRecord.class)
                        .where(MarkedRecord.NUMBER.eq(number))
                        .get()
                        .toList();
                MarkedRecord record = null;
                if (records.size() > 0) {
                    record = records.get(0);
                }
                return record;
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void updateMarkedRecord(String number) {
        Observable.just(number)
                .observeOn(Schedulers.io())
                .subscribe(new Consumer<String>() {
                    @Override
                    public void accept(String number) {
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
        Observable.fromIterable(callers)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<Caller, Observable<Caller>>() {
                    @Override
                    public Observable<Caller> apply(Caller caller) {
                        return Observable.just(caller);
                    }
                })
                .subscribe(new Consumer<Caller>() {
                    @Override
                    public void accept(Caller caller) {
                        mDataStore.upsert(caller);
                    }
                });
    }

    @Override
    public void addInCallers(List<InCall> inCalls) {
        Observable.fromIterable(inCalls)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<InCall, Observable<InCall>>() {
                    @Override
                    public Observable<InCall> apply(InCall inCall) {
                        return Observable.just(inCall);
                    }
                })
                .subscribe(new Consumer<InCall>() {
                    @Override
                    public void accept(InCall inCall) {
                        mDataStore.insert(inCall);
                    }
                });
    }

    @Override
    public void addMarkedRecords(List<MarkedRecord> markedRecords) {
        Observable.fromIterable(markedRecords)
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<MarkedRecord, Observable<MarkedRecord>>() {
                    @Override
                    public Observable<MarkedRecord> apply(MarkedRecord record) {
                        return Observable.just(record);
                    }
                })
                .subscribe(new Consumer<MarkedRecord>() {
                    @Override
                    public void accept(MarkedRecord markedRecord) {
                        MarkedRecord record = mDataStore.select(MarkedRecord.class)
                                .where(MarkedRecord.NUMBER.eq(markedRecord.getNumber()))
                                .get()
                                .firstOr(markedRecord);

                        if (record != markedRecord) {
                            record.setCount(markedRecord.getCount());
                            record.setReported(false);
                            record.setSource(markedRecord.getSource());
                            record.setTime(markedRecord.getTime());
                            record.setType(markedRecord.getType());
                            record.setTypeName(markedRecord.getTypeName());
                        }

                        mDataStore.upsert(record);
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
            findMarkedRecord(number.getNumber()).subscribe(new Consumer<MarkedRecord>() {
                @Override
                public void accept(MarkedRecord record) {
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
        Observable.just(record).observeOn(Schedulers.io()).subscribe(new Consumer<MarkedRecord>() {
            @Override
            public void accept(MarkedRecord record) {
                mDataStore.delete(record);
                removeCaller(findCallerSync(record.getNumber()));
            }
        });
    }

    private static class SingletonHelper {
        private final static DatabaseImpl INSTANCE = new DatabaseImpl();
    }

}
