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

import android.app.Activity;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothPan;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.UserManager;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.datausage.DataSaverBackend;
import com.android.settings.wifi.WifiApDialog;
import com.android.settings.wifi.WifiApEnabler;
import com.android.settings.wifi.tether.WifiTetherPreferenceController;
import com.android.settings.wifi.tether.WifiTetherSettings;
import com.android.settingslib.TetherUtil;


import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

import static android.net.ConnectivityManager.TETHERING_BLUETOOTH;
import static android.net.ConnectivityManager.TETHERING_USB;
import static android.net.ConnectivityManager.TETHERING_WIFI;
//add by zhiyong.song forP10025858 20181114 begin
import android.os.Message;
//add by zhiyong.song forP10025858 20181114 end
//Begin added by jiabin.zheng.hz for XR6699294 on 2018/08/03
import com.tct.wifi.TctWifiSettingsManager;
//End added by jiabin.zheng.hz for XR6699294 on 2018/08/03
/*
 * Displays preferences for Tethering.
 */
//Begin modified by yucheng.luo for XR6897886 on 18-09-5
public class TetherSettings extends RestrictedSettingsFragment
        implements DataSaverBackend.Listener{

    private static final String USB_TETHER_SETTINGS = "usb_tether_settings";
    //private static final String ENABLE_WIFI_AP = "enable_wifi_ap";
    private static final String ENABLE_BLUETOOTH_TETHERING = "enable_bluetooth_tethering";
    private static final String DATA_SAVER_FOOTER = "disabled_on_data_saver";
    //Begin added by yucheng.luo for XR6897886 on 18-9-1
    public static final String KEY_WIFI_TETHER = "wifi_tether_settings";
    private Preference mWifiTether;
    //End added by yucheng.luo for XR6897886 on 18-9-1

    //private static final int DIALOG_AP_SETTINGS = 1;

    private static final String TAG = "TetheringSettings";

    private SwitchPreference mUsbTether;

    /*    private WifiApEnabler mWifiApEnabler;
        private SwitchPreference mEnableWifiAp;*/

    private SwitchPreference mBluetoothTether;

    private BroadcastReceiver mTetherChangeReceiver;

    private String[] mUsbRegexs;

    private String[] mWifiRegexs;

    private String[] mBluetoothRegexs;
    private AtomicReference<BluetoothPan> mBluetoothPan = new AtomicReference<BluetoothPan>();

    private Handler mHandler = new Handler();
    private OnStartTetheringCallback mStartTetheringCallback;

    //private static final String WIFI_AP_SSID_AND_SECURITY = "wifi_ap_ssid_and_security";
    //private static final int CONFIG_SUBTEXT = R.string.wifi_tether_configure_subtext;

    /*private String[] mSecurityType;
    private Preference mCreateNetwork;*/

     /*private WifiApDialog mDialog;
    private WifiManager mWifiManager;
    private WifiConfiguration mWifiConfig = null;*/
    private ConnectivityManager mCm;

   /*private WifiTetherPreferenceController mWifiTetherPreferenceController;

    private boolean mRestartWifiApAfterConfigChange;*/

    private boolean mUsbConnected;
    private boolean mMassStorageActive;

    private boolean mBluetoothEnableForTether;
    private boolean mUnavailable;

    private DataSaverBackend mDataSaverBackend;
    private boolean mDataSaverEnabled;
    private Preference mDataSaverFooter;
    //add by zhiyong.song forP10025858 20181114 begin
    private boolean mUsbRndisEnable = false;
    private boolean mUsbTethering = false;
    private int mUsbRndisFlag = -1;
    //add by zhiyong.song forP10025858 20181114 begin
    //Begin added by jiabin.zheng.hz for XR6699294 on 2018/08/03
    private TctWifiSettingsManager mTctWifiMgr = null;
    //End added by jiabin.zheng.hz for XR6699294 on 2018/08/03

   /* //Begin added by yucheng.luo for XR6610449 on 18-7-26
    // start:BBSECURE_TETH_TO
    // Define a new Action which will open the MHS set up dialog
    public static final String ACTION_WIFI_AP_SETTINGS = "com.android.settings.WIFI_AP_SETTINGS";
    // end:BBSECURE_TETH_TO
    //End added by yucheng.luo for XR6610449 on 18-7-26*/
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.TETHER;
    }

    public TetherSettings() {
        super(UserManager.DISALLOW_CONFIG_TETHERING);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
       /* mWifiTetherPreferenceController =
                new WifiTetherPreferenceController(context, getLifecycle());*/
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        addPreferencesFromResource(R.xml.tether_prefs);
        mFooterPreferenceMixin.createFooterPreference()
            .setTitle(R.string.tethering_footer_info);

        mDataSaverBackend = new DataSaverBackend(getContext());
        mDataSaverEnabled = mDataSaverBackend.isDataSaverEnabled();
        mDataSaverFooter = findPreference(DATA_SAVER_FOOTER);

        //Begin added by jiabin.zheng.hz for XR6699294 on 2018/08/03
        mTctWifiMgr = TctWifiSettingsManager.getTctWifiSettingsManager(getContext());
        //End added by jiabin.zheng.hz for XR6699294 on 2018/08/03

        setIfOnlyAvailableForAdmins(true);
        if (isUiRestricted()) {
            mUnavailable = true;
            getPreferenceScreen().removeAll();
            return;
        }

        final Activity activity = getActivity();
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        //Begin modified by miaoliu for XR7107006 on 2019/1/15
        //Avoid reference leak when BT is off
        if (adapter != null && adapter.getState() == BluetoothAdapter.STATE_ON) {
            adapter.getProfileProxy(activity.getApplicationContext(), mProfileServiceListener,
                    BluetoothProfile.PAN);
        }
        //End modified by miaoliu for XR7107006 on 2019/1/15

       /* mEnableWifiAp =
                (SwitchPreference) findPreference(ENABLE_WIFI_AP);

        Preference wifiApSettings = findPreference(WIFI_AP_SSID_AND_SECURITY);*/
        mUsbTether = (SwitchPreference) findPreference(USB_TETHER_SETTINGS);
        mBluetoothTether = (SwitchPreference) findPreference(ENABLE_BLUETOOTH_TETHERING);

        mDataSaverBackend.addListener(this);

        mCm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        //mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        mUsbRegexs = mCm.getTetherableUsbRegexs();
        mWifiRegexs = mCm.getTetherableWifiRegexs();
        mBluetoothRegexs = mCm.getTetherableBluetoothRegexs();

        final boolean usbAvailable = mUsbRegexs.length != 0;
        final boolean wifiAvailable = mWifiRegexs.length != 0;
        final boolean bluetoothAvailable = mBluetoothRegexs.length != 0;

        if (!usbAvailable || Utils.isMonkeyRunning()) {
            getPreferenceScreen().removePreference(mUsbTether);
        }

        //mWifiTetherPreferenceController.displayPreference(getPreferenceScreen());
        //Begin modified by yucheng.luo for XR6897886 on 18-9-1
        //Begin modified by jiabin.zheng.hz for XR6699294 on 2018/08/03
        if (WifiTetherSettings.isTetherSettingPageEnabled() || !mTctWifiMgr.isHotspotShow()) {
        //End modified by jiabin.zheng.hz for XR6699294 on 2018/08/03
            /*removePreference(ENABLE_WIFI_AP);
            removePreference(WIFI_AP_SSID_AND_SECURITY);*/
            removePreference(KEY_WIFI_TETHER);
        } else {
            if (wifiAvailable && !Utils.isMonkeyRunning()) {
                //mWifiApEnabler = new WifiApEnabler(activity, mDataSaverBackend, mEnableWifiAp);
                initWifiTetherPreference();
            } else {
                /*getPreferenceScreen().removePreference(mEnableWifiAp);
                getPreferenceScreen().removePreference(wifiApSettings);*/
                removePreference(KEY_WIFI_TETHER);
            }
        }
        //End added by yucheng.luo for XR6897886 on 18-9-1

        if (!bluetoothAvailable) {
            getPreferenceScreen().removePreference(mBluetoothTether);
        } else {
            BluetoothPan pan = mBluetoothPan.get();
            if (pan != null && pan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
            } else {
                mBluetoothTether.setChecked(false);
            }
        }
        // Set initial state based on Data Saver mode.
        onDataSaverChanged(mDataSaverBackend.isDataSaverEnabled());
    }

    @Override
    public void onDestroy() {
        mDataSaverBackend.remListener(this);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothProfile profile = mBluetoothPan.getAndSet(null);
        if (profile != null && adapter != null) {
            adapter.closeProfileProxy(BluetoothProfile.PAN, profile);
        }

        super.onDestroy();
    }

    @Override
    public void onDataSaverChanged(boolean isDataSaving) {
        mDataSaverEnabled = isDataSaving;
        //mEnableWifiAp.setEnabled(!mDataSaverEnabled);
        //Begin modified by jiabin.zheng.hz for XR7238353 on 2018/12/25
        //mUsbTether.setEnabled(!mDataSaverEnabled);
        mUsbTether.setEnabled(!mDataSaverEnabled && mUsbConnected);
        //End modified by jiabin.zheng.hz for XR7238353 on 2018/12/25
        mBluetoothTether.setEnabled(!mDataSaverEnabled);
        mDataSaverFooter.setVisible(mDataSaverEnabled);
    }

    @Override
    public void onWhitelistStatusChanged(int uid, boolean isWhitelisted) {
    }

    @Override
    public void onBlacklistStatusChanged(int uid, boolean isBlacklisted)  {
    }

    /*private void initWifiTethering() {
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
    }*/

    /*@Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_AP_SETTINGS) {
            final Activity activity = getActivity();
            mDialog = new WifiApDialog(activity, this, mWifiConfig);
            return mDialog;
        }

        return null;
    }*/

    /*@Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_AP_SETTINGS) {
            return MetricsEvent.DIALOG_AP_SETTINGS;
        }
        return 0;
    }*/

    private class TetherChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context content, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ConnectivityManager.ACTION_TETHER_STATE_CHANGED)) {
                // TODO - this should understand the interface types
                ArrayList<String> available = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_AVAILABLE_TETHER);
                ArrayList<String> active = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ACTIVE_TETHER);
                ArrayList<String> errored = intent.getStringArrayListExtra(
                        ConnectivityManager.EXTRA_ERRORED_TETHER);
                updateState(available.toArray(new String[available.size()]),
                        active.toArray(new String[active.size()]),
                        errored.toArray(new String[errored.size()]));
                /*if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_DISABLED
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
                }*/
            } else if (action.equals(Intent.ACTION_MEDIA_SHARED)) {
                mMassStorageActive = true;
                updateState();
            } else if (action.equals(Intent.ACTION_MEDIA_UNSHARED)) {
                mMassStorageActive = false;
                updateState();
            } else if (action.equals(UsbManager.ACTION_USB_STATE)) {
                mUsbConnected = intent.getBooleanExtra(UsbManager.USB_CONNECTED, false);
                //add by zhiyong.song forP10025858 20181114 begin
                if(!mUsbConnected){
                    mUsbRndisFlag = -1;
                }
                mUsbRndisEnable = intent.getBooleanExtra(UsbManager.USB_FUNCTION_RNDIS, false);
                //add by zhiyong.song forP10025858 20181114 end
                updateState();
            } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                if (mBluetoothEnableForTether) {
                    switch (intent
                            .getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        case BluetoothAdapter.STATE_ON:
                            //Begin added by taoyang.hz for XR7438108 on 2019/02/13
                            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                            if (adapter != null) {
                                adapter.getProfileProxy(content, mProfileServiceListener,
                                        BluetoothProfile.PAN);
                            }
                            //End added by taoyang.hz for XR7438108 on 2019/02/13
                            startTethering(TETHERING_BLUETOOTH);
                            mBluetoothEnableForTether = false;
                            break;

                        case BluetoothAdapter.STATE_OFF:
                        case BluetoothAdapter.ERROR:
                            mBluetoothEnableForTether = false;
                            break;

                        default:
                            // ignore transition states
                    }
                }
                updateState();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if (mUnavailable) {
            if (!isUiRestrictedByOnlyAdmin()) {
                getEmptyTextView().setText(R.string.tethering_settings_not_available);
            }
            getPreferenceScreen().removeAll();
            return;
        }

        final Activity activity = getActivity();

        mStartTetheringCallback = new OnStartTetheringCallback(this);

        mMassStorageActive = Environment.MEDIA_SHARED.equals(Environment.getExternalStorageState());
        mTetherChangeReceiver = new TetherChangeReceiver();
        IntentFilter filter = new IntentFilter(ConnectivityManager.ACTION_TETHER_STATE_CHANGED);
        //filter.addAction(WifiManager.WIFI_AP_STATE_CHANGED_ACTION);
        Intent intent = activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_STATE);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNSHARED);
        filter.addDataScheme("file");
        activity.registerReceiver(mTetherChangeReceiver, filter);

        filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        activity.registerReceiver(mTetherChangeReceiver, filter);

        if (intent != null) mTetherChangeReceiver.onReceive(activity, intent);
       /* if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(this);
            mWifiApEnabler.resume();
        }*/

        updateState();
       /* //Begin added by yucheng.luo for XR6610449 on 18-7-26
        // start:BBSECURE_TETH_TO
        String action = activity.getIntent().getAction();
        if (action != null) {
            if (ACTION_WIFI_AP_SETTINGS.equals(action)) {
                showDialog(DIALOG_AP_SETTINGS);
            }
        }
        // end:BBSECURE_TETH_TO
        //End added by yucheng.luo for XR6610449 on 18-7-26*/
    }

    @Override
    public void onStop() {
        super.onStop();
        //add by zhiyong.song forP10025858 20181114 begin
        if(!mUsbRndisEnable && mUsbRndisFlag ==0 ){
            mUsbRndisFlag = -1;
        }
        //add by zhiyong.song forP10025858 20181114 end
        if (mUnavailable) {
            return;
        }
        getActivity().unregisterReceiver(mTetherChangeReceiver);
        mTetherChangeReceiver = null;
        mStartTetheringCallback = null;
       /* if (mWifiApEnabler != null) {
            mEnableWifiAp.setOnPreferenceChangeListener(null);
            mWifiApEnabler.pause();
        }*/
    }

    private void updateState() {
        String[] available = mCm.getTetherableIfaces();
        String[] tethered = mCm.getTetheredIfaces();
        String[] errored = mCm.getTetheringErroredIfaces();
        updateState(available, tethered, errored);
    }

    private void updateState(String[] available, String[] tethered,
            String[] errored) {
        updateUsbState(available, tethered, errored);
        updateBluetoothState();
    }


    private void updateUsbState(String[] available, String[] tethered,
            String[] errored) {
        boolean usbAvailable = mUsbConnected && !mMassStorageActive;
        int usbError = ConnectivityManager.TETHER_ERROR_NO_ERROR;
        for (String s : available) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) {
                    if (usbError == ConnectivityManager.TETHER_ERROR_NO_ERROR) {
                        usbError = mCm.getLastTetherError(s);
                    }
                }
            }
        }
        boolean usbTethered = false;
        for (String s : tethered) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbTethered = true;
            }
        }
        boolean usbErrored = false;
        for (String s: errored) {
            for (String regex : mUsbRegexs) {
                if (s.matches(regex)) usbErrored = true;
            }
        }

        if (usbTethered) {
            
            mUsbTether.setEnabled(!mDataSaverEnabled);
            mUsbTether.setChecked(true);
            //add by zhiyong.song forP10025858 20181114 begin
            mUsbTethering = false;
            //add by zhiyong.song forP10025858 20181114 end
        } else if (usbAvailable) {
            //add by zhiyong.song forP10025858 20181114 begin
            if(mUsbRndisFlag == -1){
                mUsbTether.setEnabled(!mDataSaverEnabled);
            }else{
                if(!mUsbRndisEnable && mUsbRndisFlag == 0){
                    mUsbTetherHandler.sendEmptyMessageDelayed(0,800);
                }
            }
            if (mDataSaverEnabled || !mUsbTethering) {
                mUsbTether.setChecked(false);
                
            }
            //add by zhiyong.song forP10025858 20181114 end
           // mUsbTether.setEnabled(!mDataSaverEnabled);
           // mUsbTether.setChecked(false);

        } else {
            mUsbTether.setEnabled(false);
            mUsbTether.setChecked(false);
            //add by zhiyong.song forP10025858 20181114 begin
            mUsbTethering = false;
            //add by zhiyong.song forP10025858 20181114 end
        }
    }

//add by zhiyong.song forP10025858 20181114 begin
    private Handler mUsbTetherHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            mUsbTether.setEnabled(!mDataSaverEnabled);
        }
    };
//add by zhiyong.song forP10025858 20181114 end
    private void updateBluetoothState() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        int btState = adapter.getState();
        if (btState == BluetoothAdapter.STATE_TURNING_OFF) {
            mBluetoothTether.setEnabled(false);
        } else if (btState == BluetoothAdapter.STATE_TURNING_ON) {
            mBluetoothTether.setEnabled(false);
        } else {
            BluetoothPan bluetoothPan = mBluetoothPan.get();
            if (btState == BluetoothAdapter.STATE_ON && bluetoothPan != null
                    && bluetoothPan.isTetheringOn()) {
                mBluetoothTether.setChecked(true);
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
                //[TCT-ROM][BUGFIX]Begin added by yucheng.luo for P10024520 on 18-10-17
                mBluetoothTether.setSummary(R.string.bluetooth_tethering_subtext);
                //[TCT-ROM][BUGFIX]End added by yucheng.luo for P10024520 on 18-10-17
            } else {
                mBluetoothTether.setEnabled(!mDataSaverEnabled);
                mBluetoothTether.setChecked(false);
                //[TCT-ROM][BUGFIX]Begin added by yucheng.luo for P10024520  on 18-10-17
                 mBluetoothTether.setSummary(R.string.bluetooth_tethering_off_subtext);
                //[TCT-ROM][BUGFIX]End added by yucheng.luo for P10024520 on 18-10-17
            }
        }
    }

  /*  @Override
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
            }
            //End modified by yucheng.luo for XR6699294 on 18-8-6
        } else {
            mCm.stopTethering(TETHERING_WIFI);
        }
        return false;
    }
*/
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
        if (choice == TETHERING_BLUETOOTH) {
            // Turn on Bluetooth first.
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter.getState() == BluetoothAdapter.STATE_OFF) {
                mBluetoothEnableForTether = true;
                adapter.enable();
                mBluetoothTether.setSummary(R.string.bluetooth_turning_on);
                mBluetoothTether.setEnabled(false);
                return;
            }
        }

        mCm.startTethering(choice, true, mStartTetheringCallback, mHandler);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mUsbTether) {
            if (mUsbTether.isChecked()) {
                //Begin added by jiabin.zheng.hz for XR6699294 on 2018/08/03
                if (mTctWifiMgr.isRestraintNetwork()) {
                    if (mUsbTether.isChecked()) {
                        if (!mTctWifiMgr.isDataEnabled() && !mTctWifiMgr.isWifiConnected()) {
                            //Begin modified by chenglong.cai for XR7133785 on 2018/11/17
                            mTctWifiMgr.createConnectNetworkDialog(getContext());
                            //End modified by chenglong.cai for XR7133785 on 2018/11/17
                            mUsbTether.setChecked(false);
                            return false;
                        }
                    }
                }
                //End added by jiabin.zheng.hz for XR6699294 on 2018/08/03
                startTethering(TETHERING_USB);
                //add by zhiyong.song forP10025858 20181114 begin
                mUsbTethering = true;
                //add by zhiyong.song forP10025858 20181114 end
            } else {
                mCm.stopTethering(TETHERING_USB);
            }
        } else if (preference == mBluetoothTether) {
            if (mBluetoothTether.isChecked()) {
                startTethering(TETHERING_BLUETOOTH);
            } else {
                mCm.stopTethering(TETHERING_BLUETOOTH);
                // No ACTION_TETHER_STATE_CHANGED is fired or bluetooth unless a device is
                // connected. Need to update state manually.
                updateState();
            }
        }
       /* else if (preference == mCreateNetwork) {
            showDialog(DIALOG_AP_SETTINGS);
        }*/

        return super.onPreferenceTreeClick(preference);
    }

    /*public void onClick(DialogInterface dialogInterface, int button) {
        if (button == DialogInterface.BUTTON_POSITIVE) {
            mWifiConfig = mDialog.getConfig();
            if (mWifiConfig != null) {
                *//**
                 * if soft AP is stopped, bring up
                 * else restart with new config
                 * TODO: update config on a running access point when framework support is added
                 *//*
                if (mWifiManager.getWifiApState() == WifiManager.WIFI_AP_STATE_ENABLED) {
                    Log.d("TetheringSettings",
                            "Wifi AP config changed while enabled, stop and restart");
                    mRestartWifiApAfterConfigChange = true;
                    mCm.stopTethering(TETHERING_WIFI);
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
    }*/

    @Override
    public int getHelpResource() {
        return R.string.help_url_tether;
    }

    private BluetoothProfile.ServiceListener mProfileServiceListener =
            new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            mBluetoothPan.set((BluetoothPan) proxy);
        }
        public void onServiceDisconnected(int profile) {
            //Begin added by miaoliu for XR7107006 on 2019/1/15
             //Avoid reference leak when BT is off
            BluetoothPan pan = mBluetoothPan.get();
            if (pan != null) {
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter != null) {
                    adapter.closeProfileProxy(BluetoothProfile.PAN, pan);
                }
                pan = null;
            }
            //End added by miaoliu for XR7107006 on 2019/1/15
            mBluetoothPan.set(null);
        }
    };

    private static final class OnStartTetheringCallback extends
            ConnectivityManager.OnStartTetheringCallback {
        final WeakReference<TetherSettings> mTetherSettings;

        OnStartTetheringCallback(TetherSettings settings) {
            mTetherSettings = new WeakReference<TetherSettings>(settings);
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
            TetherSettings settings = mTetherSettings.get();
            if (settings != null) {
                settings.updateState();
            }
        }
    }

  /*  //Begin added by yucheng.luo for XR6699294 on 18-8-6
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
    //End added by yucheng.luo for XR6699294 on 18-8-6*/

   //Begin added by yucheng.luo for XR6897886 on 18-9-1
   private void initWifiTetherPreference() {
       // create wifi hotspot preference
       mWifiTether =findPreference(KEY_WIFI_TETHER);
       getPreferenceScreen().addPreference(mWifiTether);
   }
   //End added by yucheng.luo for XR6897886 on 18-9-1
}
//Begin modified by yucheng.luo for XR6897886 on 18-09-5

