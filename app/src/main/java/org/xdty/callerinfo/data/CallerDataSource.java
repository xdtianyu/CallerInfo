package org.xdty.callerinfo.data;

import org.xdty.callerinfo.model.db.Caller;

import java.util.Map;

import rx.Observable;

public interface CallerDataSource {

    Observable<Caller> getCaller(String number);

    Observable<Map<String, Caller>> loadCallerMap();

}
