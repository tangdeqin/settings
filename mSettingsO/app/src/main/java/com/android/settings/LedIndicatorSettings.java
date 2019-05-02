/* ----------|----------------------|----------------------|----------------- */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* -------------------------------------------------------------------------- */
/* 2017/6/07 |     xiaojing.wang    |      Defect4832356   |  LED Settings   */
/* 2017/8/30 |     zhixiong.liu.hz  |   task  task 5198616 |  merge          */
/* 2017/11/28|     zhixiong.liu.hz  |       task 5198616   |   new ergo      */
/******************************************************************************/
package com.android.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.widget.SwitchBar;
import android.support.v7.preference.Preference;
import android.support.v7.preference.CheckBoxPreference;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;


public class LedIndicatorSettings extends SettingsPreferenceFragment  
         implements Preference.OnPreferenceChangeListener {

    private static final String TAG = "LEDSettings";
    private Context mContext;
    private ContentResolver cr;
    private Activity mActivity;

    private SwitchBar mSwitchBar;
	
    private SwitchPreference mChargingPreference;
    private SwitchPreference mEventsPreference;
    private static final String KEY_CHARGING_PREFERENCE = "charging_key";
    private static final String KEY_EVENTS_PREFERENCE = "events_key";
    
    private SwitchPreference mLowBatteryPreference;
    private static final String KEY_LOWBATTERY_PREFERENCE = "low_battery";
   
    private static final String LED_INDICATOR_ENABLED = "led_indicator_enabled";
    private static final String CHARGING_ENABLE = "charging_enable";
    private static final String LOW_BATTERY_ENABLE ="low_battery_enable";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity();
        cr = getContentResolver();
        mActivity = getActivity();
        addPreferencesFromResource(R.layout.led_indicator_setting);
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        initView();
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();    
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
		
        if (mSwitchBar != null) {
            mSwitchBar.hide();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void initView() {
        mChargingPreference = (SwitchPreference) findPreference(KEY_CHARGING_PREFERENCE);
		mChargingPreference.setOnPreferenceChangeListener(this);
        mEventsPreference = (SwitchPreference) findPreference(KEY_EVENTS_PREFERENCE);
		mEventsPreference.setOnPreferenceChangeListener(this);
        boolean isChargingOn = Settings.Global.getInt(cr, CHARGING_ENABLE, 1) == 1;
        boolean isEventsOn = Settings.Global.getInt(cr, LED_INDICATOR_ENABLED, 1) == 1;
		mChargingPreference.setChecked(isChargingOn);
		mEventsPreference.setChecked(isEventsOn);
		
        mLowBatteryPreference  = (SwitchPreference) findPreference(KEY_LOWBATTERY_PREFERENCE);
        mLowBatteryPreference.setOnPreferenceChangeListener(this);
        boolean isLowBatteryOn = Settings.Global.getInt(cr, LOW_BATTERY_ENABLE, 1) == 1;
        mLowBatteryPreference.setChecked(isLowBatteryOn);
    }
    
    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if(preference == mChargingPreference){
             boolean value = (Boolean) newValue;
             Settings.Global.putInt(getContentResolver(),
                    CHARGING_ENABLE, value ? 1 : 0);
        }else if(preference == mEventsPreference){
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(getContentResolver(),
                    LED_INDICATOR_ENABLED, value ? 1 : 0);
        }else if (preference == mLowBatteryPreference){
            boolean value = (Boolean) newValue;
            Settings.Global.putInt(getContentResolver(),
                    LOW_BATTERY_ENABLE, value ? 1 : 0);
        
        }
        return true;
    }
    
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ABOUT_LEGAL_SETTINGS;
    }
}
