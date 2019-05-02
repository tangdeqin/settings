/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.wifi;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiConfiguration.AuthAlgorithm;
import android.net.wifi.WifiConfiguration.KeyMgmt;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;

import java.nio.charset.Charset;
//Begin added by yucheng.luo for XR6610449 on 18-7-26
// start:BBSECURE_TETH_TO
import android.provider.Settings;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.tct.sdk.base.wifi.TctWifiSettingsUtils;
// end:BBSECURE_TETH_TO
//End added by yucheng.luo for XR6610449 on 18-7-26

//Begin added by yucheng.luo for XR6699573 on 18-8-3
// start:Max connect
import java.util.ArrayList;
//end:Max connect
//End added by yucheng.luo for XR6699573 on 18-8-3

/**
 * Dialog to configure the SSID and security settings
 * for Access Point operation
 */
//Begin modified by yucheng.luo for XR6610449 on 18-7-26
// start:BBSECURE_TETH_TO
public class WifiApDialog extends AlertDialog implements View.OnClickListener,
        TextWatcher, AdapterView.OnItemSelectedListener, CompoundButton.OnCheckedChangeListener{
// end:BBSECURE_TETH_TO
//End modified by yucheng.luo for XR6610449 on 18-7-26

    static final int BUTTON_SUBMIT = DialogInterface.BUTTON_POSITIVE;

    private final DialogInterface.OnClickListener mListener;

    public static final int OPEN_INDEX = 0;
    public static final int WPA2_INDEX = 1;

    private View mView;
   //Begin modified by yucheng.luo for XR7097561 on 18-11-3
    private EditText mSsid;
   //End modified by yucheng.luo for XR7097561 on 18-11-3
    private int mSecurityTypeIndex = OPEN_INDEX;
    private EditText mPassword;
    private int mBandIndex = OPEN_INDEX;

    WifiConfiguration mWifiConfig;
    WifiManager mWifiManager;
    private Context mContext;

    //Begin added by yucheng.luo for XR6610449 on 18-7-26
    // start:BBSECURE_TETH_TO
    // MHS inactivity timeout options
    //Begin modified by yucheng.luo for XR6769635 on 18-8-18
    private static final int TIMEOUT_90SEC_INDEX = 0;
    private static final int TIMEOUT_5MINS_INDEX = 1;
    private static final int TIMEOUT_10MINS_INDEX = 2;
    private static final int TIMEOUT_15MINS_INDEX = 3;
    private static final int TIMEOUT_20MINS_INDEX = 4;
    private static final int TIMEOUT_30MINS_INDEX = 5;
    private static final int TIMEOUT_60MINS_INDEX = 6;
    //End modified by yucheng.luo for XR6769635 on 18-8-18

    private boolean mTimeoutEnabled = true;
    private int mTimeoutIndex = TIMEOUT_5MINS_INDEX;

    private Switch mTimeoutSwitch;
    private Spinner mTimeoutOptions;
    private TextView mTimeoutDescription;
    private int mInactivityTimeout;
    // end:BBSECURE_TETH_TO
    //End added by yucheng.luo for XR6610449 on 18-7-26

    //Begin added by yucheng.luo for XR6699573 on 18-8-3
    // start:Max connect
    private int mUserNum = 8;
    private Spinner mMaxConnSpinner;
    private int mMaxUserNumIndex = 0;
    // end:Max connect
    //End added by yucheng.luo for XR6699573 on 18-8-3

    //Begin added by yucheng.luo for XR6769635 on 18-8-18
    boolean isVFRequire = false;
    //Begin added by yucheng.luo for XR6769635 on 18-8-18

    private static final String TAG = "WifiApDialog";

    public WifiApDialog(Context context, DialogInterface.OnClickListener listener,
            WifiConfiguration wifiConfig) {
        super(context);
        mListener = listener;
        mWifiConfig = wifiConfig;
        if (wifiConfig != null) {
            mSecurityTypeIndex = getSecurityTypeIndex(wifiConfig);
        }
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        mContext =  context;
        //Begin added by yucheng.luo for XR6610449 on 18-7-26
        // start:BBSECURE_TETH_TO
        initHotspotTimeoutValue();
        // end:BBSECURE_TETH_TO
        //End added by yucheng.luo for XR6610449 on 18-7-26
    }

    public static int getSecurityTypeIndex(WifiConfiguration wifiConfig) {
        if (wifiConfig.allowedKeyManagement.get(KeyMgmt.WPA2_PSK)) {
            return WPA2_INDEX;
        }
        return OPEN_INDEX;
    }

    public WifiConfiguration getConfig() {

        WifiConfiguration config = new WifiConfiguration();

        /**
         * TODO: SSID in WifiConfiguration for soft ap
         * is being stored as a raw string without quotes.
         * This is not the case on the client side. We need to
         * make things consistent and clean it up
         */
        config.SSID = mSsid.getText().toString();

        config.apBand = mBandIndex;

        switch (mSecurityTypeIndex) {
            case OPEN_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.NONE);
                return config;

            case WPA2_INDEX:
                config.allowedKeyManagement.set(KeyMgmt.WPA2_PSK);
                config.allowedAuthAlgorithms.set(AuthAlgorithm.OPEN);
                if (mPassword.length() != 0) {
                    String password = mPassword.getText().toString();
                    config.preSharedKey = password;
                }
                return config;
        }
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean mInit = true;
        mView = getLayoutInflater().inflate(R.layout.wifi_ap_dialog, null);
        Spinner mSecurity = ((Spinner) mView.findViewById(R.id.security));
        final Spinner mChannel = (Spinner) mView.findViewById(R.id.choose_channel);

        //Begin added by taoyang.hz for XR7113541 on 2018/11/24
        if (mWifiManager != null && !mWifiManager.isDualBandSupported()) {
            mView.findViewById(R.id.band_fields).setVisibility(View.GONE);
        }
        //End added by taoyang.hz for XR7113541 on 2018/11/24
        setView(mView);
        setInverseBackgroundForced(true);

        Context context = getContext();

        setTitle(R.string.wifi_tether_configure_ap_text);
        mView.findViewById(R.id.type).setVisibility(View.VISIBLE);
        mSsid = (EditText) mView.findViewById(R.id.ssid);//Begin modified by yucheng.luo for XR7097561 on 18-11-3
        mPassword = (EditText) mView.findViewById(R.id.password);

        ArrayAdapter <CharSequence> channelAdapter;
        String countryCode = mWifiManager.getCountryCode();
        if (!mWifiManager.isDualBandSupported() || countryCode == null) {
            //If no country code, 5GHz AP is forbidden
            Log.i(TAG,(!mWifiManager.isDualBandSupported() ? "Device do not support 5GHz " :"") 
                    + (countryCode == null ? " NO country code" :"") +  " forbid 5GHz");
            channelAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_ap_band_config_2G_only, android.R.layout.simple_spinner_item);
            mWifiConfig.apBand = 0;
        } else {
            channelAdapter = ArrayAdapter.createFromResource(mContext,
                    R.array.wifi_ap_band_config_full, android.R.layout.simple_spinner_item);
        }

        channelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        setButton(BUTTON_SUBMIT, context.getString(R.string.wifi_save), mListener);
        setButton(DialogInterface.BUTTON_NEGATIVE,
        context.getString(R.string.wifi_cancel), mListener);

        if (mWifiConfig != null) {
            mSsid.setText(mWifiConfig.SSID);
            mSsid.setSelection(mWifiConfig.SSID.length());//Begin added by yucheng.luo for XR7097561 on 18-11-3
            if (mWifiConfig.apBand == 0) {
               mBandIndex = 0;
            } else {
               mBandIndex = 1;
            }

            mSecurity.setSelection(mSecurityTypeIndex);
            if (mSecurityTypeIndex == WPA2_INDEX) {
                mPassword.setText(mWifiConfig.preSharedKey);
                mPassword.setSelection(mWifiConfig.preSharedKey.length());//Begin added by yucheng.luo for XR7097561 on 18-11-3
            }
        }

        mChannel.setAdapter(channelAdapter);
        mChannel.setOnItemSelectedListener(
                new AdapterView.OnItemSelectedListener() {
                    boolean mInit = true;
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int position,
                                               long id) {
                        if (!mInit) {
                            mBandIndex = position;
                            mWifiConfig.apBand = mBandIndex;
                            Log.i(TAG, "config on channelIndex : " + mBandIndex + " Band: " +
                                    mWifiConfig.apBand);
                        } else {
                            mInit = false;
                            mChannel.setSelection(mBandIndex);
                        }

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                }
        );

        mSsid.addTextChangedListener(this);
        mPassword.addTextChangedListener(this);
        ((CheckBox) mView.findViewById(R.id.show_password)).setOnClickListener(this);
        mSecurity.setOnItemSelectedListener(this);
        //Begin modified by yucheng.luo for XR6897886 on 18-8-27
       //Begin modified by yucheng.luo for XR6952957 on 18-9-4
        if(TctWifiSettingsUtils.getPlatformInfo() != 0){
       //End modified by yucheng.luo for XR6952957 on 18-9-4
            //Begin added by yucheng.luo for XR6610449 on 18-7-26
            initHotspotTimeoutView();
            //End added by yucheng.luo for XR6610449 on 18-7-26

            //Begin added by yucheng.luo for XR6699573 on 18-8-3
            initHotspotMaxClientView();
            //End added by yucheng.luo for XR6699573 on 18-8-3
        }else{
            mView.findViewById(R.id.fields_max_client).setVisibility(View.GONE);
            mView.findViewById(R.id.fields_timeout).setVisibility(View.GONE);
        }
        //End modified by yucheng.luo for XR6897886 on 18-8-27
        super.onCreate(savedInstanceState);

        showSecurityFields();
        validate();
    }
    
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT |
                (((CheckBox) mView.findViewById(R.id.show_password)).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
    }

    private void validate() {
        String mSsidString = mSsid.getText().toString();
        if ((mSsid != null && mSsid.length() == 0)
                || ((mSecurityTypeIndex == WPA2_INDEX) && mPassword.length() < 8)
                || (mSsid != null &&
                Charset.forName("UTF-8").encode(mSsidString).limit() > 32)) {
            getButton(BUTTON_SUBMIT).setEnabled(false);
        } else {
            getButton(BUTTON_SUBMIT).setEnabled(true);
        }
    }

    public void onClick(View view) {
        mPassword.setInputType(
                InputType.TYPE_CLASS_TEXT | (((CheckBox) view).isChecked() ?
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD :
                InputType.TYPE_TEXT_VARIATION_PASSWORD));
        //Begin added by junyan.wan.hz for XR 7108439 on 2018/11/16
        mPassword.setSelection(mPassword.getText().length());
        //End added by junyan.wan.hz for XR 7108439 on 2018/11/16
    }

    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    public void afterTextChanged(Editable editable) {
        validate();
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

     //Begin modified by yucheng.luo for XR6610449 on 18-7-26
        // start:BBSECURE_TETH_TO
        /*
        mSecurityTypeIndex = position;
        showSecurityFields();
        validate();
        */
        if (parent.getId() == R.id.security) {
            mSecurityTypeIndex = position;
            showSecurityFields();
            validate();
        } else if (parent.getId() == R.id.inactivity_timeout_options) {
            mTimeoutIndex = position;
        //Begin modeified by yucheng.luo for XR6699573 on 18-8-3
            // start:Max connect
        }else if(parent.getId() == R.id.max_connection_num){
            mMaxUserNumIndex = position;
        }
        //end:Max connect
        //End modeified by yucheng.luo for XR6699573 on 18-8-3
        // end:BBSECURE_TETH_TO
     //End modified by yucheng.luo for XR6610449 on 18-7-26
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    private void showSecurityFields() {
        if (mSecurityTypeIndex == OPEN_INDEX) {
            mView.findViewById(R.id.fields).setVisibility(View.GONE);
            return;
        }
        mView.findViewById(R.id.fields).setVisibility(View.VISIBLE);
    }

    //Begin added by yucheng.luo for XR6610449 on 18-7-26
    // start:BBSECURE_TETH_TO
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        mTimeoutEnabled = isChecked;
        if (isChecked) {
            mTimeoutOptions.setVisibility(View.VISIBLE);
            mTimeoutDescription.setVisibility(View.VISIBLE);
            mTimeoutOptions.setSelection(mTimeoutIndex);
        } else {
            mTimeoutOptions.setVisibility(View.GONE);
            mTimeoutDescription.setVisibility(View.GONE);
        }
    }

    private void initHotspotTimeoutView(){
        mTimeoutSwitch = (Switch) mView.findViewById(R.id.inactivity_timeout);
        mTimeoutOptions = (Spinner) mView.findViewById(R.id.inactivity_timeout_options);
        mTimeoutDescription = (TextView) mView.findViewById(R.id.inactivity_timeout_description);
        mTimeoutSwitch.setChecked(mTimeoutEnabled);
        onCheckedChanged(mTimeoutSwitch, mTimeoutEnabled);

        //Begin modified by yucheng.luo for XR6769635 on 18-8-18
        ArrayAdapter<CharSequence> timeOutAdapter;
        String[] orginalStr = mContext.getResources().getStringArray(
                R.array.wifi_hotspot_inactivity_timeout_options);
        ArrayList<String> timeValueShow = new ArrayList<String>();
        if(isVFRequire){
            for (int i = 0; i < orginalStr.length; i++) {
                timeValueShow.add(orginalStr[i]);
            }
        }else{
            for (int i = 1; i < orginalStr.length; i++) {
                timeValueShow.add(orginalStr[i]);
            }
        }

        timeOutAdapter = new ArrayAdapter(mContext,
                android.R.layout.simple_spinner_item, timeValueShow);
        timeOutAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mTimeoutOptions.setAdapter(timeOutAdapter);
        //End modified by yucheng.luo for XR6769635 on 18-8-18
        
        mTimeoutOptions.setSelection(mTimeoutIndex);
        mTimeoutSwitch.setOnCheckedChangeListener(this);
        mTimeoutOptions.setOnItemSelectedListener(this);
    }

    private void initHotspotTimeoutValue(){
        //Begin added by yucheng.luo for XR6769635 on 18-8-18
        //[TCT ROM]Begin modified by yucheng.luo for P10024215 on 18-10-9
        isVFRequire = TctWifiSettingsUtils.isVfTimeoutEntries(mContext);
        //[TCT ROM]End modified by yucheng.luo for P10024215 on 18-10-9
        //Begin added by yucheng.luo for XR6769635 on 18-8-18
        mInactivityTimeout = TctWifiSettingsUtils.getHotspotInactivityTimeout(mContext);
        if (mInactivityTimeout > 0) {
            mTimeoutEnabled = true;
        } else {
            mTimeoutEnabled = false;
        }
        int timeout = Math.abs(mInactivityTimeout);

        //Begin modified by yucheng.luo for XR6769635 on 18-8-18
        switch(timeout) {
            case 1:
                mTimeoutIndex = isVFRequire ? TIMEOUT_5MINS_INDEX : TIMEOUT_5MINS_INDEX - 1;
                break;
            case 2:
                mTimeoutIndex = isVFRequire ? TIMEOUT_10MINS_INDEX : TIMEOUT_10MINS_INDEX - 1;
                break;
            case 3:
                mTimeoutIndex = isVFRequire ? TIMEOUT_15MINS_INDEX : TIMEOUT_15MINS_INDEX - 1;
                break;
            case 4:
                mTimeoutIndex = isVFRequire ? TIMEOUT_20MINS_INDEX : TIMEOUT_20MINS_INDEX - 1;
                break;
            case 6:
                mTimeoutIndex =  isVFRequire ? TIMEOUT_30MINS_INDEX : TIMEOUT_30MINS_INDEX - 1;
                break;
            case 12:
                mTimeoutIndex =  isVFRequire ? TIMEOUT_60MINS_INDEX : TIMEOUT_60MINS_INDEX - 1;
                break;
            case 90:
                mTimeoutIndex = TIMEOUT_90SEC_INDEX;
                break;
            default:
                mTimeoutIndex = isVFRequire ? TIMEOUT_5MINS_INDEX :TIMEOUT_5MINS_INDEX - 1;
                break;
        }
        //End modified by yucheng.luo for XR6769635 on 18-8-18
    }


    public int getInactivityTimeoutInMins() {
        //Begin modified by yucheng.luo for XR6769635 on 18-8-18
        switch(mTimeoutIndex) {
            case 0:
                mInactivityTimeout =  isVFRequire ?   90 : TIMEOUT_5MINS_INDEX;
                break;
            case 1:
                mInactivityTimeout =  isVFRequire ? TIMEOUT_5MINS_INDEX : TIMEOUT_10MINS_INDEX;
                break;
            case 2:
                mInactivityTimeout = isVFRequire ? TIMEOUT_10MINS_INDEX : TIMEOUT_15MINS_INDEX;
                break;
            case 3:
                mInactivityTimeout = isVFRequire ? TIMEOUT_15MINS_INDEX : TIMEOUT_20MINS_INDEX;
                break;
            case 4:
                mInactivityTimeout =  isVFRequire ? TIMEOUT_20MINS_INDEX : (TIMEOUT_30MINS_INDEX + 1);
                break;
            case 5:
                mInactivityTimeout = isVFRequire ? (TIMEOUT_30MINS_INDEX + 1) : (TIMEOUT_60MINS_INDEX * 2);
                break;
            case 6:
                mInactivityTimeout = TIMEOUT_60MINS_INDEX * 2;
                break;
            default:
                mInactivityTimeout = isVFRequire ? TIMEOUT_5MINS_INDEX : 0;
                break;
        }
        //End modified by yucheng.luo for XR6769635 on 18-8-18
        /**
         * If the inactivity timeout feature is disabled, return a negative
         * number; and the absolute value of this number is used to represent
         * the previous inactivity timeout value.
         */
        if (!mTimeoutEnabled) {
            mInactivityTimeout = 0;
        }
        return mInactivityTimeout;
    }
    // end:BBSECURE_TETH_TO
    //End added by yucheng.luo for XR6610449 on 18-7-26
    
    //Begin added by yucheng.luo for XR6699573 on 18-8-3
    //start:Max connect
    private void initHotspotMaxClientView(){
        mMaxConnSpinner = (Spinner) mView.findViewById(R.id.max_connection_num);
        //Begin modified by yucheng.luo for XR7029347 on 18-9-19
        //[TCT ROM]Begin modified by yucheng.luo for P10024215 on 18-10-9
        mUserNum = TctWifiSettingsUtils.getHotspotCustomMaxClient(mContext);
        //[TCT ROM]End modified by yucheng.luo for P10024215 on 18-10-9
        //End modified by yucheng.luo for XR7029347 on 18-9-19
        ArrayAdapter<CharSequence> connAdapter;
        String[] orginalStr = mContext.getResources().getStringArray(
                R.array.wifi_ap_max_connection_entries);
        ArrayList<String> userShow = new ArrayList<String>();
        for (int i = 0; i < mUserNum; i++) {
            userShow.add(orginalStr[i]);
        }
        connAdapter = new ArrayAdapter(mContext,
                android.R.layout.simple_spinner_item, userShow);
        connAdapter
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mMaxConnSpinner.setAdapter(connAdapter);
        mMaxConnSpinner.setOnItemSelectedListener(this);
        mMaxUserNumIndex= TctWifiSettingsUtils.getHotspotMaxClientNum(mContext);
        mMaxConnSpinner.setSelection(mMaxUserNumIndex - 1);
    }

    public int getMaxUserNum(){
        return mMaxUserNumIndex+1;
    }
    //end:Max connect
    //End added by yucheng.luo for XR6699573 on 18-8-3
}
