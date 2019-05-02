package com.android.settings.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.TwoStatePreference;
import android.util.Log;

import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle; //Added by yingying.qiao.hz for defect5774194 on 2018/01/03
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

/**
 * Created for task 6631231 by junliang.liu 2018.8.7
 */
public class SilentModePreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener,
        LifecycleObserver, OnResume, OnPause {
    private static final String KEY_SILENT_MODE_SWITCH = "silent_mode_switch";
    private static final String TAG = "SilentModeController";
    public static final String SETTINGS_LAST_SILENT_MODE = "last_silent_mode";

    private Context mContext;
    private TwoStatePreference mSwitch;
    private AudioManager audioManager;
    private BroadcastReceiver mRingerModeReceiver;
    private static boolean hasReveiverRegister = false;

    public SilentModePreferenceController(Context context, Lifecycle lifecycle) {
        super(context);
        Log.d(TAG, "SilentModePreferenceController constructor.");
        mContext = context;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        //Begin add by yingying.qiao.hz for defect5774194 on 2018/01/03
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
        //Begin add by yingying.qiao.hz for defect5774194 on 2018/01/03
        mRingerModeReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //Begin modify by yingying.qiao.hz for defect5774194 on 2018/01/02
                final String action = intent.getAction();
                Log.d(TAG, "onReceive() intent action = " + action);
                if (action.equals(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION)) {
                    if (mSwitch != null) {
                        updateState(mSwitch);
                    }
                    //End modify by yingying.qiao.hz for defect5774194 on 2018/01/02
                }
            }
        };
    }

    private void registerRingerModeReceiver(boolean register) {
        Log.d(TAG, "registerRingerModeReceiver() " + register);
        if (mRingerModeReceiver != null) {
            if (register == true) {
                if (!hasReveiverRegister) {
                    IntentFilter filter = new IntentFilter();
                    filter.addAction(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION);
                    mContext.registerReceiver(mRingerModeReceiver, filter);
                    hasReveiverRegister = true;
                }
            } else {
                if (hasReveiverRegister) {
                    mContext.unregisterReceiver(mRingerModeReceiver);
                    hasReveiverRegister = false;
                }
            }
        } else {
            Log.e(TAG, "mRingerModeReceiver is null, can not register or unregister!");
        }
    }

    //Begin added by junyan.wan.hz for XR 7238303 2019/01/04
    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        Log.d(TAG, "displayPreference()");
        mSwitch = (TwoStatePreference) screen.findPreference(KEY_SILENT_MODE_SWITCH);
        if (mSwitch != null) {
            mSwitch.setPersistent(false);
            registerRingerModeReceiver(true);
        }
    }

    @Override
    public void onPause() {
        Log.d(TAG, "onPause()");
        registerRingerModeReceiver(false);
    }

    @Override
    public void onResume() {
        Log.d(TAG, "onResume()");
        registerRingerModeReceiver(true);
    }

    @Override
    public boolean isAvailable() {
        Log.d(TAG, "isAvailable()");
        return true;
    }

    @Override
    public String getPreferenceKey() {
        Log.d(TAG, "getPreferenceKey()");
        return KEY_SILENT_MODE_SWITCH;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        final boolean isChecked = (Boolean) newValue;
        Log.d(TAG, "onPreferenceChange() isChecked = " + isChecked);
        //Begin modify by yingying.qiao.hz for defect5774194 on 2018/01/02
        if (isChecked) {
            audioManager.setRingerModeInternal(AudioManager.RINGER_MODE_SILENT);
        } else {
            int previousMode = Settings.Global.getInt(mContext.getContentResolver(),
                    SETTINGS_LAST_SILENT_MODE, AudioManager.RINGER_MODE_NORMAL);
            Log.d(TAG, "onPreferenceChange() isChecked false, previousMode = " + previousMode);
            audioManager.setRingerModeInternal(previousMode);
        }
        return true;
        //End modify by yingying.qiao.hz for defect5774194 on 2018/01/02
    }

    @Override
    public void updateState(Preference preference) {
        Log.d(TAG, "updateState()");
        int ringerMode = audioManager.getRingerModeInternal();
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            ((TwoStatePreference) preference).setChecked(true);
        } else {
            ((TwoStatePreference) preference).setChecked(false);
        }

    }
}
