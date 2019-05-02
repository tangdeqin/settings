package com.android.settings.notification;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.TwoStatePreference;
import android.text.TextUtils;
import android.util.Log;

import com.android.settings.DefaultRingtonePreference;
import com.android.settings.RingtonePreference;
import com.android.settings.SettingsPreferenceFragment;
//import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;

/**
 * Created for task 5571741 by yonglin.liao 2017/11/08.
 */

public class SoundDualSIM1 extends SettingsPreferenceFragment {

    private static final String KEY_SIM1_RINGTONE = "sound_dual_sim1_ringtone";
    private static final String KEY_SIM1_VIBRATE = "sound_dual_sim1_vibrate";
    private static final String TAG = "SoundDualSIM1";
    private static final String SELECTED_PREFERENCE_KEY = "selected_preference";
    private static final int REQUEST_CODE = 200;

    private DefaultRingtonePreference sim1Ringtone;
    private TwoStatePreference sim1Vibrate;
    private Context mContext;
    private RingtonePreference mRequestPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.sound_dual_sim1);
        mContext = getActivity();
        sim1Ringtone = (DefaultRingtonePreference)getPreferenceScreen().findPreference(KEY_SIM1_RINGTONE);
        sim1Vibrate = (TwoStatePreference) getPreferenceScreen().findPreference(KEY_SIM1_VIBRATE);
        sim1Vibrate.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                final boolean val = (Boolean) newValue;
                return Settings.System.putInt(getContentResolver(),
                        Settings.System.VIBRATE_WHEN_RINGING,
                        val ? 1 : 0);
            }
        });

        if (savedInstanceState != null) {
            String selectedPreference = savedInstanceState.getString(SELECTED_PREFERENCE_KEY, null);
            if (!TextUtils.isEmpty(selectedPreference)) {
                mRequestPreference = (RingtonePreference) findPreference(selectedPreference);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sim1Ringtone.setSummary(updateRingtoneName());
        updateRingVibrateSwitch();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference instanceof RingtonePreference) {
            mRequestPreference = (RingtonePreference) preference;
            mRequestPreference.onPrepareRingtonePickerIntent(mRequestPreference.getIntent());
            startActivityForResult(preference.getIntent(), REQUEST_CODE);
            return true;
        }

        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mRequestPreference != null) {
            mRequestPreference.onActivityResult(requestCode, resultCode, data);
            mRequestPreference = null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mRequestPreference != null) {
            outState.putString(SELECTED_PREFERENCE_KEY, mRequestPreference.getKey());
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SOUND_DUAL_SIM1_SETTINGS;
    }

    private CharSequence updateRingtoneName() {
        if (mContext == null) {
            Log.e(TAG, "Unable to update ringtone name, no context provided");
            return null;
        }
        Uri ringtoneUri = RingtoneManager.getActualDefaultRingtoneUri(mContext, RingtoneManager.TYPE_RINGTONE);
        CharSequence summary = mContext.getString(R.string.unknown);//[TCT-ROM][ Sound]Action by junliang.liu for XRP10025398 on 20181103
        // Is it a silent ringtone?
        if (ringtoneUri == null) {
            summary = mContext.getString(R.string.ringtone_silent);//[TCT-ROM][ Sound]Action by junliang.liu for XRP10025398 on 20181103
        } else {
            Cursor cursor = null;
            try {
                if (MediaStore.AUTHORITY.equals(ringtoneUri.getAuthority())) {
                    // Fetch the ringtone title from the media provider
                    cursor = mContext.getContentResolver().query(ringtoneUri,
                            new String[] { MediaStore.Audio.Media.TITLE }, null, null, null);
                } else if (ContentResolver.SCHEME_CONTENT.equals(ringtoneUri.getScheme())) {
                    cursor = mContext.getContentResolver().query(ringtoneUri,
                            new String[] { OpenableColumns.DISPLAY_NAME }, null, null, null);
                }
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        summary = cursor.getString(0);
                    }
                }
            } catch (SQLiteException sqle) {
                // Unknown title for the ringtone
            } catch (IllegalArgumentException iae) {
                // Some other error retrieving the column from the provider
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        return summary;
    }

    private void updateRingVibrateSwitch() {
        if (sim1Vibrate == null) return;
        sim1Vibrate.setChecked(Settings.System.getInt(getContentResolver(),
                Settings.System.VIBRATE_WHEN_RINGING, 0) != 0);
    }
}
