package com.android.settings.gestures;

/**
 * Copyright (C) 2018 Tcl Corporation Limited
 * Created by yimin.guo on 18-1-3.
 */
import android.content.Context;
import android.content.Intent;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.util.Log;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class ThreeFingerPreferenceController extends AbstractPreferenceController implements
        PreferenceControllerMixin ,Preference.OnPreferenceChangeListener{
    private static final String TAG = "ThreeFingerPreferenceController";
    private final int ON = 1;
    private final int OFF = 0;

    private static final String KEY_THREE_FINGER = "gesture_finger_screenshot";

    private static final String TCT_FINGER_SCREENSHOT = "tct_finger_screenshot";

    //Begin add by yimin.guo for XR P23338 on 8/28/18
    private boolean isShowGesture;
    //End add by yimin.guo for XR P23338 on 8/28/18;


    public ThreeFingerPreferenceController(Context context) {
        super(context);
        Log.d(TAG,"init");
        //Begin add by yimin.guo for XR   on 8/28/18
        isShowGesture = context.getResources().getBoolean(R.bool.def_setting_gesture_menu_enable);
        //End add by yimin.guo for XR P23338 on 8/28/18
    }

    @Override
    public boolean isAvailable() {
        Log.d(TAG,"[isAvailable] true");
        //Begin modified by yimin.guo for XR P23338 on 8/28/18
        return isShowGesture;
        //End modified by yimin.guo for XR P23338 on 8/28/18
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG,"[onPreferenceChange] changed");
        final boolean enabled = (boolean) newValue;
        Settings.System.putInt(mContext.getContentResolver(), TCT_FINGER_SCREENSHOT, enabled ? ON : OFF);
        return true;
    }
    @Override
    public String getPreferenceKey() {
        return KEY_THREE_FINGER;
    }

    @Override
    public void updateState(Preference preference) {
        boolean allowThreeFinger = Settings.System.getInt(mContext.getContentResolver(), TCT_FINGER_SCREENSHOT, 1) == 1;
        if (preference != null) {
            if (preference instanceof SwitchPreference) {
                ((SwitchPreference)preference).setChecked(allowThreeFinger);
            }
        }

    }


}
