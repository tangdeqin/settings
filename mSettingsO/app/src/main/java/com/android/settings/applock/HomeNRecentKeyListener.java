package com.android.settings.applock;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.app.KeyguardManager;

class HomeNRecentKeyListener extends BroadcastReceiver{
    Activity mHost;

    private static final String SYSTEM_REASON = "reason";
    private static final String SYSTEM_HOME_KEY = "homekey";
    private static final String SYSTEM_RECENT_APPS = "recentapps";

    public HomeNRecentKeyListener register(Activity activity){
        mHost = activity;
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);//added by dongchi.chen for XRP10025585 on 20181114
        mHost.registerReceiver(this, intentFilter);
        return this;
    }

    public void unregister(){
        if(null != mHost) {
            mHost.unregisterReceiver(this);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (action != null && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
            String reason = intent.getStringExtra(SYSTEM_REASON);
            if((null != reason) &&
                    (reason.equalsIgnoreCase(SYSTEM_HOME_KEY) || reason.equalsIgnoreCase(SYSTEM_RECENT_APPS))) { //modified by dongchi.chen for XR7116802 on 20181115
                mHost.finish();
            }
        //Begin added by dongchi.chen for XRP10025585 on 20181114
        } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
            mHost.finish();
        }
        //End added by dongchi.chen for XRP10025585 on 20181114
    }
}