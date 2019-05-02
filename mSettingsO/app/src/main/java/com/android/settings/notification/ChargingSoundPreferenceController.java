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

import static com.android.settings.notification.SettingPref.TYPE_GLOBAL;

import android.content.Context;

import android.provider.Settings.Global;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R;

public class ChargingSoundPreferenceController extends SettingPrefController {

    private static final String KEY_CHARGING_SOUNDS = "charging_sounds";

    public ChargingSoundPreferenceController(Context context, SettingsPreferenceFragment parent,
            Lifecycle lifecycle) {
        super(context, parent, lifecycle);
        //Begin modified by ronghui.yi for Task 6855426 on 2018/08/21
        mPreference = new SettingPref(
            TYPE_GLOBAL, KEY_CHARGING_SOUNDS, Global.CHARGING_SOUNDS_ENABLED, DEFAULT_ON) {
            @Override
            public boolean isApplicable(Context context) {
                return context.getResources().getBoolean(R.bool.has_charge_sounds);
            }
        };
        //End modified by ronghui.yi for Task 6855426 on 2018/08/21
    }

}
