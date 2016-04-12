package org.xdty.callerinfo.model.database;

import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;

import java.util.List;

import rx.Observable;

public interface Database {

    Observable<List<InCall>> fetchInCalls();

    void clearAllInCalls(List<InCall> inCallList);

    void removeInCall(InCall inCall);

    Observable<Caller> findCaller(String number);

    void removeCaller(Caller caller);

    void saveCaller(Caller caller);

    void saveInCall(InCall inCall);

}
