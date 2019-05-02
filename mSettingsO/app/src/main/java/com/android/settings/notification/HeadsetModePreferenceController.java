package com.android.settings.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;//Added by yingying.qiao.hz for task5852151 on 2018/01/03
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Created by nanbing.zou for Task:6628160 at 2018-07-27
 */
public class HeadsetModePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {

    private static final String TAG = "HeadsetMode";
    private static final String HEADSET_MODE_PREFERENCE_KEY = "headset_mode_preference";

    private Context mContext;
    private TwoStatePreference mCheckSwitch;
    private BroadcastReceiver mHeadsetStateReceiver;
    private AudioManager audioManager;
    private static boolean hasReveiverRegister = false;

    //Added by nanbing.zou for code standard with Task6628160 on 2018-08-01 begin
    private static int HEADSET_STATE_PLUGEGD = 1;//state - 0 for unplugged, 1 for plugged.
    private static String HEADSET_MODE_DISABLE = "0";//disable
    private static String HEADSET_MODE_HEADSET = "1";//headset only
    private static String HEADSET_MODE_BOTH = "2";//both the headset and the speaker
    private static String HEADSET_MODE_SYS_VALUE_CHECKED = "1";//checked,headset only
    private static String HEADSET_MODE_SYS_VALUE_UNCHECKED = "2";//unchecked,both the headset and the speaker
    private static String HEADSET_MODE_SYS_VALUE_DEFAULT = "-1";//sys value default
    //Added by nanbing.zou for code standard with Task6628160 on 2018-08-01 end

    public HeadsetModePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        mContext = context;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        //Begin added by yingying.qiao.hz for task5852151 on 2018/01/03
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        //End added by yingying.qiao.hz for task5852151 on 2018/01/03
        mHeadsetStateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                Log.d(TAG, "onReceive() intent action = " + action);

                if (action.equals(Intent.ACTION_HEADSET_PLUG)) {
                    if (mCheckSwitch == null) {
                        return;
                    }
                    //Modified by nanbing.zou for code standard with Task6628160 on 2018-08-01 begin
                    final boolean isHeadsetPlugged = (intent.getIntExtra("state", 0) == HEADSET_STATE_PLUGEGD); //extra values: state - 0 for unplugged, 1 for plugged.
                    if (isHeadsetPlugged) {
                        mCheckSwitch.setEnabled(true);
                        String headsetmodeValue = SystemProperties.get("persist.sys.headset_mode", HEADSET_MODE_BOTH);
                        final boolean needCheck = headsetmodeValue.equals(HEADSET_MODE_HEADSET);
                        mCheckSwitch.setChecked(needCheck);
                    //Modified by nanbing.zou for code standard with Task6628160 on 2018-08-01 end
                    } else {
                        mCheckSwitch.setEnabled(false);
                    }

                }
            }
        };
    }

    @Override
    public void onPause() {
        registerHeadsetModeReceiver(false);
    }

    @Override
    public void onResume() {
        registerHeadsetModeReceiver(true);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return HEADSET_MODE_PREFERENCE_KEY;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isChecked = (Boolean) newValue;
        if (isChecked) {
            mCheckSwitch.setChecked(true);
            //Modified by nanbing.zou for code standard with Task6628160 on 2018-08-01 begin
            SystemProperties.set("persist.sys.headset_mode", HEADSET_MODE_SYS_VALUE_CHECKED);
        } else {
            mCheckSwitch.setChecked(false);
            SystemProperties.set("persist.sys.headset_mode", HEADSET_MODE_SYS_VALUE_UNCHECKED);
            //Modified by nanbing.zou for code standard with Task6628160 on 2018-08-01 end
        }
        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        registerHeadsetModeReceiver(true);
        mCheckSwitch = (TwoStatePreference) screen.findPreference(HEADSET_MODE_PREFERENCE_KEY);
        //Modified by nanbing.zou for code standard with Task6628160 on 2018-08-01 begin
        String defaultValue = SystemProperties.get("ro.headset_mode", HEADSET_MODE_BOTH);
        String headsetmodeValue = SystemProperties.get("persist.sys.headset_mode", HEADSET_MODE_SYS_VALUE_DEFAULT);//Added by yingying.qiao.hz for defect5876078 on 2018/01/16
        if (HEADSET_MODE_DISABLE.equals(defaultValue)) {
            if (mCheckSwitch != null) {
                screen.removePreference(mCheckSwitch);
                mCheckSwitch = null;
                return;
            }
            //Begin added by yingying.qiao.hz for defect5876078 on 2018/01/16
        } else if ((HEADSET_MODE_HEADSET.equals(defaultValue) || HEADSET_MODE_BOTH.equals(defaultValue))&& headsetmodeValue.equals(HEADSET_MODE_SYS_VALUE_DEFAULT)){
            if (defaultValue.equals(HEADSET_MODE_HEADSET)) {
                mCheckSwitch.setChecked(true);
                SystemProperties.set("persist.sys.headset_mode", HEADSET_MODE_SYS_VALUE_CHECKED);
            } else {
                mCheckSwitch.setChecked(false);
                SystemProperties.set("persist.sys.headset_mode", HEADSET_MODE_SYS_VALUE_UNCHECKED);
            }
            //End added by yingying.qiao.hz for defect5876078 on 2018/01/16
        }

        if (audioManager.isWiredHeadsetOn() || audioManager.isBluetoothA2dpOn()) {
            mCheckSwitch.setEnabled(true);
        } else {
            mCheckSwitch.setEnabled(false);
        }

        String headsetmodeValue2 = SystemProperties.get("persist.sys.headset_mode", HEADSET_MODE_SYS_VALUE_DEFAULT);//Modify by yingying.qiao.hz for defect5876078 on 2018/01/16
        if (headsetmodeValue2.equals(HEADSET_MODE_SYS_VALUE_CHECKED)) {
        //Modified by nanbing.zou for code standard with Task6628160 on 2018-08-01 end
            mCheckSwitch.setChecked(true);
        } else {
            mCheckSwitch.setChecked(false);
        }

    }

    private void registerHeadsetModeReceiver(boolean register) {
        if (mHeadsetStateReceiver != null) {
            if (register) {
                if (!hasReveiverRegister) {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(Intent.ACTION_HEADSET_PLUG);
                    mContext.registerReceiver(mHeadsetStateReceiver, filter);
                    hasReveiverRegister = true;
                    Log.d(TAG, "registerHeadsetModeReceiver() true");
                }
            } else {
                if (hasReveiverRegister) {
                    mContext.unregisterReceiver(mHeadsetStateReceiver);
                    hasReveiverRegister = false;
                    Log.d(TAG, "registerHeadsetModeReceiver() false");
                }
            }
        }
    }
}
