/*
 * Add this file by shilei.zhang for Night mode Dev Automate schedule
 * And fixed XR5429845 on 2017/11/01
 * Path:Settings->Display->Night mode->Schedule plan
 */

package com.android.settings.display;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.widget.TimePicker;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;

import java.text.DateFormat;
import java.time.LocalTime;
import java.util.Calendar;
import java.util.TimeZone;
import android.util.Log;
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;

/**
 * Settings screen for Night mode.
 * modify this file by shilei.zhang for eye_comfort_mode ergo dev XR7033708 on 2018/09/25
 */
public class SchedulePlanSettings extends SettingsPreferenceFragment
        implements NightDisplayController.Callback, Preference.OnPreferenceChangeListener,MyRadioButtonPreference.OnClickListener {
    private static final String TAG = "SchedulePlanSettings";

    private static final String KEY_SUNSET_TO_SUNRISE = "sunset_to_sunrise";
    private static final String KEY_CUSTOM_SCHEDULE = "custom_schedule";
    private static final String KEY_NIGHT_DISPLAY_START_TIME = "night_display_start_time";
    private static final String KEY_NIGHT_DISPLAY_END_TIME = "night_display_end_time";

    private static final int DIALOG_START_TIME = 0;
    private static final int DIALOG_END_TIME = 1;

    private NightDisplayController mController;
    private DateFormat mTimeFormatter;

    private Preference mStartTimePreference;
    private Preference mEndTimePreference;
    private MyRadioButtonPreference mSunsetToSunrise;
    private MyRadioButtonPreference mCustomSchedule;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Context context = getContext();
        mController = new NightDisplayController(context);

        mTimeFormatter = android.text.format.DateFormat.getTimeFormat(context);
        mTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        // Load the preferences from xml.
        addPreferencesFromResource(R.xml.schedule_plan_preference);

        mSunsetToSunrise = (MyRadioButtonPreference)findPreference(KEY_SUNSET_TO_SUNRISE);
        mCustomSchedule = (MyRadioButtonPreference)findPreference(KEY_CUSTOM_SCHEDULE);
        mStartTimePreference = findPreference(KEY_NIGHT_DISPLAY_START_TIME);
        mEndTimePreference = findPreference(KEY_NIGHT_DISPLAY_END_TIME);

        mSunsetToSunrise.setOnClickListener(this);
        mCustomSchedule.setOnClickListener(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Listen for changes only while visible.
        mController.setListener(this);

        onCustomStartTimeChanged(mController.getCustomStartTime());
        onCustomEndTimeChanged(mController.getCustomEndTime());
        initRadioButtonPreferenceStatus();
    }

    @Override
    public void onStop() {
        super.onStop();

        // Stop listening for state changes.
        mController.setListener(null);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mStartTimePreference) {
            showDialog(DIALOG_START_TIME);
            return true;
        } else if (preference == mEndTimePreference) {
            showDialog(DIALOG_END_TIME);
            return true;
        }else if (preference == mSunsetToSunrise){
            Log.d(TAG,"mSunsetToSunrise onPreferenceTreeClick");
        }else if (preference == mCustomSchedule){
            Log.d(TAG,"mCustomSchedule onPreferenceTreeClick");
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public Dialog onCreateDialog(final int dialogId) {
        if (dialogId == DIALOG_START_TIME || dialogId == DIALOG_END_TIME) {
            final LocalTime initialTime;
            if (dialogId == DIALOG_START_TIME) {
                initialTime = mController.getCustomStartTime();
            } else {
                initialTime = mController.getCustomEndTime();
            }

            final Context context = getContext();
            final boolean use24HourFormat = android.text.format.DateFormat.is24HourFormat(context);
            return new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                    final LocalTime time = LocalTime.of(hourOfDay, minute);
                    if (dialogId == DIALOG_START_TIME) {
                        mController.setCustomStartTime(time);
                    } else {
                        mController.setCustomEndTime(time);
                    }
                }
            }, initialTime.getHour(), initialTime.getMinute(), use24HourFormat);
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_START_TIME:
                return MetricsEvent.DIALOG_NIGHT_DISPLAY_SET_START_TIME;
            case DIALOG_END_TIME:
                return MetricsEvent.DIALOG_NIGHT_DISPLAY_SET_END_TIME;
            default:
                return 0;
        }
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

    @Override
    public void onCustomStartTimeChanged(LocalTime startTime){
        mStartTimePreference.setSummary(getFormattedTimeString(startTime));
    }

    @Override
    public void onCustomEndTimeChanged(LocalTime endTime) {
        mEndTimePreference.setSummary(getFormattedTimeString(endTime));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference == mSunsetToSunrise){
            Log.d(TAG,"mSunsetToSunrise onPreferenceChange");
        }else if (preference == mCustomSchedule){
            Log.d(TAG,"mCustomSchedule onPreferenceChange");
        }
        return false;
    }

    @Override
    public void onRadioButtonClicked(MyRadioButtonPreference emiter) {
        if(emiter == mSunsetToSunrise){
            Settings.Secure.putInt(getContentResolver(), "ecm_auto_mode", 2);
            mController.setAutoMode(NightDisplayController.AUTO_MODE_TWILIGHT);
            mStartTimePreference.setEnabled(false);
            mEndTimePreference.setEnabled(false);
        }else  if(emiter == mCustomSchedule){
            Settings.Secure.putInt(getContentResolver(), "ecm_auto_mode", 1);
            mController.setAutoMode(NightDisplayController.AUTO_MODE_CUSTOM);
            mStartTimePreference.setEnabled(true);
            mEndTimePreference.setEnabled(true);
        }
        updateRadioButtonPreferenceStatus(emiter);
    }
    protected  void updateRadioButtonPreferenceStatus(MyRadioButtonPreference emiter) {
        resetRadioButton();
        if (emiter == mSunsetToSunrise){
            mSunsetToSunrise.setChecked(true);
        }else if(emiter == mCustomSchedule){
            mCustomSchedule.setChecked(true);
        }
    }
    private void resetRadioButton () {
        mSunsetToSunrise.setChecked(false);
        mCustomSchedule.setChecked(false);
    }

    private void initRadioButtonPreferenceStatus(){
        int autoMode = mController.getAutoMode();
        int temp = Settings.Secure.getInt(getContentResolver(), "ecm_auto_mode", 1);
        if(autoMode == NightDisplayController.AUTO_MODE_CUSTOM || temp == 1){
            Settings.Secure.putInt(getContentResolver(), "ecm_auto_mode", 1);
            mCustomSchedule.setChecked(true);
            mSunsetToSunrise.setChecked(false);
            mStartTimePreference.setEnabled(true);
            mEndTimePreference.setEnabled(true);
        }else{
            Settings.Secure.putInt(getContentResolver(), "ecm_auto_mode", 2);
            mSunsetToSunrise.setChecked(true);
            mCustomSchedule.setChecked(false);
            mStartTimePreference.setEnabled(false);
            mEndTimePreference.setEnabled(false);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NIGHT_MODE_SCHEDULE_PLAN;
    }
}
