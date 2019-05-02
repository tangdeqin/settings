/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Fragment;
import android.content.Context;
//import android.database.ContentObserver;
//import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.Handler;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;
import com.android.settings.widget.MasterSwitchController;
import com.android.settings.widget.MasterSwitchPreference;
import com.android.settings.widget.SummaryUpdater;
import com.android.settings.widget.SummaryUpdater.OnSummaryChangeListener;

import java.util.List;

public class NfcMasterSwitchPreferenceController 
        extends AbstractPreferenceController
        implements PreferenceControllerMixin, 
                   LifecycleObserver, 
                   OnSummaryChangeListener, 
                   OnResume, 
                   OnPause {

    static class NfcSummaryUpdater 
            extends SummaryUpdater 
            implements NfcEnabler.INfcStateChangedCallback {

        private int mState;
        private NfcAdapter mNfcAdapter;

        public NfcSummaryUpdater(Context context, OnSummaryChangeListener listener, NfcAdapter adapter/*, NfcEnabler.INfcStateChangedCallback cb*/) {
            super(context, listener);
            mNfcAdapter = adapter;
        }

        @Override
        public void onStateOff() {
            notify(NfcAdapter.STATE_OFF);
        }

        @Override
        public void onStateOn() {
            notify(NfcAdapter.STATE_ON);
        }

        @Override
        public void onStateTurningOff() {
            notify(NfcAdapter.STATE_TURNING_OFF);
        }

        @Override
        public void onStateTurningOn() {
            notify(NfcAdapter.STATE_TURNING_ON);
        }

        @Override
        public void onSwitchChanged(boolean on) {
        }

        private void notify(int state) {
            mState = state;
            notifyChangeIfNeeded();
        }

        @Override
        public void register(boolean listening) {
            Log.d(TAG, "listening:" + listening);
            if (mNfcAdapter == null) {
                return;
            }
            if (listening) {
                notifyChangeIfNeeded();
            }
        }

        //Begin modified by jianhao.zeng for defect:5945378 on 2018/02/01
        @Override
        public String getSummary() {
            if (null == mNfcAdapter || !mNfcAdapter.isEnabled()) {
                return mContext.getString(R.string.switch_off_text); // inherit wifi string is OK.
            }
            switch (mState) {
            case NfcAdapter.STATE_TURNING_OFF:
            case NfcAdapter.STATE_OFF:
            default:
                return mContext.getString(R.string.switch_off_text);
            case NfcAdapter.STATE_ON:
                return mContext.getString(R.string.switch_on_text);
            case NfcAdapter.STATE_TURNING_ON:
                return mContext.getString(R.string.switch_on_text);
            }
        }
        //End modified by jianhao.zeng for defect:5945378 on 2018/02/01
    } 

    private static final String TAG = "NfcMasterSwitch";
    public  static final String KEY_TOGGLE_NFC = "toggle_nfc_master";

    public  NfcEnabler mNfcEnabler;
    private NfcSummaryUpdater mSummaryUpdater;
    private NfcAdapter mNfcAdapter;
    private final Fragment mFragment;
    private final SettingsActivity mActivity;
    private MasterSwitchPreference mSwitch;
    private NfcMasterSwitcher mMasterSwitcher;

    public NfcMasterSwitchPreferenceController(Context context, Fragment fragment, SettingsActivity activity) {
        super(context);
        Log.d(TAG, "constructor.");
        mNfcEnabler = new NfcEnabler(mContext);
        mNfcAdapter = mNfcEnabler.Nfc;
        mFragment = fragment;
        mActivity = activity;
    }

    private MasterSwitchPreference createMaster(PreferenceScreen screen) {
        MasterSwitchPreference master = new MasterSwitchPreference(mContext);
        master.setKey(getPreferenceKey());
        master.setIcon(R.drawable.ic_nfc);
        master.setOrder(-28);
        master.setTitle(R.string.nfc_quick_toggle_title);
        master.setFragment(NfcSettings.class.getName());
        screen.addPreference(master);
        return master;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Log.d(TAG, "displayPreference");
        mNfcAdapter = mNfcEnabler.Nfc;
        mSwitch = (MasterSwitchPreference) screen.findPreference(KEY_TOGGLE_NFC);
        if (!isAvailable()) {
            Log.d(TAG, "NFC is not available.");
            if (null != mSwitch) removePreference(screen, KEY_TOGGLE_NFC);
            mNfcEnabler = null;
            return;
        }

        if (null == mSwitch) {
            Log.d(TAG, "not exist!");
            mSwitch = createMaster(screen);
        }

        mSummaryUpdater = new NfcSummaryUpdater(mContext, this, mNfcAdapter);
        mMasterSwitcher = new NfcMasterSwitcher(mContext, new MasterSwitchController(mSwitch));
    }

    @Override
    public void updateNonIndexableKeys(List<String> keys) {
        final NfcManager manager = (NfcManager) mContext.getSystemService(Context.NFC_SERVICE);
        if (manager != null) {
            NfcAdapter adapter = manager.getDefaultAdapter();
            if (adapter == null) {
                keys.add(KEY_TOGGLE_NFC);
            }
        }
    }

    @Override
    public boolean isAvailable() {
        return mNfcAdapter != null;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_TOGGLE_NFC;
    }

    public void onResume() {
        if (mNfcEnabler != null) {
            mSummaryUpdater.register(true);
            mNfcEnabler.register(mSummaryUpdater);
            mNfcEnabler.register(mMasterSwitcher);
            mNfcEnabler.resume(mContext);
            mMasterSwitcher.resume(mContext);
        }
    }

    @Override
    public void onPause() {
        if (mNfcEnabler != null) {
            mSummaryUpdater.register(false);
            mNfcEnabler.pause();
            mNfcEnabler.clear();
            mMasterSwitcher.pause();
        }
    }

    /*@Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_TOGGLE_NFC.equals(preference.getKey())) {
            mActivity.startPreferencePanelAsUser(mFragment, NfcSettings.class.getName(), null,
                    R.string.nfc_quick_toggle_summary, null, new UserHandle(UserHandle.myUserId()));
            Log.d(TAG, "binjian:KEY_TOGGLE_NFC click");
            return true;
        }
        return super.handlePreferenceTreeClick(preference);
    }*/


    @Override
    public void onSummaryChanged(String summary) {
        if (mSwitch != null) {
            mSwitch.setSummary(summary);
        }
    }

}
