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

package com.android.settings.display;


import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.TwoStatePreference;



import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import android.util.Log;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;
import android.net.Uri;
import android.os.Handler;
import android.content.pm.ResolveInfo;

import java.util.List;
import java.util.ArrayList;
import android.content.pm.PackageManager;
import android.content.Intent;
import android.content.IntentFilter;
import java.text.Collator;

import java.util.Collections;
import java.util.Comparator;
import com.android.internal.app.NightDisplayController;
/**
 * Settings screen for Reading mode.
 * Add this file by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
 */
public class ReadingModeSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener{
    private static final String KEY_READING_MODE_ALWAYS_ENABLE = "reading_mode_always_enable";
    private static final String KEY_READING_MODE_APPS_CATEGORY = "reading_mode_apps_category";
    private static final String KEY_READING_MODE_ADD_APPS = "reading_mode_add_apps";

    private SwitchPreference mAlwaysEnablePreference;
    private PreferenceCategory readingAppsCategory;
    private Preference readingmodeapps;

    List<AppListPreference> readingEnabledList = new ArrayList<AppListPreference>();
    private PackageManager mPackageManager;
    private Context mContext;
    private NightDisplayController mController;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getContext();
        mPackageManager = mContext.getPackageManager();
        mController = new NightDisplayController(mContext);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_night_display;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);
        // Load the preferences from xml.
        addPreferencesFromResource(R.xml.reading_mode_settings);
        mAlwaysEnablePreference = (SwitchPreference)findPreference(KEY_READING_MODE_ALWAYS_ENABLE);

        mAlwaysEnablePreference.setOnPreferenceChangeListener(this);

        readingAppsCategory  = (PreferenceCategory) findPreference(KEY_READING_MODE_APPS_CATEGORY);
        readingmodeapps = (Preference)findPreference(KEY_READING_MODE_ADD_APPS);
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        //if list had change, then invoke
        mDislpayContentObserver.register(mContext.getContentResolver());
        updateReadingModeAppsViews();
    }

    @Override
    public void onPause() {
        mDislpayContentObserver.unregister(getContentResolver());

        super.onPause();
    }
    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mAlwaysEnablePreference) {
            handleToggleAlwaysEnablePreferenceChange((Boolean) newValue);
            return true;
        }
        return false;
    }

    private final DisplayContentObserver mDislpayContentObserver =
            new DisplayContentObserver(new Handler()) {
                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    if((Settings.System.getUriFor("reading_mode_always_enable")).equals(uri)){
                        updateReadingModeAppsViews();
                    }
                }
            };

    private void handleToggleAlwaysEnablePreferenceChange(boolean enable){
        mAlwaysEnablePreference.setChecked(enable);
        Settings.System.putInt(mContext.getContentResolver(),"reading_mode_always_enable", enable ? 1:0);
        if(enable && mController.isActivated()){
            mController.setActivated(false);
            if(Settings.Secure.getInt(getContentResolver(),"night_theme_enabled", 0) == 1){
                //Begin modify by shilei.zhang for fixed XR7065062 on 2018/10/25
                //Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
                Settings.Secure.putInt(getContentResolver(), "night_theme_display_screen", 0);
                //End modify by shilei.zhang for fixed XR7065062 on 2018/10/25
            }
        }
    }

    private void updateReadingModeAppsViews(){
        List<ResolveInfo> items = getAllPackages();
        if(mController.isActivated()){
            Settings.System.putInt(getContext().getContentResolver(),"reading_mode_always_enable", 0) ;
        }
        boolean isAlwaysEnable =  Settings.System.getInt(getContext().getContentResolver(),"reading_mode_always_enable", 0) == 1;
        mAlwaysEnablePreference.setChecked(isAlwaysEnable);
        readingAppsCategory.removeAll();
        readingAppsCategory.addPreference(readingmodeapps);
        for (ResolveInfo info : items) {
            if(Settings.Secure.getInt(mContext.getContentResolver(), info.activityInfo.name, 0) == 1){
                AppListPreference item = new AppListPreference(mContext, info, false);
                readingAppsCategory.addPreference(item);
            }
        }
    }

    private List<ResolveInfo> getAllPackages() {
        List<ResolveInfo> items = new ArrayList<ResolveInfo>();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        items = mPackageManager.queryIntentActivities(mainIntent, 0);
        items.sort(mResolveInfoComparator);
        return items;
    }

    private Comparator<ResolveInfo> mResolveInfoComparator = new Comparator<ResolveInfo>() {

        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(ResolveInfo obj1, ResolveInfo obj2) {
            ResolveInfo r1 = (ResolveInfo) obj1;
            ResolveInfo r2 = (ResolveInfo) obj2;

            return sCollator.compare((r1.activityInfo.loadLabel(mPackageManager)).toString(),
                    (r2.activityInfo.loadLabel(mPackageManager)).toString());
        }
    };

    @Override
    public int getMetricsCategory() {
        return 0;
    }
}
