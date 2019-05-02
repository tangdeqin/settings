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

package com.android.settings;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.provider.SearchIndexableResource;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.deviceinfo.AdditionalSystemUpdatePreferenceController;
import com.android.settings.deviceinfo.BasebandVersionPreferenceController;
import com.android.settings.deviceinfo.BuildNumberPreferenceController;
import com.android.settings.deviceinfo.DeviceModelPreferenceController;
import com.android.settings.deviceinfo.FccEquipmentIdPreferenceController;
import com.android.settings.deviceinfo.FeedbackPreferenceController;
import com.android.settings.deviceinfo.FirmwareVersionPreferenceController;
// Add by xdayi for diagnostics XR7046105 on 2008/10/10
import com.android.settings.deviceinfo.InformedConsentPreferenceController;
// End by xdayi for diagnostics XR7046105 on 2008/10/10
import com.android.settings.deviceinfo.KernelVersionPreferenceController;
import com.android.settings.deviceinfo.ManualPreferenceController;
import com.android.settings.deviceinfo.RegulatoryInfoPreferenceController;
import com.android.settings.deviceinfo.SELinuxStatusPreferenceController;
import com.android.settings.deviceinfo.SafetyInfoPreferenceController;
import com.android.settings.deviceinfo.SecurityPatchPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//begin  zhixiong.liu.hz for XR 6107743 2018/3/14
import android.os.Bundle;
import android.os.SystemProperties;
import com.android.settings.deviceinfo.UMPreferenceController;
// end  zhixiong.liu.hz for XR 6107743 2018/3/14
//Begin added by miaoliu for XR6172796 on 2018/4/8
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
//End added by miaoliu for XR6172796 on 2018/4/8
//Begin added by miaoliu for Settings ergo 2.0.1 on 2018/7/23
import com.android.settings.deviceinfo.SystemUpdatePreferenceController;
import android.os.UserManager;
//End added by miaoliu for Settings ergo 2.0.1 on 2018/7/23

//begin  zhixiong.liu.hz for 6696757 20180801
import com.tct.deviceinfo.RomVersionController;
//end  zhixiong.liu.hz for 6696757 20180801

public class DeviceInfoSettings extends DashboardFragment implements Indexable {

    private static final String LOG_TAG = "DeviceInfoSettings";

    private static final String KEY_LEGAL_CONTAINER = "legal_container";
    //begin zhixiong.liu.hz for XR 6272377 2018/4/29
    //begin zhixiong.liu.hz remove useless code defect 7169297 20181130
    //private static final String KEY_CUSTOM_BUILD_VERSION = "custom_build_version";
    //end zhixiong.liu.hz remove useless code defect 7169297 20181130
    private static final String PROPERTY_CUSTOM_BUILD_VERSION = "ro.mediatek.version.release";
    private static final String KEY_BUILD_NUMBER = "build_number";    
    private static final String KEY_TYPE_CODE = "type_code";
    private static final String KEY_BASEBAND_VERSION = "baseband_version";
    //end zhixiong.liu.hz for defect 6272377 2018/4/29

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        //begin zhixiong.liu.hz for XR 6107743 2018/3/14        
        //begin zhixiong.liu.hz remove useless code defect 7169297 20181130
        //if (!getResources().getBoolean(R.bool.def_settings_custom_build_version_enable)) {
        //    removePreference(KEY_CUSTOM_BUILD_VERSION);
        //} else {
        //    setValueSummary(KEY_CUSTOM_BUILD_VERSION, PROPERTY_CUSTOM_BUILD_VERSION);
        //}      
        //end zhixiong.liu.hz remove useless code defect 7169297 20181130
        if (!getResources().getBoolean(R.bool.def_Settings_buildnum_display_enable)) {
             removePreference(KEY_BUILD_NUMBER);
        }

        if (!getResources().getBoolean(R.bool.def_Settings_typecode_enable)) {
             removePreference(KEY_TYPE_CODE);
        }else{
             findPreference(KEY_TYPE_CODE).setSummary(getResources().getString(R.string.def_Settings_typecode));
        }
        //end zhixiong.liu.hz for XR 6107743 2018/3/14
        
        //Begin added by miaoliu for XR6172796 on 2018/4/8
        if(!checkPackageExist(getActivity(), "com.tct.gdpr")){
              removePreference("policy");
        }
        //End added by miaoliu for XR6172796 on 2018/4/8
         //begin zhixiong.liu.hz for defect 6272377 2018/4/29
        if(!getResources().getBoolean(R.bool.def_settings_baseband_show)){
            removePreference(KEY_BASEBAND_VERSION);
        }
        //end zhixiong.liu.hz for defect 6272377 2018/4/29        

    }

    //Begin added by miaoliu for XR6172796 on 2018/4/8
    private  boolean checkPackageExist(Context context, String pkg) {
        final PackageManager packageManager = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    //End added by miaoliu for XR6172796 on 2018/4/8

    //begin zhixiong.liu.hz for XR 6107743 2018/3/14
    private void setValueSummary(String preference, String property) {
        try {
            findPreference(preference).setSummary(SystemProperties.get(property, getString(R.string.device_info_default)));
        } catch (RuntimeException e) {
            // No recovery
        }
    }
    //end zhixiong.liu.hz for XR 6107743 2018/3/14

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_uri_about;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        final BuildNumberPreferenceController buildNumberPreferenceController =
                getPreferenceController(BuildNumberPreferenceController.class);
        if (buildNumberPreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected String getLogTag() {
        return LOG_TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.device_info_settings;
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getActivity(), this /* fragment */,
                getLifecycle());
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final SummaryLoader mSummaryLoader;
        //begin zhixiong.liu.hz for XR 6107743 2018/3/14
        private final Context mContext; 
        
        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }
        //end zhixiong.liu.hz for XR 6107743 2018/3/14
        @Override
        public void setListening(boolean listening) {
            if (listening) {
                //begin zhixiong.liu.hz for XR 6107743 2018/3/14
                if (!mContext.getResources().getBoolean(R.bool.def_Settings_typecode_enable)) {
                    mSummaryLoader.setSummary(this, DeviceModelPreferenceController.getDeviceModel());
                }
                //begin zhixiong.liu.hz for defect 7442064 20190213
                else if(mContext.getResources().getBoolean(R.bool.def_settings_typecode_system_summary_show)){
                    mSummaryLoader.setSummary(this,mContext.getResources().getString(R.string.def_Settings_typecode));
                }
                //end zhixiong.liu.hz for defect 7442064 20190213
                //end zhixiong.liu.hz for XR 6107743 2018/3/14
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
             //begin zhixiong.liu.hz for XR 6107743 2018/3/14
             return new SummaryProvider(activity,summaryLoader);
             //return new SummaryProvider(summaryLoader);
             //end zhixiong.liu.hz for XR 6107743 2018/3/14
        }
    };

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Activity activity, Fragment fragment, Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(
                new BuildNumberPreferenceController(context, activity, fragment, lifecycle));
        controllers.add(new AdditionalSystemUpdatePreferenceController(context));
        controllers.add(new ManualPreferenceController(context));
        controllers.add(new FeedbackPreferenceController(fragment, context));
        controllers.add(new KernelVersionPreferenceController(context));
        controllers.add(new BasebandVersionPreferenceController(context));
        controllers.add(new FirmwareVersionPreferenceController(context, lifecycle));
        //Begin add by xdayi for XR5557067 7046105 on 2018/10/10
        controllers.add(new InformedConsentPreferenceController(context));
        //End add by xdayi for XR5557067 7046105 on 2018/10/10
        controllers.add(new RegulatoryInfoPreferenceController(context));
        controllers.add(new DeviceModelPreferenceController(context, fragment));
        controllers.add(new SecurityPatchPreferenceController(context));
        controllers.add(new FccEquipmentIdPreferenceController(context));
        controllers.add(new SELinuxStatusPreferenceController(context));
        controllers.add(new SafetyInfoPreferenceController(context));
        //begin zhixiong.liu.hz for XR 6107743 2018/3/14
        controllers.add(new UMPreferenceController(context));
        //end zhixiong.liu.hz for XR 6107743 2018/3/14
        //controllers.add(new SystemUpdatePreferenceController(context, UserManager.get(context)));//Deleted by miaoliu for XR7134981 on 2018/12/04
        //begin  zhixiong.liu.hz for 6696757 20180801
        controllers.add(new RomVersionController(context));
        //end  zhixiong.liu.hz for 6696757 20180801
        return controllers;
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {

                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.device_info_settings;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null /*activity */,
                            null /* fragment */, null /* lifecycle */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    keys.add(KEY_LEGAL_CONTAINER);

                    //begin zhixiong.liu.hz for XR 6107743 2018/3/14
                    if (!context.getResources().getBoolean(R.bool.def_Settings_buildnum_display_enable)) {
                        keys.add(KEY_BUILD_NUMBER);
                    }
                    //begin zhixiong.liu.hz remove useless code defect 7169297 20181130
                    //if (!context.getResources().getBoolean(R.bool.def_settings_custom_build_version_enable)) {
                    //    keys.add(KEY_CUSTOM_BUILD_VERSION);
                    //}
                    //end zhixiong.liu.hz remove useless code defect 7169297 20181130
                    if (!context.getResources().getBoolean(R.bool.def_Settings_typecode_enable)) {
                         keys.add(KEY_TYPE_CODE);
                    }
                    //end zhixiong.liu.hz for XR 6107743 2018/3/14
                    return keys;
                }
            };
}
