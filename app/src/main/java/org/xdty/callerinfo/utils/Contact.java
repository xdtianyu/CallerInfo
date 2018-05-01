package org.xdty.callerinfo.utils;

import android.database.Cursor;
import android.provider.ContactsContract;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.permission.Permission;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public final class Contact {

    @Inject
    Permission mPermission;

    private Map<String, String> mContactMap = new HashMap<>();
    private long lastUpdateTime;

    private Contact() {
        Application.getAppComponent().inject(this);
        loadContactCache();
    }

    public boolean isExist(String number) {
        loadContactCache();
        return mContactMap.containsKey(number);
    }

    public String getName(String number) {
        if (mContactMap.containsKey(number)) {
            return mContactMap.get(number);
        }
        return "";
    }

    private void loadContactCache() {
        if (mPermission.canReadContact() && (System.currentTimeMillis() - lastUpdateTime)
                > Constants.CONTACT_CACHE_INTERVAL) {
            loadContactMap().subscribe(new Action1<Map<String, String>>() {
                @Override
                public void call(Map<String, String> map) {
                    mContactMap.clear();
                    mContactMap.putAll(map);
                    lastUpdateTime = System.currentTimeMillis();
                }
            });
        }
    }

    private Observable<Map<String, String>> loadContactMap() {

        return Observable.create(new Observable.OnSubscribe<Map<String, String>>() {
            @Override
            public void call(final Subscriber<? super Map<String, String>> subscriber) {
                Map<String, String> contactsMap = new HashMap<>();

                Cursor cursor = Application.getApplication()
                        .getContentResolver()
                        .query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null,
                                null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String name = cursor.getString(cursor.getColumnIndex(
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String number = cursor.getString(
                                cursor.getColumnIndex(
                                        ContactsContract.CommonDataKinds.Phone.NUMBER));
                        number = number.replaceAll("[^\\d]", "");
                        contactsMap.put(number, name);
                    }
                    cursor.close();
                }

                subscriber.onNext(contactsMap);
                subscriber.onCompleted();
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    public static Contact getInstance() {
        return SingletonHelper.sINSTANCE;
    }

    private final static class SingletonHelper {
        private final static Contact sINSTANCE = new Contact();
    }
}
