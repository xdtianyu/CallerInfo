package org.xdty.callerinfo.contract;

import android.content.Context;
import android.support.annotation.NonNull;

import org.xdty.callerinfo.model.db.Caller;
import org.xdty.callerinfo.model.db.InCall;
import org.xdty.phone.number.model.INumber;

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

        void requestDrawOverlays(int requestCode);

        int checkPermission(String permission);

        void requestPermissions(@NonNull String[] permissions, int requestCode);

        void handleResponse(INumber number, boolean isOnline);

        void handleResponseFailed(INumber number, boolean isOnline);

        void clearSearch();
    }
}
