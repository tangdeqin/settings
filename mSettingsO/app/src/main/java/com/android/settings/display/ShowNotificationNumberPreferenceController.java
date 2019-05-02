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

package com.android.settings.display;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;


public class ShowNotificationNumberPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    private static final String KEY_SHOW_NOTIFICATION_NUMBER = "show_notification_number";
    private static final String SHOW_NOTIFICATION_NUMBER = "TCT_SHOW_NOTIFICATION_NUMBER";
    private static final int ON = 1;
    private static final int OFF = 0;

    private SettingObserver mSettingObserver;

    public ShowNotificationNumberPreferenceController(Context context,Lifecycle lifecycle) {
        super(context);
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Preference preference = screen.findPreference(KEY_SHOW_NOTIFICATION_NUMBER);
        if (preference != null) {
            mSettingObserver = new SettingObserver(preference);
        }
    }

    @Override
    public void onResume() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), true /* register */);
        }
    }

    @Override
    public void onPause() {
        if (mSettingObserver != null) {
            mSettingObserver.register(mContext.getContentResolver(), false /* register */);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_SHOW_NOTIFICATION_NUMBER;
    }

    @Override
    public boolean isAvailable() {
        //begin modify by wenli for P23942 on 20180917
        try {
            return mContext.getResources().getBoolean(R.bool.def_show_notification_folder_preference);
        } catch (Exception e) {
            Log.e("liwen","error to get def_show_notification_folder_preference");
            return false;
        }
        //end modify by wenli for P23942 on 20180917
    }

    @Override
    public void updateState(Preference preference) {
        //begin modify by wenli for P23398 on 20180904
        //default value ON -> OFF
        final boolean checked = Settings.Global.getInt(mContext.getContentResolver(),
                SHOW_NOTIFICATION_NUMBER, OFF) == ON;
        //end modify by wenli for P23398 on 20180904
        ((TwoStatePreference) preference).setChecked(checked);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean val = (Boolean) newValue;
        return Settings.Global.putInt(mContext.getContentResolver(),
                SHOW_NOTIFICATION_NUMBER, val ? ON : OFF);
    }

    class SettingObserver extends ContentObserver {

        private final Uri NOTIFICATION_BADGING_URI =
                Settings.Global.getUriFor(SHOW_NOTIFICATION_NUMBER);

        private final Preference mPreference;

        public SettingObserver(Preference preference) {
            super(new Handler());
            mPreference = preference;
        }

        public void register(ContentResolver cr, boolean register) {
            if (register) {
                cr.registerContentObserver(NOTIFICATION_BADGING_URI, false, this);
            } else {
                cr.unregisterContentObserver(this);
            }
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            if (NOTIFICATION_BADGING_URI.equals(uri)) {
                updateState(mPreference);
            }
        }
    }
}
