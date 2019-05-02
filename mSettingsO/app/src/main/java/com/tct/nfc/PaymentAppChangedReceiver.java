
/*************************************************
 Copyright (C), 1999-2016, TCL communication Co., Ltd.
 File name: PaymentAppChangedReceiver.java
 Author: binjian.tu.hz@tcl.com
 Version: 1.0
 Date: 2016/06/08
 Description: Receiver of com.android.nfc.PAYMENT_APP_CHANGED
 History:
 *************************************************/

package com.tct.nfc;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

public class PaymentAppChangedReceiver extends BroadcastReceiver {

    public static final String ACTION = "com.android.nfc.PAYMENT_APP_CHANGED";
    public static final String ROLLBACK_KEY = "com.tct.nfc.ROLLBACK_";
    public static final String PREVIOUS_PAYMENT_APP_KEY = "com.tct.nfc.PREVIOUS_PAYMENT_APP_";
    public static final String NFC_PAYMENT_PERSISTENT = "com.tct.nfc.NFC_PAYMENT_CHANGED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!ACTION.equals(intent.getAction())) {
            return;
        }

        /**
         * Settings overrides getSharedPreferences.
         * When you try to get default shared preference,
         * it will always return a new logger instead of file persistent preference to caller!
         */
        //SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences preferences = context.getSharedPreferences(NFC_PAYMENT_PERSISTENT, Context.MODE_PRIVATE);
        final String previousApp = intent.getStringExtra("from");
        final int userId = intent.getIntExtra("user", ActivityManager.getCurrentUser());
        final String paymentKey = PREVIOUS_PAYMENT_APP_KEY + userId;
        final boolean isFull = intent.getBooleanExtra("full", false);
        SharedPreferences.Editor e = preferences.edit();
        e.putString(PREVIOUS_PAYMENT_APP_KEY + userId, previousApp);
        e.putBoolean(ROLLBACK_KEY + userId, isFull);
        e.apply();
    }
}
