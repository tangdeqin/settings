/*
 * Copyright (C) 2008 The Android Open Source Project
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
 /*
  * add by yeqing.lv for task3682179 at 2106-12-19
  */
package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.util.Log;

import com.android.settings.datetime.AutoTimePreferenceController;
import com.android.settings.datetime.AutoTimeZonePreferenceController;
import com.android.settings.datetime.DatePreferenceController;
import com.android.settings.datetime.TimeChangeListenerMixin;
import com.android.settings.datetime.TimeFormatPreferenceController;
import com.android.settings.datetime.TimePreferenceController;
import com.android.settings.datetime.TimeZonePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.datetime.ZoneGetter;
import com.android.settings.datetime.NTPPreferenceController;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.android.settingslib.core.AbstractPreferenceController;

import android.location.LocationManager;

public class SetupWizardDateTimeSettings extends DateTimeSettings {
 
    private static final String TAG = "DateTimeSettings";

    private LocationManager mLocationManager = null;

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Activity activity = getActivity();
        final Intent intent = activity.getIntent();
        final boolean isFromSUW = true;

        final AutoTimeZonePreferenceController autoTimeZonePreferenceController =
                new AutoTimeZonePreferenceController(
                        activity, this /* UpdateTimeAndDateCallback */, false);
        final AutoTimePreferenceController autoTimePreferenceController =
                new AutoTimePreferenceController(
                        activity, this /* UpdateTimeAndDateCallback */);

        controllers.add(autoTimeZonePreferenceController);
        controllers.add(autoTimePreferenceController);
        
        controllers.add(new TimeFormatPreferenceController(
                activity, this /* UpdateTimeAndDateCallback */, false));
        controllers.add(new TimeZonePreferenceController(
                activity, autoTimeZonePreferenceController));
        controllers.add(new TimePreferenceController(
                activity, this /* UpdateTimeAndDateCallback */, autoTimePreferenceController));
        //add by yeqing.lv for XR-P24111 on 2018-9-20 begin
        controllers.add(new DatePreferenceController(
                activity, this /* UpdateTimeAndDateCallback */, autoTimePreferenceController));
        //add by yeqing.lv for XR-P24111 on 2018-9-20 end
        controllers.add(new NTPPreferenceController(activity,this));
        return controllers;
    }
    @Override
    public void onAttach(Context context) {
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        super.onAttach(context);
        getLifecycle().addObserver(new TimeChangeListenerMixin(context, this));
    }
 
}
