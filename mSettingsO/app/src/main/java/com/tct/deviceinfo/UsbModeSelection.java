/* ----------|----------------------|----------------------|----------------- */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* -------------------------------------------------------------------------- */
/* 2018/8/03 |     zhixiong.liu.hz  |       6700093    |     create      */
/******************************************************************************/



package com.tct.deviceinfo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbManager;
import com.android.settings.deviceinfo.UsbModeChooserActivity;
import com.android.settings.R;

import android.os.Handler;
import android.util.Log;
//begin zhixiong.liu.hz for XR 6816404 20180816
import android.os.SystemProperties;
import android.provider.Settings;
//end zhixiong.liu.hz for XR 6816404 20180816
//begin zhixiong.liu.hz for P23475 2018/9/21
import android.os.UserHandle;
//end zhixiong.liu.hz for P23475 2018/9/21
//begin zhixiong.liu.hz for 7139358 2018/11/23
import android.app.ActivityManager;
import java.util.List;
import android.util.Log;
import com.android.internal.util.ArrayUtils;
//end  zhixiong.liu.hz for 7139358 2018/11/23


public final class UsbModeSelection extends BroadcastReceiver {
	
    static private boolean mConnected;
    //begin zhixiong.liu.hz for 7139358 2018/11/23,some condition ,not popup
    private static final String[] EXTRA_PACKAGE_WHITE_LIST = {
            "com.android.mmi",
            //"com.tcl.camera",
    };
    //end zhixiong.liu.hz for 7139358 2018/11/23
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (context.getResources().getBoolean(R.bool.def_settings_usb_plugin_display) 
              && UsbManager.ACTION_USB_STATE.equals(action) 
              && UserHandle.myUserId() == 0) { //add check user by zhixiong.liu.hz for P23475 2018/9/21
        	 boolean connected = intent != null
                     && intent.getExtras().getBoolean(UsbManager.USB_CONNECTED,false);
             //begin zhixiong.liu.hz for XR 6816404 20180816
             boolean isMmitest = SystemProperties.getBoolean("ro.mmitest", false);
             int deviceProvisioned = Settings.Global.getInt(context.getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0);
             String voldDecrypt = SystemProperties.get("vold.decrypt");
             //begin zhxiong.liu.hz for XR6972953 20180906
             boolean isBootCompleted = SystemProperties.get("sys.boot_completed").equals("1");

             if(!isMmitest && deviceProvisioned == 1 && !"trigger_restart_min_framework".equals(voldDecrypt) && isBootCompleted) {
             //end zhxiong.liu.hz for XR6972953 20180906
                 if(connected != mConnected && connected == true ){
                     //begin zhixiong.liu.hz for 7137334 2018/11/21
                     if(!isWhitelistAppRunning(context)) {
                         Intent usbIntent = new Intent(context, UsbModeChooserActivity.class);
                         context.startActivity(usbIntent);
                     }
                     //end zhixiong.liu.hz for 7137334 2018/11/21
                 }
             }
             //end zhixiong.liu.hz for XR 6816404 20180816
             mConnected = connected;
        }

    }

    //begin zhixiong.liu.hz for 7137334 2018/11/21
    //if some acitivity is run,may not popup temp
    private boolean isWhitelistAppRunning(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        
        if (!tasks.isEmpty()) {
            if (ArrayUtils.contains(EXTRA_PACKAGE_WHITE_LIST,
                    tasks.get(0).topActivity.getPackageName())) {
                return true;
            }
        }
        return false;

    }
    //end zhixiong.liu.hz for 7137334 2018/11/21
}