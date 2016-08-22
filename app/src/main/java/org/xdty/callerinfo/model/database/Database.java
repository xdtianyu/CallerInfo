package org.xdty.callerinfo.model.database;

import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.callerinfo.model.db.MarkedRecord;
import org.xdty.phone.number.model.INumber;

import java.util.List;

import rx.Observable;

public interface Database {

    Observable<List<InCall>> fetchInCalls();

    Observable<List<Caller>> fetchCallers();

    Observable<Void> clearAllInCalls();

    void clearAllInCallSync();

    void removeInCall(InCall inCall);

    Observable<Caller> findCaller(String number);

    Caller findCallerSync(String number);

    void removeCaller(Caller caller);

    void clearAllCallerSync();

    void updateCaller(Caller caller);

    void saveInCall(InCall inCall);

    void saveMarked(MarkedRecord markedRecord);

    void updateMarked(MarkedRecord markedRecord);

    void updateCaller(MarkedRecord markedRecord);

    Observable<List<MarkedRecord>> fetchMarkedRecords();

    Observable<MarkedRecord> findMarkedRecord(String number);

    void updateMarkedRecord(String number);

    List<Caller> fetchCallersSync();

    List<InCall> fetchInCallsSync();

    List<MarkedRecord> fetchMarkedRecordsSync();

    void addCallers(List<Caller> callers);

    void addInCallers(List<InCall> inCalls);

    void addMarkedRecords(List<MarkedRecord> markedRecords);

    void clearAllMarkedRecordSync();

    int getInCallCount(String number);

    void addInCallersSync(List<InCall> inCalls);

    void saveMarkedRecord(INumber number, String uid);

    void removeRecord(MarkedRecord record);
}
