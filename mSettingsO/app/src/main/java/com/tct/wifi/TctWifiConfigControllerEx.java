package com.tct.wifi;

import android.content.Context;
import android.net.NetworkInfo.DetailedState;
import android.net.StaticIpConfiguration;
import android.net.IpConfiguration.IpAssignment;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settingslib.wifi.AccessPoint;
import com.android.settings.wifi.WifiConfigController;
import com.android.settings.wifi.WifiConfigUiBase;
import com.tct.sdk.base.wifi.TctWifiSettingsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;

/**
 * WifiConfigController extension for tct requirement
 *
 * @author jiabin.zheng.hz
 * @Time 2018-07-31
 */
public class TctWifiConfigControllerEx {
    private static final String TAG = "TctWifiConfigControllerEx";

    // EAP SIM/AKA SIM slot selection
    private static final int WIFI_EAP_METHOD_DUAL_SIM = 2;
    private Spinner mSimSlot;

    // Add for plug in
    private Context mContext;
    private View mView;
    private WifiConfigUiBase mConfigUi;
    private WifiConfigController mController;

    private static final int SIM_UNSPECIFIED = 0;
    private final int SIM_SLOT_1 = 1;
    private final int SIM_SLOT_2 = 2;

    public TctWifiConfigControllerEx(WifiConfigController controller, WifiConfigUiBase configUi,
            View view) {
        mController = controller;
        mConfigUi = configUi;
        mContext = mConfigUi.getContext();
        mView = view;
    }

    public void addViews(WifiConfigUiBase configUi, String security) {
        ViewGroup group = (ViewGroup) mView.findViewById(R.id.info);
        // add security information
        View row = configUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(configUi.getContext().getString(
                R.string.wifi_security));
        ((TextView) row.findViewById(R.id.value)).setText(security);
        group.addView(row);
    }

    // EAP SIM/AKA sim slot config
    public void setSimSlotConfig(WifiConfiguration config, Spinner eapMethodSpinner) {

        TctWifiSettingsUtils.setSimSlot(config, -1);
        String eapMethodStr = (String) eapMethodSpinner.getSelectedItem();

        int selPos = eapMethodSpinner.getSelectedItemPosition();
        Log.d(TAG, "selected eap method:" + eapMethodStr + ", selected position:" + selPos);
        if (selPos == Eap.SIM || selPos == Eap.AKA || selPos == Eap.AKA_PRIME) {
            if (mSimSlot == null) {
                mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
            }

            if (TelephonyManager.getDefault().getPhoneCount() == WIFI_EAP_METHOD_DUAL_SIM) {
                int simSlot = mSimSlot.getSelectedItemPosition() - 1;
                if (simSlot > -1) {
                    TctWifiSettingsUtils.setSimSlot(config, simSlot);
                }
            }
            Log.d(TAG, "EAP SIM/AKA config: " + config.toString());
        }
    }

    public void setEapmethodSpinnerAdapter() {
        // set array for eap method spinner. CMCC will show only eap and sim
        Context context = mConfigUi.getContext();
        String[] eapString = context.getResources().getStringArray(R.array.wifi_eap_method);
        ArrayList<String> eapList = new ArrayList<String>(Arrays.asList(eapString));
        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                android.R.layout.simple_spinner_item, eapList);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Add for triggering onItemSelected
        Spinner eapMethodSpinner = (Spinner) mView.findViewById(R.id.method);
        eapMethodSpinner.setAdapter(adapter);
    }

    public void setEapMethodFields(boolean edit) {
        Spinner eapMethodSpinner = (Spinner) mView.findViewById(R.id.method);
        int eapMethod = eapMethodSpinner.getSelectedItemPosition();

        Log.d(TAG, "showSecurityFields modify method = " + eapMethod);
    }

    /**
     * Show EAP SIM/AKA sim slot by method.
     *
     * @param eapMethod
     *            The EAP method of AP.
     */
    public void showEapSimSlotByMethod(int eapMethod) {

        if (eapMethod == WifiConfigController.WIFI_EAP_METHOD_SIM
                || eapMethod == WifiConfigController.WIFI_EAP_METHOD_AKA
                || eapMethod == WifiConfigController.WIFI_EAP_METHOD_AKA_PRIME) {
            if (TelephonyManager.getDefault().getPhoneCount() == WIFI_EAP_METHOD_DUAL_SIM) {
                mView.findViewById(R.id.sim_slot_fields).setVisibility(View.VISIBLE);
                mSimSlot = (Spinner) mView.findViewById(R.id.sim_slot);
                Context context = mConfigUi.getContext();
                String[] tempSimAkaMethods = context.getResources()
                        .getStringArray(R.array.sim_slot);
                TelephonyManager telephonyManager = (TelephonyManager) mContext
                        .getSystemService(Context.TELEPHONY_SERVICE);
                int sum = telephonyManager.getSimCount();
                Log.d(TAG, "the num of sim slot is :" + sum);
                String[] simAkaMethods = new String[sum + 1];
                for (int i = 0; i < (sum + 1); i++) {
                    if (i < tempSimAkaMethods.length) {
                        simAkaMethods[i] = tempSimAkaMethods[i];
                    } else {
                        simAkaMethods[i] = tempSimAkaMethods[1].replaceAll("1", "" + i);
                    }
                }
                final ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                        android.R.layout.simple_spinner_item, simAkaMethods);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSimSlot.setAdapter(adapter);

                boolean isSIM1Inserted = false;
                boolean isSIM2Inserted = false;
                if (telephonyManager != null) {
                    isSIM1Inserted = telephonyManager.hasIccCard(0);
                    if (sum > 1) {
                        isSIM2Inserted =telephonyManager.hasIccCard(1);
                    }
                    Log.i(TAG, "isSIM1Inserted:"+isSIM1Inserted +", and isSIM2Inserted:"+isSIM2Inserted);
                } else {
                    Log.d(TAG, "updateSimInsertedStatus, telephonyManager is null");
                }

                if (isSIM1Inserted && !isSIM2Inserted){
                    mSimSlot.setSelection(SIM_SLOT_1);
                    mSimSlot.setEnabled(false);
                } else if (!isSIM1Inserted && isSIM2Inserted){
                    mSimSlot.setSelection(SIM_SLOT_2);
                    mSimSlot.setEnabled(false);
                } else if (!isSIM1Inserted && !isSIM2Inserted) {
                    mSimSlot.setSelection(SIM_UNSPECIFIED);
                    mSimSlot.setEnabled(false);
                } else {
                    //if the AP is saved, set the last selected sim slot of AP
                    if (mController.getAccessPoint() != null
                            && mController.getAccessPoint().isSaved()) {
                        WifiConfiguration config = getAccessPointConfig();
                        if (config != null) {
                            int simSlot = TctWifiSettingsUtils.getSimSlot(config);
                            mSimSlot.setSelection(simSlot + 1);
                        }
                    }
                    //Begin added by jiabin.zheng.hz for XRP10024331 on 2018/10/11
                    else {
                        //Set SIM_SLOT_1 by default when dual sim inserted
                        mSimSlot.setSelection(SIM_SLOT_1);
                    }
                    //End added by jiabin.zheng.hz for XRP10024331 on 2018/10/11
                }
            }
        } else {
            if (TelephonyManager.getDefault().getPhoneCount() == WIFI_EAP_METHOD_DUAL_SIM) {
                mView.findViewById(R.id.sim_slot_fields).setVisibility(View.GONE);
            }
        }
    }

    public boolean enableSubmitIfAppropriate(TextView passwordView, int accessPointSecurity,
            boolean pwInvalid) {
        boolean passwordInvalid = pwInvalid;
        if (passwordView != null) {
            if ((accessPointSecurity == AccessPoint.SECURITY_WEP && !isWepKeyValid(passwordView.getText().toString())) 
                    || (accessPointSecurity == AccessPoint.SECURITY_PSK && passwordView.length() < 8)) {
                passwordInvalid = true;
            }
        }
        return passwordInvalid;
    }

    //Begin modified by jiabin.zheng.hz for XR7223922 on 2018/12/13
    public boolean isWepKeyValid(String password) {
    //End modified by jiabin.zheng.hz for XR7223922 on 2018/12/13
        if (password == null || password.length() == 0) {
            return false;
        }
        int keyLength = password.length();
        if (((keyLength == 10 || keyLength == 26)  && password.matches("[0-9A-Fa-f]*"))
                || (keyLength == 5 || keyLength == 13)) {
            return true;
        }
        return false;
    }

    private WifiConfiguration getAccessPointConfig() {
        if (mController.getAccessPoint() != null) {
            return mController.getAccessPoint().getConfig();
        }
        return null;
    }

    public void addIpAddressRows(WifiConfiguration config, ViewGroup group) {
        final Context context = mConfigUi.getContext();
        WifiManager wifiManager =
                (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo lastInfo = wifiManager.getConnectionInfo();
        int currentConnNetworkId = -1;
        if (lastInfo != null) {
            currentConnNetworkId = lastInfo.getNetworkId();
        }

        Log.d(TAG, "currentConnNetworkId = " + currentConnNetworkId);
        Log.d(TAG, "config.networkId = " + config.networkId);
        // this ap is the current connected ap
        if (currentConnNetworkId == config.networkId) {
            String ipAddress = Utils.getWifiIpAddresses(context);
            Log.d(TAG, "the ipAddress is : " + ipAddress);
            if (ipAddress != null) {
                String[] ipAddresses = ipAddress.split("\\n");
                int ipAddressesLength = ipAddresses.length;
                //need to separate ipv4 and ipv6
                for (int i = 0; i < ipAddressesLength; i++) {
                    if (ipAddresses[i].indexOf(":") == -1) {
                        if (config.getIpAssignment() == IpAssignment.STATIC) {
                            StaticIpConfiguration staticConfig = config.getStaticIpConfiguration();
                            if (staticConfig != null && staticConfig.ipAddress != null){
                                //static ip do nothing
                            } else {
                                addRow(group, R.string.wifi_advanced_ipv4_address_title, ipAddresses[i]);
                            }
                        } else {
                            addRow(group, R.string.wifi_advanced_ipv4_address_title, ipAddresses[i]);
                        }
                    }
                }

                for (int i = ipAddressesLength; i > 0; i--) {
                    if (ipAddresses[i-1].indexOf(":") != -1) {
                        addRow(group, R.string.wifi_advanced_ipv6_address_title, ipAddresses[i-1]);
                    }
                }
            } 
        }
    }

    private void addRow(ViewGroup group, int name, String value) {
        View row = mConfigUi.getLayoutInflater().inflate(R.layout.wifi_dialog_row, group, false);
        ((TextView) row.findViewById(R.id.name)).setText(name);
        ((TextView) row.findViewById(R.id.value)).setText(value);
        group.addView(row);
    }
}
