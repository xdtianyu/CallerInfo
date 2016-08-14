package org.xdty.callerinfo.contract;

import android.content.Context;

import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.phone.number.model.INumber;
import org.xdty.phone.number.model.caller.Status;

import java.util.List;
import java.util.Map;

public interface MainContract {

    interface View extends BaseView<Presenter> {

        void showNoCallLog(boolean show);

        void showLoading(boolean active);

        void showCallLogs(List<InCall> inCalls);

        void showEula();

        void showSearchResult(INumber number);

        void showSearching();

        void showSearchFailed(boolean isOnline);

        void attachCallerMap(Map<String, Caller> callerMap);

        Context getContext();

        void notifyUpdateData(Status status);

        void showUpdateData(Status status);

        void updateDataFinished(boolean result);

        void showBottomSheet(InCall inCall);
    }

    interface Presenter extends BasePresenter {

        void result(int requestCode, int resultCode);

        void loadInCallList();

        void loadCallerMap();

        void removeInCallFromList(InCall inCall);

        void removeInCall(InCall inCall);

        void clearAll();

        void search(String number);

        void checkEula();

        void setEula();

        boolean canDrawOverlays();

        int checkPermission(String permission);

        void clearSearch();

        void dispatchUpdate(Status status);

        Caller getCaller(String number);

        void clearCache();

        void itemOnLongClicked(InCall inCall);

        void invalidateDataUpdate(boolean isInvalidate);
    }
}
