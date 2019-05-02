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
package com.android.settings.network;

import android.content.Context;
import android.support.v7.preference.Preference;
import android.telephony.TelephonyManager;

import com.android.ims.ImsManager;
import com.android.settings.WifiCallingSettings;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
//Begin modify by chenli.gao.hz for XR6507882 on 2018/08/06
import com.tcl.sdk.TclPluginManager; //Modify by chenli.gao.hz for XR6998744 on 2018/09/12
import android.text.TextUtils;
import android.os.SystemProperties;
import com.android.settings.R;
import android.telephony.TelephonyManager;
//End modify by chenli.gao.hz for XR6507882 on 2018/08/06
import com.android.settings.Utils;//Added by miaoliu for XR7201659 on 2018/12/27
public class WifiCallingPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private static final String KEY_WFC_SETTINGS = "wifi_calling_settings";
    private TelephonyManager mTm;

    public WifiCallingPreferenceController(Context context) {
        super(context);
        mTm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    }

    @Override
    public void updateState(Preference preference) {
        preference.setSummary(WifiCallingSettings.getWfcModeSummary(
                mContext, ImsManager.getWfcMode(mContext, mTm.isNetworkRoaming())));
        setWfcSettingsTitleAndSummary(preference); //add by chenli.gao.hz for XR6507882 on 2018/07/20
    }

    //Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
    private void setWfcSettingsTitleAndSummary(Preference preference){
        if(TclPluginManager.getTclSdkAdapter(mContext).isOrangePolandCC()) { //modify by chenli.gao.hz for XR6507882 on 2018/08/06
            preference.setSummary(null);
        } else {
            String titleStr = getWfcSettingsTitleString();
            //Begin modified by miaoliu for XR7201659 on 2018/12/27
            // if(mContext.getResources().getBoolean(R.bool.def_Settings_rename_wificalling_menu)) {
            //     preference.setTitle(mContext.getString(R.string.def_Settings_wificalling_menu_list));
            if(Utils.getBoolean(mContext, "def_Settings_rename_wificalling_menu", "com.tct")) {
                preference.setTitle(Utils.getString(mContext, "def_Settings_wificalling_menu_list","com.tct"));
            //End modified by miaoliu for XR7201659 on 2018/12/27
            } else {
                if (!TextUtils.isEmpty(titleStr)) {
                    preference.setTitle(titleStr);
                }
            }
            //begin add by zhixiong.liu.hz for task7026677  20180917
            int resId = WifiCallingSettings.getCustomizeWfcModeSummary(mContext, -1, getWfcMode());
            if(resId != -1){
                String summary = mContext.getString(resId);
                if (!TextUtils.isEmpty(summary)) {
                    preference.setSummary(summary); 
                }
            }
            //end add by zhixiong.liu.hz for task7026677  20180917
        }
    }

    private int getWfcMode(){
        return ImsManager.getWfcMode(mContext, mTm.isNetworkRoaming());
    }

    /**
     * Germany 262 01 SPN :Telekom.de, T-Mobile D, Business, Privat, congstar, congstar.de should be WLAN Call.
     Polan:260 02 SPN: T-Mobile.pl, T-Mobile.pl Q, heyah should be Po??czenie przez Wi-Fi.
     Greece:202 01 should be COSMOTE WiFi Calling.
     */
    private String getWfcSettingsTitleString() {
        String titleStr = "";
        if(SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x01) {
            String mccMnc = mTm.getSimOperator();
            String spn = mTm.getSimOperatorName();
            if ("26201".equals(mccMnc)) {
                if ("Telekom.de".equals(spn) || "T-Mobile D".equals(spn) || "Business".equals(spn)
                        || "Privat".equals(spn) || "congstar".equals(spn) || "congstar.de".equals(spn)) {
                    titleStr = mContext.getResources().getString(R.string.wifi_calling_settings_title_Germany);
                }
            } else if ("26002".equals(mccMnc)) {
                if ("T-Mobile.pl".equals(spn) || "T-Mobile.pl Q".equals(spn) || "heyah".equals(spn)) {
                    titleStr = mContext.getResources().getString(R.string.wifi_calling_settings_title_Polan);
                }
            } else if ("20201".equals(mccMnc)) {
                titleStr = mContext.getResources().getString(R.string.wifi_calling_settings_title_Greece);
            }
        }
        return titleStr;
    }
    //End //add by chenli.gao.hz for XR6507882 on 2018/07/20

    @Override
    public boolean isAvailable() {
        return ImsManager.isWfcEnabledByPlatform(mContext)
                && ImsManager.isWfcProvisionedOnDevice(mContext)
                && TclPluginManager.getTclSdkAdapter(mContext).getDisplay4GVolteSettings(); //modify by chenli.gao.hz for XR6507882 on 2018/08/06
    }

    @Override
    public String getPreferenceKey() {
        return KEY_WFC_SETTINGS;
    }
}
