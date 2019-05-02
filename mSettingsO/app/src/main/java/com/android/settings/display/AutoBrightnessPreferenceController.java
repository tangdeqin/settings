/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */
package com.android.settings.display;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.DisplaySettings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.search.DatabaseIndexingUtils;
import com.android.settings.search.InlineSwitchPayload;
import com.android.settings.search.ResultPayload;
import com.android.settings.R;
import com.android.settingslib.core.AbstractPreferenceController;

import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
import static android.provider.Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL;

//Begin added by zhibin.zhong for defect 5787238 on 2018/03/28
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.support.v7.preference.TwoStatePreference;
import android.database.ContentObserver;
import android.os.Looper;
//End added by zhibin.zhong for defect 5787238 on 2018/03/28
import com.android.settings.Utils;//Added by miaoliu for XR7139665 on 2018/11/21
public class AutoBrightnessPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin, Preference.OnPreferenceChangeListener ,
    //Begin added by zhibin.zhong for defect 5787238 on 2018/03/28
        LifecycleObserver,OnResume, OnPause {
     private TwoStatePreference mPreference;
    //End added by zhibin.zhong for defect 5787238 on 2018/03/28
    private final String mAutoBrightnessKey;

    private final String SYSTEM_KEY = SCREEN_BRIGHTNESS_MODE;
    private final int DEFAULT_VALUE = SCREEN_BRIGHTNESS_MODE_MANUAL;

      //Begin modified by zhibin.zhong for defect 5787238 on 2018/03/28
    public AutoBrightnessPreferenceController(Context context, String key,Lifecycle lifecycle) {
        super(context);
        mAutoBrightnessKey = key;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        //End added by zhibin.zhong for defect 5787238 on 2018/03/28
    }

    @Override
    public boolean isAvailable() {
        //Begin modified by miaoliu for XR7139665 on 2018/11/21
        //Begin modify by shilei.zhang for un-use frameworks resource and fixed XR6873798 on 2018/08/24
       // int isAvailableId = mContext.getResources().getIdentifier("config_automatic_brightness_available","bool","android");
        //return mContext.getResources().getBoolean(isAvailableId);
        boolean isSupported = Utils.getBoolean(mContext, "config_automatic_brightness_available","com.android.systemui",true);
        return isSupported;
        /*return mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_automatic_brightness_available);*/
        //End modify by shilei.zhang for un-use frameworks resource and fixed XR6873798 on 2018/08/24
        //End modified by miaoliu for XR7139665 on 2018/11/21
    }

    @Override
    public String getPreferenceKey() {
        return mAutoBrightnessKey;
    }

    @Override
    public void updateState(Preference preference) {
        //Begin added by zhibin.zhong for defect 5787238 on 2018/03/28
        mPreference = (TwoStatePreference) preference;
        //End added by zhibin.zhong for defect 5787238 on 2018/03/28
        int brightnessMode = Settings.System.getInt(mContext.getContentResolver(),
                SYSTEM_KEY, DEFAULT_VALUE);
        ((SwitchPreference) preference).setChecked(brightnessMode != DEFAULT_VALUE);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        boolean auto = (Boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), SYSTEM_KEY,
                auto ? SCREEN_BRIGHTNESS_MODE_AUTOMATIC : DEFAULT_VALUE);
        return true;
    }

    @Override
    public ResultPayload getResultPayload() {
        final Intent intent = DatabaseIndexingUtils.buildSubsettingIntent(mContext,
                DisplaySettings.class.getName(), mAutoBrightnessKey,
                mContext.getString(R.string.display_settings));

        return new InlineSwitchPayload(SYSTEM_KEY,
                ResultPayload.SettingsSource.SYSTEM, SCREEN_BRIGHTNESS_MODE_AUTOMATIC, intent,
                isAvailable(), DEFAULT_VALUE);
    }
    //Begin added by zhibin.zhong for defect 5787238 on 2018/03/28
    @Override
    public void onResume() {
       mContext.getContentResolver().registerContentObserver(
           Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS_MODE), false,
           mBrightnessEnableObserver, UserHandle.USER_ALL);
    }

    @Override
    public void onPause() {
        if (mBrightnessEnableObserver != null) {
            mContext.getContentResolver().unregisterContentObserver(mBrightnessEnableObserver);
        }
    }

    private final ContentObserver mBrightnessEnableObserver = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override
          public void onChange(boolean selfChange) {
            if (mPreference != null) {
                updateState( mPreference);
            }
        }
   };
    //End added by zhibin.zhong for defect 5787238 on 2018/03/28
}