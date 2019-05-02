/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

/**
 * created by xdayi for XR 5557067 on 2018/10/10
 */
public class InformedConsentPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String TAG = "InformedConsentPref";
    private static final String KEY_INFORMED_CONSENT = "informed_consent";
    private Preference mPreference;

    public InformedConsentPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_INFORMED_CONSENT);
    }

    @Override
    public boolean isAvailable() {
        if (isDiagnosticInstalled()) {
            return true;
        }
        return false;
    }

    private boolean isDiagnosticInstalled() {
        PackageManager pm = mContext.getPackageManager();
        if (null == pm) {
            return false;
        }
        boolean installed;
        try {
            pm.getApplicationIcon("com.tct.diagnostics");
            installed = true;
        } catch (PackageManager.NameNotFoundException e) {
            installed = false;
        }
        return installed;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        if (!TextUtils.equals(preference.getKey(), KEY_INFORMED_CONSENT)) {
            return;
        }

        //Begin modify by xdayi for XR5756673 on 2017/12/08
        //modify default summary for diagnostic
        int diagnostic_on = Settings.Global.getInt(mContext.getContentResolver(), "def.diagnostic.on", -1);
        //End modify by xdayi for XR5756673 on 2017/12/08
        if (1 == diagnostic_on) {
            preference.setSummary(R.string.switch_on_text);
        } else {
            preference.setSummary(R.string.switch_off_text);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_INFORMED_CONSENT;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_INFORMED_CONSENT)) {
            return false;
        }

        final Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setAction("com.tct.diagnostic.INFORMEDCONSENT");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            mContext.startActivity(intent);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Unable to start activity " + intent.toString());
        }
        return false;
    }
}
