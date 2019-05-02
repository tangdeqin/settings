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

/**
 * To Customize TCT WiFi Tethering menu
 * add on 6897886
 * @author yucheng.luo
 * @Time 2018-09-01
 */

package com.tct.wifi.hotspot;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import com.android.settings.wifi.tether.WifiTetherPreferenceController;
import com.android.settings.wifi.tether.WifiTetherSettings;
import com.android.settingslib.TetherUtil;

//Begin added by junyan.wan.hz for XR 7108532 on 2018/11/16
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;
import android.content.res.Resources;
import java.util.Arrays;
//End added by junyan.wan.hz for XR 7108532 on 2018/11/16

import java.lang.ref.WeakReference;
import java.util.List;
import static android.net.ConnectivityManager.TETHERING_WIFI;
//Begin added by yucheng.luo for XR6610449 on 18-7-26
// start:BBSECURE_TETH_TO
import com.tct.sdk.base.wifi.TctWifiSettingsUtils;
// end:BBSECURE_TETH_TO
//End added by yucheng.luo for XR6610449 on 18-7-26
import com.tct.wifi.TctWifiSettingsManager;

//Begin added by yucheng.luo for XR6699294 on 18-8-6
import android.view.KeyEvent;
import android.telephony.TelephonyManager;
import android.content.DialogInterface.OnClickListener;
import android.app.AlertDialog;
import android.view.View;
//End added by yucheng.luo for XR6699294 on 18-8-6

//[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.text.TextUtils;
import java.util.ArrayList;
//[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-29

/*
 * Displays preferences for Tethering.
 */
//Begin Modified by junyan.wan.hz for XR 7108532 on 2018/11/16
public class TctTetherWifiSettings extends SettingsPreferenceFragment
        implements OnClickListener, Preference.OnPreferenceChangeListener,
        DataSaverBackend.Listener,TctButtonPreference.OnButtonClickCallback, Indexable {
//End Modified by junyan.wan.hz for XR 7108532 on 2018/11/16

    private static final String TAG = "TctTetherWifiSettings";

    //Dialog type
    private static final int DIALOG_AP_SETTINGS = 1;
    //Begin added by yucheng.luo for XR7031036 on 18-9-28
    //WPS menu
    private static final int DIALOG_WPS_CONNECT = 2;
    //End added by yucheng.luo for XR7031036 on 18-9-28

    //WiFi tether
    private static final String ENABLE_WIFI_AP = "enable_wifi_ap";


    private WifiApEnabler mWifiApEnabler;
    private SwitchPreference mEnableWifiAp;

    //Connected Client
    private PreferenceCategory mConnectedCategory;
    private static final String CONNECTED_CATEGORY = "connected_category";
    private List<String> mClientList;

    //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
    //Block Client list
    private static final String BLOCKED_CATEGORY = "blocked_category";
    private PreferenceCategory mBlockedCategory;
    private List<String> blockClientList = new ArrayList<>();
    private static final String TCT_HOTSPOST_SHARED_PREFERENCES_NAME = "tct_tether_settings";
    private static final String TCT_HOTSPOST_SHARED_PREFERENCES_KEY = "block_list";
    //[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-29

    //Broadcast
    private TetherChangeReceiver mTetherChangeReceiver;

    private String[] mWifiRegexs;


    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;

    private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    private String[] mSecurityType;
    private Preference mCreateNetwork;

    private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;
    private ConnectivityManager mCm;

    private WifiTetherPreferenceController mWifiTetherPreferenceController;

    private boolean mRestartWifiApAfterConfigChange;

    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private TctWifiSettingsManager mTctWifiMgr = null;

    private boolean mStopTethering = false;

    //Begin added by yucheng.luo for XR6610449 on 18-7-26
    // start:BBSECURE_TETH_TO
    // Define a new Action which will open the MHS set up dialog
    public static final String ACTION_WIFI_AP_SETTINGS = "com.android.settings.WIFI_AP_SETTINGS";
    // end:BBSECURE_TETH_TO
    //End added by yucheng.luo for XR6610449 on 18-7-26

    //Begin added by yucheng.luo for XR7031036 on 18-9-28
    //WPS menu
    private static final String WPS_CONNECT = "wps_connect";
    private Preference mWpsConnect;
    //End added by yucheng.luo for XR7031036 on 18-9-28
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.TETHER;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mWifiTetherPreferenceController =
                new WifiTetherPreferenceController(context, getLifecycle());
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.tether_wifi_prefs);

        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();

        mTctWifiMgr = TctWifiSettingsManager.getTctWifiSettingsManager(getContext());

        final Activity activity = getActivity();

        mEnableWifiAp =
                (SwitchPreference) findPreference(ENABLE_WIFI_AP);

        mConnectedCategory = (PreferenceCategory) findPreference(CONNECTED_CATEGORY);

        //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-26
        mBlockedCategory = (PreferenceCategory) findPreference(BLOCKED_CATEGORY);
        //[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-26

        Preference wifiApSettings = findPreference(WIFI_AP_SSID_AND_SECURITY);

        mDataSaverBackend.addListener(this);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mWifiRegexs = mCm.getTetherableWifiRegexs();

        final boolean wifiAvailable = mWifiRegexs.length != 0;


        mWifiTetherPreferenceController.displayPreference(getPreferenceScreen());
        //Begin modified by jiabin.zheng.hz for XR6699294 on 2018/08/03
        if (WifiTetherSettings.isTetherSettingPageEnabled() || !mTctWifiMgr.isHotspotShow()) {
            //End modified by jiabin.zheng.hz for XR6699294 on 2018/08/03
            removePreference(ENABLE_WIFI_AP);
            removePreference(WIFI_AP_SSID_AND_SECURITY);
        } else {
            if (wifiAvailable && !Utils.isMonkeyRunning()) {
                mWifiApEnabler = new WifiApEnabler(activity, mDataSaverBackend, mEnableWifiAp);
                initWifiTethering();
            } else {
                getPreferenceScreen().removePreference(mEnableWifiAp);
                getPreferenceScreen().removePreference(wifiApSettings);
            }
        }

       //Begin added by yucheng.luo for XR7031036 on 18-9-28
        if(TctWifiSettingsUtils.getPlatformInfo() != 0){
            mWpsConnect = findPreference(WPS_CONNECT);
            mWpsConnect.setEnabled(false);
        }else{
            removePreference(WPS_CONNECT);
        }
       //End added by yucheng.luo for XR7031036 on 18-9-28

        // Set initial state based on Data Saver mode.
        onDataSaverChanged(mDataSaverBackend.isDataSaverEnabled());
    }

    @Override
    public void onResume() {
        super.onResume();

        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
       //Begin modified by yucheng.luo for XR7304195 on 18-12-29
        //filter.addAction(TctWifiSettingsUtils.TCT_WIFI_HOTSPOT_CLIENTS_CHANGED_ACTION);
        //[TCT-ROM][Tethering Receiver]Begin added by yucheng.luo for XR7215668 on 18-12-06
        filter.addAction("codeaurora.net.conn.TETHER_CONNECT_STATE_CHANGED");
        //[TCT-ROM][Tethering Receiver]End added by yucheng.luo for XR7215668 on 18-12-06
        filter.addAction("android.net.wifi.WIFI_HOTSPOT_CLIENTS_IP_READY");
        filter.addAction("android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED");
       //End modified by yucheng.luo for XR7304195 on 18-12-29
        getActivity().registerReceiver(mTetherChangeReceiver, filter);
        //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-26
        if(mEnableWifiAp.isChecked()){
            blockClientList = getHotspotBlockList();
        }
        //[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-26
        handleWifiApClientsChanged();
    }

    @Override
    public void onDestroy() {
        mDataSaverBackend.remListener(this);
        super.onDestroy();
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        mEnableWifiAp.setEnabled(!mDataSaverEnabled);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted)  {
    }

    private void initWifiTethering() {
        final Activity activity = getActivity();
        mWifiConfig = mWifiManager.getWifiApConfiguration();
        mSecurityType = getResources().getStringArray(R.array.wifi_ap_security);

        mCreateNetwork = findPreference(WIFI_AP_SSID_AND_SECURITY);

        mRestartWifiApAfterConfigChange = false;

        if (mWifiConfig == null) {
            //Begin modified by yucheng.luo for XR6873798 on 18-8-28
            final String s = activity.getString(
                    R.string.wifi_tether_configure_ssid_default);
            //End modified by yucheng.luo for XR6873798 on 18-8-28
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    s, mSecurityType[WifiApDialog.OPEN_INDEX]));
        } else {
            int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
            mCreateNetwork.setSummary(String.format(activity.getString(CONFIG_SUBTEXT),
                    mWifiConfig.SSID,
                    mSecurityType[index]));
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            return mDialog;
       //Begin modified by yucheng.luo for XR7031036 on 18-9-28
        //WPS menu
        }else if (id == DIALOG_WPS_CONNECT) {
            TctWifiApWpsDialog wpsDialog = new TctWifiApWpsDialog(getActivity());
            Log.d(TAG, "onCreateDialog, return wpsDialog");
            return wpsDialog;
        }
       //End modified by yucheng.luo for XR7031036 on 18-9-28

        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
       //Begin modified by yucheng.luo for XR7031036 on 18-9-28
//        if (dialogId == DIALOG_AP_SETTINGS) {
//            return MetricsEvent.DIALOG_AP_SETTINGS;
//        }
        return  MetricsEvent.WIFI;
       //End modified by yucheng.luo for XR7031036 on 18-9-28
    }

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG,
                    "Receiver action: "+action);
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_DISABLED
                        && mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    Log.d(TAG, "Restarting WifiAp due to prior config change.");
                    //Begin add by yucheng.luo for XR6699294 on 18-8-6
                    showDatasetDialog();
                    //End add by yucheng.luo for XR6699294 on 18-8-6
                    startTethering(TETHERING_WIFI);
                }
            } else if (action.equals(WifiManager.WIFI_AP_STATE_CHANGED_ACTION)) {
                int state = intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE, 0);
                if (state == WifiManager.WIFI_AP_STATE_DISABLED
                        && mRestartWifiApAfterConfigChange) {
                    mRestartWifiApAfterConfigChange = false;
                    Log.d(TAG, "Restarting WifiAp due to prior config change.");
                    //Begin add by yucheng.luo for XR6699294 on 18-8-6
                    showDatasetDialog();
                    //End add by yucheng.luo for XR6699294 on 18-8-6
                    startTethering(TETHERING_WIFI);
                }
                //Begin added by yucheng.luo for XR7031036 on 18-9-28
                handleWifiApStateChanged(intent.getIntExtra(WifiManager.EXTRA_WIFI_AP_STATE,
                        WifiManager.WIFI_AP_STATE_FAILED));
                //End added by yucheng.luo for XR7031036 on 18-9-28
                //Begin modified by yucheng.luo for XR7304195 on 18-12-29
                //[TCT-ROM][Tethering Receiver]Begin modified by yucheng.luo for XR7215668 on 18-12-06
            }else if("android.net.wifi.WIFI_HOTSPOT_CLIENTS_IP_READY".equals(action)
                    || "codeaurora.net.conn.TETHER_CONNECT_STATE_CHANGED".equals(action)
                    || "android.net.wifi.WIFI_HOTSPOT_CLIENTS_CHANGED".equals(action)){
                //[TCT-ROM][Tethering Receiver]End added by yucheng.luo for XR7215668 on 18-12-06
                //End modified by yucheng.luo for XR7304195 on 18-12-29
                //Begin modified by yucheng.luo for XR7311625 on 19-1-8
                    /*if (mStopTethering) {
                        mStopTethering = false;
                        clearWifiApClients();
                    } else {
                        handleWifiApClientsChanged();
                    }*/
                if (!mStopTethering) {
                    handleWifiApClientsChanged();
                }
                //End modified by yucheng.luo for XR7311625 on 19-1-8
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        final Activity activity = getActivity();

        mStartTetheringCallback = new OnStartTetheringCallback(this);

        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(this);
            mWifiApEnabler.resume();
        }

        //Begin added by yucheng.luo for XR6610449 on 18-7-26
        // start:BBSECURE_TETH_TO
        String action = activity.getIntent().getAction();
        if (action != null) {
            if (ACTION_WIFI_AP_SETTINGS.equals(action)) {
                showDialog(DIALOG_AP_SETTINGS);
            }
        }
        // end:BBSECURE_TETH_TO
        //End added by yucheng.luo for XR6610449 on 18-7-26

        if(TctWifiSettingsUtils.getPlatformInfo() == 0) {
            getPreferenceManager().getPreferenceScreen().removePreference(mConnectedCategory);
        }
    }

    //Begin added by xuhui.li.hz for XR P10025797 on 2018/11/12
    @Override
    public void onPause() {
        if(mTetherChangeReceiver != null) {
            getActivity().unregisterReceiver(mTetherChangeReceiver);
        }
        mTetherChangeReceiver = null;
        super.onPause();
    }
    //End added by xuhui.li.hz for XR P10025797 on 2018/11/12

    @Override
    public void onStop() {
        super.onStop();
        //Begin deleted by xuhui.li.hz for XR P10025797 on 2018/11/12
        //getActivity().unregisterReceiver(mTetherChangeReceiver);
        //mTetherChangeReceiver = null;
        //End deleted by xuhui.li.hz for XR P10025797 on 2018/11/12
        mStartTetheringCallback = null;
        //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-28
        if(mEnableWifiAp.isChecked()){
            sharePreferencesCommitEditor(false);
        }
        //[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-28
        if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(null);
            mWifiApEnabler.pause();
        }
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        boolean enable = (Boolean) value;

        if (enable) {
            //Begin add by yucheng.luo for XR6699294 on 18-8-6
            boolean isDialogShown = showDatasetDialog();
            if (isDialogShown) {
                if(mEnableWifiAp != null){
                    mEnableWifiAp.setChecked(false);
                }
            } else {
                startTethering(TETHERING_WIFI);
            //Begin add by zhiyong.song  on 18-10-17
                mEnableWifiAp.setChecked(true);
                mEnableWifiAp.setEnabled(false);
            //End add by zhiyong.song  on 18-10-17
            }
            //End modified by yucheng.luo for XR6699294 on 18-8-6
        } else {
            mCm.stopTethering(TETHERING_WIFI);
            mStopTethering = true;
            clearWifiApClients();
        }
        return false;
    }

    public static boolean isProvisioningNeededButUnavailable(Context context) {
        return (TetherUtil.isProvisioningNeeded(context)
                && !isIntentAvailable(context));
    }

    private static boolean isIntentAvailable(Context context) {
        //Begin modified by yucheng.luo for XR6873798 on 18-8-28
        String[] provisionApp = context.getResources().getStringArray(
                R.array.config_mobile_hotspot_provision_app);
        //End modified by yucheng.luo for XR6873798 on 18-8-28
        if (provisionApp.length < 2) {
            return false;
        }
        final PackageManager packageManager = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.setClassName(provisionApp[0], provisionApp[1]);

        return (packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY).size() > 0);
    }

    private void startTethering(int choice) {
        mCm.startTethering(choice, true, mStartTetheringCallback, mHandler);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
     //Begin modified by yucheng.luo for XR7031036 on 18-9-28
     if (preference == mCreateNetwork) {
            showDialog(DIALOG_AP_SETTINGS);
        }else if (preference == mWpsConnect) {
            showDialog(DIALOG_WPS_CONNECT);
        }
     //End modified by yucheng.luo for XR7031036 on 18-9-28

        return super.onPreferenceTreeClick(preference);
    }

    public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                /**
                 * if soft AP is stopped, bring up
                 * else restart with new config
                 * TODO: update config on a running access point when framework support is added
                 */
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    Log.d(TAG,
                            "Wifi AP config changed while enabled, stop and restart");
                    mRestartWifiApAfterConfigChange = true;
                    mCm.stopTethering(TETHERING_WIFI);
                    clearWifiApClients();
                }
                mWifiManager.setWifiApConfiguration(mWifiConfig);
                int index = WifiApDialog.getSecurityTypeIndex(mWifiConfig);
                mCreateNetwork.setSummary(String.format(getActivity().getString(CONFIG_SUBTEXT),
                        mWifiConfig.SSID,
                        mSecurityType[index]));
                //Begin added by yucheng.luo for XR6610449 on 18-7-26
                // start:BBSECURE_TETH_TO
                int inactivityTimeout = mDialog.getInactivityTimeoutInMins();
                final Activity activity = getActivity();

                // Save the MHS inactivity timeout value
                TctWifiSettingsUtils.setHotspotInactivityTimeout(activity,inactivityTimeout);
                // end:BBSECURE_TETH_TO
                //End added by yucheng.luo for XR6610449 on 18-7-26

                //Begin added by yucheng.luo for XR6699573 on 18-8-3
                // start:Max connect
                TctWifiSettingsUtils.setHotspotMaxClientNum(activity,mDialog.getMaxUserNum());
                //end:Max connect
                //End added by yucheng.luo for XR6699573 on 18-8-3
            }
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private static final class OnStartTetheringCallback extends
            ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TctTetherWifiSettings> mTetherWifiSettings;

        OnStartTetheringCallback(TctTetherWifiSettings settings) {
            mTetherWifiSettings = new WeakReference<TctTetherWifiSettings>(settings);
        }

        @Override
        public void onTetheringStarted() {
            update();
        }

        @Override
        public void onTetheringFailed() {
            update();
        }

        private void update() {
        }
    }

    //Begin added by yucheng.luo for XR6699294 on 18-8-6
    private boolean showDatasetDialog() {

        TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(
                Context.TELEPHONY_SERVICE);

        if (!mTctWifiMgr.isHotspotShowTips()) {
            return false;
        }

        OnClickListener dataDialogListener = new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(mEnableWifiAp != null){
                    mEnableWifiAp.setChecked(true);
                }
                startTethering(TETHERING_WIFI);

                dialog.dismiss();
            }
        };

        if (mTctWifiMgr.isDataEnabled()) {
            if(mWifiManager.getWifiState() == WifiManager.WIFI_STATE_DISABLED){
                return false;
            }
            AlertDialog dialog = new AlertDialog.Builder(getActivity(),
                    R.style.wifi_display_dialog)
                    .setCancelable(true)
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode,
                                             KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK
                                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                                //mSwitchBar.setEnabled(true);
                                dialog.dismiss();
                            }
                            return false;
                        }
                    })
                    .setPositiveButton(R.string.dialog_button_ok,
                            dataDialogListener)
                    .setNegativeButton(R.string.dialog_button_cancel,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }
                    ).create();
            dialog.setTitle(getString(R.string.dialog_cellular_data_enable_title));
            dialog.setMessage(getString(R.string.dialog_cellular_data_enable_msg));
            dialog.show();
            return true;
        } else {
            AlertDialog dialog = new AlertDialog.Builder(getActivity(),
                    R.style.wifi_display_dialog)
                    .setCancelable(true)
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialog, int keyCode,
                                             KeyEvent event) {
                            if (keyCode == KeyEvent.KEYCODE_BACK
                                    && event.getAction() == KeyEvent.ACTION_DOWN) {
                                dialog.dismiss();
                            }
                            return false;
                        }
                    })
                    .setPositiveButton(R.string.dialog_button_ok,
                            new OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            }).create();
            dialog.setMessage(getString(R.string.dialog_cellular_data_wifi_hotspot_promption));
            dialog.show();
        }
        return false;
    }
    //End added by yucheng.luo for XR6699294 on 18-8-6

    //Start Connected client
    private void clearWifiApClients() {
        final Activity activity = getActivity();
        mConnectedCategory.removeAll();

        Preference connectedPreference = new Preference(activity);
        connectedPreference.setTitle(R.string.wifi_ap_no_connected);
        connectedPreference.setSelectable(false);//Begin added by yucheng.luo for XR7304195 on 18-12-29
        mConnectedCategory.addPreference(connectedPreference);

        //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
        blockClientList.clear();
        sharePreferencesCommitEditor(true);
        mBlockedCategory.removeAll();

        Preference blockedPreference = new Preference(activity);
        blockedPreference.setTitle(R.string.wifi_ap_no_blocked);
        blockedPreference.setSelectable(false);//Begin added by yucheng.luo for XR7304195 on 18-12-29
        mBlockedCategory.addPreference(blockedPreference);
      //[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
    }

    private void handleWifiApClientsChanged() {
        final Activity activity = getActivity();

        mConnectedCategory.removeAll();
        mClientList = TctWifiSettingsUtils.getHotspotConnectedList(activity);
        if (mClientList != null && mClientList.size() != 0) {
            Log.d(TAG, "client number is " + mClientList.size());
            for (String deviceAddress : mClientList) {
                TctButtonPreference preference = new TctButtonPreference(activity, deviceAddress, false,this); //[TCT-ROM][Tethering Unblock]Begin modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29
                preference.setTitle(deviceAddress);
                preference.setButtonText(getResources().getString(
                            R.string.wifi_ap_client_block_title));
                mConnectedCategory.addPreference(preference);
                Log.d(TAG, "connected client is " + deviceAddress);
            }
        }else{
            Preference preference = new Preference(activity);
            preference.setTitle(R.string.wifi_ap_no_connected);
            preference.setSelectable(false);//Begin added by yucheng.luo for XR7304195 on 18-12-29
            mConnectedCategory.addPreference(preference);
        }

        //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
        mBlockedCategory.removeAll();
        if(blockClientList != null && blockClientList.size() != 0){
            for (String blockAddress : blockClientList) {
                TctButtonPreference preference = new TctButtonPreference(activity, blockAddress, true, this);
                preference.setTitle(blockAddress);
                preference.setButtonText(getResources().getString(
                        R.string.wifi_ap_client_unblock_button));
                mBlockedCategory.addPreference(preference);
                Log.d(TAG, "blocked client is " + blockAddress);
            }

        }else{
            Preference blockedPreference = new Preference(activity);
            blockedPreference.setTitle(R.string.wifi_ap_no_blocked);
            blockedPreference.setSelectable(false);//Begin added by yucheng.luo for XR7304195 on 18-12-29
            mBlockedCategory.addPreference(blockedPreference);
            sharePreferencesCommitEditor(true);
        }
        //[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
    }

    //[TCT-ROM][Tethering Unblock]Begin modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29
    @Override
    public void onClick(View v, String deviceAddress,boolean block) {
        if (v.getId() == R.id.preference_button && deviceAddress != null) {
            if(block){
                boolean unblockSuccess = TctWifiSettingsUtils.setWifiHotspotUnblockClient(getContext(),deviceAddress);
                if(unblockSuccess){
                    blockClientList.remove(deviceAddress);
                }
            }else{

                boolean blockSuccess = TctWifiSettingsUtils.setWifiHotspotBlockClient(getContext(),deviceAddress);
                if(blockSuccess){
                    blockClientList.add(deviceAddress);
                }
            }
            //[TCT-ROM][Tethering Unblock]End modified by yucheng.luo for XR7051529 & 7054243 on 18-10-29
            handleWifiApClientsChanged();
        }
    }
    //End Connected client

    //Begin added by yucheng.luo for XR7031036 on 18-9-28
    //WPS menu
    private void handleWifiApStateChanged(int state) {
        switch (state) {
            case WifiManager.WIFI_AP_STATE_ENABLING:
                setPreferenceState(false);
                break;
            case WifiManager.WIFI_AP_STATE_ENABLED:
                setPreferenceState(true);
                //Begin added by yucheng.luo for XR7311625 on 19-1-16
                mStopTethering=false;
                //End added by yucheng.luo for XR7311625 on 19-1-16
                break;
            case WifiManager.WIFI_AP_STATE_DISABLING:
            case WifiManager.WIFI_AP_STATE_DISABLED:
                setPreferenceState(false);
                removeDialog(DIALOG_WPS_CONNECT);
                //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
                clearWifiApClients();
                //[TCT-ROM][Tethering Unblock]End added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
                break;
            default:
                break;
        }
    }

    private void setPreferenceState(boolean enabled) {
        Log.d(TAG, "setPreferenceState, enabled = " + enabled);
        if(TctWifiSettingsUtils.getPlatformInfo() != 0) {
            mWpsConnect.setEnabled(enabled);
        }
    }
    //End added by yucheng.luo for XR7031036 on 18-9-28

    //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-29
    private void sharePreferencesCommitEditor(boolean clean){
        String blockAddressList = "";
        SharedPreferences tetherSP =getActivity().getSharedPreferences(TCT_HOTSPOST_SHARED_PREFERENCES_NAME,Context.MODE_PRIVATE);
        Editor editor=tetherSP.edit();
        if(clean){
            editor.putString(TCT_HOTSPOST_SHARED_PREFERENCES_KEY,"");
        }else{
            if(blockClientList.size() != 0){
                for (String blockClientAdress : blockClientList){
                    blockAddressList = blockAddressList+blockClientAdress+ ",";
                }
                editor.putString(TCT_HOTSPOST_SHARED_PREFERENCES_KEY,blockAddressList);
            }
        }
        editor.apply();
    }

    private List<String> getHotspotBlockList(){
        List<String> hotspotBlockAddressList = new ArrayList();
        SharedPreferences tetherSP =getActivity().getSharedPreferences(TCT_HOTSPOST_SHARED_PREFERENCES_NAME,Context.MODE_PRIVATE);
        String blockAddressList = tetherSP.getString(TCT_HOTSPOST_SHARED_PREFERENCES_KEY,"");
        if(!TextUtils.isEmpty(blockAddressList)){
            String[] str = blockAddressList.split(",");
            for (String address :str){
                hotspotBlockAddressList.add(address);
            }
        }
        return hotspotBlockAddressList;
    }
    //[TCT-ROM][Tethering Unblock]Begin added by yucheng.luo for XR7051529 & 7054243 on 18-10-29

    //Begin added by junyan.wan.hz for XR 7108532 on 2018/11/16
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                final Resources res = context.getResources();
                SearchIndexableRaw data = new SearchIndexableRaw(context);
                data = new SearchIndexableRaw(context);
                data.title = res.getString(R.string.wifi_hotspot_checkbox_text);
                data.screenTitle = res.getString(R.string.wifi_hotspot_checkbox_text);
                result.add(data);
                return result;
            }
        };
    //End added by junyan.wan.hz for XR 7108532 on 2018/11/16
}
