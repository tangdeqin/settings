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

import com.android.internal.app.NightDisplayController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
//Begin add by shilei.zhang for fixed XRP10025304,P10025354 on 2018/11/05
import com.android.settings.Utils;
import android.util.Log;
//End add by shilei.zhang for fixed XRP10025304,P10025354 on 2018/11/05
public class NightDisplayPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin {

    private static final String KEY_NIGHT_DISPLAY = "night_display";

    public NightDisplayPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        //Begin modify by shilei.zhang for fixed XRP10025304,P10025354 on 2018/11/05
        return NightDisplayController.isAvailable(mContext) && !Utils.isDemoUser(mContext);
        //End modify by shilei.zhang for fixed XRP10025304,P10025354 on 2018/11/05
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NIGHT_DISPLAY;
    }
}
