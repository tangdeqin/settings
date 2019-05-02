package com.android.settings.fingerprint.ex;

import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.provider.SearchIndexableResource;

import java.util.Arrays;
import java.util.List;
import com.tct.sdk.base.fingerprint.FingerprintConstants;

/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Created by jinlong.lu on 17-9-22.
 * for A3A Fingerprint Ergo
 */
//Begin added by jinlong.lu for XR6618444 on 18-7-31

public class FingerprintSensorSettings extends SubSettings {

    private static final String TAG = "FingerprintSensorSettings";
    //Begin modified by jinlong.lu for XRP23211 on 18-8-24
    private static boolean isPrivateModeSupport = false;
    //End modified by jinlong.lu for XRP23211 on 18-8-24
    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FpSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FpSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg = getText(R.string.fingerprints_sensor_fragment);
        setTitle(msg);
    }

    public static class FpSettingsFragment extends SettingsPreferenceFragment implements
            OnPreferenceChangeListener , Indexable {

        private final static String TAG = "FpSettingsFragment";

        public static final String KEY_FINGERPRINT_SETTINGS_FRAGMENT = "fingerprint_settings_fragment";
        private static final String KEY_BACKTO_HOME_SCREEN = "backto_home_screen_key";
        private static final String KEY_TAKE_PHOTO = "take_photo_key";
        private static final String KEY_PICK_UP_CALL = "pick_up_call";
        private static final String KEY_SHOW_NOTIFICATION = "show_notification_key";
        private static final String KEY_PREVIEW_ZOOM = "preview_zoom_key";
        private static final String KEY_SWITCH_PICTURE = "switch_picture_key";
        private static final String KEY_MANAGE_FINGERPRINTS = "key_manage_fingerprints";
        private static final String KEY_CLEAR_NOTIFICATION = "clear_notification";
        private static final String KEY_SWITCH_NORMAL_MODE = "switch_normal_mode";
        private static final String KEY_FINGERPRINTS_SENSOR_FRAGMENT ="key_fingerprints_sensor_fragment";
        private static final String KEY_FINGERPRINT_FUNCTIONS_TITLE="key_fingerprint_functions_title";
        private static final String KEY_FINGERPRINTS_ON_TOUCH="key_fingerprints_on_touch";
        private static final String SWITCH_PREFERENCE_KEYS[] = {
                KEY_BACKTO_HOME_SCREEN, KEY_TAKE_PHOTO, KEY_PICK_UP_CALL, KEY_SHOW_NOTIFICATION, KEY_PREVIEW_ZOOM,
                KEY_SWITCH_PICTURE, KEY_CLEAR_NOTIFICATION, KEY_SWITCH_NORMAL_MODE
        };

        private SwitchPreference mBackHome;
        private SwitchPreference mTakePhoto;
        private SwitchPreference mPickUpCall;
        private SwitchPreference mShowNotifi;
        private SwitchPreference mPreviewZoom;
        private SwitchPreference mSwitchPicture;
        private SwitchPreference mSwitchNormal;
        private SwitchPreference mClearAllNotification;
        private PreferenceCategory mfingerprintsOnTouch;
        private static int mUserId;
        private boolean isSwitchNormal;
        @Override
        public int getMetricsCategory() {
            return MetricsEvent.FINGERPRINT;
        }

        @Override
        public void onCreate(Bundle icicle) {
            super.onCreate(icicle);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onResume() {
            super.onResume();
            updatePreferences();
            if (mBackHome != null) {
                mBackHome.setChecked(Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_BACK_TO_HOME, 0) != 0);
            }
            if (mTakePhoto != null) {
                mTakePhoto.setChecked(Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_TAKE_PHOTO, 1) != 0);
            }
            if (mPickUpCall != null) {
                mPickUpCall.setChecked(Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_PICK_UP_CALL, 0) != 0);
            }
            if (mShowNotifi != null) {
                mShowNotifi.setChecked(Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_SHOW_NOTIFICATION, 1) != 0);
            }
            if (mSwitchPicture != null) {
                //old settings,deleted
                /*mSwitchPicture.setChecked(Settings.System.getInt(getContentResolver(),
                        "tct_fingerprint_switch_photo", 0) != 0);*/
            }
            if (mSwitchNormal != null) {
                mSwitchNormal.setChecked(Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_SWITCH_NORMAL_MODE, 0) != 0);
            }
            if (mClearAllNotification != null) {
                mClearAllNotification.setChecked(Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_CLEAR_NOTIFICATION, 1) != 0);
            }
            // private mode is off
            if (Settings.System.getInt(getContentResolver(),  FingerprintUtils.PRIVACY_MODE_ON, 0) == 0) {
                Settings.System.putInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_SWITCH_NORMAL_MODE, 0);
                mSwitchNormal.setChecked(false);
                mSwitchNormal.setEnabled(false);
            } else {
                mSwitchNormal.setEnabled(true);
            }
            isSwitchNormal =Settings.System.getInt(getContentResolver(),
                    FingerprintConstants.TCT_FINGERPRINT_SWITCH_NORMAL_MODE, 0) != 0;
            if(mClearAllNotification!=null){
                mClearAllNotification.setEnabled(!isSwitchNormal);
            }
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
        }

        public static Preference getFingerprintPreferenceForUser(Context context, final FragmentManager manager, int userId) {
            final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(context);
            if (fpm == null || !fpm.isHardwareDetected()) {
                Log.v(TAG, "No fingerprint hardware detected!!");
                return null;
            }
            mUserId = userId;
            Preference fingerprintPreference = new Preference(context);
            fingerprintPreference.setKey(KEY_FINGERPRINT_SETTINGS_FRAGMENT);
            fingerprintPreference.setTitle(R.string.fingerprints_sensor_fragment);
            return fingerprintPreference;
        }

        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            addPreferencesFromResource(R.xml.security_settings);
            root = getPreferenceScreen();
            addPreferencesFromResource(R.xml.fingerprint_sensor_fragment);
            mBackHome = (SwitchPreference) root.findPreference(KEY_BACKTO_HOME_SCREEN);
            mTakePhoto = (SwitchPreference) root.findPreference(KEY_TAKE_PHOTO);
            mPickUpCall = (SwitchPreference) root.findPreference(KEY_PICK_UP_CALL);
            mShowNotifi = (SwitchPreference) root.findPreference(KEY_SHOW_NOTIFICATION);
            mPreviewZoom = (SwitchPreference) root.findPreference(KEY_PREVIEW_ZOOM);
            mSwitchPicture = (SwitchPreference) root.findPreference(KEY_SWITCH_PICTURE);
            mClearAllNotification = (SwitchPreference) root.findPreference(KEY_CLEAR_NOTIFICATION);
            mSwitchNormal = (SwitchPreference) root.findPreference(KEY_SWITCH_NORMAL_MODE);
            mfingerprintsOnTouch = (PreferenceCategory) root.findPreference(KEY_FINGERPRINTS_ON_TOUCH);
            //Begin modified by jinlong.lu for XRP23211 on 18-8-24
            if (!isPrivateModeSupport) {
                mfingerprintsOnTouch.removePreference(mSwitchNormal);
            }
            //End modified by jinlong.lu for XRP23211 on 18-8-24


            final Preference mManageFingerprintsPref = root.findPreference(KEY_MANAGE_FINGERPRINTS);
            if (mManageFingerprintsPref != null) {
                mManageFingerprintsPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        final Context context = preference.getContext();
                        Intent intent = new Intent();
                        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);//add back bnt
                        intent.setClassName("com.android.settings", com.android.settings.fingerprint.FingerprintSettings.class.getName());
                        context.startActivity(intent);
                        return true;
                    }
                });
            }
            for (int i = 0; i < SWITCH_PREFERENCE_KEYS.length; i++) {
                final Preference pref = findPreference(SWITCH_PREFERENCE_KEYS[i]);
                if (pref != null)
                    pref.setOnPreferenceChangeListener(this);
            }
            return root;
        }

        private void addFingerprintPreference(PreferenceGroup category, int userId) {
            Preference fingerprintPreference =
                    com.android.settings.fingerprint.FingerprintSettings.getFingerprintPreferenceForUser(
                            category.getContext(), userId);
            if (fingerprintPreference != null) {
                category.addPreference(fingerprintPreference);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            boolean result = true;
            final String key = preference.getKey();
            switch (key) {
                case KEY_BACKTO_HOME_SCREEN:
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_BACK_TO_HOME,
                            (boolean) newValue ? 1 : 0);
                    break;
                case KEY_TAKE_PHOTO:
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_TAKE_PHOTO,
                            (boolean) newValue ? 1 : 0);
                    break;
                case KEY_PICK_UP_CALL:
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_PICK_UP_CALL,
                            (boolean) newValue ? 1 : 0);
                    break;
                case KEY_SHOW_NOTIFICATION:
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_SHOW_NOTIFICATION,
                            (boolean) newValue ? 1 : 0);
                    break;
                /*case KEY_SWITCH_PICTURE:
                    Settings.System.putInt(getContentResolver(),
                            "tct_fingerprint_switch_photo",
                            (boolean) newValue ? 1 : 0);
                    break;*/
                case KEY_CLEAR_NOTIFICATION:
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_CLEAR_NOTIFICATION,
                            (boolean) newValue ? 1 : 0);
                    if ((boolean) newValue) {
                        Settings.System.putInt(getContentResolver(),
                                FingerprintConstants.TCT_FINGERPRINT_SWITCH_NORMAL_MODE, 0);
                        if(mSwitchNormal!=null){
                            mSwitchNormal.setChecked(false);
                        }
                    }
                    break;
                case KEY_SWITCH_NORMAL_MODE:
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_SWITCH_NORMAL_MODE,
                            (boolean) newValue ? 1 : 0);
                    if ((boolean) newValue) {
                        //close clear notification switch
                        Settings.System.putInt(getContentResolver(),
                                FingerprintConstants.TCT_FINGERPRINT_CLEAR_NOTIFICATION, 0);
                        if(mClearAllNotification!=null){
                            mClearAllNotification.setChecked(false);
                            mClearAllNotification.setEnabled(false);
                        }
                    }else {
                        mClearAllNotification.setEnabled(true);
                    }
                    break;
                case KEY_MANAGE_FINGERPRINTS:
                    final Context context = preference.getContext();
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings", com.android.settings.fingerprint.FingerprintSettings.class.getName());
                    context.startActivity(intent);
                    break;

                default:
                    break;
            }
            return result;
        }
        public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
                new BaseSearchIndexProvider() {
                    @Override
                    public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                                boolean enabled) {
                        final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(context);
                        if (fpm != null && fpm.isHardwareDetected()) {
                            final SearchIndexableResource sir = new SearchIndexableResource(context);
                            sir.xmlResId = R.xml.fingerprint_sensor_fragment;
                            return Arrays.asList(sir);
                        }else {
                            return null;
                        }

                    }

                    @Override
                    public List<String> getNonIndexableKeys(Context context) {
                        List<String> keys = super.getNonIndexableKeys(context);
                        keys.add(KEY_FINGERPRINTS_SENSOR_FRAGMENT);
                        keys.add(KEY_FINGERPRINT_FUNCTIONS_TITLE);
                        //Begin modified by jinlong.lu for XRP23211 on 18-8-24
                        if (!isPrivateModeSupport) {
                            keys.add(KEY_SWITCH_NORMAL_MODE);
                        }
                        //End modified by jinlong.lu for XRP23211 on 18-8-24
                        return keys;
                    }
                };
    }
}