package org.xdty.callerinfo.contract;

import android.content.Context;

import org.xdty.callerinfo.model.SearchMode;
import org.xdty.phone.number.model.INumber;

public interface PhoneStateContract {

    interface View {

        void show(INumber number);

        void showFailed(boolean isOnline);

        void showSearching();

        void hide(String number);

        void close(String number);

        boolean isShowing();

        Context getContext();

    }

    interface Presenter extends BasePresenter {

        boolean matchIgnore(String number);

        void handleRinging(String number);

        void handleOffHook(String number);

        void handleIdle(String number);

        void resetCallRecord();

        boolean checkClose(String number);

        boolean isIncoming(String number);

        void saveInCall();

        boolean isRingOnce();

        boolean ignoreContact(String number);

        SearchMode getSearchMode(String number);

        void searchNumber(String number);

        void handleResponse(INumber number, boolean isOnline);

        void handleResponseFailed(INumber number, boolean isOnline);

        void setOutGoingNumber(String number);

    }
}
