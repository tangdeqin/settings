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
import android.nfc.NfcAdapter;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedPreference;

import android.os.SystemProperties; 

/**
 * NfcEnabler is a helper to manage the Nfc on/off checkbox preference. It is
 * turns on/off Nfc and ensures the summary of the preference reflects the
 * current state.
 */
public class NfcSwitcher 
        implements NfcEnabler.INfcStateChangedCallback, 
                   Preference.OnPreferenceChangeListener {

    private final boolean mBeamDisallowedBySystem;
    private final NfcEnabler mNfcEnabler;
    private final SwitchPreference mSwitch;
    private final RestrictedPreference mAndroidBeam;

    public final NfcAdapter Nfc;

    public NfcSwitcher(Context context, SwitchPreference switchPreference, RestrictedPreference androidBeam) {
        mNfcEnabler = new NfcEnabler(context);
        Nfc = mNfcEnabler.Nfc;
        mSwitch = switchPreference;

        mAndroidBeam = androidBeam;
        mBeamDisallowedBySystem = RestrictedLockUtils.hasBaseUserRestriction(context,
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        if (mNfcEnabler.Nfc == null) {
            // NFC is not supported
            mSwitch.setEnabled(false);
            return;
        }

        if (mBeamDisallowedBySystem) {
            mAndroidBeam.setEnabled(false);
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (!mNfcEnabler.onPreferenceChange(preference, value)) {
            SwitchPreference sw = (SwitchPreference)preference;
            if (null != sw) sw.setChecked(false);
        }
        return true;
    }

    @Override
    public void onStateOff() {
        mSwitch.setChecked(false);
        mSwitch.setEnabled(true);
        mAndroidBeam.setEnabled(false);
        mAndroidBeam.setSummary(R.string.android_beam_disabled_summary);
    }

    @Override
    public void onStateOn() {
        mSwitch.setChecked(true);
        mSwitch.setEnabled(true);
        if (mBeamDisallowedBySystem) {
            mAndroidBeam.setDisabledByAdmin(null);
            mAndroidBeam.setEnabled(false);
        } else {
            mAndroidBeam.checkRestrictionAndSetDisabled(UserManager.DISALLOW_OUTGOING_BEAM);
        }
        if (mNfcEnabler.Nfc.isNdefPushEnabled() && mAndroidBeam.isEnabled()) {
            mAndroidBeam.setSummary(R.string.android_beam_on_summary);
        } else {
            mAndroidBeam.setSummary(R.string.android_beam_off_summary);
        }        
    }

    @Override
    public void onStateTurningOn() {
        mSwitch.setChecked(true);
        mSwitch.setEnabled(false);
        mAndroidBeam.setEnabled(false);
    }

    @Override
    public void onStateTurningOff() {
        mSwitch.setChecked(false);
        mSwitch.setEnabled(false);
        mAndroidBeam.setEnabled(false);
    }

    @Override
    public void onSwitchChanged(boolean on) {
        mSwitch.setChecked(on);
        mSwitch.setEnabled(false);
    }

    public void resume() {
        mNfcEnabler.register(this);
        mNfcEnabler.resume(null);
        mSwitch.setOnPreferenceChangeListener(this);
    }

    public void pause() {
        mNfcEnabler.pause();
        mNfcEnabler.clear();
        mSwitch.setOnPreferenceChangeListener(null);
    }
}
