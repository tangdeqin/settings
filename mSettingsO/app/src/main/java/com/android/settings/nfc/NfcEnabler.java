/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import android.content.ContentResolver;
import android.widget.Toast;

import android.util.Log;
import java.util.ArrayList;

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It is
 * turns on/off Nfc and ensures the summary of the preference reflects the
 * current state.
 */
public class NfcEnabler /*implements Preference.OnPreferenceChangeListener*/ {

    public interface INfcStateChangedCallback {
        void onStateOff();
        void onStateOn();
        void onStateTurningOn();
        void onStateTurningOff();
        void onSwitchChanged(boolean on);
    }

    public final static String TAG = "NfcEnabler";

    private  Context mContext;
    public  final NfcAdapter Nfc;
    private final IntentFilter mIntentFilter;
    private final ArrayList<INfcStateChangedCallback> mCallbacks = new ArrayList<>();

    private ContentResolver mContentResolver;


    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                handleNfcStateChanged(intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE,
                        NfcAdapter.STATE_OFF));
            }
        }
    };

    public void register(INfcStateChangedCallback callback) {
        mCallbacks.add(callback);
    }

    public void unregister(INfcStateChangedCallback callback) {
        mCallbacks.remove(callback);
    }

    public void clear() {
        mCallbacks.clear();
    }

    private void fireStateOff() {
        for (INfcStateChangedCallback callback : mCallbacks) {
            callback.onStateOff();
        }
    }

    private void fireStateOn() {
        for (INfcStateChangedCallback callback : mCallbacks) {
            callback.onStateOn();
        }
    }

    private void fireStateTurningOff() {
        for (INfcStateChangedCallback callback : mCallbacks) {
            callback.onStateTurningOff();
        }
    }

    private void fireStateTurningOn() {
        for (INfcStateChangedCallback callback : mCallbacks) {
            callback.onStateTurningOn();
        }
    }

    private void fireSwitchChanged(boolean desiredState) {
        for (INfcStateChangedCallback callback : mCallbacks) {
            callback.onSwitchChanged(desiredState);
        }        
    }

    private boolean isAirplaneModeOn() {
        return Settings.System.getInt(mContentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    public NfcEnabler(Context context) {
        mContext = context;
        Nfc = NfcAdapter.getDefaultAdapter(context);
        //final UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        //mBeamDisallowedBySystem = um.hasBaseUserRestriction(UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.of(UserHandle.myUserId()));

        if (Nfc == null) {
            // NFC is not supported
            mIntentFilter = null;
            return;
        }

        mContentResolver = context.getContentResolver();
        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
    }

    public void resume(Context context) {
        if (Nfc == null) {
            return;
        }
        if (context != null) {
            mContext = context;
        }
        handleNfcStateChanged(Nfc.getAdapterState());
        mContext.registerReceiver(mReceiver, mIntentFilter);
    }

    public void pause() {
        if (Nfc == null) {
            return;
        }
        mContext.unregisterReceiver(mReceiver);
    }

    /*@Override*/
    public boolean onPreferenceChange(Preference preference, Object value) {
        // Turn NFC on/off
        final boolean desiredState = (Boolean) value;
        // BEGIN XR#6151053 Added by binjian.tu on 2018/04/12
        if (desiredState && isAirplaneModeOn() && !NfcPreferenceController.isToggleableInAirplaneMode(mContext)) {
            Toast.makeText(mContext, com.android.settings.R.string.condition_airplane_title, Toast.LENGTH_SHORT).show();
            return false;
        }
        // END XR#6151053 Added by binjian.tu on 2018/04/12
        fireSwitchChanged(desiredState);

        if (desiredState) {
            Nfc.enable();
        } else {
            Nfc.disable();
        }

        return true;
    }

    private void handleNfcStateChanged(int newState) {
        if (0 == mCallbacks.size()) {
            Log.e(TAG, "mCallbacks doesn't have any callback, directly rurn. state:" + newState);
            return;
        }

        switch (newState) {
        case NfcAdapter.STATE_OFF:
            fireStateOff();
            break;
        case NfcAdapter.STATE_ON:
            fireStateOn();
            break;
        case NfcAdapter.STATE_TURNING_ON:
            fireStateTurningOn();
            break;
        case NfcAdapter.STATE_TURNING_OFF:
            fireStateTurningOff();
            break;
        }
    }
}
