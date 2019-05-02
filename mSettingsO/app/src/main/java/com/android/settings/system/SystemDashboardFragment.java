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
package com.android.settings.system;

import android.content.Context;
import android.os.UserManager;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.backup.BackupSettingsActivityPreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.deviceinfo.AdditionalSystemUpdatePreferenceController;
import com.android.settings.deviceinfo.SystemUpdatePreferenceController;
import com.android.settings.gestures.GesturesSettingPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
//begin zhixiong.liu.hz for e-label may not exist  2017.12.4
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.support.v7.preference.Preference;
//end zhixiong.liu.hz for e-label may not exist  2017.12.4
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SystemDashboardFragment extends DashboardFragment {

    private static final String TAG = "SystemDashboardFrag";

    private static final String KEY_RESET = "reset_dashboard";

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_SYSTEM_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.system_dashboard_fragment;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_system_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context);
    }


    //begin zhixiong.liu.hz for XR 5707358 e-label may not exist  2017/12/4
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
 
        boolean needRemoveElable = false;
        ApplicationInfo info = null;
        String packageName = "com.jrdcom.Elabel";

        try {
           info = getPackageManager().getApplicationInfo(packageName, 0);
           if(info == null){
               needRemoveElable = true;
           }
        } catch (Exception e) {
           needRemoveElable = true;
        }

        if(needRemoveElable == true){
            Preference pref = findPreference("regulatory_safety_info");
            if(pref != null){
                 getPreferenceScreen().removePreference(pref);
            }
        }

    }
    //end zhixiong.liu.hz for  XR 5707358 e-label may not exist  2017/12/4

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new SystemUpdatePreferenceController(context, UserManager.get(context)));//Added by miaoliu for XR7134981 on 2018/12/04
        controllers.add(new AdditionalSystemUpdatePreferenceController(context));
        controllers.add(new BackupSettingsActivityPreferenceController(context));
       // controllers.add(new GesturesSettingPreferenceController(context));//Deleted by miaoliu for XR5429846 on 2017/10/25 
        return controllers;
    }

    /**
     * For Search.
     */
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.system_dashboard_fragment;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add((new BackupSettingsActivityPreferenceController(context)
                            .getPreferenceKey()));
                    keys.add(KEY_RESET);
                    return keys;
                }
            };
}
