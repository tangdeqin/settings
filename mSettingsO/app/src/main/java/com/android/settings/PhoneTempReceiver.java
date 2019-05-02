/**
 * function:disable gms function
 * owner:made by xi.peng.hz@tcl.com
 * data: 2018/09/12
 * FR:P23596
 */

package com.android.settings;

import java.util.List;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.Log;

public class PhoneTempReceiver extends BroadcastReceiver {
    private static final String TAG = "PhoneTempReceiver";
    private static final String ACTION_GOOGLE_APP_CHANGED = "action.google.app.changed";
    private static final String PROPERTY_GOOGLEAPP_ENABLED = "persist.sys.google.enabled";
    private PackageManager mPm;
    private ActivityManager mAm;
    private static final int MSG_FORCE_STOP_GOOGLE_APPS = 0;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
            case MSG_FORCE_STOP_GOOGLE_APPS:
                List<PackageInfo> packageInfos = mPm.getInstalledPackages(0);
                Log.i(TAG, "Handle stop gms message, start");
                for (int i = 0; i < packageInfos.size(); i++) {
                    ApplicationInfo info = packageInfos.get(i).applicationInfo;
                    if (((info.packageName).startsWith("com.google") || (info.packageName).startsWith("com.android.vending")
                            || (info.packageName).startsWith("com.facebook")
                            || (info.packageName).startsWith("com.tcl.simplelauncher")
                            || (info.packageName).startsWith("com.tct.video")
                            || (info.packageName).startsWith("com.tcl.live")
                            || (info.packageName).startsWith("com.tcl.aota")
                            || (info.packageName).startsWith("com.tcl.usercare"))
                            /* && (((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0))*/ && isDisablePackage(info)) {
                        mAm.forceStopPackage(info.packageName);
                    }
                }
                Log.i(TAG, "Handle stop gms message, end");
                break;
            }
        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "Intent action = " + action);
        mPm = context.getPackageManager();
        mAm = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            String gEnabled = SystemProperties.get(PROPERTY_GOOGLEAPP_ENABLED, "notset");
            if ("notset".equals(gEnabled)) {
                //Just do once
                List<PackageInfo> packageInfos = mPm.getInstalledPackages(0);
                SystemProperties.set(PROPERTY_GOOGLEAPP_ENABLED, "nogms");
                for (int i = 0; i < packageInfos.size(); i++) {
                    ApplicationInfo info = packageInfos.get(i).applicationInfo;
                    if (((info.packageName).startsWith("com.google"))) {
                        SystemProperties.set(PROPERTY_GOOGLEAPP_ENABLED, "enabled");
                        break;
                    }
                }
            }
            return;
        } else if (ACTION_GOOGLE_APP_CHANGED.equals(action)) {
            //State: notset, enabled, disabled, nogms
            String extra = intent.getStringExtra("googleConfig");
            if (extra == null) {
                Log.i(TAG, "no extra found, return");
                return;
            }
            Log.i(TAG, "start processing google apps!!! gEnabled = " + extra);

            List<PackageInfo> packageInfos = mPm.getInstalledPackages(0);
            Log.i(TAG, "packageInfos.size() = " + packageInfos.size());
            if ("0".equals(extra)) {  //From enable to disable
                for (int i = 0; i < packageInfos.size(); i++) {
                    ApplicationInfo info = packageInfos.get(i).applicationInfo;
                    if (((info.packageName).startsWith("com.google") || (info.packageName).startsWith("com.android.vending")
                                 || (info.packageName).startsWith("com.facebook")
                                 || (info.packageName).startsWith("com.tcl.simplelauncher")
                                 || (info.packageName).startsWith("com.tct.video")
                                 || (info.packageName).startsWith("com.tcl.live")
                                 || (info.packageName).startsWith("com.tcl.aota")
                                 || (info.packageName).startsWith("com.tcl.usercare"))
                            /* && (((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0))*/) {
                        if (info.enabled && isDisablePackage(info)) {
                            new DisableChanger(this, info,
                                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER)
                            .execute((Object)null);
                            Log.i(TAG, "disable packageName = " + info.packageName + " , info.enabled = " + info.enabled);
                        }
                    }
                }
                Log.i(TAG, "send delayed msg to stop google apps!!!");
                mHandler.removeMessages(MSG_FORCE_STOP_GOOGLE_APPS);
                mHandler.sendEmptyMessageDelayed(MSG_FORCE_STOP_GOOGLE_APPS, 10 * 1000);
                SystemProperties.set(PROPERTY_GOOGLEAPP_ENABLED, "disabled");
            } else {  //From disable to enable
                for (int i = 0; i < packageInfos.size(); i++) {
                    ApplicationInfo info = packageInfos.get(i).applicationInfo;
                    if (((info.packageName).startsWith("com.google") || (info.packageName).startsWith("com.android.vending")
                                 || (info.packageName).startsWith("com.facebook")
                                 || (info.packageName).startsWith("com.tcl.simplelauncher")
                                 || (info.packageName).startsWith("com.tct.video")
                                 || (info.packageName).startsWith("com.tcl.live")
                                 || (info.packageName).startsWith("com.tcl.aota")
                                 || (info.packageName).startsWith("com.tcl.usercare"))
                           /*&& (((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0))*/) {
                        if (!(info.enabled)) {
                            new DisableChanger(this, info,
                                    PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
                            .execute((Object)null);
                        }
                        Log.i(TAG, "enable packageName = " + info.packageName + " , info.enabled = " + info.enabled);
                    }
                }
                SystemProperties.set(PROPERTY_GOOGLEAPP_ENABLED, "enabled");
            }
            return;
        }
    }

    private boolean isDisablePackage (ApplicationInfo info) {
        if (/*(info.packageName).startsWith("com.google.android.inputmethod.latin") || (info.packageName).startsWith("com.google.android.gms") ||*/ (info.packageName).startsWith("com.google.android.packageinstaller")) {
            return false;
        }
        return true;
    }

    static class DisableChanger extends AsyncTask<Object, Object, Object> {
        final PackageManager mPm;
        final ApplicationInfo mInfo;
        final int mState;

        DisableChanger(PhoneTempReceiver receiver, ApplicationInfo info, int state) {
            mPm = receiver.mPm;
            mInfo = info;
            mState = state;
        }

        @Override
        protected Object doInBackground(Object... params) {
            mPm.setApplicationEnabledSetting(mInfo.packageName, mState, 0);
            return null;
        }
    }
}
