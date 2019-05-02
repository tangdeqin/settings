package com.android.settings.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;
import android.telephony.TelephonyManager;
import com.android.internal.telephony.IccCardConstants;
import com.tct.wifi.TctPredefineApLoader;
import com.tct.wifi.TctWifiUtil;

/**
 * receiver for updating predefined AP when
 * sim card or wifi state changed
 *
 * @author jiabin.zheng.hz
 * @Time 2018-07-28
 */
public class TctWifiReceiver extends BroadcastReceiver{

    private static final String TAG = "TctWifiReceiver";
    private Context mContext;
    private boolean isSIM1Inserted = false;
    private boolean isSIM2Inserted = false;

    private WifiManager mWifiManager = null;
    private TelephonyManager mTm = null;
    private TctPredefineApLoader mTctAploader = null;

    //Begin added by jiabin.zheng.hz for XRP10024912 on 2018/10/30
    /* ABSENT means ICC is missing */
    public static final String INTENT_VALUE_ICC_ABSENT = "ABSENT";
    //Begin modified by jiabin.zheng.hz for XR7136395 on 2018/12/04
    /* LOADED means all ICC records, including IMSI, are loaded */
    public static final String INTENT_VALUE_ICC_LOADED = "LOADED";
    //End modified by jiabin.zheng.hz for XR7136395 on 2018/12/04
    //End added by jiabin.zheng.hz for XRP10024912 on 2018/10/30

    @Override
    public void onReceive(Context context, Intent intent) {
        mContext = context;
        if (mWifiManager == null) {
            mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }

        if (mTctAploader == null) {
            mTctAploader = TctPredefineApLoader.getTctPredefineApLoader(mContext);
        }
        
        String action = intent.getAction();
        Log.d(TAG, "action:" + action);

        if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)) {
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN);
            String strWifiState = TctWifiUtil.getWifiStateStr(wifiState);
            Log.d(TAG, "wifiState:" + strWifiState + "  [" + strWifiState + "]-"); 
            if (wifiState == WifiManager.WIFI_STATE_ENABLING || wifiState == WifiManager.WIFI_STATE_ENABLED) {
                getCurrentSimcardState();
                Log.i(TAG, "Received wifi is enabled, isSIM1Inserted is " + isSIM1Inserted + ", isSIM2Inserted is " + isSIM2Inserted);
                mTctAploader.loadPredefineNetworks();
            }
        } else if (TelephonyIntents.ACTION_SIM_STATE_CHANGED.equals(action)) {
            String iccState = intent.getStringExtra(IccCardConstants.INTENT_KEY_ICC_STATE);
            getCurrentSimcardState();
            Log.i(TAG, "Received sim card changed state = " + iccState + " isSIM1Inserted is " + isSIM1Inserted + ", isSIM2Inserted is " + isSIM2Inserted);

            //Begin modified by jiabin.zheng.hz for XR7136395 on 2018/12/04
            //Begin added by jiabin.zheng.hz for XRP10024912 on 2018/10/30
            //just concern about below two states, ignore the procedure states
            if (!INTENT_VALUE_ICC_ABSENT.equals(iccState) && !INTENT_VALUE_ICC_LOADED.equals(iccState)) {
                return;
            }
            //End added by jiabin.zheng.hz for XRP10024912 on 2018/10/30
            //End modified by jiabin.zheng.hz for XR7136395 on 2018/12/04

            int wifiState = mWifiManager.getWifiState();
            if (wifiState == WifiManager.WIFI_STATE_ENABLED || wifiState == WifiManager.WIFI_STATE_ENABLING) {
                if (isSIM1Inserted || isSIM2Inserted) {
                    mTctAploader.loadPredefineNetworks();
                }
            }
        }
    }

    public void getCurrentSimcardState() {
        if (mTm == null) {
            mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        isSIM1Inserted = mTm.getSimState(0) == TelephonyManager.SIM_STATE_READY;
        int simSum = mTm.getSimCount();
        if (simSum == 2) {
            isSIM2Inserted = mTm.getSimState(1) == TelephonyManager.SIM_STATE_READY;
        }
        Log.d(TAG, "isSIM1Inserted is " + isSIM1Inserted + " ,isSIM2Inserted is " + isSIM2Inserted);
    }

}
