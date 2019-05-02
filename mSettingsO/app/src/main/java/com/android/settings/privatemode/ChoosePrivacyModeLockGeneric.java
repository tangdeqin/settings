package com.android.settings.privatemode;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v14.preference.SwitchPreference;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.RemovalCallback;
import android.util.EventLog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockPassword;
import com.android.settings.password.ChooseLockPattern;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.Utils;
import com.android.settings.EncryptionInterstitial;
import com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.fingerprint.FingerprintEnrollIntroduction;
import com.android.settings.fingerprint.FingerprintSettings;
import java.util.List;

import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;

import com.android.settings.R;
import com.android.settings.EventLogTags;

import com.tct.sdk.base.fingerprint.TctFingerprintUtils;

public class ChoosePrivacyModeLockGeneric extends SettingsActivity {
    public static final String CONFIRM_CREDENTIALS = "confirm_credentials";
    public static final String ACTION_SET_NEW_PRIVATE_PASSWORD = "android.app.action.SET_NEW_PRIVATE_PASSWORD";

    private static final int FP_TAG_COMMON = 0;
    private static final int FP_TAG_PRIVATE_MODE = FP_TAG_COMMON + 1;

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ChoosePrivacyModeLockGenericFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return ChoosePrivacyModeLockGenericFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class InternalActivity extends ChoosePrivacyModeLockGeneric {
    }

    public static class ChoosePrivacyModeLockGenericFragment extends SettingsPreferenceFragment implements OnPreferenceChangeListener {
        private static final String TAG = "ChoosePrivacyModeLockGenericFragment";
        private static final int MIN_PASSWORD_LENGTH = 4;
        private static final String KEY_UNLOCK_SET_OFF = "unlock_set_off";
        private static final String KEY_UNLOCK_SET_NONE = "unlock_set_none";
        private static final String KEY_UNLOCK_SET_PIN = "unlock_set_pin";
        private static final String KEY_UNLOCK_SET_PASSWORD = "unlock_set_password";
        private static final String KEY_UNLOCK_SET_PATTERN = "unlock_set_pattern";
        private static final String KEY_UNLOCK_SET_MANAGED = "unlock_set_managed";

        private static final String PASSWORD_CONFIRMED = "password_confirmed";    
        private static final String WAITING_FOR_CONFIRMATION = "waiting_for_confirmation";
        public static final String MINIMUM_QUALITY_KEY = "minimum_quality";
        public static final String HIDE_DISABLED_PREFS = "hide_disabled_prefs";
        public static final String ENCRYPT_REQUESTED_QUALITY = "encrypt_requested_quality";
        public static final String ENCRYPT_REQUESTED_DISABLED = "encrypt_requested_disabled";
        public static final String TAG_FRP_WARNING_DIALOG = "frp_warning_dialog";
        private static final String KEY_PRIVATE_FINGERPRINT_CATEGORY = "private_fingerprint_category";
        private static final String KEY_PRIVATE_FINGERPRINT = "private_fingerprint";

        private static final String KEY_PRIVATE_FINGERPRINT_UNLOCK = "private_fingerprint_unlock";
        private static final String KEY_ADD_PRIVATE_FINGERPRINT = "add_private_fingerprint";

        private static final String TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN = "tct_private_fingerprint_unlock_screen";
        private static final String TCT_UNLOCK_SCREEN = "tct_unlock_screen";

        private static final int CONFIRM_EXISTING_REQUEST = 100;
        private static final int ENABLE_ENCRYPTION_REQUEST = 101;
        private static final int CHOOSE_LOCK_REQUEST = 102;
        private static final int CHANGE_PASSWORD = 103;
        private static final int ADD_PRIVATE_FINGERPRINT_REQUEST = 104;
        private static final int ENABLE_FINGERPRINT_UNLOCK_SCREEEN_REQUEST = 105;

        private static final int CHANGE_PRIVATE_FINGERPRINT_REQUEST = 106;
        private static final int ADD_PRIVATE_FINGERPRINT_FOR_ENABLE_UNLOCK_REQUEST = 107;

        private static final int ENROLL_FINGERPRINT_UNLOCK_SCREEEN_REQUEST = 108;

        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private DevicePolicyManager mDPM;
        private boolean mHasChallenge = false;
        private long mChallenge;
        private boolean mPasswordConfirmed = false;
        private boolean mWaitingForConfirmation = false;
        private int mEncryptionRequestQuality;
        private boolean mEncryptionRequestDisabled;
        private boolean mRequirePassword;
        private String mUserPassword;
        private LockPatternUtils mLockPatternUtils;
        private FingerprintManager mFingerprintManager;

        private TctFingerprintUtils mTctFingerprintUtils;

        private int mUserId;
        private byte[] mToken;
        private static final String TOKEN = "token";
        private SwitchPreference mUnlockDevice;
        private int mPrivateFpId = -1;

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.CHOOSE_LOCK_GENERIC;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            mFingerprintManager =
                    (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
            mTctFingerprintUtils = new TctFingerprintUtils(getActivity());

            mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(this.getActivity());
            mLockPatternUtils = new LockPatternUtils(getActivity());

            // Defaults to needing to confirm credentials
            final boolean confirmCredentials = getActivity().getIntent()
                    .getBooleanExtra(CONFIRM_CREDENTIALS, true);
            if (getActivity() instanceof ChoosePrivacyModeLockGeneric.InternalActivity) {
                mPasswordConfirmed = !confirmCredentials;
            }

            mHasChallenge = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            mChallenge = getActivity().getIntent().getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);

            if (savedInstanceState != null) {
                mPasswordConfirmed = savedInstanceState.getBoolean(PASSWORD_CONFIRMED);
                mWaitingForConfirmation = savedInstanceState.getBoolean(WAITING_FOR_CONFIRMATION);
                mEncryptionRequestQuality = savedInstanceState.getInt(ENCRYPT_REQUESTED_QUALITY);
                mEncryptionRequestDisabled = savedInstanceState.getBoolean(ENCRYPT_REQUESTED_DISABLED);
            int targetUser = Utils.getSecureTargetUser(
                    getActivity().getActivityToken(),
                    UserManager.get(getActivity()),
                    null,
                    getActivity().getIntent().getExtras()).getIdentifier();
                mToken = savedInstanceState.getByteArray(TOKEN);
            }

            mUserId = Utils.getSecureTargetUser(
                    getActivity().getActivityToken(),
                    UserManager.get(getActivity()),
                    getArguments(),
                    getActivity().getIntent().getExtras()).getIdentifier();

            if (mPasswordConfirmed) {
                updatePreferencesOrFinish();
            } else if (!mWaitingForConfirmation) {
                ChooseLockSettingsHelper helper =
                        new ChooseLockSettingsHelper(this.getActivity(), this);
                //todo: check the code
                if(helper.launchPrivacyModeConfirmationActivity(CONFIRM_EXISTING_REQUEST,
                        getString(R.string.private_mode),
                        null, null, mChallenge)) {
                    mPasswordConfirmed = true; // no password set, so no need to confirm
                    updatePreferencesOrFinish();
                } else {
                    mWaitingForConfirmation = true;
                }
            }

            addHeaderView();
        }

        protected void addHeaderView() {
            setHeaderView(R.layout.choose_lock_generic_privacy_mode_header);
        }


        @Override
        public boolean onPreferenceTreeClick(Preference preference) {
            final String key = preference.getKey();
            if(null == key){
                return true;
            }
            if (!isUnlockMethodSecure(key) && mLockPatternUtils.isSecure(mUserId)) {
                // Show the disabling FRP warning only when the user is switching from a secure
                // unlock method to an insecure one
                showFactoryResetProtectionWarningDialog(key);
                return true;
            } else if (KEY_PRIVATE_FINGERPRINT.equals(key)) {
                showChangeRenameDeleteDialog();
                return true;
            } else if (KEY_PRIVATE_FINGERPRINT_UNLOCK.equals(key)) {
                return true;
            } else if (KEY_ADD_PRIVATE_FINGERPRINT.equals(key)) {
                addPrivateFingerprint();
                return true;
            }
            else {
                return setUnlockMethod(key);
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            String key = preference.getKey();
            if (KEY_PRIVATE_FINGERPRINT_UNLOCK.equals(key)) {
                if ((Boolean) (value)) {
                    return enblePrivateFingerprintUnlockDevice();
                } else {
                    Settings.System.putInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 0);
                }
            }
            return true;
        }


        /**
         * If the device has encryption already enabled, then ask the user if they
         * also want to encrypt the phone with this password.
         *
         * @param quality
         * @param disabled
         */
        // TODO: why does this take disabled, its always called with a quality higher than
        // what makes sense with disabled == true
        private void maybeEnableEncryption(int quality, boolean disabled) {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
            if (UserManager.get(getActivity()).isAdminUser()
                    && LockPatternUtils.isDeviceEncryptionEnabled()
                    && !dpm.getDoNotAskCredentialsOnBoot()) {
                mEncryptionRequestQuality = quality;
                mEncryptionRequestDisabled = disabled;
                // Get the intent that the encryption interstitial should start for creating
                // the new unlock method.
                Intent unlockMethodIntent = getIntentForUnlockMethod(quality, disabled);
                startActivityForResult(unlockMethodIntent, CHANGE_PASSWORD);
            } else {
                mRequirePassword = false; // device encryption not enabled or not device owner.
                updateUnlockMethodAndFinish(quality, disabled);
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            mWaitingForConfirmation = false;
            if (requestCode == CONFIRM_EXISTING_REQUEST && resultCode == Activity.RESULT_OK) {
                mPasswordConfirmed = true;
                mUserPassword = data.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                updatePreferencesOrFinish();
                if (data != null) {
                    mToken = data.getByteArrayExtra(
                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                }
            } else if (requestCode == ENABLE_ENCRYPTION_REQUEST
                    && resultCode == Activity.RESULT_OK) {
                mRequirePassword = data.getBooleanExtra(
                        EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true);
                updateUnlockMethodAndFinish(mEncryptionRequestQuality, mEncryptionRequestDisabled);
            } else if (requestCode == CHOOSE_LOCK_REQUEST) {
                getActivity().setResult(resultCode, data);
                finish();
            } else if (requestCode == CHANGE_PASSWORD && resultCode == Activity.RESULT_OK) {
                updatePreferenceSummary();
                if (data != null) {
                    mToken = data.getByteArrayExtra(
                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                }
            } else if (requestCode == ENABLE_FINGERPRINT_UNLOCK_SCREEEN_REQUEST) {
                if(isFingerPrintUnlockSettingEnabled()){
                    enablePrivateFingerprintToUnclockScreen();
                }
            } else if (requestCode == ENROLL_FINGERPRINT_UNLOCK_SCREEEN_REQUEST) {
                if(isEnrolledDeviceFingerprint()){
                    android.provider.Settings.System.putInt(getContentResolver(), TCT_UNLOCK_SCREEN, 1);
                    if(isEnrolledPrivateFingerprint()){
                        enablePrivateFingerprintToUnclockScreen();
                    }
                }
            } else if (requestCode == ADD_PRIVATE_FINGERPRINT_REQUEST) {
                if(isFingerPrintUnlockSettingEnabled()){
                    enablePrivateFingerprintToUnclockScreen();
                }
                updateFingerprintPreference();
                updatePrivateFingerprintUnlockPreference();
            } else if (requestCode == CHANGE_PRIVATE_FINGERPRINT_REQUEST) {

                List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
                if (items != null && items.size() > 1) {
                    for (int i=0; i<items.size(); i++) {
                        if (items.get(i).getFingerId() == mPrivateFpId) {
                            mFingerprintManager.remove(items.get(i), mUserId, mRemoveOriCallback);
                            break;
                        }
                    }
                }
            } else if (requestCode == ADD_PRIVATE_FINGERPRINT_FOR_ENABLE_UNLOCK_REQUEST) {
                List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
                if (items != null && items.size() > 0) {
                    enablePrivateFingerprintToUnclockScreen();
                    updateFingerprintPreference();
                }
            } else {
                getActivity().setResult(Activity.RESULT_CANCELED);
                finish();
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            // Saved so we don't force user to re-enter their password if configuration changes
            outState.putBoolean(PASSWORD_CONFIRMED, mPasswordConfirmed);
            outState.putBoolean(WAITING_FOR_CONFIRMATION, mWaitingForConfirmation);
            outState.putInt(ENCRYPT_REQUESTED_QUALITY, mEncryptionRequestQuality);
            outState.putBoolean(ENCRYPT_REQUESTED_DISABLED, mEncryptionRequestDisabled);
			outState.putByteArray(TOKEN, mToken);
        }

        private void updatePreferencesOrFinish() {
            Intent intent = getActivity().getIntent();
            int quality = intent.getIntExtra(LockPatternUtils.PASSWORD_TYPE_KEY, -1);
            if (quality == -1) {
                // If caller didn't specify password quality, show UI and allow the user to choose.
                quality = intent.getIntExtra(MINIMUM_QUALITY_KEY, -1);
                quality = upgradeQuality(quality);
                final boolean hideDisabledPrefs = intent.getBooleanExtra(
                        HIDE_DISABLED_PREFS, false);
                final PreferenceScreen prefScreen = getPreferenceScreen();
                if (prefScreen != null) {
                    prefScreen.removeAll();
                }
                addPreferences();
                disableUnusablePreferences(quality, hideDisabledPrefs);
                updateCurrentPreference();
                updatePreferenceSummaryIfNeeded();
            } else {
                updateUnlockMethodAndFinish(quality, false);
            }
        }

        protected void addPreferences() {
            addPreferencesFromResource(R.xml.security_settings_picker_for_privacy_mode);

            // Used for testing purposes
            if (null == mFingerprintManager || !mFingerprintManager.isHardwareDetected()) {
                removePreference(KEY_PRIVATE_FINGERPRINT_CATEGORY);
                removePreference(KEY_PRIVATE_FINGERPRINT_UNLOCK);
            } else {
                updateFingerprintPreference();
                mUnlockDevice = (SwitchPreference) findPreference(KEY_PRIVATE_FINGERPRINT_UNLOCK);
                mUnlockDevice.setOnPreferenceChangeListener(this);
                updatePrivateFingerprintUnlockPreference();
            }
        }



        private void updateCurrentPreference() {
            String currentKey = getKeyForCurrent();
            Preference preference = findPreference(currentKey);
            if (preference != null) {
                preference.setSummary(R.string.current_screen_lock);
            }
        }

        private String getKeyForCurrent() {
            final int credentialOwner = UserManager.get(getContext())
                    .getCredentialOwnerProfile(mUserId);
            if (mLockPatternUtils.isLockScreenDisabled(credentialOwner)) {
                return KEY_UNLOCK_SET_OFF;
            }
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(credentialOwner)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return KEY_UNLOCK_SET_PATTERN;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return KEY_UNLOCK_SET_PIN;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return KEY_UNLOCK_SET_PASSWORD;
                case DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED:
                    return KEY_UNLOCK_SET_NONE;
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    return KEY_UNLOCK_SET_MANAGED;
            }

            Log.e(TAG, "get unknown unlock method (" + credentialOwner + ") and return null");
            return null;
        }

        /** increases the quality if necessary */
        private int upgradeQuality(int quality) {
            quality = upgradeQualityForDPM(quality);
            return quality;
        }

        private int upgradeQualityForDPM(int quality) {
            // Compare min allowed password quality
            int minQuality = mDPM.getPasswordQuality(null, mUserId);
            if (quality < minQuality) {
                quality = minQuality;
            }
            return quality;
        }

        /***
         * Disables preferences that are less secure than required quality. The actual
         * implementation is in disableUnusablePreferenceImpl.
         *
         * @param quality the requested quality.
         * @param hideDisabledPrefs if false preferences show why they were disabled; otherwise
         * they're not shown at all.
         */
        protected void disableUnusablePreferences(final int quality, boolean hideDisabledPrefs) {
            disableUnusablePreferencesImpl(quality, hideDisabledPrefs);
        }

        /***
         * Disables preferences that are less secure than required quality.
         *
         * @param quality the requested quality.
         * @param hideDisabled whether to hide disable screen lock options.
         */
        protected void disableUnusablePreferencesImpl(final int quality,
                                                      boolean hideDisabled) {
            final PreferenceScreen entries = getPreferenceScreen();

            for (int i = entries.getPreferenceCount() - 1; i >= 0; --i) {
                Preference pref = entries.getPreference(i);
                if (pref instanceof PreferenceScreen) {
                    final String key = pref.getKey();
                    boolean enabled = true;
                    boolean visible = true;
                    if (KEY_UNLOCK_SET_OFF.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                    } else if (KEY_UNLOCK_SET_NONE.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
                    } else if (KEY_UNLOCK_SET_PATTERN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
                    } else if (KEY_UNLOCK_SET_PIN.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
                    } else if (KEY_UNLOCK_SET_PASSWORD.equals(key)) {
                        enabled = quality <= DevicePolicyManager.PASSWORD_QUALITY_COMPLEX;
                    }
                    if (hideDisabled) {
                        visible = enabled;
                    }
                    if (!visible) {
                        entries.removePreference(pref);
                    } else if (!enabled) {
                        pref.setSummary(R.string.unlock_set_unlock_disabled_summary);
                        pref.setEnabled(false);
                    }
                }
            }
        }

        private void updatePreferenceSummaryIfNeeded() {

            if (AccessibilityManager.getInstance(getActivity()).getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_ALL_MASK).isEmpty()) {
                return;
            }

            CharSequence summary = getString(R.string.secure_lock_encryption_warning);

            PreferenceScreen screen = getPreferenceScreen();
            final int preferenceCount = screen.getPreferenceCount();
            for (int i = 0; i < preferenceCount; i++) {
                Preference preference = screen.getPreference(i);
                if(null == preference.getKey()){
                    continue;
                }
                switch (preference.getKey()) {
                    case KEY_UNLOCK_SET_PATTERN:
                    case KEY_UNLOCK_SET_PIN:
                    case KEY_UNLOCK_SET_PASSWORD: {
                        preference.setSummary(summary);
                    } break;
                }
            }
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                                               int minLength, final int maxLength,
                                               boolean requirePasswordToDecrypt, boolean confirmCredentials) {
            return ChoosePrivacyModeLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, confirmCredentials);
        }

        protected Intent getLockPasswordIntent(Context context, int quality,
                                               int minLength, final int maxLength,
                                               boolean requirePasswordToDecrypt, long challenge) {
            return ChoosePrivacyModeLockPassword.createIntent(context, quality, minLength,
                    maxLength, requirePasswordToDecrypt, challenge);
        }

        protected Intent getLockPasswordIntent(Context context, int quality, int minLength,
                                               final int maxLength, boolean requirePasswordToDecrypt, String password) {
            return ChoosePrivacyModeLockPassword.createIntent(context, quality, minLength, maxLength,
                    requirePasswordToDecrypt, password);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                                              final boolean confirmCredentials) {
            return ChoosePrivacyModeLockPattern.createIntent(context, requirePassword,
                    confirmCredentials);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                                              long challenge) {
            return ChoosePrivacyModeLockPattern.createIntent(context, requirePassword, challenge);
        }

        protected Intent getLockPatternIntent(Context context, final boolean requirePassword,
                                              final String pattern) {
            return ChoosePrivacyModeLockPattern.createIntent(context, requirePassword, pattern);
        }

        protected Intent getEncryptionInterstitialIntent(Context context, int quality,
                                                         boolean required, Intent unlockMethodIntent) {
            return EncryptionInterstitial.createStartIntent(context, quality, required, unlockMethodIntent);
        }

        /**
         * Invokes an activity to change the user's pattern, password or PIN based on given quality
         * and minimum quality specified by DevicePolicyManager. If quality is
         * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}, password is cleared.
         *
         * @param quality the desired quality. Ignored if DevicePolicyManager requires more security
         * @param disabled whether or not to show LockScreen at all. Only meaningful when quality is
         * {@link DevicePolicyManager#PASSWORD_QUALITY_UNSPECIFIED}
         */
        void updateUnlockMethodAndFinish(int quality, boolean disabled) {
            // Sanity check. We should never get here without confirming user's existing password.
            if (!mPasswordConfirmed) {
                throw new IllegalStateException("Tried to update password without confirming it");
            }

            quality = upgradeQuality(quality);

            final Context context = getActivity();
            if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                int minLength = mDPM.getPasswordMinimumLength(null);
                if (minLength < MIN_PASSWORD_LENGTH) {
                    minLength = MIN_PASSWORD_LENGTH;
                }
                final int maxLength = mDPM.getPasswordMaximumLength(quality);
                Intent intent;
                if (mHasChallenge) {
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mChallenge);
                } else {
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mUserPassword);
                }
                startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                Intent intent;
                if (mHasChallenge) {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mChallenge);
                } else {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mUserPassword);
                }
                startActivityForResult(intent, CHOOSE_LOCK_REQUEST);
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
                mChooseLockSettingsHelper.utils().clearLock(mUserPassword, mUserId);
                mChooseLockSettingsHelper.utils().setLockScreenDisabled(disabled,
                        mUserId);
                removeAllFingerprintTemplatesAndFinish();
                getActivity().setResult(Activity.RESULT_OK);
            } else {
                removeAllFingerprintTemplatesAndFinish();
            }
        }

        private void removeAllFingerprintTemplatesAndFinish() {
            if (mFingerprintManager != null && mFingerprintManager.isHardwareDetected()
                    && mFingerprintManager.getEnrolledFingerprints().size() > 0) {
                mFingerprintManager.remove(new Fingerprint(null, 0, 0, 0), mUserId,
                        new RemovalCallback() {
                            @Override
                            public void onRemovalError(Fingerprint fp, int errMsgId,
                                                       CharSequence errString) {
                                Log.e(TAG, String.format("Can't remove fingerprint %d in group %d. Reason: %s", fp.getFingerId(), fp.getGroupId(), errString));
                            }

                            @Override
                            public void onRemovalSucceeded(Fingerprint fp, int remaining) {
                                Log.v(TAG, "Fingerprint removed: " + fp.getFingerId());
                                if (mFingerprintManager.getEnrolledFingerprints().size() == 0) {
                                    finish();
                                }
                            }
                        });
            } else {
                finish();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
        }

        @Override
        protected int getHelpResource() {
            return R.string.help_url_choose_lockscreen;
        }

        private int getResIdForFactoryResetProtectionWarningMessage() {
            boolean hasFingerprints = (null == mFingerprintManager) ? false :
                    mFingerprintManager.hasEnrolledFingerprints();
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_pattern_fingerprint
                            : R.string.unlock_disable_frp_warning_content_pattern;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_pin_fingerprint
                            : R.string.unlock_disable_frp_warning_content_pin;
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_password_fingerprint
                            : R.string.unlock_disable_frp_warning_content_password;
                default:
                    return hasFingerprints
                            ? R.string.unlock_disable_frp_warning_content_unknown_fingerprint
                            : R.string.unlock_disable_frp_warning_content_unknown;
            }
        }

        private boolean isUnlockMethodSecure(String unlockMethod) {
            return !(KEY_UNLOCK_SET_OFF.equals(unlockMethod) ||
                    KEY_UNLOCK_SET_NONE.equals(unlockMethod));
        }

        private boolean setUnlockMethod(String unlockMethod) {
            EventLog.writeEvent(EventLogTags.LOCK_SCREEN_TYPE, unlockMethod);

            String currentKey = getKeyForCurrent();
            if (null == currentKey || !unlockMethod.equals(currentKey)) {
                showChangeScreenlockMethodDialog();
                return true;
            }

            if (KEY_UNLOCK_SET_OFF.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, true /* disabled */ );
            } else if (KEY_UNLOCK_SET_NONE.equals(unlockMethod)) {
                updateUnlockMethodAndFinish(
                        DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED, false /* disabled */ );
            } else if (KEY_UNLOCK_SET_PATTERN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING, false);
            } else if (KEY_UNLOCK_SET_PIN.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX, false);
            } else if (KEY_UNLOCK_SET_PASSWORD.equals(unlockMethod)) {
                maybeEnableEncryption(
                        DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC, false);
            }else {
                Log.e(TAG, "Encountered unknown unlock method to set: " + unlockMethod);
                return false;
            }
            return true;
        }

        private void showFactoryResetProtectionWarningDialog(String unlockMethodToSet) {
            int message = getResIdForFactoryResetProtectionWarningMessage();
            FactoryResetProtectionWarningDialog dialog =
                    FactoryResetProtectionWarningDialog.newInstance(message, unlockMethodToSet);
            dialog.show(getChildFragmentManager(), TAG_FRP_WARNING_DIALOG);
        }

        public static class FactoryResetProtectionWarningDialog extends DialogFragment {

            private static final String ARG_MESSAGE_RES = "messageRes";
            private static final String ARG_UNLOCK_METHOD_TO_SET = "unlockMethodToSet";

            public static FactoryResetProtectionWarningDialog newInstance(int messageRes,
                                                                          String unlockMethodToSet) {
                FactoryResetProtectionWarningDialog frag =
                        new FactoryResetProtectionWarningDialog();
                Bundle args = new Bundle();
                args.putInt(ARG_MESSAGE_RES, messageRes);
                args.putString(ARG_UNLOCK_METHOD_TO_SET, unlockMethodToSet);
                frag.setArguments(args);
                return frag;
            }

            @Override
            public void show(FragmentManager manager, String tag) {
                if (manager.findFragmentByTag(tag) == null) {
                    // Prevent opening multiple dialogs if tapped on button quickly
                    super.show(manager, tag);
                }
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                final Bundle args = getArguments();

                return new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.unlock_disable_frp_warning_title)
                        .setMessage(args.getInt(ARG_MESSAGE_RES))
                        .setPositiveButton(R.string.unlock_disable_frp_warning_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ((ChoosePrivacyModeLockGenericFragment) getParentFragment())
                                                .setUnlockMethod(
                                                        args.getString(ARG_UNLOCK_METHOD_TO_SET));
                                    }
                                }
                        )
                        .setNegativeButton(R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dismiss();
                                    }
                                }
                        )
                        .create();
            }
        }

        // add by hunter begin
        private Intent getIntentForUnlockMethod(int quality, boolean disabled) {
            Intent intent = null;
            final Context context = getActivity();
            if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
                int minLength = mDPM.getPasswordMinimumLength(null, mUserId);
                if (minLength < MIN_PASSWORD_LENGTH) {
                    minLength = MIN_PASSWORD_LENGTH;
                }
                final int maxLength = mDPM.getPasswordMaximumLength(quality);
                if (mHasChallenge) {
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mChallenge);
                } else {
                    intent = getLockPasswordIntent(context, quality, minLength,
                            maxLength, mRequirePassword, mUserPassword);
                }
            } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
                if (mHasChallenge) {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mChallenge);
                } else {
                    intent = getLockPatternIntent(context, mRequirePassword,
                            mUserPassword);
                }
            }
            return intent;
        }

        private void showChangeScreenlockMethodDialog() {
            new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.change_screen_lock_method)
                    .setMessage(R.string.change_screen_lock_method_msg)
                    .setPositiveButton(R.string.dlg_ok,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent intent = new Intent(getActivity(), ChooseLockGeneric.class);
                                    intent.setAction(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                                    intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_UNLOCK_SCREEN_PREF,
                                            true);
                                    intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                                            DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                                    intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHANGE_NORMAL_AND_PRIVACY_PASSWORD, true);
                                    //Begin modified by dongchi.chen for XRP23578 on 18-9-8
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, mChallenge);
                                    //End modified by dongchi.chen for XRP23578 on 18-9-8
                                    startActivityForResult(intent, CHANGE_PASSWORD);
                                }
                            }
                    )
                    .setNegativeButton(R.string.cancel, null)
                    .create()
                    .show();
        }

        private void updatePreferenceSummary() {
            Preference pattern = findPreference(KEY_UNLOCK_SET_PATTERN);
            if (pattern != null) {
                pattern.setSummary(null);
            }
            Preference pin = findPreference(KEY_UNLOCK_SET_PIN);
            if (pin != null) {
                pin.setSummary(null);
            }
            Preference password = findPreference(KEY_UNLOCK_SET_PASSWORD);
            if (password != null) {
                password.setSummary(null);
            }
            updateCurrentPreference();
            updatePreferenceSummaryIfNeeded();
        }

        private void updateFingerprintPreferenceSummary() {
            Preference fingerprint = findPreference(KEY_PRIVATE_FINGERPRINT);
            if (fingerprint == null) {
                return;
            }

            if(!isFingerPrintUnlockSettingEnabled()){
                fingerprint.setSummary(R.string.fingerprint_unlock_device_off);
            } else {

                List<Fingerprint> privateFp = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
                if (privateFp != null && privateFp.size() > 0) {
                    fingerprint.setSummary(privateFp.get(0).getName());
                } else {
                    fingerprint.setSummary(R.string.no_private_fingerprint);
                }
            }
        }

        private boolean enblePrivateFingerprintUnlockDevice() {
            boolean result = false;
            if(!isFingerPrintUnlockSettingEnabled()){
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setCancelable(false)
                        .setTitle(R.string.turn_on_unlock_device)
                        .setMessage(R.string.turn_on_unlock_device_msg)
                        .setPositiveButton(R.string.go_label, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                Intent intent = new Intent();
                                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FROM_PRIVATE_MODE_SETTINGS, true);
                                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, mChallenge);
                                List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_COMMON);
                                int fingerprintCount = items != null ? items.size() : 0;
                                String clazz;
                                if (fingerprintCount > 0) {
                                    clazz = FingerprintSettings.class.getName();
                                    intent.setClassName("com.android.settings", clazz);
                                    startActivityForResult(intent, ENABLE_FINGERPRINT_UNLOCK_SCREEEN_REQUEST);
                                } else {
                                    clazz = FingerprintEnrollIntroduction.class.getName();
                                    intent.setClassName("com.android.settings", clazz);
                                    startActivityForResult(intent, ENROLL_FINGERPRINT_UNLOCK_SCREEEN_REQUEST);
                                }
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .show();
            } else {
                List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
                int fingerprintCount = items != null ? items.size() : 0;
                if (fingerprintCount == 0) {
                    AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                            .setCancelable(false)
                            .setTitle(R.string.set_private_fingerprint)
                            .setMessage(R.string.set_private_fingerprint_msg)
                            .setPositiveButton(R.string.lockpassword_continue_label, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    Intent intent = new Intent();
                                    intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                                    intent.setClassName("com.android.settings",
                                            FingerprintEnrollEnrolling.class.getName());
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_ENROLL_FINGERPRINT_TAG, FP_TAG_PRIVATE_MODE);
                                    startActivityForResult(intent, ADD_PRIVATE_FINGERPRINT_FOR_ENABLE_UNLOCK_REQUEST);
                                }
                            })
                            .setNegativeButton(R.string.dlg_cancel, null)
                            .show();
                } else {
                    enablePrivateFingerprintToUnclockScreen();
                    result = true;
                }
            }
            return result;
        }


        private void updateFingerprintPreference() {
            PreferenceScreen root = getPreferenceScreen();
            if (root == null) {
                return;
            }
            PreferenceCategory fpCategory;
            fpCategory = (PreferenceCategory) root.findPreference(KEY_PRIVATE_FINGERPRINT_CATEGORY);
            if (fpCategory == null) {
                return;
            }
            fpCategory.removeAll();
            List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
            int fingerprintCount = items != null ? items.size() : 0;
            if (fingerprintCount > 0) {
                Preference fpPreference = new Preference(root.getContext());
                fpPreference.setKey(KEY_PRIVATE_FINGERPRINT);
                fpPreference.setTitle(items.get(0).getName());
                fpPreference.setSummary(R.string.private_fingerprint_summary);
                fpCategory.addPreference(fpPreference);
            } else {
                Preference addPreference = new Preference(root.getContext());
                addPreference.setKey(KEY_ADD_PRIVATE_FINGERPRINT);
                addPreference.setTitle(R.string.add_private_fingerprint);
                addPreference.setIcon(R.drawable.ic_add_24dp);
                fpCategory.addPreference(addPreference);
                mPrivateFpId = -1;
            }
        }

        private void updatePrivateFingerprintUnlockPreference() {
            boolean enablePrivateFingerPrint = Settings.System.getInt(getContentResolver(),
                    TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 0) == 1;
            if (enablePrivateFingerPrint && isEnrolledPrivateFingerprint()) {
                mUnlockDevice.setChecked(true);
            } else {
                mUnlockDevice.setChecked(false);
            }
        }

        private void enablePrivateFingerprintToUnclockScreen(){
            android.provider.Settings.System.putInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 1);
            mUnlockDevice.setChecked(true);
        }

        private void disablePrivateFingerprintToUnclockScreen(){
            android.provider.Settings.System.putInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 0);
            mUnlockDevice.setChecked(false);
        }

        public static class ChangeRenameDeleteDialog extends DialogFragment {

            private Fingerprint mFp;
            private EditText mDialogTextField;
            private String mFingerName;
            private Boolean mTextHadFocus;
            private int mTextSelectionStart;
            private int mTextSelectionEnd;

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                if (savedInstanceState != null) {
                    mFingerName = savedInstanceState.getString("fingerName");
                    mTextHadFocus = savedInstanceState.getBoolean("textHadFocus");
                    mTextSelectionStart = savedInstanceState.getInt("startSelection");
                    mTextSelectionEnd = savedInstanceState.getInt("endSelection");
                }
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setView(R.layout.fingerprint_rename_dialog)
                        .setPositiveButton(R.string.dlg_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String newName =
                                                mDialogTextField.getText().toString();
                                        final CharSequence name = mFp.getName();
                                        if (!newName.equals(name)) {
                                            ChoosePrivacyModeLockGenericFragment parent
                                                    = (ChoosePrivacyModeLockGenericFragment)
                                                    getTargetFragment();
                                            parent.renamePrivateFingerprint(mFp.getFingerId(), newName);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        .setNeutralButton(
                                R.string.dlg_change,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        ChoosePrivacyModeLockGenericFragment parent
                                                = (ChoosePrivacyModeLockGenericFragment)
                                                getTargetFragment();
                                        parent.changePrivateFingerprint();
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        ChoosePrivacyModeLockGenericFragment parent
                                                = (ChoosePrivacyModeLockGenericFragment) getTargetFragment();
                                        parent.deletePrivateFingerprint();
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        mDialogTextField = (EditText) alertDialog.findViewById(
                                R.id.fingerprint_rename_field);
                        CharSequence name = mFingerName == null ? mFp.getName() : mFingerName;
                        mDialogTextField.setText(name);
                        if (mTextHadFocus == null) {
                            mDialogTextField.selectAll();
                        } else {
                            mDialogTextField.setSelection(mTextSelectionStart, mTextSelectionEnd);
                        }

                        mDialogTextField.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                if(s == null || s.toString().trim().equals("")){
                                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                }else{
                                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                                }
                            }
                        });
                    }
                });
                if (mTextHadFocus == null || mTextHadFocus) {
                    // Request the IME
                    alertDialog.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                return alertDialog;
            }

            @Override
            public void onSaveInstanceState(Bundle outState) {
                super.onSaveInstanceState(outState);
                if (mDialogTextField != null) {
                    outState.putString("fingerName", mDialogTextField.getText().toString());
                    outState.putBoolean("textHadFocus", mDialogTextField.hasFocus());
                    outState.putInt("startSelection", mDialogTextField.getSelectionStart());
                    outState.putInt("endSelection", mDialogTextField.getSelectionEnd());
                }
            }
        }

        private void showChangeRenameDeleteDialog() {
            List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
            if (items != null && items.size() > 0) {
                mPrivateFpId = items.get(0).getFingerId();
                ChangeRenameDeleteDialog dlg = new ChangeRenameDeleteDialog();
                Bundle args = new Bundle();
                args.putParcelable("fingerprint", items.get(0));
                dlg.setArguments(args);
                dlg.setTargetFragment(this, 0);
                dlg.show(getFragmentManager(), ChangeRenameDeleteDialog.class.getName());
            }
        }

        private void addPrivateFingerprint() {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings",
                    FingerprintEnrollEnrolling.class.getName());
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_ENROLL_FINGERPRINT_TAG, FP_TAG_PRIVATE_MODE);
            startActivityForResult(intent, ADD_PRIVATE_FINGERPRINT_REQUEST);
        }

        private void renamePrivateFingerprint(int fingerId, String newName) {
            mFingerprintManager.rename(fingerId, mUserId, newName);
            updateFingerprintPreference();
        }

        private void changePrivateFingerprint() {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings",
                    FingerprintEnrollEnrolling.class.getName());
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_ENROLL_FINGERPRINT_TAG, FP_TAG_PRIVATE_MODE);
            startActivityForResult(intent, CHANGE_PRIVATE_FINGERPRINT_REQUEST);
        }

        private void deletePrivateFingerprint() {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setCancelable(false)
                    .setTitle(R.string.delete_private_fingerprint)
                    .setMessage(R.string.delete_private_fingerprint_msg)
                    .setPositiveButton(R.string.delete_label, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
                            if (items != null && items.size() > 0) {
                                mFingerprintManager.remove(items.get(0), mUserId, mRemoveCallback);
                            }
                        }
                    })
                    .setNegativeButton(R.string.dlg_cancel, null)
                    .show();
        }

        private RemovalCallback mRemoveCallback = new RemovalCallback() {

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint, int remaining) {
                mHandler.obtainMessage(0,
                        fingerprint.getFingerId(), 0).sendToTarget();
            }

            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                }
            }
        };

        private RemovalCallback mRemoveOriCallback = new RemovalCallback() {

            @Override
            public void onRemovalSucceeded(Fingerprint fingerprint, int remaining) {
                mHandler.obtainMessage(1,
                        fingerprint.getFingerId(), 0).sendToTarget();
            }

            @Override
            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                }
            }
        };

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case 0:
                        //delete fingerprint successfully
                        disablePrivateFingerprintToUnclockScreen();
                        updateFingerprintPreference();
                        break;
                    case 1:
                        updateFingerprintPreference();
                        break;
                    default:
                        break;
                }
            };
        };

        private boolean isEnrolledPrivateFingerprint(){
            List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
            if (items != null && items.size() > 0) {
                return true;
            }
            return  false;
        }

        private boolean isEnrolledDeviceFingerprint(){
            List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_COMMON);
            if (items != null && items.size() > 0) {
                return true;
            }
            return  false;
        }

        private boolean isFingerPrintUnlockSettingEnabled(){
            try {
                return Settings.System.getInt(getContentResolver(), TCT_UNLOCK_SCREEN) == 1
                        && isEnrolledDeviceFingerprint();
            }catch (Settings.SettingNotFoundException e){
                return isEnrolledDeviceFingerprint();
            }
        }
    }
}
