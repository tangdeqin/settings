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


import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;
import android.nfc.NfcAdapter;

import com.android.settings.R;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.WirelessUtils;

import android.provider.Settings;
import android.content.ContentResolver;

public final class NfcMasterSwitcher 
        implements SwitchWidgetController.OnSwitchChangeListener,
                   NfcEnabler.INfcStateChangedCallback {
    private static final String TAG = "NfcMasterSwitcher";
    private final SwitchWidgetController mSwitchWidget;
    private Context mContext;
    private final NfcAdapter mNfcAdapter;
    private boolean mValidListener;
    private boolean mUpdateStatusOnly = false;
    private NfcEnabler mNfcEnabler;
    private ContentResolver mContentResolver;

    public NfcMasterSwitcher(Context context, SwitchWidgetController switchWidget) {
        mContext = context;
        mNfcEnabler = new NfcEnabler(context);
        mContentResolver = context.getContentResolver();
        mSwitchWidget = switchWidget;
        mSwitchWidget.setListener(this);
        mNfcAdapter = mNfcEnabler.Nfc;
        mValidListener = false;

        if (null == mNfcAdapter) {
            mSwitchWidget.setEnabled(false);
        }
    }

    public void setupSwitchController() {
        mSwitchWidget.setupView();
    }

    public void teardownSwitchController() {
        mSwitchWidget.teardownView();
    }

    public void resume(Context context) {
        Log.d(TAG, "resume");
        if (mContext != context) {
            mContext = context;
        }

        if (mNfcAdapter == null) {
            mSwitchWidget.setEnabled(false);
            return;
        }

        mNfcEnabler.resume(context);
        mNfcEnabler.register(this);
        mSwitchWidget.startListening();
        mValidListener = true;
    }

    public void pause() {
        Log.d(TAG, "pause");
        if (mNfcAdapter == null) {
            return;
        }
        if (mValidListener) {
            mNfcEnabler.pause();
            mNfcEnabler.clear();
            mSwitchWidget.stopListening();
            mValidListener = false;
        }
    }

    @Override
    public void onStateOff() {
        mUpdateStatusOnly = true;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to true");
        setChecked(false);
        mSwitchWidget.setEnabled(true);
        mUpdateStatusOnly = false;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to false");
    }

    @Override
    public void onStateOn() {
        mUpdateStatusOnly = true;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to true");
        setChecked(true);
        mSwitchWidget.setEnabled(true);
        mUpdateStatusOnly = false;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to false");
    }

    @Override
    public void onStateTurningOn() {
        mUpdateStatusOnly = true;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to true");
        setChecked(true);
        mSwitchWidget.setEnabled(false);
        mUpdateStatusOnly = false;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to false");
    }

    @Override
    public void onStateTurningOff() {
        mUpdateStatusOnly = true;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to true");
        setChecked(false);
        mSwitchWidget.setEnabled(false);
        mUpdateStatusOnly = false;
        Log.d(TAG, "Begin update status: set mUpdateStatusOnly to false");
    }

    @Override
    public void onSwitchChanged(boolean on) {
        onSwitchToggled(on);
    }

    private void setChecked(boolean isChecked) {
        Log.d(TAG, "binjian:" + mSwitchWidget);
        final boolean currentState =
                (mSwitchWidget.getSwitch() != null) && mSwitchWidget.getSwitch().isChecked();
        if (isChecked != currentState) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitchWidget.stopListening();
            }
            mSwitchWidget.setChecked(isChecked);
            if (mValidListener) {
                mSwitchWidget.startListening();
            }
        }
    }

    private boolean isAirplaneModeOn() {
        return Settings.System.getInt(mContentResolver,
                Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        Log.d(TAG, "onSwitchChanged to " + isChecked);
        if (!mUpdateStatusOnly) {
            if (isChecked) {
                // BEGIN XR#6151053 Added by binjian.tu on 2018/04/12
                if (isAirplaneModeOn() && !NfcPreferenceController.isToggleableInAirplaneMode(mContext)) {
                    Toast.makeText(mContext, com.android.settings.R.string.condition_airplane_title, Toast.LENGTH_SHORT).show();
                    return false;
                }
                // END XR#6151053 Added by binjian.tu on 2018/04/12
                mNfcAdapter.enable();
            } else {
                mNfcAdapter.disable();
            }
        }
        mSwitchWidget.setEnabled(false);
        return true;
    }

}
