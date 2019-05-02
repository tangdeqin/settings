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

package com.android.settings.notification;



import android.content.Context;


import android.os.SystemProperties;//Added by ronghui.yi for XR P23825 on 2018/10/10
import android.os.Vibrator;
import android.support.v7.preference.PreferenceScreen;


import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.R;//Added by ronghui.yi for XR P23825 on 2018/09/19


public class VibrationSettingsPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_VIBRATION_SETTINGS = "vibration_duration";
    private Vibrator mVibrator;
    private Context context;

    public VibrationSettingsPreferenceController(Context context) {
        super(context);
        mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

    }

    @java.lang.Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if(!isAvailable()){
            //Preference preference = screen.findPreference(KEY_VIBRATION_SETTINGS);
            removePreference(screen,KEY_VIBRATION_SETTINGS);
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_VIBRATION_SETTINGS;
    }

    @Override
    public boolean isAvailable() {
        return SystemProperties.getBoolean("def_settings_vibration_duration_switch",true);//Modified by ronghui.yi for XR P23825 on 2018/10/10
    }
}
