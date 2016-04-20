package org.xdty.callerinfo.contract;

import android.support.annotation.NonNull;

import org.xdty.callerinfo.model.db.InCall;
import org.xdty.phone.number.model.INumber;

import java.util.List;

public interface MainContract {

    interface View extends BaseView<Presenter> {

        void showNoCallLog(boolean show);

        void showLoading(boolean active);

        void showCallLogs(List<InCall> inCalls);

        void showEula();

        void showSearchResult(INumber number);

        void showSearching();

        void showSearchFailed(boolean isOnline);
    }

    interface Presenter extends BasePresenter {

        void result(int requestCode, int resultCode);

        void loadInCallList();

        void removeInCallFromList(int position);

        void removeInCall(int position);

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
