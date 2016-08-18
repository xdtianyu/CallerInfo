package org.xdty.callerinfo.data;

import org.xdty.callerinfo.model.SearchMode;
import org.xdty.callerinfo.model.db.Caller;

import java.util.Map;

import rx.Observable;

public interface CallerDataSource {

    Caller getCallerFromCache(String number);

    Observable<Caller> getCaller(String number);

    Observable<Caller> getCaller(String number, boolean forceOffline);

    Observable<Map<String, Caller>> loadCallerMap();

    void setOnDataUpdateListener(OnDataUpdateListener listener);

    Observable<Void> clearCache();

    void updateCaller(String number, int type, String typeText);

    boolean isIgnoreContact(String number);

    SearchMode getSearchMode(String number);

    interface OnDataUpdateListener {

        void onDataUpdate(Caller caller);
    }

}
