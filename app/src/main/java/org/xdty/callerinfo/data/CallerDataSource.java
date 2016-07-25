package org.xdty.callerinfo.data;

import org.xdty.callerinfo.model.db.Caller;

import java.util.Map;

import rx.Observable;

public interface CallerDataSource {

    Caller getCallerFromCache(String number);

    Observable<Caller> getCaller(String number);

    Observable<Map<String, Caller>> loadCallerMap();

    void setOnDataUpdateListener(OnDataUpdateListener listener);

    interface OnDataUpdateListener {

        void onDataUpdate(Caller caller);

        void onDataLoadFailed(String number, boolean isOnline);
    }

}
