package com.tct.wifi;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import android.telephony.TelephonyManager;
import com.android.settings.R;
import android.os.SystemProperties;
import com.tct.sdk.base.wifi.TctWifiSettingsUtils;
import android.app.AlertDialog;

import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.support.v4.text.BidiFormatter;
import android.support.v7.preference.Preference;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.util.StringJoiner;

/**
 * to manage common tct function for wifi in settings
 *
 * @author jiabin.zheng.hz
 * @Time 2018-07-28
 */
public class TctWifiSettingsManager {

    private static final String TAG = "TctWifiSettingsManager";

    //Begin modified by jiabin.zheng.hz for XR6999128 on 2018/09/12
    private Context mContext = null;
    private static TctWifiSettingsManager mTctWifiSettingsManager = null;
    //End modified by jiabin.zheng.hz for XR6999128 on 2018/09/12

    private TelephonyManager mTm = null;
    private WifiManager mWifiMgr = null;

    private boolean mIsVFProduct = false;
    private boolean mIsDTProduct = false;
    private int mSimCount = -1;

    private boolean mIsNoModify = false;
    private boolean mIsNoForget = false;
    private boolean mIsRestraintNetwork = false;
    private boolean mIsHotspotShow = true;

    //Begin added by yucheng.luo for XR6699294 on 18-8-6
    private boolean mIsHotspotShowTips = false;
    //End added by yucheng.luo for XR6699294 on 18-8-6

    /**
     * constructor method to create instance of class PredefineApLoader.java
     * but this mthod is private so as to implement singleton model
     */
    //Begin modified by jiabin.zheng.hz for XR6999128 on 2018/09/12
    private TctWifiSettingsManager(Context context) {
        mContext = context;
    //End modified by jiabin.zheng.hz for XR6999128 on 2018/09/12
        if (mTm == null) {
            mTm = (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (mWifiMgr == null) {
            mWifiMgr = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        }

        mIsVFProduct = SystemProperties.get("wifi.auto.join.for.vf", "0").equals("1");
        mIsDTProduct = SystemProperties.get("wifi.connect.for.dt", "0").equals("1");

        mIsNoModify = mContext.getResources().getBoolean(R.bool.def_Settings_wifi_preconfig_ap_no_edit);
        mIsNoForget = mContext.getResources().getBoolean(R.bool.def_Settings_wifi_preconfig_ap_no_forget);
        mIsRestraintNetwork = mContext.getResources().getBoolean(R.bool.def_usb_tethering_on_restraint_data_connect);
        mIsHotspotShow = mContext.getResources().getBoolean(R.bool.def_settings_hotspot_enable);

        //Begin added by yucheng.luo for XR6699294 on 18-8-6
        mIsHotspotShowTips = mContext.getResources().getBoolean(R.bool.def_settings_showtips_switch_wifi_hotspot);
        //End added by yucheng.luo for XR6699294 on 18-8-6
    }

    /**
     * method to return instance of class TctWifiSettingsManager.java
     *
     * @param context Context object from caller
     * @return instance of TctWifiSettingsManager
     */
    public static TctWifiSettingsManager getTctWifiSettingsManager(Context context) {
        //Begin modified by jiabin.zheng.hz for XR6999128 on 2018/09/12
        if (mTctWifiSettingsManager == null && context != null) {
            mTctWifiSettingsManager = new TctWifiSettingsManager(context.getApplicationContext());
        }
        //End modified by jiabin.zheng.hz for XR6999128 on 2018/09/12
        return mTctWifiSettingsManager;
    }

    public boolean isDualSim() {
        if (mSimCount == -1 && mTm != null) {
            mSimCount = mTm.getSimCount();
        }

        return mSimCount > 1;
    }

    public boolean isVFProduct() {
        return mIsVFProduct;
    }

    public boolean isDTProduct() {
        return mIsDTProduct;
    }

    public boolean isNoModify() {
        return mIsNoModify;
    }

    public boolean isNoforget() {
        return mIsNoForget;
    }

    public boolean isPresetAP(WifiConfiguration config) {
        return TctWifiSettingsUtils.getIsPresetAP(config);
    }

    public int getSimSlot(WifiConfiguration config) {
        return TctWifiSettingsUtils.getSimSlot(config);
    }

    public boolean isHotspotShow() {
        return mIsHotspotShow;
    }

    public boolean isRestraintNetwork() {
        return mIsRestraintNetwork;
    }

    public boolean isDataEnabled() {
        return mTm != null && mTm.getSimState() == TelephonyManager.SIM_STATE_READY
                           && mTm.getDataEnabled();
    }

    public boolean isWifiConnected() {
        WifiInfo wifiInfo = mWifiMgr.getConnectionInfo();
        return mWifiMgr.isWifiEnabled()
                && null != wifiInfo && wifiInfo.getNetworkId() != -1;
    }
    //Begin modified by chenglong.cai for XR7133785 on 2018/11/17
    public void createConnectNetworkDialog(Context context){
        AlertDialog.Builder ad = new AlertDialog.Builder(context);
        //End modified by chenglong.cai for XR7133785 on 2018/11/17
        ad.setTitle(R.string.wifi_hotspot_warning_title);
        ad.setMessage(R.string.dialog_cellular_data_wifi_sp_promption);

        ad.setNegativeButton(R.string.okay,new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int i) {
                 //Begin modified by miaoliu for XR7025065 on 2018/10/18
                // Intent intent = new Intent();
                // ComponentName component = new ComponentName("com.android.phone",
                //         "com.android.phone.MobileNetworkSettings");
                // intent.setComponent(component);
                Intent intent = com.android.settings.Utils.getMobileNetworkSettingsIntent(mContext);
                //End modified by miaoliu for XR7025065 on 2018/10/18
                mContext.startActivity(intent);
            }
        });
        ad.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener(){
            public void onClick(DialogInterface dialog, int i){
                dialog.dismiss();
            }
        });
        ad.show();
    }

    //Begin add by yucheng.luo for XR6699294 on 18-8-6
    public boolean isHotspotShowTips() {
        return mIsHotspotShowTips;
    }
    //End add by yucheng.luo for XR6699294 on 18-8-6

    // Set IPv4 and IPv6 addresses to Preference
    public void setIpAddress(Preference ipv4AddressPref, Preference ipv6AddressPref) {
        String ipv4Address = null;
        StringJoiner ipv6Addresses = new StringJoiner("\n");
        LinkProperties linkProperties = null;
        Network mNetwork = mWifiMgr.getCurrentNetwork();
        if (mNetwork != null) {
            ConnectivityManager cm = (ConnectivityManager)
                    mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            linkProperties = cm.getLinkProperties(mNetwork);
        }

        if (linkProperties != null) {
            for (LinkAddress addr : linkProperties.getLinkAddresses()) {
                if (addr.getAddress() instanceof Inet4Address) {
                    ipv4Address = addr.getAddress().getHostAddress();
                } else if (addr.getAddress() instanceof Inet6Address) {
                    ipv6Addresses.add(addr.getAddress().getHostAddress());
                }
            }
        }

        if (ipv4AddressPref != null) {
            ipv4AddressPref.setSummary(ipv4Address == null
                    ? mContext.getString(R.string.status_unavailable)
                    : BidiFormatter.getInstance().unicodeWrap(ipv4Address));
        }

        if (ipv6AddressPref!=null) {
            ipv6AddressPref.setSummary(ipv6Addresses.length() > 0
                    ? BidiFormatter.getInstance().unicodeWrap(ipv6Addresses.toString())
                    : mContext.getString(R.string.status_unavailable));
        }
    }

    //Begin added by jiabin.zheng.hz for XR7311748 on 2019/01/07
    public void setIpAddressStatus(Preference ipv4AddressPref, Preference ipv6AddressPref, String ipAddress) {
        String[] ipAddresses = null;
        if (ipAddress != null) {
            ipAddresses = ipAddress.split("\n");
            int ipAddressesLength = ipAddresses.length;
            for (int i = 0; i < ipAddressesLength; i++) {
                if (ipAddresses[i].indexOf(":") == -1) {
                    ipv4AddressPref.setSummary(ipAddresses[i] == null ?
                            mContext.getString(R.string.status_unavailable) : ipAddresses[i]);
                    if (ipAddressesLength == 1) {
                        ipv6AddressPref.setSummary(mContext.getString(R.string.status_unavailable));
                    }
                } else if (ipAddresses[i].length() > 2) {
                    //set one ipv6 address one line
                    String ipSummary = "";
                    if(ipAddresses[i] == null) {
                        ipSummary = mContext.getString(R.string.status_unavailable);
                    } else {
                        String[] ipv6Addresses = ipAddresses[i].split("; ");
                        int len = ipv6Addresses.length;
                        for (int j = 0; j < len; j++) {
                            if (j == (len - 1)) {
                                ipSummary += ipv6Addresses[j];
                            } else {
                                ipSummary += ipv6Addresses[j]+"\n";
                            }
                        }
                    }
                    ipv6AddressPref.setSummary(ipSummary.trim());
                    if (ipAddressesLength == 1) {
                        ipv4AddressPref.setSummary(mContext.getString(R.string.status_unavailable));
                    }
                }
            }
        } else {
            ipv4AddressPref.setSummary(mContext.getString(R.string.status_unavailable));
            ipv6AddressPref.setSummary(mContext.getString(R.string.status_unavailable));
        }
    }
    //End added by jiabin.zheng.hz for XR7311748 on 2019/01/07
}
