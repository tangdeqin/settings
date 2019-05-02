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
import android.support.v7.preference.Preference;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.DeviceInfoUtils;
import com.android.settingslib.core.AbstractPreferenceController;
//begin zhixiong.liu.hz for XR 6107743 2018/3/14
import com.android.settings.R;
//end zhixiong.liu.hz for XR 6107743 2018/3/14
public class KernelVersionPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_KERNEL_VERSION = "kernel_version";

    public KernelVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        //begin zhixiong.liu.hz for XR 6107743 2018/3/1
        //preference.setSummary(DeviceInfoUtils.getFormattedKernelVersion());
        preference.setSummary(mContext.getResources().getString(R.string.def_kernel_about_phone));
        //end zhixiong.liu.hz for XR 6107743 2018/3/1
    }

    @Override
    public String getPreferenceKey() {
        return KEY_KERNEL_VERSION;
    }
}
