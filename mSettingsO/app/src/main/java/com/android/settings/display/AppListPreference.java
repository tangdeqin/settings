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


import android.content.Context;

import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;

import com.android.settingslib.RestrictedSwitchPreference;

import java.text.Collator;
import java.util.List;
import android.content.pm.ResolveInfo;
import android.provider.Settings;
import android.util.Log;
/**
 * Reading mode app list preference.
 *
 * This preference represents an app.
 * Add this file by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
 */
public class AppListPreference extends RestrictedSwitchPreference implements OnPreferenceClickListener,
        OnPreferenceChangeListener {
    private static final String TAG = "ReadingModeAppListPreference";
    private static final String EMPTY_TEXT = "";
    private static final int NO_WIDGET = 0;
    private final ResolveInfo mInfo;
    private boolean isEnabler = false; 
    public Context mContext;
    public AppListPreference(final Context context, final ResolveInfo info,
            final boolean isNeedSwitch) {
        super(context);
        setPersistent(false);
        mInfo = info;
        mContext = context;
        if (!isNeedSwitch) {
            // Remove switch widget.
            setWidgetLayoutResource(NO_WIDGET);
        }
        // Disable on/off switch texts.
        setSwitchTextOn(EMPTY_TEXT);
        setSwitchTextOff(EMPTY_TEXT);
        setKey(info.activityInfo.packageName);
        setTitle(info.loadLabel(context.getPackageManager()));
        setIcon(info.loadIcon(context.getPackageManager()));

        if (isNeedSwitch) {
            setOnPreferenceChangeListener(this);
        }
    }

    public boolean getIsEnabler(){
        return isEnabler;
    }

    public void setIsEnabler(boolean isEnable){
        isEnabler = isEnable;
    }

    public ResolveInfo getResolveInfo() {
        return mInfo;
    }

    private boolean isNeedSwitch() {
        return getWidgetLayoutResource() != NO_WIDGET;
    }

    @Override
    public boolean onPreferenceChange(final Preference preference, final Object newValue) {
        if (!isNeedSwitch()) {
            return false;
        }
        if (isChecked()) {
            setCheckedInternal(false);
            return false;
        }
        setCheckedInternal(true);
        return false;
    }
    private void setCheckedInternal(boolean checked) {
        super.setChecked(checked);
        setIsEnabler(checked);
        Settings.Secure.putInt(mContext.getContentResolver(), mInfo.activityInfo.name, checked ? 1:0);
        Log.d("SHILEI_READING","setCheckedInternal  checked "+checked+", mInfo.activityInfo.name is "+mInfo.activityInfo.name);
        notifyChanged();
    }
    @Override
    public boolean onPreferenceClick(final Preference preference) {

        return true;
    }

    public void updatePreferenceViews(){
        boolean enable = (Settings.Secure.getInt(mContext.getContentResolver(), mInfo.activityInfo.name, 0) == 1);

        Log.d("SHILEI_READING","updatePreferenceViews enable "+enable+", mInfo.activityInfo.name is "+mInfo.activityInfo.name);
        setChecked(enable);
    }
}
