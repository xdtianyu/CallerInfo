package org.xdty.callerinfo.model.database;

import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;

import java.util.List;

import rx.Observable;

public interface Database {

    Observable<List<InCall>> fetchInCalls();

    Observable<List<Caller>> fetchCallers();

    void clearAllInCalls(List<InCall> inCallList);

    void removeInCall(InCall inCall);

    Observable<Caller> findCaller(String number);

    void removeCaller(Caller caller);

    void saveCaller(Caller caller);

    void saveInCall(InCall inCall);

    void saveMarked(MarkedRecord markedRecord);

    void saveCaller(MarkedRecord markedRecord);
}
