package org.xdty.callerinfo.utils;

import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;

import org.xdty.callerinfo.application.Application;

public final class Contact {

    public boolean isExist(String number) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {
                ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.DISPLAY_NAME
        };
        Cursor cur = Application.getApplication()
                .getContentResolver()
                .query(lookupUri, mPhoneNumberProjection, null,
                        null, null);
        if (cur != null) {
            try {
                if (cur.moveToFirst()) {
                    return true;
                }
            } finally {
                cur.close();
            }
        }
        return false;
    }

    public String getName(String number) {
        Uri lookupUri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(number));
        String[] mPhoneNumberProjection = {
                ContactsContract.PhoneLookup._ID, ContactsContract.PhoneLookup.NUMBER,
                ContactsContract.PhoneLookup.DISPLAY_NAME
        };
        Cursor cur = Application.getApplication()
                .getContentResolver()
                .query(lookupUri, mPhoneNumberProjection, null,
                        null, null);
        if (cur != null) {
            try {
                if (cur.moveToFirst()) {
                    return cur.getString(
                            cur.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                }
            } finally {
                cur.close();
            }
        }
        return "";
    }
}
