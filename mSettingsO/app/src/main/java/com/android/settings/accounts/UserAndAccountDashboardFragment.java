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
package com.android.settings.accounts;

import static android.provider.Settings.EXTRA_AUTHORITIES;

import android.app.Activity;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.drawer.Tile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//Begin added by miaoliu for XRP10025756 on 2018/11/8
import com.android.settingslib.core.lifecycle.Lifecycle;
import android.content.Intent;
//End added by miaoliu for XRP10025756 on 2018/11/8
import tct.appclone.PackageCloneManager;//Deleted by miaoliu for XRP10025756 on 2018/11/13

public class UserAndAccountDashboardFragment extends DashboardFragment {

    private static final String TAG = "UserAndAccountDashboard";
    private static final String METADATA_IA_ACCOUNT = "com.android.settings.ia.account";
    //Begin deleted by miaoliu for XRP10025756 on 2018/11/13
    /// Added by yunqi.song.hz for JoyUI dualApp TASK 6766810 ,@TCT{
    // @Override
    // public void onCreate(Bundle bundle) {
    //     super.onCreate(bundle);
    //     if(PackageCloneManager.getInstance().isCloneEnabled()){
    //         PreferenceScreen screen = getPreferenceScreen();
    //         screen.findPreference("auto_sync_personal_account_data").setTitle(R.string.account_settings_menu_auto_sync_original);
    //         screen.findPreference("auto_sync_work_account_data").setTitle(R.string.account_settings_menu_auto_sync_cloned);
    //     }
    // }
    ///TCT}
   //End deleted by miaoliu for XRP10025756 on 2018/11/13
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ACCOUNT;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.user_and_accounts_settings;
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_user_and_account_dashboard;
    }
    //Begin modified by miaoliu for XRP10025756 on 2018/11/8
    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, this, getLifecycle(), getIntent());
    }

     private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            UserAndAccountDashboardFragment fragment, Lifecycle lifecycle, Intent intent) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(new EmergencyInfoPreferenceController(context));
        AddUserWhenLockedPreferenceController addUserWhenLockedPrefController =
                new AddUserWhenLockedPreferenceController(context);
        controllers.add(addUserWhenLockedPrefController);
       
        controllers.add(new AutoSyncDataPreferenceController(context, fragment));
        controllers.add(new AutoSyncPersonalDataPreferenceController(context, fragment));
        controllers.add(new AutoSyncWorkDataPreferenceController(context, fragment));
        //Begin added by miaoliu for XRP10025756 on 2018/11/13
         controllers.add(new AutoSyncPersonalDataPreferenceControllerClone(context, fragment));
        controllers.add(new AutoSyncWorkDataPreferenceControllerClone(context, fragment));
        //End added by miaoliu for XRP10025756 on 2018/11/13
        String[] authorities = null;
        if(intent != null){
            authorities = intent.getStringArrayExtra(EXTRA_AUTHORITIES);
        }
        final AccountPreferenceController accountPrefController =
                new AccountPreferenceController(context, fragment, authorities);
        if(lifecycle != null){
             lifecycle.addObserver(addUserWhenLockedPrefController);
             lifecycle.addObserver(accountPrefController);
        }
        controllers.add(accountPrefController);
        return controllers;
     }
 //End modified by miaoliu for XRP10025756 on 2018/11/8
    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
                UserInfo info = mContext.getSystemService(UserManager.class).getUserInfo(
                        UserHandle.myUserId());
                mSummaryLoader.setSummary(this,
                    mContext.getString(R.string.users_and_accounts_summary, info.name));
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.user_and_accounts_settings;
                    return Arrays.asList(sir);
                }
                //Begin addded by miaoliu for XRP10025756 on 2018/11/8
                 @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context context) {
                    return buildPreferenceControllers(context, null, null, null);
                }
                //End added by miaoliu for XRP10025756 on 2018/11/8
            };
}