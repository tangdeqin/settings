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

package com.android.settings.applications.defaultapps;

import android.content.ComponentName;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.UserInfo;
import android.os.Build;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.List;

//begin add by zhixiong.liu.hz for task7026677 20180917
import android.app.ActivityManager;
import android.util.Log;
import android.widget.Toast;
import android.content.Intent;
//end add by zhixiong.liu.hz for task7026677 20180917


public class DefaultHomePicker extends DefaultAppPickerFragment {

    private String mPackageName;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mPackageName = context.getPackageName();
    }
    
    //Begin added by miaoliu for XRP10024320 on 2018/10/11
    @Override
    public void onResume() {
        super.onResume();
        updateCheckedState(getDefaultKey());

    }
    //End added by miaoliu for XRP10024320 on 2018/10/11
    
    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.DEFAULT_HOME_PICKER;
    }

    @Override
    protected List<DefaultAppInfo> getCandidates() {
        final boolean mustSupportManagedProfile = hasManagedProfile();
        final List<DefaultAppInfo> candidates = new ArrayList<>();
        final List<ResolveInfo> homeActivities = new ArrayList<>();
        mPm.getHomeActivities(homeActivities);

        for (ResolveInfo resolveInfo : homeActivities) {
            final ActivityInfo info = resolveInfo.activityInfo;
            final ComponentName activityName = new ComponentName(info.packageName, info.name);
            if (info.packageName.equals(mPackageName)) {
                continue;
            }

            final String summary;
            boolean enabled = true;
            if (mustSupportManagedProfile && !launcherHasManagedProfilesFeature(resolveInfo)) {
                summary = getContext().getString(R.string.home_work_profile_not_supported);
                enabled = false;
            } else {
                summary = null;
            }
            final DefaultAppInfo candidate =
                    new DefaultAppInfo(mPm, mUserId, activityName, summary, enabled);
            candidates.add(candidate);
        }
        return candidates;
    }

    @Override
    protected String getDefaultKey() {
        final ArrayList<ResolveInfo> homeActivities = new ArrayList<>();
        final ComponentName currentDefaultHome = mPm.getHomeActivities(homeActivities);
        if (currentDefaultHome != null) {
            return currentDefaultHome.flattenToString();
        }
        return null;
    }

    @Override
    protected boolean setDefaultKey(String key) {
        if (!TextUtils.isEmpty(key)) {
            final ComponentName component = ComponentName.unflattenFromString(key);
            final List<ResolveInfo> homeActivities = new ArrayList<>();
            mPm.getHomeActivities(homeActivities);
            final List<ComponentName> allComponents = new ArrayList<>();
                        
            //begin add by zhixiong.liu.hz for task7026677 20180917
            boolean newlauncher = false;
            ComponentName componentOld = null;
    	    if(getDefaultKey() != null) {
    		    componentOld = ComponentName.unflattenFromString(getDefaultKey());
    	    }

    	    if (!key.equals(getDefaultKey())){
                newlauncher = true;
            }
            //end add by zhixiong.liu.hz for  task7026677 20180917
            
            for (ResolveInfo info : homeActivities) {
                final ActivityInfo appInfo = info.activityInfo;
                ComponentName activityName = new ComponentName(appInfo.packageName, appInfo.name);
                allComponents.add(activityName);
            }
            mPm.replacePreferredActivity(
                    DefaultHomePreferenceController.HOME_FILTER,
                    IntentFilter.MATCH_CATEGORY_EMPTY,
                    allComponents.toArray(new ComponentName[0]),
                    component);
                    
            //begin add by zhixiong.liu.hz for task7026677 20180917
            if(newlauncher){
                Toast.makeText(getActivity(), getResources().getString(R.string.settings_license_activity_loading), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                getActivity().startActivity(intent);
            }

            if (componentOld != null && !(componentOld.getPackageName().equals(component.getPackageName()))){
                ActivityManager mAm = (ActivityManager)getActivity().getSystemService(Context.ACTIVITY_SERVICE);
                mAm.forceStopPackage(componentOld.getPackageName());
            }
            //end add by zhixiong.liu.hz for task7026677 20180917  
            return true;
        }
        return false;
    }

    private boolean hasManagedProfile() {
        final Context context = getContext();
        List<UserInfo> profiles = mUserManager.getProfiles(context.getUserId());
        for (UserInfo userInfo : profiles) {
            if (userInfo.isManagedProfile()) {
                return true;
            }
        }
        return false;
    }

    private boolean launcherHasManagedProfilesFeature(ResolveInfo resolveInfo) {
        try {
            ApplicationInfo appInfo = mPm.getPackageManager().getApplicationInfo(
                    resolveInfo.activityInfo.packageName, 0 /* default flags */);
            return versionNumberAtLeastL(appInfo.targetSdkVersion);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean versionNumberAtLeastL(int versionNumber) {
        return versionNumber >= Build.VERSION_CODES.LOLLIPOP;
    }
}
