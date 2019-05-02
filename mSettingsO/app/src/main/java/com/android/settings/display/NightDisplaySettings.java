/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.display;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.DropDownPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.widget.TimePicker;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.widget.SeekBarPreference;
import com.android.settings.SettingsPreferenceFragment;

import java.text.DateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;

import android.util.Log;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.os.Handler;
import android.net.Uri;

/**
 * Settings screen for Night display.
 * modify this file by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
 */
public class NightDisplaySettings extends SettingsPreferenceFragment
        implements NightDisplayController.Callback, Preference.OnPreferenceChangeListener ,MyRadioButtonPreference.OnClickListener{

    private static final String TAG = "NightDisplaySettings";

    private static final String KEY_NIGHT_DISPLAY_AUTO_MODE = "night_display_auto_mode";
    private static final String KEY_NIGHT_DISPLAY_TEMPERATURE = "night_display_temperature";
    private static final String PREF_AUTO_SCHEDULE = "auto_schedule";
    public static final String PREF_SCHEDULE_PLAN = "schedule_plan";
    private static final String KEY_ECM_TURN_ON_NOW = "turn_on_now";
    private static final String KEY_ECM_COLOR_SPACE = "ecm_color_space";
    private static final String KEY_ECM_NORMAL_THEME = "ecm_color_space_normal_theme";
    private static final String KEY_ECM_NIGHT_THEME = "ecm_color_space_night_theme";

    private NightDisplayController mController;
    private DateFormat mTimeFormatter;
    private SwitchPreference mTurnOnNow;
    private NightDisplaySeekBarPreference mTemperaturePreference;
    private SwitchPreference mAutoSchedule;
    private Preference mSchedulePlan;
    private Preference mColorSpace;
    private MyRadioButtonPreference mNormalTheme;
    private MyRadioButtonPreference mNightTheme;

    private boolean mAutoModeEnable = false;
    private int mAutoMode = 0;
    private int mBackupAutoMode = 0;

    private Context context;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = getContext();
        mController = new NightDisplayController(context);

        mTimeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        mTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        mTemperaturePreference.setMax(convertTemperature(mController.getMinimumColorTemperature()));
        mTemperaturePreference.setContinuousUpdates(true);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_night_display;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        // Load the preferences from xml.
        addPreferencesFromResource(R.xml.night_display_settings);

        mTurnOnNow = (SwitchPreference) findPreference(KEY_ECM_TURN_ON_NOW);
        mTemperaturePreference = (NightDisplaySeekBarPreference) findPreference(KEY_NIGHT_DISPLAY_TEMPERATURE);
        mAutoSchedule = (SwitchPreference)findPreference(PREF_AUTO_SCHEDULE);
        mSchedulePlan = findPreference(PREF_SCHEDULE_PLAN);
        mColorSpace = findPreference(KEY_ECM_COLOR_SPACE);
        mNightTheme = (MyRadioButtonPreference)findPreference(KEY_ECM_NIGHT_THEME);
        mNormalTheme = (MyRadioButtonPreference)findPreference(KEY_ECM_NORMAL_THEME);

        mTurnOnNow.setOnPreferenceChangeListener(this);
        mTemperaturePreference.setOnPreferenceChangeListener(this);
        mNightTheme.setOnClickListener(this);
        mNormalTheme.setOnClickListener(this);
        mAutoSchedule.setOnPreferenceChangeListener(this);

        initStatus();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Listen for changes only while visible.
        mController.setListener(this);

        onActivated(mController.isActivated());
        mAutoMode = mController.getAutoMode();

        onColorTemperatureChanged(mController.getColorTemperature());
        updatePref();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop listening for state changes.
        mController.setListener(null);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivated(boolean activated) {
        mTurnOnNow.setChecked(activated);
        if(activated){
            Settings.System.putInt(context.getContentResolver(),"reading_mode_always_enable", 0) ;
        }
        updateRadioButtonPreferenceStatus();
        updateNightThemeStatus(activated);
    }

    @Override
    public void onColorTemperatureChanged(int colorTemperature) {
        mTemperaturePreference.setProgress(convertTemperature(colorTemperature));
    }

    private String getFormattedTimeString(LocalTime localTime) {
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(mTimeFormatter.getTimeZone());
        c.set(Calendar.HOUR_OF_DAY, localTime.getHour());
        c.set(Calendar.MINUTE, localTime.getMinute());
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return mTimeFormatter.format(c.getTime());
    }

    /**
     * Inverts and range-adjusts a raw value from the SeekBar (i.e. [0, maxTemp-minTemp]), or
     * converts an inverted and range-adjusted value to the raw SeekBar value, depending on the
     * adjustment status of the input.
     */
    private int convertTemperature(int temperature) {
        return mController.getMaximumColorTemperature() - temperature;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mTurnOnNow) {
            return mController.setActivated((Boolean) newValue);
        } else if (preference == mTemperaturePreference) {
            return mController.setColorTemperature(convertTemperature((Integer) newValue));
        }else if(preference == mAutoSchedule){
            if(Settings.Secure.getInt(getContentResolver(), "activated_automode", 0) == 0){
                Settings.Secure.putInt(getContentResolver(), "activated_automode", 1);
            }
            mAutoSchedule.setChecked((Boolean) newValue);
            mSchedulePlan.setEnabled((Boolean) newValue);
            if(!mAutoSchedule.isChecked()){
                mController.setAutoMode(NightDisplayController.AUTO_MODE_DISABLED);
            }else{
                int temp = Settings.Secure.getInt(getContentResolver(), "ecm_auto_mode", 1);
                if (1 == temp) {
                    mController.setAutoMode(NightDisplayController.AUTO_MODE_CUSTOM);
                }else if(2 == temp){
                    mController.setAutoMode(NightDisplayController.AUTO_MODE_TWILIGHT);
                }
            }
            updateRadioButtonPreferenceStatus();
        }
        return false;
    }

    private void initStatus(){
        mTemperaturePreference.setEnabled(true);
        mColorSpace.setEnabled(false);
        //Begin add by shilei.zhang for fixed XR7139886 on 2018/11/23
        mColorSpace.setSelectable(false);
        //End add by shilei.zhang for fixed XR7139886 on 2018/11/23
        mNormalTheme.setEnabled(false);
        mNightTheme.setEnabled(false);
        mTurnOnNow.setChecked(false);
    }

    private void updatePref()
    {
        int autoMode = mController.getAutoMode();
        String summary = getFormattedTimeString(mController.getCustomStartTime()) + " - " + getFormattedTimeString(mController.getCustomEndTime());
        if(NightDisplayController.AUTO_MODE_CUSTOM ==autoMode
                || NightDisplayController.AUTO_MODE_TWILIGHT == autoMode){
            if (NightDisplayController.AUTO_MODE_CUSTOM == autoMode){
                mSchedulePlan.setSummary(summary);
            }else{
                mSchedulePlan.setSummary(R.string.night_mode_sunset_plan);
            }
            if(Settings.Secure.getInt(getContentResolver(), "activated_automode", 0)  == 1){
                mAutoSchedule.setChecked(true);
            }
        }else{
            int temp = Settings.Secure.getInt(getContentResolver(), "ecm_auto_mode", 1);
            if (temp == 1){
                mSchedulePlan.setSummary(summary);
            }else if(temp == 2){
                mSchedulePlan.setSummary(R.string.night_mode_sunset_plan);
            }else{
                mSchedulePlan.setSummary(R.string.night_display_auto_mode_never);
            }
        }

        if(mAutoSchedule.isChecked()){
            mSchedulePlan.setEnabled(true);
        }else{
            mSchedulePlan.setEnabled(false);
        }

        updateRadioButtonPreferenceStatus();
    }

    @Override
    public void onRadioButtonClicked(MyRadioButtonPreference emiter) {
        if(emiter == mNormalTheme){
            setNormalTheme();
        }else  if(emiter == mNightTheme){
            setNightTheme();
        }
        changeRadioButtonPreferenceStatus(emiter);
    }

    protected  void changeRadioButtonPreferenceStatus(MyRadioButtonPreference emiter) {
        resetRadioButton();
        if (emiter == mNormalTheme){
            mNormalTheme.setChecked(true);
        }else if(emiter == mNightTheme){
            mNightTheme.setChecked(true);
        }
    }

    private void resetRadioButton () {
        mNormalTheme.setChecked(false);
        mNightTheme.setChecked(false);
    }

    private void updateNightThemeStatus(boolean activated){
        if(Settings.Secure.getInt(getContentResolver(),"night_theme_enabled", 0) == 1){
            //Begin modify by shilei.zhang for fixed XR7065062 on 2018/10/25
            if(activated){
                /*Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1);
                Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, 0);
                Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 1);*/
                Settings.Secure.putInt(getContentResolver(), "reading_mode_display_screen", 1);
                Settings.Secure.putInt(getContentResolver(), "night_theme_display_screen", 1);
            }else{
                if(Settings.System.getInt(context.getContentResolver(),"reading_mode_always_enable", -1) == 0){
                    /*Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
                    Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);*/
                    Settings.Secure.putInt(getContentResolver(), "reading_mode_display_screen", 0);
                    Settings.Secure.putInt(getContentResolver(), "night_theme_display_screen", 0);
                }
            }
            //End modify by shilei.zhang for fixed XR7065062 on 2018/10/25
        }else{
            if(Settings.System.getInt(context.getContentResolver(),"reading_mode_always_enable", 0) == 0){
                //Begin modify by shilei.zhang for fixed XR7065062 on 2018/10/25
                //Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
                Settings.Secure.putInt(getContentResolver(), "reading_mode_display_screen", 0);
                //End modify by shilei.zhang for fixed XR7065062 on 2018/10/25
            }
        }
    }
    private void updateRadioButtonPreferenceStatus(){
        if(mAutoSchedule.isChecked() || mTurnOnNow.isChecked()){
            mColorSpace.setEnabled(true);
            mNormalTheme.setEnabled(true);
            mNightTheme.setEnabled(true);
        }else{
            mColorSpace.setEnabled(false);
            mNormalTheme.setEnabled(false);
            mNightTheme.setEnabled(false);
        }
        if(Settings.Secure.getInt(getContentResolver(),"night_theme_enabled", 0) == 1){
            mNightTheme.setChecked(true);
            mNormalTheme.setChecked(false);
        }else{
            mNightTheme.setChecked(false);
            mNormalTheme.setChecked(true);
        }
    }

    private void setNormalTheme(){
        Settings.Secure.putInt(getContentResolver(),"night_theme_enabled", 0);
        if(mController.isActivated()){
            //Begin modify by shilei.zhang for fixed XR7065062 on 2018/10/25
            /*Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);*/
            Settings.Secure.putInt(getContentResolver(), "reading_mode_display_screen", 0);
            Settings.Secure.putInt(getContentResolver(), "night_theme_display_screen", 0);
            //End modify by shilei.zhang for fixed XR7065062 on 2018/10/25
        }
    }

    private void setNightTheme(){
        Settings.Secure.putInt(getContentResolver(),"night_theme_enabled", 1);
        if(mController.isActivated()){
            //Begin modify by shilei.zhang for fixed XR7065062 on 2018/10/25
            /*Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER, 0);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 1);*/
            Settings.Secure.putInt(getContentResolver(), "reading_mode_display_screen", 1);
            Settings.Secure.putInt(getContentResolver(), "night_theme_display_screen", 1);
            //End modify by shilei.zhang for fixed XR7065062 on 2018/10/25
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NIGHT_DISPLAY_SETTINGS;
    }
}
