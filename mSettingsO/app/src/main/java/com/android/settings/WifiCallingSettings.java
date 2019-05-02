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

package com.android.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Switch;
import android.widget.TextView;

import com.android.ims.ImsConfig;
import com.android.ims.ImsManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.Phone;
import com.android.settings.widget.SwitchBar;

//Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
import android.os.SystemProperties;
import android.content.SharedPreferences;
import android.view.View;
import android.view.Menu;
import android.view.Gravity;
import android.view.MenuInflater;
import android.view.LayoutInflater;
import android.app.ActionBar;
import android.os.UserHandle;
//Begin modify by chenli.gao.hz for XR6998744 on 2018/09/12
import com.tcl.plugin.ITclSdkAdapter;
import com.tcl.sdk.TclPluginManager;
//End modify by chenli.gao.hz for XR6998744 on 2018/09/12
import com.tcl.telephony.wfc.TclWifiCallingHelpDialogActivity;
import com.tcl.telephony.wfc.TclWifiCallingHelp;
//End add by chenli.gao.hz for XR6507882 on 2018/07/20
//Begin added by miaoliu for XR7239554 on 2018/12/29
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
//End added by miaoliu for XR7239554 on 2018/12/29
//Begin added by miaoliu for XR7435271 on 2019/1/31
import com.android.settings.sim.tct.SimHotSwapHandler;
import com.android.settings.sim.tct.SimHotSwapHandler.OnSimHotSwapListener;
//End added by miaoliu for XR7435271 on 2019/1/31
/**
 * "Wi-Fi Calling settings" screen.  This preference screen lets you
 * enable/disable Wi-Fi Calling and change Wi-Fi Calling mode.
 */
public class WifiCallingSettings extends SettingsPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener,
        Preference.OnPreferenceChangeListener {

    private static final String TAG = "WifiCallingSettings";

    //String keys for preference lookup
    private static final String BUTTON_WFC_MODE = "wifi_calling_mode";
    private static final String BUTTON_WFC_ROAMING_MODE = "wifi_calling_roaming_mode";
    private static final String PREFERENCE_EMERGENCY_ADDRESS = "emergency_address_key";

    private static final int REQUEST_CHECK_WFC_EMERGENCY_ADDRESS = 1;

    public static final String EXTRA_LAUNCH_CARRIER_APP = "EXTRA_LAUNCH_CARRIER_APP";

    public static final int LAUCH_APP_ACTIVATE = 0;
    public static final int LAUCH_APP_UPDATE = 1;

    //UI objects
    private SwitchBar mSwitchBar;
    private Switch mSwitch;
    private ListPreference mButtonWfcMode;
    private ListPreference mButtonWfcRoamingMode;
    private Preference mUpdateAddress;
    private TextView mEmptyView;

    private boolean mValidListener = false;
    private boolean mEditableWfcMode = true;
    private boolean mEditableWfcRoamingMode = true;
     //Begin added by miaoliu for XR7239554 on 2018/12/29
    private int[] mCallState = null;
    private PhoneStateListener[] mPhoneStateListener = null;
     private final int DEFAULT_PHONE_ID = 0;
    private int mPhoneId = DEFAULT_PHONE_ID;
    private ImsManager mImsMgr = null;
   //End added by miaoliu for XR7239554 on 2018/12/29
    private ITclSdkAdapter mTclSettingsPlugin = null; //add by chenli.gao.hz for XR6507882 on 2018/08/06
   //Begin deleted by miaoliu for XR7239554 on 2018/12/29
    // private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
    //     /*
    //      * Enable/disable controls when in/out of a call and depending on
    //      * TTY mode and TTY support over VoLTE.
    //      * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
    //      * java.lang.String)
    //      */
    //     @Override
    //     public void onCallStateChanged(int state, String incomingNumber) {
    //         final SettingsActivity activity = (SettingsActivity) getActivity();
    //         boolean isNonTtyOrTtyOnVolteEnabled = ImsManager
    //                 .isNonTtyOrTtyOnVolteEnabled(activity);
    //         final SwitchBar switchBar = activity.getSwitchBar();
    //         boolean isWfcEnabled = switchBar.getSwitch().isChecked()
    //                 && isNonTtyOrTtyOnVolteEnabled;

    //         switchBar.setEnabled((state == TelephonyManager.CALL_STATE_IDLE)
    //                 && isNonTtyOrTtyOnVolteEnabled);

    //         boolean isWfcModeEditable = true;
    //         boolean isWfcRoamingModeEditable = false;
    //         final CarrierConfigManager configManager = (CarrierConfigManager)
    //                 activity.getSystemService(Context.CARRIER_CONFIG_SERVICE);
    //         if (configManager != null) {
    //             PersistableBundle b = configManager.getConfig();
    //             if (b != null) {
    //                 isWfcModeEditable = b.getBoolean(
    //                         CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL);
    //                 isWfcRoamingModeEditable = b.getBoolean(
    //                         CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL);
    //             }
    //         }

    //         Preference pref = getPreferenceScreen().findPreference(BUTTON_WFC_MODE);
    //         if (pref != null) {
    //             pref.setEnabled(isWfcEnabled && isWfcModeEditable
    //                     && (state == TelephonyManager.CALL_STATE_IDLE));
    //         }
    //         Preference pref_roam = getPreferenceScreen().findPreference(BUTTON_WFC_ROAMING_MODE);
    //         if (pref_roam != null) {
    //             pref_roam.setEnabled(isWfcEnabled && isWfcRoamingModeEditable
    //                     && (state == TelephonyManager.CALL_STATE_IDLE));
    //         }
    //     }
    // };
   //End deleted by miaoliu for XR7239554 on 2018/12/29
    private final OnPreferenceClickListener mUpdateAddressListener =
            new OnPreferenceClickListener() {
                /*
                 * Launch carrier emergency address managemnent activity
                 */
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final Context context = getActivity();
                    Intent carrierAppIntent = getCarrierActivityIntent(context);
                    if (carrierAppIntent != null) {
                        carrierAppIntent.putExtra(EXTRA_LAUNCH_CARRIER_APP, LAUCH_APP_UPDATE);
                        startActivity(carrierAppIntent);
                    }
                    return true;
                }
    };

    private SimHotSwapHandler mSimHotSwapHandler;//Added by miaoliu for XR7435271 on 2019/1/31

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitch = mSwitchBar.getSwitch();
        mSwitchBar.show();
        //Begin modified by miaoliu for XR7201659 on 2018/12/27
        //Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
        // if (getResources().getBoolean(R.bool.def_Settings_rename_wificalling_menu)) {
        //     activity.setTitle(getResources().getString(R.string.def_Settings_wificalling_menu_list));
        // }
        if (Utils.getBoolean(activity, "def_Settings_rename_wificalling_menu", "com.tct")) {
            activity.setTitle(Utils.getString(activity, "def_Settings_wificalling_menu_list","com.tct"));
        }
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
        //End modified by miaoliu for XR7201659 on 2018/12/27
        mEmptyView = (TextView) getView().findViewById(android.R.id.empty);
        setEmptyView(mEmptyView);
        String emptyViewText = activity.getString(R.string.wifi_calling_off_explanation)
                + activity.getString(R.string.wifi_calling_off_explanation_2);
        mEmptyView.setText(emptyViewText);
        //Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
        if(mTclSettingsPlugin.isOrangePolandCC()){//modify by chenli.gao.hz for XR6507882 on 2018/08/06
            mEmptyView.setText(null);
        }
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mSwitchBar.hide();
    }

    private void showAlert(Intent intent) {
        Context context = getActivity();

        CharSequence title = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_TITLE);
        CharSequence message = intent.getCharSequenceExtra(Phone.EXTRA_KEY_ALERT_MESSAGE);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(message)
                .setTitle(title)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok, null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private IntentFilter mIntentFilter;

    private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(ImsManager.ACTION_IMS_REGISTRATION_ERROR)) {
                // If this fragment is active then we are immediately
                // showing alert on screen. There is no need to add
                // notification in this case.
                //
                // In order to communicate to ImsPhone that it should
                // not show notification, we are changing result code here.
                setResultCode(Activity.RESULT_CANCELED);

                // UX requirement is to disable WFC in case of "permanent" registration failures.
                mSwitch.setChecked(false);

                showAlert(intent);
            }
        }
    };

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.WIFI_CALLING;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.wifi_calling_settings);

        mButtonWfcMode = (ListPreference) findPreference(BUTTON_WFC_MODE);
        mButtonWfcMode.setOnPreferenceChangeListener(this);

        mButtonWfcRoamingMode = (ListPreference) findPreference(BUTTON_WFC_ROAMING_MODE);
        mButtonWfcRoamingMode.setOnPreferenceChangeListener(this);

        mUpdateAddress = (Preference) findPreference(PREFERENCE_EMERGENCY_ADDRESS);
        mUpdateAddress.setOnPreferenceClickListener(mUpdateAddressListener);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(ImsManager.ACTION_IMS_REGISTRATION_ERROR);
        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        boolean isWifiOnlySupported = true;
        if (configManager != null) {
           //Begin modfied by miaoliu for XR7239554 on 2018/12/29
            PersistableBundle b = configManager.getConfigForSubId(getSubscriptionId());
             //End modified by miaoliu for XR7239554 on 2018/12/29
            if (b != null) {
                mEditableWfcMode = b.getBoolean(CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL);
                mEditableWfcRoamingMode = b.getBoolean(
                        CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL);
                isWifiOnlySupported = b.getBoolean(
                        CarrierConfigManager.KEY_CARRIER_WFC_SUPPORTS_WIFI_ONLY_BOOL, true);
            }
        }

        if (!isWifiOnlySupported) {
            mButtonWfcMode.setEntries(R.array.wifi_calling_mode_choices_without_wifi_only);
            mButtonWfcMode.setEntryValues(R.array.wifi_calling_mode_values_without_wifi_only);
            mButtonWfcRoamingMode.setEntries(
                    R.array.wifi_calling_mode_choices_v2_without_wifi_only);
            mButtonWfcRoamingMode.setEntryValues(
                    R.array.wifi_calling_mode_values_without_wifi_only);
        }

        //Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
        mEditableWfcMode = getEditableWfcMode(mEditableWfcMode);
        mEditableWfcRoamingMode = getEditableWfcRoamingMode(mEditableWfcRoamingMode);
        customizeButtonWfcModeEntries(getActivity());
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
        //Begin add by chenli.gao.hz for XR6507882 on 2018/08/06
        mTclSettingsPlugin = TclPluginManager.getTclSdkAdapter(getActivity()); //Modify by chenli.gao.hz for XR6998744 on 2018/09/12
        //End add by chenli.gao.hz for XR6507882 on 2018/08/06
        //Begin added by miaoliu for XR7239554 on 2018/12/29
        mPhoneId = getIntent().getIntExtra("phoneId", -1);
        if(mPhoneId == -1){
          int phoneId = mTclSettingsPlugin.getMainPhoneId();
          mPhoneId = phoneId == SubscriptionManager.INVALID_PHONE_INDEX ? 0 : phoneId;
        }
        mImsMgr = ImsManager.getInstance(getActivity(), mPhoneId);
        mPhoneStateListener = new PhoneStateListener[TelephonyManager.getDefault().getPhoneCount()];
        mCallState = new int[mPhoneStateListener.length];
        //Begin added by miaoliu for XR7239554 on 2018/12/29
        //Begin added by miaoliu for XR7435271 on 2019/1/31
        // for [SIM Hot Swap] 
        mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(TAG, "onSimHotSwap, finish activity");
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
        //End added by miaoliu for XR7435271 on 2019/1/31
    }
    //Begin added by miaoliu for XR7435271 on 2019/1/31
     @Override
    public void onDestroy() {
        if (mSimHotSwapHandler != null) {
            mSimHotSwapHandler.unregisterOnSimHotSwap();
        }
        super.onDestroy();
    }
    //End added by miaoliu for XR7435271 on 2019/1/31


    @Override
    public void onResume() {
        super.onResume();

        final Context context = getActivity();
        //Begin modified by miaoliu for XR7239554 on 2018/12/29
        // NOTE: Buttons will be enabled/disabled in mPhoneStateListener
        boolean wfcEnabled = mImsMgr.isWfcEnabledByUserForSlot()
                && mImsMgr.isNonTtyOrTtyOnVolteEnabledForSlot();
        mSwitch.setChecked(wfcEnabled);
        int wfcMode = mImsMgr.getWfcModeForSlot(false);
        int wfcRoamingMode = mImsMgr.getWfcModeForSlot(true);
        mButtonWfcMode.setValue(Integer.toString(wfcMode));
        mButtonWfcRoamingMode.setValue(Integer.toString(wfcRoamingMode));
        updateButtonWfcMode(context, wfcEnabled, wfcMode, wfcRoamingMode);

        if (mImsMgr.isWfcEnabledByPlatformForSlot()) {
            registerPhoneStateListeners(context);

            mSwitchBar.addOnSwitchChangeListener(this);

            mValidListener = true;
        }
       //End modified by miaoliu for XR7239554 on 2018/12/29
        context.registerReceiver(mIntentReceiver, mIntentFilter);

        Intent intent = getActivity().getIntent();
        if (intent.getBooleanExtra(Phone.EXTRA_KEY_ALERT_SHOW, false)) {
            showAlert(intent);
        }
        //Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
        mEditableWfcMode = getEditableWfcMode(mEditableWfcMode);
        mEditableWfcRoamingMode = getEditableWfcRoamingMode(mEditableWfcRoamingMode);
        customizeButtonWfcModeEntries(context);
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
    }

    @Override
    public void onPause() {
        super.onPause();

        final Context context = getActivity();

        if (mValidListener) {
            mValidListener = false;
            //Begin modified by miaoliu for XR7239554 on 2018/12/29
            // TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            // tm.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
            unRegisterPhoneStateListeners(context);
             //End modified by miaoliu for XR7239554 on 2018/12/29
            mSwitchBar.removeOnSwitchChangeListener(this);
        }

        context.unregisterReceiver(mIntentReceiver);
    }

    /**
     * Listens to the state change of the switch.
     */
    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        final Context context = getActivity();
        Log.d(TAG, "onSwitchChanged(" + isChecked + ")");

        if (!isChecked) {
            updateWfcMode(context, false);
            return;
        }

        // Call address management activity before turning on WFC
        Intent carrierAppIntent = getCarrierActivityIntent(context);
        if (carrierAppIntent != null) {
            carrierAppIntent.putExtra(EXTRA_LAUNCH_CARRIER_APP, LAUCH_APP_ACTIVATE);
            startActivityForResult(carrierAppIntent, REQUEST_CHECK_WFC_EMERGENCY_ADDRESS);
        } else {
            updateWfcMode(context, true);
        }
    }

    /*
     * Get the Intent to launch carrier emergency address management activity.
     * Return null when no activity found.
     */
    private static Intent getCarrierActivityIntent(Context context) {
        // Retrive component name from carrirt config
        CarrierConfigManager configManager = context.getSystemService(CarrierConfigManager.class);
        if (configManager == null) return null;

        PersistableBundle bundle = configManager.getConfig();
        if (bundle == null) return null;

        String carrierApp = bundle.getString(
                CarrierConfigManager.KEY_WFC_EMERGENCY_ADDRESS_CARRIER_APP_STRING);
        if (TextUtils.isEmpty(carrierApp)) return null;

        ComponentName componentName = ComponentName.unflattenFromString(carrierApp);
        if (componentName == null) return null;

        // Build and return intent
        Intent intent = new Intent();
        intent.setComponent(componentName);
        return intent;
    }

    /*
     * Turn on/off WFC mode with ImsManager and update UI accordingly
     */
    private void updateWfcMode(Context context, boolean wfcEnabled) {
        Log.i(TAG, "updateWfcMode(" + wfcEnabled + ")");
        //Begin modified by miaoliu for XR7239554 on 2018/12/29
        //ImsManager.setWfcSetting(context, wfcEnabled);
        // int wfcMode = ImsManager.getWfcMode(context, false);
        // int wfcRoamingMode = ImsManager.getWfcMode(context, true);
        // updateButtonWfcMode(context, wfcEnabled, wfcMode, wfcRoamingMode);
        mImsMgr.setWfcSettingForSlot(wfcEnabled);

        int wfcMode = mImsMgr.getWfcModeForSlot(false);
        int wfcRoamingMode = mImsMgr.getWfcModeForSlot(true);
        updateButtonWfcMode(context, wfcEnabled, wfcMode, wfcRoamingMode);
        //End modified by miaoliu for XR7239554 on 2018/12/29
        if (wfcEnabled) {
            mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), wfcMode);
        } else {
            mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), -1);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final Context context = getActivity();

        if (requestCode == REQUEST_CHECK_WFC_EMERGENCY_ADDRESS) {
            Log.d(TAG, "WFC emergency address activity result = " + resultCode);

            if (resultCode == Activity.RESULT_OK) {
                updateWfcMode(context, true);
            }
        }
    }
//Begin modified by miaoliu for XR7239554 on 2018/12/29
    private void updateButtonWfcMode(Context context, boolean wfcEnabled,
                                     int wfcMode, int wfcRoamingMode) {
        mButtonWfcMode.setSummary(getWfcModeSummaryForSlot(context, wfcMode));
        mButtonWfcMode.setEnabled(wfcEnabled && mEditableWfcMode);
        // mButtonWfcRoamingMode.setSummary is not needed; summary is just selected value.
        mButtonWfcRoamingMode.setEnabled(wfcEnabled && mEditableWfcRoamingMode);

        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        boolean updateAddressEnabled = (getCarrierActivityIntent(context) != null);
        if (wfcEnabled) {
            if (mEditableWfcMode) {
                preferenceScreen.addPreference(mButtonWfcMode);
            } else {
                // Don't show WFC (home) preference if it's not editable.
                preferenceScreen.removePreference(mButtonWfcMode);
            }
            if (mEditableWfcRoamingMode) {
                preferenceScreen.addPreference(mButtonWfcRoamingMode);
            } else {
                // Don't show WFC roaming preference if it's not editable.
                preferenceScreen.removePreference(mButtonWfcRoamingMode);
            }
            if (updateAddressEnabled) {
                preferenceScreen.addPreference(mUpdateAddress);
            } else {
                preferenceScreen.removePreference(mUpdateAddress);
            }
        } else {
            preferenceScreen.removePreference(mButtonWfcMode);
            preferenceScreen.removePreference(mButtonWfcRoamingMode);
            preferenceScreen.removePreference(mUpdateAddress);
        }
        //Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
        updateCustomizeButtonWfcMode(context, wfcEnabled, wfcMode);
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
    }
  
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final Context context = getActivity();
        if (preference == mButtonWfcMode) {
            mButtonWfcMode.setValue((String) newValue);
            int buttonMode = Integer.valueOf((String) newValue);
            int currentWfcMode = mImsMgr.getWfcModeForSlot(false);
            if (buttonMode != currentWfcMode) {
                mImsMgr.setWfcModeForSlot(buttonMode, false);
                mButtonWfcMode.setSummary(getWfcModeSummaryForSlot(context, buttonMode));
                mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), buttonMode);
            }
            if (!mEditableWfcRoamingMode) {
                int currentWfcRoamingMode = mImsMgr.getWfcModeForSlot(true);
                if (buttonMode != currentWfcRoamingMode) {
                    mImsMgr.setWfcModeForSlot(buttonMode, true);
                    // mButtonWfcRoamingMode.setSummary is not needed; summary is selected value
                }
            }
        } else if (preference == mButtonWfcRoamingMode) {
            mButtonWfcRoamingMode.setValue((String) newValue);
            int buttonMode = Integer.valueOf((String) newValue);
            int currentMode = mImsMgr.getWfcModeForSlot(true);
            if (buttonMode != currentMode) {
                mImsMgr.setWfcModeForSlot(buttonMode, true);
                // mButtonWfcRoamingMode.setSummary is not needed; summary is just selected value.
                mMetricsFeatureProvider.action(getActivity(), getMetricsCategory(), buttonMode);
            }
        }
        return true;
    }
    //End modified by miaoliu for XR7239554 on 2018/12/29
    public static int getWfcModeSummary(Context context, int wfcMode) {
        int resId = R.string.wifi_calling_off_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
        int custResId = getCustomizeWfcModeSummary(context, resId, wfcMode);
        if(resId != custResId) {
            return custResId;
        }
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
        if (ImsManager.isWfcEnabledByUser(context)) {
            switch (wfcMode) {
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                    resId = R.string.wfc_mode_wifi_only_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                    resId = R.string.wfc_mode_cellular_preferred_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                    resId = R.string.wfc_mode_wifi_preferred_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
                    break;
                default:
                    Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
            }
        }
        return resId;
    }

    //Begin added by miaoliu for XR7239554 on 2018/12/29
    public int getWfcModeSummaryForSlot(Context context, int wfcMode) {
        int resId = R.string.wifi_calling_off_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
        int custResId = getCustomizeWfcModeSummary(context, resId, wfcMode);
        if(resId != custResId) {
            return custResId;
        }
        //End add by chenli.gao.hz for XR6507882 on 2018/07/20
        if (mImsMgr.isWfcEnabledByUserForSlot()) {//Modified by miaoliu for XR7239554 on 2018/12/29
            switch (wfcMode) {
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_ONLY:
                    resId = R.string.wfc_mode_wifi_only_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                    resId = R.string.wfc_mode_cellular_preferred_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                    resId = R.string.wfc_mode_wifi_preferred_summary;//Modified by miaoliu for XR6873798 on 2018/8/25
                    break;
                default:
                    Log.e(TAG, "Unexpected WFC mode value: " + wfcMode);
            }
        }
        return resId;
    }
   
    private int getSubscriptionId() {
        SubscriptionManager subscriptionManager = SubscriptionManager.from(getActivity());
        if (subscriptionManager == null) {
            return subscriptionManager.INVALID_SUBSCRIPTION_ID;
        }

        SubscriptionInfo subInfo = subscriptionManager.
                getActiveSubscriptionInfoForSimSlotIndex(mPhoneId);
        if (subInfo == null) {
            return subscriptionManager.INVALID_SUBSCRIPTION_ID;
        }
        return subInfo.getSubscriptionId();
    }
    private void registerPhoneStateListeners(Context context) {
        TelephonyManager tm =
                (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subMgr = SubscriptionManager.from(getActivity());
        if (tm == null || subMgr == null) {
            Log.e(TAG, "TelephonyManager or SubscriptionManager is null");
            return;
        }

        for (int i = 0; i < mPhoneStateListener.length; i++) {
            final SubscriptionInfo subInfo =
                    subMgr.getActiveSubscriptionInfoForSimSlotIndex(i);
            if (subInfo == null) {
                Log.e(TAG, "registerPhoneStateListener subInfo : " + subInfo +
                        " for phone Id: " + i);
                continue;
            }

            final int phoneId = i;
            /*
            * Enable/disable controls when in/out of a call and depending on
            * TTY mode and TTY support over VoLTE.
            * @see android.telephony.PhoneStateListener#onCallStateChanged(int,
            * java.lang.String)
            */
            mPhoneStateListener[i]  = new PhoneStateListener(subInfo.getSubscriptionId()) {
                @Override
                public void onCallStateChanged(int state, String incomingNumber) {
                    Log.d(TAG, "PhoneStateListener onCallStateChanged: state is " + state +
                            " SubId: " + mSubId);
                    final SettingsActivity activity = (SettingsActivity) getActivity();
                    if (activity == null) {
                        return;
                    }
                    boolean isNonTtyOrTtyOnVolteEnabled =
                            mImsMgr.isNonTtyOrTtyOnVolteEnabledForSlot();
                    final SwitchBar switchBar = activity.getSwitchBar();
                    boolean isWfcEnabled = switchBar.getSwitch().isChecked()
                            && isNonTtyOrTtyOnVolteEnabled;

                    mCallState[phoneId] = state;
                    switchBar.setEnabled(isCallStateIdle() && isNonTtyOrTtyOnVolteEnabled);

                    boolean isWfcModeEditable = true;
                    boolean isWfcRoamingModeEditable = false;
                    final CarrierConfigManager configManager = (CarrierConfigManager)
                            activity.getSystemService(Context.CARRIER_CONFIG_SERVICE);
                    if (configManager != null) {
                        PersistableBundle b = configManager.getConfigForSubId(getSubscriptionId());
                        if (b != null) {
                            isWfcModeEditable = b.getBoolean(
                                    CarrierConfigManager.KEY_EDITABLE_WFC_MODE_BOOL);
                            isWfcRoamingModeEditable = b.getBoolean(
                                    CarrierConfigManager.KEY_EDITABLE_WFC_ROAMING_MODE_BOOL);
                        }
                    }

                    Preference pref = getPreferenceScreen().findPreference(BUTTON_WFC_MODE);
                    if (pref != null) {
                        pref.setEnabled(isWfcEnabled && isWfcModeEditable && isCallStateIdle());
                    }
                    Preference pref_roam = getPreferenceScreen().
                            findPreference(BUTTON_WFC_ROAMING_MODE);
                    if (pref_roam != null) {
                        pref_roam.setEnabled(isWfcEnabled && isWfcRoamingModeEditable &&
                                isCallStateIdle());
                    }
                }
            };
            Log.d(TAG, "Register for call state change for phone Id: " + i);
            tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_CALL_STATE);
        }
    }

    private void unRegisterPhoneStateListeners(Context context) {
        TelephonyManager tm =
               (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneStateListener.length; i++) {
            if (mPhoneStateListener[i] != null) {
                Log.d(TAG, "unRegister for call state change for phone Id: " + i);
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }
    }
    private boolean isCallStateIdle() {
        for (int i = 0; i < mCallState.length; i++) {
            if (TelephonyManager.CALL_STATE_IDLE != mCallState[i]) {
                return false;
            }
        }
        return true;
    }
    //End added by miaoliu for XR7239554 on 2018/12/29

    //Begin add by chenli.gao.hz for XR6507882 on 2018/07/20
    private ActionBar mActionBar;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        customizeWfcSettingsActionBar();
    }

    private void customizeWfcSettingsActionBar() {
        final Context context = getActivity();
        //Begin added Orange Belgium/ROMANIA and  by cong.tao.hz for XR6176909/XR6226890 on 2018/04/26
        if(!mTclSettingsPlugin.isSupportWfcActionBar()) { //modify by chenli.gao.hz for XR6507882 on 2018/08/06
            return;
        }
        //End added Orange Belgium/ROMANIA by cong.tao.hz for XR6176909/XR6226890 on 2018/04/26

        View view =  LayoutInflater.from(context).inflate(R.layout.action_bar_wfc_settings, null);
        TextView txtHelp =  (TextView) view.findViewById(R.id.helpbutton);
        /**
         * This requirement is for Orange Poland operator
         * For PL hide the preferred selected option in WiFi Calling menu,
         * So the tips of wifi calling should be changed for PL help function
         */
        txtHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log("customizeWfcSettingsActionBar");
                if(mTclSettingsPlugin.isOrangePolandCC()) { //modify by chenli.gao.hz for XR6507882 on 2018/08/06
                    Intent intent = new Intent(context, TclWifiCallingHelp.class);
                    intent.putExtra("help_type", TclWifiCallingHelp.WFC_SETTINGS_HELP);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivityAsUser(intent, UserHandle.OWNER);
                } else {
                    Intent intent = new Intent(context, TclWifiCallingHelpDialogActivity.class);
                    intent.putExtra("dialog_type", TclWifiCallingHelpDialogActivity.WFC_HELP_BUTTON_DIALOG);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            }
        });
        mActionBar = (ActionBar)getActivity().getActionBar();
        mActionBar.setDisplayShowCustomEnabled(true);
        mActionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM,
                ActionBar.DISPLAY_SHOW_CUSTOM);
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setCustomView(view,
                new ActionBar.LayoutParams(ActionBar.LayoutParams.WRAP_CONTENT,
                        ActionBar.LayoutParams.WRAP_CONTENT, Gravity.CENTER_VERTICAL
                        | Gravity.END));
    }


    /**
     * This requirement is for Orange Spain, Orange Poland, Orange Romania operator
     * See the Orange requirement doc <07_Orange volte-vowifi UX template v2.0>
     * Enable WiFi Calling for the first time, there should has a pop-up appears
     */
    private void wfcTurnOnFirstTimesTips(Context context) {
        if (!mTclSettingsPlugin.isSupportWfcFirstTimesTips()) {//modify by chenli.gao.hz for XR6507882 on 2018/08/06
            return;
        }

        SharedPreferences pref = context.getSharedPreferences("countNum", Context.MODE_PRIVATE);
        int count = pref.getInt("count", 0);
        log("wfcTurnOnFirstTimesTips count=" + count);
        if (count == 0) {
            Intent intent = new Intent(context, TclWifiCallingHelpDialogActivity.class);
            if (mTclSettingsPlugin.isOrangePolandCC()) { //modify by chenli.gao.hz for XR6507882 on 2018/08/06
                intent.putExtra("dialog_type", TclWifiCallingHelpDialogActivity.WFC_SWITCH_FIRST_POLAND_DIALOG);
            } else {
                intent.putExtra("dialog_type", TclWifiCallingHelpDialogActivity.WFC_SWITCH_FIRST_DIALOG);
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt("count", ++count);
            editor.commit();
        }
    }
    //End add by chenli.gao.hz for XR6507882 on 2018/07/20

    //Begin modify by chenli.gao.hz for XR5870382 on 2018/07/20
    private static final int WFC_DEFAULT_OPEARTOR = -1;
    private static final int WFC_DT_OPEARTOR = 1;

    private boolean customizeButtonWfcModeEntries(Context context) {
        boolean retVal = false;
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x01) {
            TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            String mccMnc = tm.getSimOperator();
            String spn = tm.getSimOperatorName();
            log("customizeButtonWfcModeEntries mccMnc:" + mccMnc + ", spn:" + spn + ", mButtonWfcMode:" + mButtonWfcMode + ", mButtonWfcRoamingMode:" + mButtonWfcRoamingMode);
            String[] entries = context.getResources().getStringArray(R.array.wifi_calling_mode_choices_for_dt);
            String[] entriesValues = context.getResources().getStringArray(R.array.wifi_calling_mode_values_for_dt);
            if ("26201".equals(mccMnc) && ("Telekom.de".equals(spn) || "T-Mobile D".equals(spn) || "Business".equals(spn)
                    || "Privat".equals(spn) || "congstar".equals(spn) || "congstar.de".equals(spn))
                    || "26002".equals(mccMnc)&& ("T-Mobile.pl".equals(spn) || "T-Mobile.pl Q".equals(spn) || "heyah".equals(spn))
                    || "20201".equals(mccMnc)
                    || "23001".equals(mccMnc) && "T-Mobile CZ".equals(spn)) {
                if(mButtonWfcMode != null) {
                    mButtonWfcMode.setEntries(entries);
                    mButtonWfcMode.setEntryValues(entriesValues);
                }
                if(mButtonWfcRoamingMode != null) {
                    mButtonWfcRoamingMode.setEntries(entries);
                    mButtonWfcMode.setEntryValues(entriesValues);
                }
                retVal = true;
            }
        }
        return retVal;
    }

    private boolean getEditableWfcMode(boolean defaultValue){
        boolean retVal = defaultValue;
        log("chenli getEditableWfcMode opid :" + SystemProperties.getInt("RO_OPERATOR_REQ", 0x00));
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x01) {
            TelephonyManager tm = TelephonyManager.getDefault();
            String mccMnc = tm.getSimOperator();
            String spn = tm.getSimOperatorName();
            log("chenli getEditableWfcMode mccMnc:" + mccMnc + ", spn:" + spn);
            if ("26201".equals(mccMnc)) {
                if ("Telekom.de".equals(spn) || "T-Mobile D".equals(spn) || "Business".equals(spn)
                        || "Privat".equals(spn) || "congstar".equals(spn) || "congstar.de".equals(spn)) {
                    retVal = false;
                }
            } else if ("26002".equals(mccMnc) && ("T-Mobile.pl".equals(spn) || "T-Mobile.pl Q".equals(spn) || "heyah".equals(spn))
                    || "23001".equals(mccMnc) && ("T-Mobile CZ".equals(spn))
                    || "20201".equals(mccMnc)){
                retVal = true;
            }
            log("getEditableWfcMode mccMnc:" + mccMnc + ", spn:" + spn + ", retVal:" + retVal);
        }else if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x0B) { //H3G
            log("getEditableWfcMode for H3G retVal = false");
            retVal = false;
        }
        return retVal;
    }

    private boolean getEditableWfcRoamingMode(boolean defaultValue){
        boolean retVal = defaultValue;
        log("chenli getEditableWfcRoamingMode opid :" + SystemProperties.getInt("RO_OPERATOR_REQ", 0x00));
        if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x01) {
            TelephonyManager tm = TelephonyManager.getDefault();
            String mccMnc = tm.getSimOperator();
            String spn = tm.getSimOperatorName();
            if (("26201".equals(mccMnc) && ("Telekom.de".equals(spn) || "T-Mobile D".equals(spn) || "Business".equals(spn)
                    || "Privat".equals(spn) || "congstar".equals(spn) || "congstar.de".equals(spn)))
                    || "20201".equals(mccMnc)
                    || ("23001".equals(mccMnc) && "T-Mobile CZ".equals(spn))) {
                retVal = false;
            } else if ("26002".equals(mccMnc) && ("T-Mobile.pl".equals(spn) || "T-Mobile.pl Q".equals(spn) || "heyah".equals(spn))) {
                retVal = true;
            }
            log("getEditableWfcRoamingMode mccMnc:" + mccMnc + ", spn:" + spn + ", retVal:" + retVal);
            //Modify by kai.du.hz for XR6567055 on 2018/07/12 begin
        }else if (SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x0B) { //H3G
            log("getEditableWfcRoamingMode for H3G retVal = false");
            retVal = false;
        }
        //Modify by kai.du.hz for XR6567055 on 2018/07/12 end
        return retVal;
    }

    public void updateCustomizeButtonWfcMode(Context context, boolean wfcEnabled, int wfcMode){
        TelephonyManager tm = TelephonyManager.getDefault();
        String mccMnc = tm.getSimOperator();
        boolean isRoaming = tm.isNetworkRoaming();
        final PreferenceScreen prefScrn = getPreferenceScreen();
        log("updateCustomizeButtonWfcMode wfcEnabled=" + wfcEnabled + ", isRoaming=" + isRoaming);
        if(wfcEnabled) {
            if(SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x03) { //"0x03" for Orange
                wfcTurnOnFirstTimesTips(context);
                final boolean isHideWfcMode = mTclSettingsPlugin.isOrangePolandCC(); //modify by chenli.gao.hz for XR6507882 on 2018/08/06
                log("updateButtonWfcMode wfcEnabled=" + wfcEnabled + ", isHideWfcMode=" + isHideWfcMode);
                if (prefScrn.findPreference(BUTTON_WFC_MODE) != null && isHideWfcMode) {
                    prefScrn.removePreference(prefScrn.findPreference(BUTTON_WFC_MODE));
                    int wfcDefaultMode = ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED;
                    if (isHideWfcMode && wfcMode != wfcDefaultMode) {
                        ImsManager.setWfcMode(context, wfcDefaultMode);
                    }
                }
            } else {
                int wfcDefaultMode;
                final boolean isHideWfcMode = "23410".equals(mccMnc) || "23433".equals(mccMnc) || "23430".equals(mccMnc);
                if (mButtonWfcMode != null && isHideWfcMode) {
                    prefScrn.removePreference(mButtonWfcMode);
                    if ("23433".equals(mccMnc) || "23430".equals(mccMnc)) {
                        wfcDefaultMode = ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED;
                    } else {
                        wfcDefaultMode = ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED;
                    }
                    if (wfcMode != wfcDefaultMode) {
                        ImsManager.setWfcMode(context, wfcDefaultMode);
                    }
                }

                if ("26201".equals(mccMnc) || (("23001".equals(mccMnc)) && isRoaming)) {
                    if (mButtonWfcMode != null) {
                        prefScrn.removePreference(mButtonWfcMode);
                        log("updateButtonWfcMode remove BUTTON_WFC_MODE");
                    }
                }

                if ("23001".equals(mccMnc) || "26201".equals(mccMnc)) {
                    if (mButtonWfcRoamingMode != null) {
                        prefScrn.removePreference(mButtonWfcRoamingMode);
                        log("updateButtonWfcMode remove BUTTON_WFC_ROAMING_MODE");
                    }
                }
            }
        }
    }

    public static int getCustomizeWfcModeSummary(Context context, int resId, int wfcMode){
        log("getWfcModeSummary WFC mode value: " + wfcMode + ", wfc enabled:" + ImsManager.isWfcEnabledByUser(context));
        if (ImsManager.isWfcEnabledByUser(context) && WFC_DT_OPEARTOR == getSimOpeartor()) {
            switch (wfcMode) {
                case ImsConfig.WfcModeFeatureValueConstants.CELLULAR_PREFERRED:
                    resId = R.string.wfc_mode_cellular_preferred_summary_dt;
                    break;
                case ImsConfig.WfcModeFeatureValueConstants.WIFI_PREFERRED:
                    resId = R.string.wfc_mode_wifi_preferred_summary_dt;
                    break;
                default:
                    log("getWfcModeSummary Unexpected WFC mode value: " + wfcMode);
            }
        }
        return resId;
    }

    private static int getSimOpeartor(){
        int simOperator = WFC_DEFAULT_OPEARTOR;
        if(SystemProperties.getInt("RO_OPERATOR_REQ", 0x00) == 0x01) { //"0x01" For DT
            String mccMnc = TelephonyManager.getDefault().getSimOperator();
            String spn = TelephonyManager.getDefault().getSimOperatorName();
            if ("26201".equals(mccMnc)) {
                if ("Telekom.de".equals(spn) || "T-Mobile D".equals(spn) || "Business".equals(spn)
                        || "Privat".equals(spn) || "congstar".equals(spn) || "congstar.de".equals(spn)) {
                    simOperator = WFC_DT_OPEARTOR;
                }
            } else if ("26002".equals(mccMnc)) {
                if ("T-Mobile.pl".equals(spn) || "T-Mobile.pl Q".equals(spn) || "heyah".equals(spn)) {
                    simOperator =  WFC_DT_OPEARTOR;
                }
            } else if ("20201".equals(mccMnc)) {
                simOperator =  WFC_DT_OPEARTOR;
            } else if ("23001".equals(mccMnc)) {
                if ("T-Mobile CZ".equals(spn)) {
                    simOperator =  WFC_DT_OPEARTOR;
                }
            }
        }
        return simOperator;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
    //End modify by chenli.gao.hz for XR5870382 on 2018/07/20
}
