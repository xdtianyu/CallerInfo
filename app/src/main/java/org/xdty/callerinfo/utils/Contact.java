package org.xdty.callerinfo.utils;

import android.annotation.SuppressLint;
import android.database.Cursor;
import android.provider.ContactsContract;

import org.xdty.callerinfo.application.Application;
import org.xdty.callerinfo.model.permission.Permission;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

@SuppressWarnings("ResultOfMethodCallIgnored")
@SuppressLint("CheckResult")
public final class Contact {

    @Inject
    Permission mPermission;

    private Map<String, String> mContactMap = new HashMap<>();
    private long lastUpdateTime;

    private Contact() {
        Application.Companion.getAppComponent().inject(this);
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
            loadContactMap().subscribe(new Consumer<Map<String, String>>() {
                @Override
                public void accept(Map<String, String> map) {
                    mContactMap.clear();
                    mContactMap.putAll(map);
                    lastUpdateTime = System.currentTimeMillis();
                }
            });
        }
    }

    private Observable<Map<String, String>> loadContactMap() {

        return Observable.create(new ObservableOnSubscribe<Map<String, String>>() {
            @Override
            public void subscribe(ObservableEmitter<Map<String, String>> emitter) throws Exception {
                Map<String, String> contactsMap = new HashMap<>();

                Cursor cursor = Application.Companion.getApplication()
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
                        if (number != null) {
                            number = number.replaceAll("[^\\d]", "");
                            contactsMap.put(number, name);
                        }
                    }
                    cursor.close();
                }

                emitter.onNext(contactsMap);
                emitter.onComplete();
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
