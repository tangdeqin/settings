/*
 * Copyright (C) 2009 The Android Open Source Project
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

package com.tct.search;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.ResolveInfo;
import android.os.UserManager;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import java.util.ArrayList;
import java.util.List;


//begin zhixiong.liu.hz for XR 5707358 2017/12/6
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
//end zhixiong.liu.hz for XR 5707358 2017/12/6
/**
 * Search for mtk feature in Settings.
 */
public class SearchExt implements Indexable {

    private static final String TAG = "SearchExt";

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER
                      = new BaseSearchIndexProvider() {
        @Override
        public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                boolean enabled) {
            List<SearchIndexableRaw> indexables = new ArrayList<SearchIndexableRaw>();
            //begin zhixiong.liu.hz for XR 5707358 2017/12/6
            //for Elabel
            String elabelPkg = "com.jrdcom.Elabel";
            String elableCls = "com.jrdcom.Elabel.SettingsRegulatoryActivity";
            if(isPkgInstalled(context, elabelPkg)) {
                Log.i(TAG, "Regualatory and Safety exists");
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                indexable.title = context.getString(R.string.regulatory_safety_title);
                indexable.intentAction = "android.intent.action.MAIN";
                indexable.intentTargetPackage = elabelPkg;
                indexable.intentTargetClass = elableCls;
                indexables.add(indexable);
            }
            
            // loading google search information
            String googlePkg = "com.google.android.gms";
            String googleCls = "com.google.android.gms.app.settings.GoogleSettingsLink";
            Intent intent = new Intent().setClassName(googlePkg, googleCls);
            List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(intent, 0);
            if(apps != null && apps.size() != 0) {
                Log.d(TAG, "google exist");
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                try {
                   Context googleCtx = context.createPackageContext(googlePkg, Context.CONTEXT_IGNORE_SECURITY);
                   PackageManager googleManager = googleCtx.getPackageManager();
                   String googleLabel = apps.get(0).loadLabel(googleManager).toString();
                   if(null != googleLabel) {
                       indexable.title = googleLabel;
                   }else {
                      indexable.title = "Google";
                   }
                }catch (Exception e) {
                   indexable.title = "Google";
                   Log.d(TAG, e.toString());
                }
                indexable.intentAction = "android.intent.action.MAIN";
                indexable.intentTargetPackage = googlePkg;
                indexable.intentTargetClass = googleCls;
                indexable.iconResId = R.drawable.ic_settings_google;
                indexables.add(indexable);
            }
            //end zhixiong.liu.hz for XR 5707358 2017/12/6

            // Begin added by lei.ye.hz for XR 7100511 on 2018-11-6
            // face unlock
            addIndexable(context, indexables,"com.tcl.faceunlock",
                    "com.tcl.faceunlock.FaceUnlockSettings", R.drawable.ic_face_unlock, "FaceUnlock");
            // End added by lei.ye.hz for XR 7100511 on 2018-11-6

            return indexables;
        }

        // Begin added by lei.ye.hz for XR 7100511 on 2018-11-6
        private void addIndexable(Context context, List<SearchIndexableRaw> indexables,
                                  String pkg, String clz, int localResId, String defTitle) {
            Intent intent = new Intent().setClassName(pkg, clz);
            List<ResolveInfo> apps = context.getPackageManager().queryIntentActivities(intent, 0);
            if(apps != null && apps.size() != 0) {
                Log.d(TAG, "pkg: " + pkg + ", clz: " + clz + " exist");
                SearchIndexableRaw indexable = new SearchIndexableRaw(context);
                try {
                    Context pkgCtx = context.createPackageContext(pkg, Context.CONTEXT_IGNORE_SECURITY);
                    PackageManager packageManager = pkgCtx.getPackageManager();
                    String label = apps.get(0).loadLabel(packageManager).toString();
                    if(validString(label)) {
                        indexable.title = label;
                    } else {
                        // failed to get activity label, so we try to get application label first
                        PackageInfo pkgInfo = packageManager.getPackageInfo(pkg, 0);
                        int pkgLabelRes = pkgInfo.applicationInfo.labelRes;
                        label = pkgCtx.getResources().getString(pkgLabelRes);
                        if (validString(label)) {
                            indexable.title = label;
                        } else {
                            indexable.title = defTitle;
                        }
                    }
                    indexable.iconResId = localResId;//pkgCtx.getResources().getIdentifier(iconRes, "drawable", pkg);
                }catch (Exception e) {
                    indexable.title = defTitle;
                    Log.d(TAG, e.toString());
                }
                indexable.intentAction = "android.intent.action.MAIN";
                indexable.intentTargetPackage = pkg;
                indexable.intentTargetClass = clz;
                indexables.add(indexable);
            }
        }

        private boolean validString(String s) {
            return (s != null && !s.isEmpty());
        }
        // End added by lei.ye.hz for XR 7100511 on 2018-11-6
        
        //begin zhixiong.liu.hz for XR 5707358 2017/12/6
        private boolean isPkgInstalled(Context context, String packageName) {
             if (packageName == null || "".equals(packageName)) {
                 return false;
             }
             ApplicationInfo info = null;
             try {
                 info = context.getPackageManager().getApplicationInfo(packageName, 0);
                 return info != null;
             }catch(Exception e) {
                 return false;
             }
        }
        //end zhixiong.liu.hz for XR 5707358 2017/12/6
        
        @Override
        public List<String> getNonIndexableKeys(Context context) {
            final ArrayList<String> result = new ArrayList<String>();
            return result;
        }
    };

}
