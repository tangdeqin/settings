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

import android.os.Bundle;
import android.os.UserManager;
import android.support.v7.preference.PreferenceScreen;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

 //Begin zhixiong.liu.hz add for RMNET OPEN FUNC merge from NB 20180809
import android.content.Context;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;
import android.text.TextUtils;
import android.os.SystemProperties;
import android.hardware.usb.UsbManager;
import android.util.Log;
 //End zhixiong.liu.hz add for RMNET OPEN FUNC  merge from NB 20180809

public class TestingSettings extends SettingsPreferenceFragment {

    //Begin zhixiong.liu.hz add for RMNET OPEN FUNC  merge from NB 20180809
    private CheckBoxPreference rmnetPreference;
    private static final String ENABLE_NET_WWAN_PREF = "enable_net_wwan_pref";
    private CheckBoxPreference rndisPreference;
    private static final String ENABLE_RNDS_MODEM_PREF = "enable_rnds_modem_pref";
    private Context mContext;
    //End zhixiong.liu.hz add for RMNET OPEN FUNC  merge from NB 20180809
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        addPreferencesFromResource(R.xml.testing_settings);

        final UserManager um = UserManager.get(getContext());
        if (!um.isAdminUser()) {
            PreferenceScreen preferenceScreen = (PreferenceScreen)
                    findPreference("radio_info_settings");
            getPreferenceScreen().removePreference(preferenceScreen);
        }

        //Begin zhixiong.liu.hz add for RMNET OPEN FUNC  merge from NB 20180809
        mContext = getContext();
 
        rmnetPreference = (CheckBoxPreference)findPreference(ENABLE_NET_WWAN_PREF);
        rmnetPreference.setChecked(isRMNETEnable());
 
        rndisPreference= (CheckBoxPreference)findPreference(ENABLE_RNDS_MODEM_PREF);
        rndisPreference.setChecked(isRNDISEnable());
        //end zhixiong.liu.hz add for RMNET OPEN FUNC  merge from NB 20180809
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.TESTING;
    }
    
    //Begin zhixiong.liu.hz add for RMNET OPEN FUNC  merge from NB 20180809
    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (ENABLE_NET_WWAN_PREF.equals(preference.getKey())){
            this.setFunction( !((CheckBoxPreference)preference).isChecked());
        }

        if (ENABLE_RNDS_MODEM_PREF.equalsIgnoreCase(preference.getKey())) {
            this.setFunctionRNDIS(!((CheckBoxPreference)preference).isChecked());
        }
    
        return super.onPreferenceTreeClick(preference);
   }
    
    
    
    public boolean isRMNETEnable(){
       boolean enable=false;
       String func = SystemProperties.get("sys.usb.config",UsbManager.USB_FUNCTION_NONE);
       if (!TextUtils.isEmpty(func)) {
           //"rmnet" for sdm660 new qcom platform
           if (func.contains("rmnet")){
              enable=true;
           }
       }
       return enable;
    }
 
    public boolean isRNDISEnable(){
        boolean enable=false;
        String func = SystemProperties.get("sys.usb.config",UsbManager.USB_FUNCTION_NONE);
       
        if (!TextUtils.isEmpty(func)) {
            if (func.contains("rndis")){
               enable=true;
            }
        }
        return enable;
    }

    private void setFunction(boolean isDefault) {
        String usbConfig = "diag,serial_smd,rmnet_bam,adb";
        String defaultUsbConfig = "mtp,adb";
        UsbManager mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        //Match the correct RMNET port parameter for the different qcom platform
        String platform = SystemProperties.get("ro.board.platform","");
        Log.d("TestingSettings","platform RMNET="+platform);
        if(!TextUtils.isEmpty(platform)){
            //if(platform.equalsIgnoreCase("sdm660")){
                usbConfig ="diag,serial_cdev,rmnet,adb";
                defaultUsbConfig="mtp,adb";
            //}
        }
        if(isDefault) {
            // change to default usb config
            mUsbManager.setCurrentFunction(defaultUsbConfig,true);
            SystemProperties.set("persist.sys.usb.config",defaultUsbConfig);
            Toast.makeText(mContext, "Disable RMNET", Toast.LENGTH_LONG).show();
        }else{
            mUsbManager.setCurrentFunction(usbConfig,true);
            SystemProperties.set("persist.sys.usb.config",usbConfig);
            Toast.makeText(mContext, "Enable RMNET", Toast.LENGTH_LONG).show();
        }
    }
    
    private void setFunctionRNDIS(boolean isDefault) {
        String usbConfig = null;
        String defaultUsbConfig = "mtp,adb";
        UsbManager mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        String platform = SystemProperties.get("ro.board.platform","");
        Log.d("TestingSettings","platform RNDIS="+platform);
        
        if(!TextUtils.isEmpty(platform)){
            //if(platform.equalsIgnoreCase("sdm660")){
                usbConfig ="rndis,serial_cdev,diag,adb";
            //}
        }
        if (isDefault) {
            // change to default usb config
            mUsbManager.setCurrentFunction(defaultUsbConfig,true);
            SystemProperties.set("persist.sys.usb.config",defaultUsbConfig);
            Toast.makeText(mContext, "Disable RNDIS+MODEM", Toast.LENGTH_SHORT).show();
        } else {
            if(usbConfig == null){
                mUsbManager.setCurrentFunction(usbConfig,false);
            }
            else{
                 mUsbManager.setCurrentFunction(usbConfig,true);
            }
            SystemProperties.set("persist.sys.usb.config",usbConfig);
            Toast.makeText(mContext, "Enable RNDIS+MODEM", Toast.LENGTH_SHORT).show();
        }
    }
    //end zhixiong.liu.hz add for RMNET OPEN FUNC  merge from NB 20180809
}
