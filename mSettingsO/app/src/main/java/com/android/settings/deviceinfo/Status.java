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

package com.android.settings.deviceinfo;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserManager;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.util.ArrayUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.List;

import static android.content.Context.CONNECTIVITY_SERVICE;
import static android.content.Context.WIFI_SERVICE;
//Begin added by jiabin.zheng.hz for XR7218331 on 2018/12/12
import com.tct.wifi.TctWifiSettingsManager;
//End added by jiabin.zheng.hz for XR7218331 on 2018/12/12
//Begin added by jiabin.zheng.hz for XR7311748 on 2019/01/07
import android.net.NetworkInfo;
//End added by jiabin.zheng.hz for XR7311748 on 2019/01/07

public class Status extends SettingsPreferenceFragment implements Indexable {

    private static final String KEY_BATTERY_STATUS = "battery_status";
    private static final String KEY_BATTERY_LEVEL = "battery_level";
    private static final String KEY_IP_ADDRESS = "wifi_ip_address";
    private static final String KEY_WIFI_MAC_ADDRESS = "wifi_mac_address";
    private static final String KEY_BT_ADDRESS = "bt_address";
    private static final String KEY_WIMAX_MAC_ADDRESS = "wimax_mac_address";
    private static final String KEY_SIM_STATUS = "sim_status";
    private static final String KEY_IMEI_INFO = "imei_info";
    //begin zhixiong.liu.hz for XR 6107743 2018/3/14
    private static final String KEY_HARDWARE_VERSION = "hardware_version";
    //end zhixiong.liu.hz for XR 6107743 2018/3/14
    //Begin added by jiabin.zheng.hz for XR7218331 on 2018/12/12
    private static final String KEY_WIFI_IPV6_ADDRESS = "wifi_ipv6_address";
    //End added by jiabin.zheng.hz for XR7218331 on 2018/12/12
    // Broadcasts to listen to for connectivity changes.
    private static final String[] CONNECTIVITY_INTENTS = {
            BluetoothAdapter.ACTION_STATE_CHANGED,
            ConnectivityManager.CONNECTIVITY_ACTION,
            WifiManager.LINK_CONFIGURATION_CHANGED_ACTION,
            WifiManager.NETWORK_STATE_CHANGED_ACTION,
            //Begin added by jiabin.zheng.hz for XRP10025773 on 2018/11/16
            WifiManager.WIFI_STATE_CHANGED_ACTION,
            //End added by jiabin.zheng.hz for XRP10025773 on 2018/11/16
    };

    private static final int EVENT_UPDATE_STATS = 500;

    private static final int EVENT_UPDATE_CONNECTIVITY = 600;

    private ConnectivityManager mCM;
    private WifiManager mWifiManager;

    private Resources mRes;

    private String mUnavailable;

    private SerialNumberPreferenceController mSerialNumberPreferenceController;

    private Preference mUptime;
    private Preference mBatteryStatus;
    private Preference mBatteryLevel;
    private Preference mBtAddress;
    private Preference mIpAddress;
    private Preference mWifiMacAddress;
    private Preference mWimaxMacAddress;
    //Begin added by jiabin.zheng.hz for XR7218331 on 2018/12/12
    private Preference mIpv6Address;
    private TctWifiSettingsManager mTctWifiMgr = null;
    //End added by jiabin.zheng.hz for XR7218331 on 2018/12/12
    private Handler mHandler;

    private static class MyHandler extends Handler {
        private WeakReference<Status> mStatus;

        public MyHandler(Status activity) {
            mStatus = new WeakReference<Status>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            Status status = mStatus.get();
            if (status == null) {
                return;
            }

            switch (msg.what) {
                case EVENT_UPDATE_STATS:
                    status.updateTimes();
                    sendEmptyMessageDelayed(EVENT_UPDATE_STATS, 1000);
                    break;

                case EVENT_UPDATE_CONNECTIVITY:
                    status.updateConnectivity();
                    break;
            }
        }
    }

    private BroadcastReceiver mBatteryInfoReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel.setSummary(Utils.getBatteryPercentage(intent));
                mBatteryStatus.setSummary(Utils.getBatteryStatus(getResources(), intent));
            }
        }
    };

    private IntentFilter mConnectivityIntentFilter;
    private final BroadcastReceiver mConnectivityReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ArrayUtils.contains(CONNECTIVITY_INTENTS, action)) {
                mHandler.sendEmptyMessage(EVENT_UPDATE_CONNECTIVITY);
            }
        }
    };

    private boolean hasBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }

    private boolean hasWimax() {
        return  mCM.getNetworkInfo(ConnectivityManager.TYPE_WIMAX) != null;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new MyHandler(this);

        mCM = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        mSerialNumberPreferenceController = new SerialNumberPreferenceController(getActivity());

        addPreferencesFromResource(R.xml.device_info_status);
        mBatteryLevel = findPreference(KEY_BATTERY_LEVEL);
        mBatteryStatus = findPreference(KEY_BATTERY_STATUS);
        mBtAddress = findPreference(KEY_BT_ADDRESS);
        mWifiMacAddress = findPreference(KEY_WIFI_MAC_ADDRESS);
        mWimaxMacAddress = findPreference(KEY_WIMAX_MAC_ADDRESS);
        mIpAddress = findPreference(KEY_IP_ADDRESS);
        //Begin added by jiabin.zheng.hz for XR7218331 on 2018/12/12
        mWifiMacAddress.setSelectable(false);
        mIpAddress.setSelectable(false);
        mIpv6Address = findPreference(KEY_WIFI_IPV6_ADDRESS);
        mIpv6Address.setSelectable(false);
        mTctWifiMgr = TctWifiSettingsManager.getTctWifiSettingsManager(getContext());
        //End added by jiabin.zheng.hz for XR7218331 on 2018/12/12
        mRes = getResources();
        mUnavailable = mRes.getString(R.string.status_unavailable);

        // Note - missing in zaku build, be careful later...
        mUptime = findPreference("up_time");
        final PreferenceScreen screen = getPreferenceScreen();
        if (!hasBluetooth()) {
            screen.removePreference(mBtAddress);
            mBtAddress = null;
        }

        if (!hasWimax()) {
            screen.removePreference(mWimaxMacAddress);
            mWimaxMacAddress = null;
        }

        mConnectivityIntentFilter = new IntentFilter();
        for (String intent: CONNECTIVITY_INTENTS) {
             mConnectivityIntentFilter.addAction(intent);
        }

        updateConnectivity();

        mSerialNumberPreferenceController.displayPreference(screen);

        // Remove SimStatus and Imei for Secondary user as it access Phone b/19165700
        // Also remove on Wi-Fi only devices.
        //TODO: the bug above will surface in split system user mode.
        if (!UserManager.get(getContext()).isAdminUser()
                || Utils.isWifiOnly(getContext())) {
            removePreferenceFromScreen(KEY_SIM_STATUS);
            removePreferenceFromScreen(KEY_IMEI_INFO);
        }
        //begin zhixiong.liu.hz for XR 6107743 2018/3/14
        if(getResources().getBoolean(R.bool.def_settings_showHardwareversion_enable)) {
            String hardware_version = SystemProperties.get("ro.def.hardware.version",mUnavailable);
            if(!TextUtils.isEmpty(hardware_version)){
                 findPreference(KEY_HARDWARE_VERSION).setSummary(hardware_version);
            }
        } else {
            removePreferenceFromScreen(KEY_HARDWARE_VERSION);
        }
        //end zhixiong.liu.hz for XR 6107743 2018/3/14
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.DEVICEINFO_STATUS;
    }

    @Override
    public void onResume() {
        super.onResume();
        getContext().registerReceiver(mConnectivityReceiver, mConnectivityIntentFilter,
                         android.Manifest.permission.CHANGE_NETWORK_STATE, null);
        getContext().registerReceiver(mBatteryInfoReceiver,
                new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mHandler.sendEmptyMessage(EVENT_UPDATE_STATS);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContext().unregisterReceiver(mBatteryInfoReceiver);
        getContext().unregisterReceiver(mConnectivityReceiver);
        mHandler.removeMessages(EVENT_UPDATE_STATS);
    }

    /**
     * Removes the specified preference, if it exists.
     * @param key the key for the Preference item
     */
    private void removePreferenceFromScreen(String key) {
        Preference pref = findPreference(key);
        if (pref != null) {
            getPreferenceScreen().removePreference(pref);
        }
    }

    private void setWimaxStatus() {
        if (mWimaxMacAddress != null) {
            String macAddress = SystemProperties.get("net.wimax.mac.address", mUnavailable);
            mWimaxMacAddress.setSummary(macAddress);
        }
    }

    private void setWifiStatus() {
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo();
        boolean hasMacAddress = wifiInfo != null && wifiInfo.hasRealMacAddress();
        //Begin modified by jiabin.zheng.hz for XRP10025773 on 2018/11/16
        boolean wifiEnabled = mWifiManager.isWifiEnabled();
        String macAddress = (wifiEnabled && hasMacAddress) ? wifiInfo.getMacAddress() : null;
        //End modified by jiabin.zheng.hz for XRP10025773 on 2018/11/16
        mWifiMacAddress.setSummary(!TextUtils.isEmpty(macAddress) ? macAddress : mUnavailable);
    }

    private void setIpAddressStatus() {
        String ipAddress = Utils.getDefaultIpAddresses(this.mCM);
        //Begin modifed by jiabin.zheng.hz for XR7311748 on 2019/01/07
        if (ipAddress != null) {
            NetworkInfo netInfo = mCM.getActiveNetworkInfo();
            if (netInfo != null && netInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                mTctWifiMgr.setIpAddress(mIpAddress, mIpv6Address);
            } else {
                mTctWifiMgr.setIpAddressStatus(mIpAddress, mIpv6Address, ipAddress);
            }
        } else {
            mIpAddress.setSummary(mUnavailable);
            mIpv6Address.setSummary(mUnavailable);
        }
        //End modifed by jiabin.zheng.hz for XR7311748 on 2019/01/07
    }

    private void setBtStatus() {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth != null && mBtAddress != null) {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : null;
            if (!TextUtils.isEmpty(address)) {
               // Convert the address to lowercase for consistency with the wifi MAC address.
                mBtAddress.setSummary(address.toLowerCase());
            } else {
                mBtAddress.setSummary(mUnavailable);
            }
        }
    }

    void updateConnectivity() {
        setWimaxStatus();
        setWifiStatus();
        setBtStatus();
        setIpAddressStatus();
    }

    void updateTimes() {
        long at = SystemClock.uptimeMillis() / 1000;
        long ut = SystemClock.elapsedRealtime() / 1000;

        if (ut == 0) {
            ut = 1;
        }

        mUptime.setSummary(convert(ut));
    }

    private String pad(int n) {
        if (n >= 10) {
            return String.valueOf(n);
        } else {
            return "0" + String.valueOf(n);
        }
    }

    private String convert(long t) {
        int s = (int)(t % 60);
        int m = (int)((t / 60) % 60);
        int h = (int)((t / 3600));

        return h + ":" + pad(m) + ":" + pad(s);
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
                    sir.xmlResId = R.xml.device_info_status;
                    return Arrays.asList(sir);
                }

                //Begin added by miaoliu for XR7074231 on 2018/11/01
                 @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    ConnectivityManager cM = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
                    if (cM != null && (cM.getNetworkInfo(ConnectivityManager.TYPE_WIMAX) == null)) {
                        keys.add(KEY_WIMAX_MAC_ADDRESS);
                    }
                    return keys;
                }
                
                //End added by miaoliu for XR7074231 on 2018/11/01
            };
}
