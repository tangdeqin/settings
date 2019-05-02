/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.SystemProperties;
import com.android.settings.applications.PackageManagerWrapper;
import com.android.settings.applications.PackageManagerWrapperImpl;
import android.content.pm.ApplicationInfo;

public class SetDefaultBrowserReceiver extends BroadcastReceiver {
    private static final String TAG = "Settings";

    @Override
    public void onReceive(Context context, Intent broadcast) {
        final PackageManager pm  = context.getPackageManager();
        boolean isFirstSetDefaultBrowser = Boolean.parseBoolean(SystemProperties.get("sys.default.browser", "true"));
        if(isFirstSetDefaultBrowser){
            String packageName = (context.getResources().getString(R.string.def_set_default_browser)).trim();
            if("".equals(packageName) || packageName.length() == 0){

            }else if(checkPackageExist(pm,packageName)){
                setDefaultBrower(pm,packageName);
            }
            SystemProperties.set("sys.default.browser", "false");
        }

    }
    private void setDefaultBrower(PackageManager pm,String packageName){
        PackageManagerWrapper mPm = new PackageManagerWrapperImpl(pm);
        mPm.setDefaultBrowserPackageNameAsUser(packageName, UserHandle.myUserId());
    }
    private  boolean checkPackageExist(PackageManager packageManager, String pkg) {
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(pkg, 0);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
