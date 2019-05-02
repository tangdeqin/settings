/*
 * Copyright (C) 2015 The Android Open Source Project
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
package com.android.settings.applications;

import static android.net.NetworkPolicyManager.POLICY_NONE;
import static android.net.NetworkPolicyManager.POLICY_REJECT_METERED_BACKGROUND;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.INotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.net.NetworkPolicyManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.webkit.IWebViewUpdateService;

import com.android.settings.R;

import java.util.List;
//Begin added by miaoliu for XR5756680 on 2017/12/13
import java.util.ArrayList;
import android.app.ActivityManagerNative;
import android.content.ComponentName;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.os.SystemProperties;
//End added by miaoliu for XR5756680 on 2017/12/13

public class ResetAppsHelper implements DialogInterface.OnClickListener,
        DialogInterface.OnDismissListener {

    private static final String EXTRA_RESET_DIALOG = "resetDialog";

    private final PackageManager mPm;
    private final IPackageManager mIPm;
    private final INotificationManager mNm;
    private final IWebViewUpdateService mWvus;
    private final NetworkPolicyManager mNpm;
    private final AppOpsManager mAom;
    private final Context mContext;

    private AlertDialog mResetDialog;

    public ResetAppsHelper(Context context) {
        mContext = context;
        mPm = context.getPackageManager();
        mIPm = IPackageManager.Stub.asInterface(ServiceManager.getService("package"));
        mNm = INotificationManager.Stub.asInterface(
                ServiceManager.getService(Context.NOTIFICATION_SERVICE));
        mWvus = IWebViewUpdateService.Stub.asInterface(ServiceManager.getService("webviewupdate"));
        mNpm = NetworkPolicyManager.from(context);
        mAom = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getBoolean(EXTRA_RESET_DIALOG)) {
            buildResetDialog();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        if (mResetDialog != null) {
            outState.putBoolean(EXTRA_RESET_DIALOG, true);
        }
    }

    public void stop() {
        if (mResetDialog != null) {
            mResetDialog.dismiss();
            mResetDialog = null;
        }
    }

    void buildResetDialog() {
        if (mResetDialog == null) {
            mResetDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.reset_app_preferences_title)
                    .setMessage(R.string.reset_app_preferences_desc)
                    .setPositiveButton(R.string.reset_app_preferences_button, this)
                    .setNegativeButton(R.string.cancel, null)
                    .setOnDismissListener(this)
                    .show();
        }
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        if (mResetDialog == dialog) {
            mResetDialog = null;
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        if (mResetDialog != dialog) {
            return;
        }
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                List<ApplicationInfo> apps = mPm.getInstalledApplications(
                        PackageManager.GET_DISABLED_COMPONENTS);
                for (int i = 0; i < apps.size(); i++) {
                    ApplicationInfo app = apps.get(i);
                    try {
                        mNm.setNotificationsEnabledForPackage(app.packageName, app.uid, true);
                    } catch (android.os.RemoteException ex) {
                    }
                    if (!app.enabled) {
                        if (mPm.getApplicationEnabledSetting(app.packageName)
                                == PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER
                                && !isNonEnableableFallback(app.packageName)) {
                            mPm.setApplicationEnabledSetting(app.packageName,
                                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT,
                                    PackageManager.DONT_KILL_APP);
                        }
                    }
                }
                try {
                    mIPm.resetApplicationPreferences(UserHandle.myUserId());
                } catch (RemoteException e) {
                }
                mAom.resetAllModes();
                final int[] restrictedUids = mNpm.getUidsWithPolicy(
                        POLICY_REJECT_METERED_BACKGROUND);
                final int currentUserId = ActivityManager.getCurrentUser();
                for (int uid : restrictedUids) {
                    // Only reset for current user
                    if (UserHandle.getUserId(uid) == currentUserId) {
                        mNpm.setUidPolicy(uid, POLICY_NONE);
                    }
                }
                //Begin added by miaoliu for XR5756680 on 2017/12/13
                resetDefaultHome();
                //End added by miaoliu for XR5756680 on 2017/12/13
            }
        });
    }

    private boolean isNonEnableableFallback(String packageName) {
        try {
            return mWvus.isFallbackPackage(packageName);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }
    }

    //Begin added by miaoliu for XR5756680 on 2017/12/13
    private void resetDefaultHome() {    
        IntentFilter homeFilter = new IntentFilter(Intent.ACTION_MAIN);
        homeFilter.addCategory(Intent.CATEGORY_HOME);
        homeFilter.addCategory(Intent.CATEGORY_DEFAULT);
        
        ArrayList<ResolveInfo> homeActivities = new ArrayList<ResolveInfo>();                
        mPm.getHomeActivities(homeActivities);
        ComponentName[] homeComponentSet = new ComponentName[homeActivities.size()];
        
        for(int i = 0; i < homeActivities.size(); i++) {
            ResolveInfo resolveInfo = homeActivities.get(i);
            homeComponentSet[i] = new ComponentName(resolveInfo.activityInfo.packageName,
                                                    resolveInfo.activityInfo.name);
        }
        
        // final String DEFAULT_PACKAGE_NAME = "com.tct.launcher";
        // final String DEFAULT_ACTIVITY_NAME = "com.tct.launcher.Launcher";        
        String DEFAULT_PACKAGE_NAME = "";
        String DEFAULT_ACTIVITY_NAME = "";
        String defaultLauncherPkg = SystemProperties.get("def.set.defaultLauncher.pkg","");
        if (defaultLauncherPkg != null && defaultLauncherPkg.trim().length() > 1) {

            //split the package and the default activity
            if (defaultLauncherPkg.contains(":")) {
                String[] pkgs = defaultLauncherPkg.split(":");
                if (pkgs != null && pkgs.length > 0) {
                    DEFAULT_PACKAGE_NAME = pkgs[0];
                    if (pkgs.length > 1) {
                        DEFAULT_ACTIVITY_NAME = pkgs[1];
                    }
                }
            }
        }

        //any of the package name or activity name is empty, use joy launcher as default
        if("".equals(DEFAULT_PACKAGE_NAME) || "".equals(DEFAULT_ACTIVITY_NAME)){
            DEFAULT_PACKAGE_NAME = "com.tct.launcher";
            DEFAULT_ACTIVITY_NAME = "com.tct.launcher.Launcher";
        }

        ComponentName defaultHome = new ComponentName(DEFAULT_PACKAGE_NAME,DEFAULT_ACTIVITY_NAME);
        mPm.replacePreferredActivity(homeFilter,IntentFilter.MATCH_CATEGORY_EMPTY,homeComponentSet,defaultHome);           
    }
    //End added by miaoliu for XR5756680 on 2017/12/13
}
