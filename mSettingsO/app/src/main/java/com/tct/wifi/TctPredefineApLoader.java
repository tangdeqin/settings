package com.tct.wifi;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.net.wifi.WifiEnterpriseConfig.Eap;
import android.net.wifi.WifiManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.security.Credentials;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import java.util.List;

import com.android.settings.R;
import com.android.internal.telephony.ITelephony;

import android.net.wifi.WifiConfiguration.KeyMgmt;
import com.android.settingslib.wifi.AccessPoint;
import android.telephony.SubscriptionManager;
import android.os.SystemProperties;
import com.tct.sdk.base.wifi.TctWifiSettingsUtils;
import android.net.wifi.WifiConfiguration.NetworkSelectionStatus;

/**
 * To load or reload AP from customization file
 *
 * @author jiabin.zheng.hz
 * @Time 2018-07-28
 */
public class TctPredefineApLoader {

    private final String TAG = "TctPredefineApLoader";
    private final String KEYSTORE_SPACE = "keystore://";
    private final String PSK = "PSK";
    private final String WEP = "WEP";
    private final String EAP = "EAP";
    private final String AP_STATE_PUBLIC = "Public";
    private static Context mContext;
    private boolean isSIMInserted = false;
    private boolean isSIM1Inserted = false;
    private boolean isSIM2Inserted = false;
    private TelephonyManager mTm;
    private WifiManager mWifiManager;
    private int highestPriority = 0;
    //Begin added by jiabin.zheng.hz for XRP10024912 on 2018/10/30
    private int mPreConfigNum = 0;
    //End added by jiabin.zheng.hz for XRP10024912 on 2018/10/30

    private static TctPredefineApLoader mPredefineApLoader;

    List<WifiConfiguration> mConfigList = null;

    TctWifiSettingsManager mTctWifiMgr = null;

    /**
     * constructor method to create instance of class PredefineApLoader.java
     * but this mthod is private so as to implement singleton model
     */
    private TctPredefineApLoader() {
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }

        if (mTm == null) {
            mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        mTctWifiMgr = TctWifiSettingsManager.getTctWifiSettingsManager(mContext);

        //Begin added by jiabin.zheng.hz for XRP10024912 on 2018/10/30
        mPreConfigNum = mContext.getResources().getInteger(com.android.settings.R.integer.def_Settings_wifi_network_exist);
        //End added by jiabin.zheng.hz for XRP10024912 on 2018/10/30
    }

    /**
     * method to return instance of class TctPredefineApLoader.java
     * 
     * @param context Context object from caller
     * @return instance of TctPredefineApLoader
     */
    public static TctPredefineApLoader getTctPredefineApLoader(Context context) {
        mContext = context.getApplicationContext();
        if (mPredefineApLoader == null) {
            mPredefineApLoader = new TctPredefineApLoader();
        }
        return mPredefineApLoader;
    }

    /**
     * modify configed networks, usually delete networks that not proper and add networks when sim card plugged in
     */
    public void loadPredefineNetworks() {
        //Begin added by jiabin.zheng.hz for XRP10024912 on 2018/10/30
        if (mPreConfigNum == 0) {
            Log.i(TAG, "There is not any configured AP");
            return;
        }
        //End added by jiabin.zheng.hz for XRP10024912 on 2018/10/30

        synchronized(this) {
            mConfigList = mWifiManager.getConfiguredNetworks();
            getCurrentSimcardState();
            deletePredefineNetworks();
            addPredefineNetworks();
        }
    }

    /**
     * delete unused networks when wifi is on or sim card status changed
     * usually, this action will be executed when one sim card has been plugged out
     */
    private void deletePredefineNetworks() {
        if (mTm == null) {
            mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (mConfigList == null || mConfigList.isEmpty()) {
            Log.d(TAG,"config is null, there's no any configured networks");
            return;
        }

        boolean hasDeleteNetwork = false;

        for (WifiConfiguration config : mConfigList) {   
            if (!TctWifiSettingsUtils.getIsPresetAP(config)) {
                // it is not define ap, no need to delete, return!
                Log.d(TAG,"it is not define ap, no need to delete, return!");
                continue;
            }
            
            String oldIdentity = config.enterpriseConfig.getIdentity();
            if (oldIdentity != null && !oldIdentity.isEmpty()) {
                Log.d(TAG,"config.SSID " + config.SSID);
                //Log.d(TAG,"config.imsi " + config.imsi);
                
                Log.d(TAG,"config.networkId " + config.networkId);

                int slot = TctWifiSettingsUtils.getSimSlot(config);
                Log.d(TAG,"config simSlot " + slot);
                // if (config.simSlot.equals("\"1\"")) {
                //     slot = 1;
                // }
                int subId = TctWifiUtil.getSubId(slot); 

                String eapMethod = TctWifiUtil.getEapMethodStr(config.enterpriseConfig.getEapMethod());

                if (!eapMethod.isEmpty()) {
                    Log.d(TAG,"telephonyEx.getSubscriberId() " + mTm.getSubscriberId(subId));
                    String sTemp = TctWifiUtil.makeNAI(mTm.getSimOperator(subId), mTm.getSubscriberId(subId), eapMethod);
                    Log.d(TAG,"makeNAI:" + sTemp);
                    if (sTemp.contains("error") && isSIMInserted) {
                        continue;
                    }

                    if (!sTemp.equals(oldIdentity)
                            || (!isSIM1Inserted && (slot == 0)) || (!isSIM2Inserted && (slot == 1))) {
                        Log.d(TAG,"this config is invalid for current sim card, so remove it");
                        mWifiManager.removeNetwork(config.networkId);
                        mWifiManager.saveConfiguration();
                        hasDeleteNetwork = true;
                        continue;
                    } 
                    Log.d(TAG,"user doesn't change or remove usim card");
                }
            }
        }

        if (hasDeleteNetwork) {
            mConfigList = mWifiManager.getConfiguredNetworks();
        }
    }

    /**
     * add networks from customization file when wifi is on or sim card status changed
     * usually, this action will be executed when one sim card has been plugged in or wifi has been on
     */
    private void addPredefineNetworks() {
        //Begin modified by jiabin.zheng.hz for XRP10024912 on 2018/10/30
        Log.d(TAG,"def_Settings_wifi_network_exist= " + mPreConfigNum);

        //load predefined networks from plf file and save wifi configuration
        //currently, support at most 3 pre-config ap
        for (int index = 0 ; index < mPreConfigNum ; index ++) {
            addPreconfigNetwork(index + 1);
        }
        //End modified by jiabin.zheng.hz for XRP10024912 on 2018/10/30
    }

    /**
     * method to judge whether sim card 1 or sim card 2 has been plugged in
     */
    public void getCurrentSimcardState() {
        if (mTm == null) {
            mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        isSIM1Inserted = mTm.getSimState(0) == TelephonyManager.SIM_STATE_READY;
        int simSum = mTm.getSimCount();
        if (simSum == 2) {
            isSIM2Inserted = mTm.getSimState(1) == TelephonyManager.SIM_STATE_READY;
        }
        isSIMInserted = isSIM1Inserted || isSIM2Inserted;
        Log.d(TAG, "isSIM1Inserted is " + isSIM1Inserted + " ,isSIM2Inserted is " + isSIM2Inserted);
    }

    private int getHighestPriority() {
        int priority = 0;
        if (mConfigList != null && !mConfigList.isEmpty()) {
            for (WifiConfiguration config : mConfigList) {
                if (!TctWifiSettingsUtils.getIsPresetAP(config) && (config.priority > priority)) {
                    priority = config.priority;
                }
            }
        }
        Log.i(TAG, "highest priority is " + priority);
        return priority;
    }

    private boolean isAlreadyLoadedEapAp(String ssid, String eapMethod, int selSlot) {
        if (mConfigList != null && !mConfigList.isEmpty()) {
            for (WifiConfiguration config : mConfigList) {
                if (config.SSID.trim().equals('"'+ssid.trim()+'"') 
                        && config.allowedKeyManagement.get(KeyMgmt.WPA_EAP)
                        && config.enterpriseConfig.getEapMethod() 
                                == TctWifiUtil.getEapMethodIndex(eapMethod)) {
                    if (TctWifiSettingsUtils.getIsPresetAP(config)) {
                        if (selSlot != TctWifiSettingsUtils.getSimSlot(config)) {
                            Log.d(TAG, "you have changed the sim slot,reload");
                            mWifiManager.forget(config.networkId, null);
                            mConfigList = mWifiManager.getConfiguredNetworks();
                            return false;
                        } else {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * add configuration from plf file
     */
    private void addPreconfigNetwork(int witchAp) {
        if (mTm == null) {
            mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        String ssid = "";
        String password = "";
        String security = "";
        String identity = "";
        String eapMethod = "";

        String mccCode = "";

        String phase2 = "";
        String caCert = "";
        String userCert = "";
        String anonymous = "";
        int ssidPriority = 0;
        String state = "";
        if (witchAp == 1) {
            ssid = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_network_ssid);
            password = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_password);
            security = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_security_mode);
            identity = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_identity);
            eapMethod = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_method);

            mccCode = mContext.getResources().getString(com.android.settings.R.string.def_Settings_preloaded_eap_sim_mcc_mnc);
            phase2 = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_phrase2_authentication);
            caCert = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_ca_certification);
            userCert = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_user_certificate);
            anonymous = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_anonymous_identity);
            ssidPriority = mContext.getResources().getInteger(com.android.settings.R.integer.def_Settings_wifi_priority);
            state = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_network_status);
        } else if (witchAp == 2) {
            ssid = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_network_ssid_2);
            password = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_password_2);
            security = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_security_mode_2);
            identity = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_identity_2);
            eapMethod = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_method_2);

            mccCode = mContext.getResources().getString(com.android.settings.R.string.def_Settings_preloaded_eap_sim_mcc_mnc_2);
            phase2 = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_phrase2_authentication_2);
            caCert = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_ca_certification_2);
            userCert = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_user_certificate_2);
            anonymous = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_anonymous_identity_2);
            ssidPriority = mContext.getResources().getInteger(com.android.settings.R.integer.def_Settings_wifi_priority_2);
            state = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_network_status_2);
        } else if (witchAp == 3) {
            ssid = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_network_ssid_3);
            password = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_password_3);
            security = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_security_mode_3);
            identity = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_identity_3);
            eapMethod = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_method_3);

            mccCode = mContext.getResources().getString(com.android.settings.R.string.def_Settings_preloaded_eap_sim_mcc_mnc_3);
            phase2 = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_phrase2_authentication_3);
            caCert = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_ca_certification_3);
            userCert = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_user_certificate_3);
            anonymous = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_eap_anonymous_identity_3);
            ssidPriority = mContext.getResources().getInteger(com.android.settings.R.integer.def_Settings_wifi_priority_3);
            state = mContext.getResources().getString(com.android.settings.R.string.def_Settings_wifi_network_status_3);
        }

        Log.i(TAG, "witchAp:" + witchAp);
        Log.i(TAG, "pre ssid:" + ssid);
        Log.i(TAG, "pre password:" + password);
        Log.i(TAG, "pre eap method:" + eapMethod);
        Log.i(TAG, "pre mccCode:" + mccCode);
        Log.i(TAG, "pre security:" + security);
        Log.i(TAG, "pre state:" + state);
        Log.i(TAG, "pre ssidPriority:" + ssidPriority);

        int selSlot = -1;
        if (security.equals("EAP") && (eapMethod.equals("SIM") || eapMethod.equals("AKA"))) {
            // For EAP or AKA authentication, if there's no sim card inserted, no need to load
            if (!isSIMInserted) {
                return;
            }
            
            selSlot = getMatchedSimSlot(mccCode);
            if (-1 == selSlot) {
                Log.i(TAG, " current inserted SIM not equal to preloaded SIM on MCC_MNC or no sim card inserted");
                return;
            }

            Log.d(TAG, "selSlot::" + selSlot);
            if (isAlreadyLoadedEapAp(ssid, eapMethod, selSlot)) {
                Log.i(TAG, " current preloaded ap is alread existing");
                return;
            }
        }

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = AccessPoint.convertToQuotedString(ssid);
        TctWifiSettingsUtils.setIsPresetAP(config, true);

        if (!TextUtils.isEmpty(security) && (security.equals(PSK))) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
            if (!TextUtils.isEmpty(password)) {
                if (password.matches("[0-9A-Fa-f]{64}")) {
                    config.preSharedKey = password;
                } else {
                    config.preSharedKey = '"' + password + '"';
                }
            }
        } else if (!TextUtils.isEmpty(security) && (security.equals(WEP))) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            if (!TextUtils.isEmpty(password)) {
                int length = password.length();
                // WEP-40, WEP-104, and 256-bit WEP (WEP-232?)
                if ((length == 10 || length == 26 || length == 58) && password.matches("[0-9A-Fa-f]*")) {
                    config.wepKeys[0] = password;
                } else {
                    config.wepKeys[0] = '"' + password + '"';
                }
            }
        } else if (!TextUtils.isEmpty(security) && (security.equals(EAP))) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.IEEE8021X);
            if (password.length() != 0) {
                config.enterpriseConfig.setPassword(password);
            }

            int eapMethodIndex = TctWifiUtil.getEapMethodIndex(eapMethod);
            if (eapMethodIndex != -1) {
                config.enterpriseConfig.setEapMethod(eapMethodIndex);
            }

            int phase2MethodIndex = TctWifiUtil.getPhase2MethodIndex(phase2);
            if (phase2MethodIndex != -1) {
                config.enterpriseConfig.setPhase2Method(phase2MethodIndex);
            }

            config.enterpriseConfig.setIdentity(identity);

            config.enterpriseConfig.setCaCertificateAlias((caCert.equals("")) ? "" :
                        KEYSTORE_SPACE + Credentials.CA_CERTIFICATE + caCert);
            config.enterpriseConfig.setClientCertificateAlias((userCert.equals("")) ? "" :
                        KEYSTORE_SPACE + Credentials.USER_CERTIFICATE + userCert);
            config.enterpriseConfig.setClientCertificateAlias((userCert.equals("")) ? "" :
                        KEYSTORE_SPACE + Credentials.USER_PRIVATE_KEY + userCert);
            config.enterpriseConfig.setAnonymousIdentity(anonymous);
            
            if ("SIM".equals(eapMethod) || "AKA".equals(eapMethod)) {
                config = setEapConfig(config, eapMethod, selSlot); 
            }
        } else {
            Log.i(TAG, "OPEN");
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        }
        
        if (!state.equals(AP_STATE_PUBLIC)) {
            config.hiddenSSID = true;
        } else {
            config.hiddenSSID = false;
        }

        TctWifiSettingsUtils.setIsPresetAP(config, true);

        //config.defineSsidPriority = ssidPriority;
        highestPriority = getHighestPriority();
        config.priority = highestPriority + ssidPriority;

        //Begin added by jiabin.zheng.hz for XR6433550 XR6591405 on 2018/07/13
        if ((mTctWifiMgr.isDTProduct() || mTctWifiMgr.isVFProduct()) 
                && mConfigList != null && !mConfigList.isEmpty()) {
            String configKey = config.configKey(true);
            for (WifiConfiguration cfg : mConfigList) {
                if (!TctWifiSettingsUtils.getIsPresetAP(cfg)) {
                    continue;
                }

                if (configKey.equals(cfg.configKey(true))) {
                    NetworkSelectionStatus status = cfg.getNetworkSelectionStatus();
                    if (status != null) {
                        if (mTctWifiMgr.isVFProduct()) {
                            boolean isAutoJoin = !status.isNetworkPermanentlyDisabled();
                            if (!isAutoJoin) {
                                Log.d(TAG, configKey + " has been disabled, do not save again");
                                return;
                            } 
                        }

                        if (mTctWifiMgr.isDTProduct() && cfg.enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.SIM
                                && config.enterpriseConfig.getEapMethod() == WifiEnterpriseConfig.Eap.AKA
                                && cfg.status == WifiConfiguration.Status.CURRENT) {
                            Log.d(TAG, configKey + " EAP SIM already connected, do not save EAP AKA again");
                            return;
                        }
                    }
                    break;
                }
            }
        }
        //Begin added by jiabin.zheng.hz for XR6433550 XR6591405 on 2018/07/13

        WifiManager.ActionListener mSaveListener = new WifiManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.i(TAG, " mWifiManager.save(config, null); is successed !!!!!!!!!!!!!!!!!!!!!!!!!!!");
            }
            @Override
            public void onFailure(int reason) {
                Log.i(TAG, " mWifiManager.save(config, null); is failuer XXXXXXXXXXXXXXXXXXXXXXXXXXX");
            }
        };
        mWifiManager.save(config, mSaveListener);
    }
    
    /*return value: -1,no sim card match
     *              0,sim1 match
     *              1,sim2,match
     *              2:sim1&sim2 are using the same mccmnc and all match. 2 don't used,just backup
     * single phone:simcc will show like 46000
     * dual phone:simcc format:sim1 only:"46000",sim2 only:",46002" dual sim : "46000,46002"
     * 
     * */
    private int getMatchedSimSlot(String definedMccCode) {
       
        Log.d(TAG, "current preloaded MccCode is: " + definedMccCode);
        if (isSIMInserted) {
            //get mcc code from phone when sim card is inserted
            String simMcc = SystemProperties.get("gsm.sim.operator.numeric", "");
            Log.d(TAG, "simMcc:" + simMcc);
            String simMccSplit[] = simMcc.split(",");

            if (simMccSplit == null || simMccSplit.length == 0) {
                Log.d(TAG,"simMccSplit length equal 0");
                return -1;
            }

            if (isSIM1Inserted && TctWifiUtil.checkSimMccMatch(simMccSplit[0], definedMccCode)) {
                return 0;
            }

            if (isSIM2Inserted && simMccSplit.length == 2 
                    && TctWifiUtil.checkSimMccMatch(simMccSplit[1], definedMccCode)) {
                return 1;
            }

        }

        return -1;
    }
    
    /* config:the wifi config
     * eapType:sim or aka
     * slot:the config slot,0 slot 1,1->slot2
     * */
    private WifiConfiguration setEapConfig(WifiConfiguration config, String eapType, int selSlot) {
        if (selSlot == 0 || selSlot == 1) {
            //int subId = TctWifiUtil.getSubId(selSlot); 
            //config.imsi = makeNAI(mTm.getSimOperator(subId), mTm.getSubscriberId(subId), eapType);
            TctWifiSettingsUtils.setSimSlot(config, selSlot);
            //config.simSlot = "\"" + String.valueOf(selSlot) + "\"";
        } else {
            Log.d(TAG,"exception selSlot found,selSlot::" + selSlot);
        }

        return config;
    }
}
