package org.xdty.callerinfo.contract;

import org.xdty.callerinfo.model.db.InCall;

import java.util.List;

public interface MainContact {
    interface View extends BaseView<Presenter> {

        void showNoCallLog(boolean show);

        void showLoading(boolean active);

        void showCallLogs(List<InCall> inCalls);

        void showSearch();

        void showTitle(String title);

        void showEula();
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
    }
}
