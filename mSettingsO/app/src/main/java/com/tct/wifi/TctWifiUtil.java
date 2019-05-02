package com.tct.wifi;

import android.net.wifi.WifiEnterpriseConfig;
import android.telephony.SubscriptionManager;
import android.text.TextUtils;
import android.util.Log;
import android.net.wifi.WifiConfiguration;

/**
* some utility funtion for tct wifi requirement
*
* @author jiabin.zheng.hz
* @Time 2018-07-28
*/
public class TctWifiUtil {

    private static final String TAG = "Settings:TctWifiUtil";
    private static final String[] EapMethods = { "PEAP", "TLS", "TTLS", "PWD", "SIM", "AKA", "AKA'", "WFA-UNAUTH-TLS" };
    private static final String[] Phase2Methods = {"NULL", "PAP", "MSCHAP", "MSCHAPV2", "GTC", "SIM", "AKA", "AKA'" };
    private static final String[] WifiStates = {"WIFI_STATE_DISABLING", "WIFI_STATE_DISABLED", "WIFI_STATE_ENABLING", 
                                                "WIFI_STATE_ENABLED", "WIFI_STATE_UNKNOWN"};

    public static int getEapMethodIndex(String eapMethod) {
        if (eapMethod == null) {
            return -1;
        }

        for (int index = 0; index < EapMethods.length; index++) {
            if (eapMethod.trim().equals(EapMethods[index].trim())) {
                return index;
            }
        }
        return -1;
    }

    public static String getEapMethodStr(int index) {
        if (index >= 0 && index < EapMethods.length) {
            return EapMethods[index];
        }

        return "";
    }

    public static int getPhase2MethodIndex(String phase2Method) {
        if (phase2Method == null) {
            return -1;
        }

        for (int index = 0; index < Phase2Methods.length; index++) {
            if (phase2Method.trim().equals(Phase2Methods[index].trim())) {
                return index;
            }
        }
        return -1;
    }

    public static String getWifiStateStr(int state) {
    	if (state >= 0 && state < WifiStates.length) {
            return WifiStates[state];
        }

        return "";
    }
    
    //match with the predefined mcc
    public static boolean checkSimMccMatch(String simMcc, String definedMccCode) {
        if (simMcc != null && definedMccCode != null && !TextUtils.isEmpty(definedMccCode)) {
            String definedMccCodeSplit[] = definedMccCode.split(";");
            for (String mccCode : definedMccCodeSplit) {
                if (simMcc.trim().equals(mccCode.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isSimMethodAP(WifiConfiguration config) {
        if (config != null && config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP)) {
            int value = config.enterpriseConfig.getEapMethod();
            if (value == WifiEnterpriseConfig.Eap.SIM
                || value == WifiEnterpriseConfig.Eap.AKA
                || value == WifiEnterpriseConfig.Eap.AKA_PRIME) {
                return true;
            }
        }

        return false;
    }

    public static int getSubId(int slotId) {
        int[] SubIds = SubscriptionManager.getSubId(slotId);
        if (SubIds != null) {
            return SubIds[0];
        } 

        return -1;
    }

    private static final int BUFFER_LENGTH = 40;
    private static final int MNC_SUB_BEG = 3;
    private static final int MNC_SUB_END = 5;
    private static final int MCC_SUB_BEG = 0;
    private static final int MCC_MNC_LENGTH = 5;

    /**
     *make NAI
     * @param simOperator mnc+mcc
     * @param imsi eapMethod
     * @return the string of NAI
     */
    public static String makeNAI(String simOperator, String imsi, String eapMethod) {

        // airplane mode & select wrong sim slot
        if (imsi == null) {
            return "\"error\"";
        }

        StringBuffer NAI = new StringBuffer(BUFFER_LENGTH);

        if (eapMethod.equals("SIM")) {
            NAI.append("1");
        } else if (eapMethod.equals("AKA")) {
            NAI.append("0");
        }

        // add imsi
        NAI.append(imsi);
        NAI.append("@wlan.mnc");
        // add mnc
        // for some operator
        Log.i(TAG, "simOperator = " + simOperator);
        if (simOperator.length() == MCC_MNC_LENGTH) {
          NAI.append("0");
          NAI.append(imsi.substring(MNC_SUB_BEG, MNC_SUB_END));
        } else {
          NAI.append(imsi.substring(MNC_SUB_BEG, MNC_SUB_END + 1));
        }
        NAI.append(".mcc");
        // add mcc
        NAI.append(imsi.substring(MCC_SUB_BEG, MNC_SUB_BEG));

        // NAI.append(imsi.substring(5));
        NAI.append(".3gppnetwork.org");
        Log.d(TAG, NAI.toString());
        Log.d(TAG, "\"" + NAI.toString() + "\"");
        return "\"" + NAI.toString() + "\"";
    }
}