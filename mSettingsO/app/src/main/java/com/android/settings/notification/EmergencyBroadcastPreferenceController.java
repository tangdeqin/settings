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

package com.android.settings.notification;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;

import com.android.settings.accounts.AccountRestrictionHelper;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.core.AbstractPreferenceController;
//Begin added by miaoliu for XR7312081 on 2019/1/15
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import java.util.List;
//End added by miaoliu for XR7312081 on 2019/1/15
/**
 * Base class for preference controller that handles preference that enforce adjust volume
 * restriction
 */
public class EmergencyBroadcastPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private final String mPrefKey;

    private AccountRestrictionHelper mHelper;
    private UserManager mUserManager;
    private PackageManager mPm;
    private boolean mCellBroadcastAppLinkEnabled;
    private SubscriptionManager mSubManager;//Added by miaoliu for XR7312081 on 2019/1/15
    public EmergencyBroadcastPreferenceController(Context context, String prefKey) {
        this(context, new AccountRestrictionHelper(context), prefKey);
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    EmergencyBroadcastPreferenceController(Context context, AccountRestrictionHelper helper,
            String prefKey) {
        super(context);
        mPrefKey = prefKey;
        mHelper = helper;
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        mPm = mContext.getPackageManager();
        // Enable link to CMAS app settings depending on the value in config.xml.
        mCellBroadcastAppLinkEnabled = isCellBroadcastAppLinkEnabled();
        //Begin added by miaoliu for XR7312081 on 2019/1/15
        mSubManager = SubscriptionManager.from(context);
        //End added by miaoliu for XR7312081 on 2019/1/15
    }

    @Override
    public void updateState(Preference preference) {
        if (!(preference instanceof RestrictedPreference)) {
            return;
        }
        ((RestrictedPreference) preference).checkRestrictionAndSetDisabled(
                UserManager.DISALLOW_CONFIG_CELL_BROADCASTS);
        //Begin added by miaoliu for XR7312081 on 2019/1/15
        if (mSubManager != null) {
            List<SubscriptionInfo> subInfoList = mSubManager.getActiveSubscriptionInfoList();
            if (subInfoList == null || subInfoList.size() == 0) {
                preference.setEnabled(false);
            }else{
                preference.setEnabled(true);
            }
        }
        //End added by miaoliu for XR7312081 on 2019/1/15
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return mPrefKey;
    }

    @Override
    public boolean isAvailable() {
        return mUserManager.isAdminUser() && mCellBroadcastAppLinkEnabled
                && !mHelper.hasBaseUserRestriction(
                UserManager.DISALLOW_CONFIG_CELL_BROADCASTS, UserHandle.myUserId());
    }

    private boolean isCellBroadcastAppLinkEnabled() {
        // Begin modify by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25
        /*boolean enabled = mContext.getResources().getBoolean(
                com.android.internal.R.bool.config_cellBroadcastAppLinks);*/
        int isCellBroadcastAppLinksId = mContext.getResources().getIdentifier("config_cellBroadcastAppLinks","bool","android");
        boolean enabled = mContext.getResources().getBoolean(isCellBroadcastAppLinksId);
        // End modify by yeqing.lv for XR6873798 un-use frameworks resource on 2018/08/25
        if (enabled) {
            try {
                if (mPm.getApplicationEnabledSetting("com.android.cellbroadcastreceiver")
                        == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
                    enabled = false;  // CMAS app disabled
                }
            } catch (IllegalArgumentException ignored) {
                enabled = false;  // CMAS app not installed
            }
        }
        return enabled;
    }

}