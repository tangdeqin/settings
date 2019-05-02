package com.android.settings.privatemode;

import java.util.List;

import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;

import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceCategory;
import android.provider.Settings;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v14.preference.SwitchPreference;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.fingerprint.FingerprintSettings.FingerprintSettingsFragment;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.fingerprint.FingerprintEnrollEnrolling;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;
import com.android.settings.widget.ToggleSwitch.OnBeforeCheckedChangeListener;
import com.android.settings.applications.LayoutPreference;
import com.android.settings.R;

import com.tct.sdk.base.fingerprint.TctFingerprintUtils;

public class PrivacyModeSettings extends SettingsPreferenceFragment implements OnBeforeCheckedChangeListener {

    private static final String UNLOCK_METHOD = "unlock_method";
    private static final String PRIVATE_FILES = "private_files";
    private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";
    private static final String AUTO_EXIT_PRIVATE_MODE = "auto_exit_private_mode";
    private static final String PRIVATE_CONTENTS = "private_contents";
    private static final String INTRO = "intro";
    private static final String APK_FILEMANAGER = "com.jrdcom.filemanager";
    private static final String APK_GALLERY = "com.tcl.gallery";

    private static final String PRIVACY_MODE_ON = "privacy_mode_on";
    private static final String TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN = "tct_private_fingerprint_unlock_screen";
    private static final String PRIVACY_MODE_PASSWORD = "privacy_mode_password";
    private static final String CURRENT_PHONE_MODE = "current_phone_mode";
    private static final String TCT_UNLOCK_SCREEN = "tct_unlock_screen";

    //Begin added by dongchi.chen for XRP23629 on 20180915
    private static final String APK_MESSAGE = "com.android.mms";
    private static final String APK_CONTACTS = "com.tct.contacts";
    private static final String PRIVATE_CONTACTS = "private_contacts";
    //End added by dongchi.chen for XRP23629 on 20180915
    private static final int MIN_PASSWORD_LENGTH = 4;

    private static final int UNLOCK_METHOD_PATTERN = 0;
    private static final int UNLOCK_METHOD_PIN = 1;
    private static final int UNLOCK_METHOD_PASSWORD = 2;

    private static final int CONFIRM_REQUEST = 601;
    private static final int CHOOSE_LOCK_GENERIC_REQUEST = 602;
    private static final int SET_PRIVACY_MODE_PASSWORD_REQUEST = 603;
    private static final int ADD_PRIVACY_FINGERPRINT_REQUEST = 604;
    private static final int CHANGE_PASSWORD_REQUEST = 605;
    private static final int ENTER_PRIVACY_MODE_REQUEST = 606;
    private static final int CONFIRM_PRIVACY_MODE_REQUEST = 607;
    private static final int ENABLE_FINGERPRINT_UNLOCK_SCREEEN_REQUEST = 608;
    private static final int RESET_PRIVACY_MODE_PASSWORD_REQUEST = 609;

    private static final int FP_TAG_COMMON = 0;
    private static final int FP_TAG_PRIVATE_MODE = FP_TAG_COMMON + 1;
    private TctFingerprintUtils mTctFingerprintUtils;

    private byte[] mToken;
    private boolean mLaunchedConfirm;
    private DevicePolicyManager mDPM;
    private FingerprintManager mFingerprintManager;
    private Preference mUnlockMethod;
    private long mChallenge;
    private TctPrivacyModeHelper mTctPrivacyModeHelper;
    private int[] mUnlockMethodSummary = new int[] {
            R.string.unlock_set_unlock_pattern_title,
            R.string.unlock_set_unlock_pin_title,
            R.string.unlock_set_unlock_password_title};

    private SwitchBar mSwitchBar;
    private int mUserId = 0;
    protected ToggleSwitch mToggleSwitch;

    // required constructor for fragments
    public PrivacyModeSettings() {
    }

    public static Intent createFragmentIntent(Context context){
        return Utils.onBuildStartFragmentIntent(context, PrivacyModeSettings.class.getName(),
                null, null, R.string.private_mode, null, false, MetricsEvent.SECURITY);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mToken = savedInstanceState.getByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
            mLaunchedConfirm = savedInstanceState.getBoolean(KEY_LAUNCHED_CONFIRM, false);
        }

        mUserId = UserHandle.myUserId();
        mDPM = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        // for fingerPrint
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        mTctFingerprintUtils = new TctFingerprintUtils(getActivity());
        mChallenge = (null == mFingerprintManager) ? 0 : mFingerprintManager.preEnroll();
        mTctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(getActivity());

        int quality = getCurrentPasswordQuality();
        if (mTctPrivacyModeHelper.isPrivacyModeEnable()) {
            if(!mTctPrivacyModeHelper.isChangedTheUnlockScreenType(quality)) {
                //Begin modified by dongchi.chen for XRP23159 on 2018/08/23
                //launchConfirmPrivacyModeLock();
                if(changePrivateModePassWordIfAutoUnlockNeeded()){
                    launchResetPrivacyModePassWorld(quality);
                }else {
                    launchConfirmPrivacyModeLock();
                }
                //End modified by dongchi.chen for XRP23159 on 2018/08/23
            }else {
                launchResetPrivacyModePassWorld(quality);
            }
        } else {
            launchChooseOrConfirmScreenLock();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.privacy_mode_settings);
        setViewsAction();
    }

    private void setViewsAction(){
        mUnlockMethod = (Preference) findPreference(UNLOCK_METHOD);
        mUnlockMethod.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                modifyPassword();
                return true;
            }
        });

        SwitchPreference autoExit = (SwitchPreference) findPreference(AUTO_EXIT_PRIVATE_MODE);
        autoExit.setChecked(Settings.System.getInt(getContentResolver(), AUTO_EXIT_PRIVATE_MODE, 0) == 1);
        autoExit.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Settings.System.putInt(getContentResolver(), AUTO_EXIT_PRIVATE_MODE, (Boolean) newValue ? 1 : 0);
                return true;
            }
        });

        Preference privateFilesPre = (Preference) findPreference(PRIVATE_FILES);
        if(null != privateFilesPre){
            if(!isPkgInstalled(getContext(), APK_FILEMANAGER)){
                PreferenceCategory privateContens = (PreferenceCategory)findPreference(PRIVATE_CONTENTS);
                privateContens.removePreference(privateFilesPre);
                LayoutPreference intro = (LayoutPreference)findPreference(INTRO);
                ImageView v = (ImageView)intro.findViewById(R.id.filemanager_icon);
                if(null != v){
                    v.setVisibility(View.GONE);
                }
            }else {
                privateFilesPre.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent();
                        intent.setAction("com.jrdcom.filemanager.action.PRIVATEMODE");
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        return true;
                    }
                });
            }
        }

        //Begin added by dongchi.chen for XRP23629 on 20180915
        Preference privateContactsPre = (Preference) findPreference(PRIVATE_CONTACTS);
        if(null != privateContactsPre){
            if(!isPkgInstalled(getContext(), APK_CONTACTS)
                    || !isPkgInstalled(getContext(), APK_MESSAGE)) {
                PreferenceCategory privateContens = (PreferenceCategory) findPreference(PRIVATE_CONTENTS);
                privateContens.removePreference(privateContactsPre);
                LayoutPreference intro = (LayoutPreference) findPreference(INTRO);
                ImageView v = (ImageView) intro.findViewById(R.id.call_icon);
                if (null != v) {
                    v.setVisibility(View.GONE);
                }
                v = (ImageView) intro.findViewById(R.id.contacts_icon);
                if (null != v) {
                    v.setVisibility(View.GONE);
                }
            }
        }
        //End added by dongchi.chen for XRP23629 on 20180915
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(!isPkgInstalled(getContext(), APK_GALLERY)){
            LayoutPreference intro = (LayoutPreference)findPreference(INTRO);
            ImageView v = (ImageView)intro.findViewById(R.id.gallery_icon);
            if(null != v){
                v.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
            || requestCode == CONFIRM_REQUEST
            || requestCode == CONFIRM_PRIVACY_MODE_REQUEST
            || requestCode == RESET_PRIVACY_MODE_PASSWORD_REQUEST) {
            if (resultCode == Activity.RESULT_FIRST_USER || resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                }
                if (mToken == null) {
                    // Didn't get an authentication, finishing
                    finish();
                    return;
                }
                if ((Settings.System.getInt(getContentResolver(), PRIVACY_MODE_ON, 0) == 0)
                        || requestCode == CHOOSE_LOCK_GENERIC_REQUEST) {
                    setupPrivacyModePassword();
                }
                if (requestCode == CONFIRM_PRIVACY_MODE_REQUEST || requestCode == RESET_PRIVACY_MODE_PASSWORD_REQUEST ) {
                    if (!mTctPrivacyModeHelper.isInPrivacyMode()) {
                        mTctPrivacyModeHelper.enterPrivacyMode(true);
                    }
                    disablePriavteFingerprintUnlockIfNeeded();
                    updateUnlockMethodPreference();
                }
            } else {
                finish();
            }
        } else if (requestCode == SET_PRIVACY_MODE_PASSWORD_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                addPrivateFingerprint();
            } else {
                finish();
            }
        } else if (requestCode == CHANGE_PASSWORD_REQUEST) {
            if (resultCode == Activity.RESULT_FIRST_USER || resultCode == Activity.RESULT_OK) {
                if (data != null) {
                    mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                }
            }
            updateUnlockMethodPreference();
        } else if (requestCode == ADD_PRIVACY_FINGERPRINT_REQUEST) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            } else {
                if(isFingerPrintUnlockSettingEnabled() && isEnrolledPrivateFingerprint()){
                    enablePrivateFingerPrintUnlockScreen();
                }
                launchFinshSetupPrivacyModeScreen();
            }
        } else if (requestCode == ENTER_PRIVACY_MODE_REQUEST) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
            } else {
                updateUnlockMethodPreference();
            }
        }
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                mToken);
        outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
    }

    private void launchChooseScreenLockActivity(){
        Intent intent = new Intent(getContext(), ChooseLockGeneric.class);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY, DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, mChallenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_SET_NORMAL_PASSWORD, true);
        intent.putExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_UNLOCK_SCREEN_PREF,
                true);
        startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
    }

    private void launchChooseOrConfirmScreenLock() {

        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
        if (!helper.launchConfirmationActivity(CONFIRM_REQUEST, getString(R.string.private_mode), null, null, mChallenge)) {
            launchChooseScreenLockActivity();
        }
    }

    private void modifyPassword() {
        Intent intent = new Intent(ChoosePrivacyModeLockGeneric.ACTION_SET_NEW_PRIVATE_PASSWORD);
        intent.putExtra(ChoosePrivacyModeLockGeneric.ChoosePrivacyModeLockGenericFragment.MINIMUM_QUALITY_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        intent.putExtra(ChoosePrivacyModeLockGeneric.ChoosePrivacyModeLockGenericFragment.HIDE_DISABLED_PREFS,
                true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, mChallenge);
        startActivityForResult(intent, CHANGE_PASSWORD_REQUEST);
    }

    private void setupPrivacyModePassword() {
        startActivityForResult(SetupScreenLockFinish.createFragmentIntent(getContext()), SET_PRIVACY_MODE_PASSWORD_REQUEST);
    }

    private void addPrivateFingerprint() {
        if (null != mFingerprintManager && mFingerprintManager.isHardwareDetected()) {
            Intent intent = new Intent(getContext(), FingerprintEnrollEnrolling.class);
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_SHOW_SKIP_BUTTON, true);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_ENROLL_FINGERPRINT_TAG, FP_TAG_PRIVATE_MODE);
            startActivityForResult(intent, ADD_PRIVACY_FINGERPRINT_REQUEST);
        } else {
            launchFinshSetupPrivacyModeScreen();
        }
    }

    private void launchFinshSetupPrivacyModeScreen() {
        startActivityForResult(SetupPrivacyModeFinish.createFragmentIntent(getContext()),
                ENTER_PRIVACY_MODE_REQUEST);
    }

    private void launchConfirmPrivacyModeLock() {
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
        helper.launchPrivacyModeConfirmationActivity(CONFIRM_PRIVACY_MODE_REQUEST, getString(R.string.private_mode), null, null, mChallenge);
    }

    private int getCurrentUnlockMethod() {
        int unlockMethod = UNLOCK_METHOD_PATTERN;
        LockPatternUtils lpu = new LockPatternUtils(getActivity());
        switch (lpu.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                unlockMethod = UNLOCK_METHOD_PATTERN;
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                unlockMethod = UNLOCK_METHOD_PIN;
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                unlockMethod = UNLOCK_METHOD_PASSWORD;
                break;
        }
        return unlockMethod;
    }

    private void updateUnlockMethodPreference() {
        int resId = mUnlockMethodSummary[getCurrentUnlockMethod()];
        List<Fingerprint> items = (null == mFingerprintManager) ? null :
                mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
        int fingerprintCount = items != null ? items.size() : 0;
        boolean enablePrivateFingerprint = Settings.System.getInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 0) == 1;
        if (fingerprintCount > 0 && enablePrivateFingerprint && isFingerPrintUnlockSettingEnabled()) {
            mUnlockMethod.setSummary(getResources().getText(resId) + " & " + getResources().getText(R.string.fingerprint_unlock_screen));
        } else {
            mUnlockMethod.setSummary(resId);
        }
    }

    private void disablePriavteFingerprintUnlockIfNeeded(){
        boolean enablePrivateFingerprint = Settings.System.getInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 0) == 1;
        if (!isFingerPrintUnlockSettingEnabled() && enablePrivateFingerprint){
            Settings.System.putInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 0);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SECURITY;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        mToggleSwitch = mSwitchBar.getSwitch();
        mSwitchBar.setChecked(true);
        mToggleSwitch.setOnBeforeCheckedChangeListener(this);
        mSwitchBar.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        mSwitchBar.hide();
    }

    @Override
    public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch,
                                          boolean checked) {
        mSwitchBar.setCheckedInternal(true);
        AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setCancelable(false)
                .setTitle(R.string.diable_privacy_mode_title)
                .setMessage( (null != mFingerprintManager && mFingerprintManager.isHardwareDetected())
                        ? R.string.diable_privacy_mode_msg : R.string.diable_privacy_mode_msg_nofingerprint)
                .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Settings.System.putInt(getContentResolver(), PRIVACY_MODE_ON, 0);
                        mTctPrivacyModeHelper.setPrivacyPassword("");
                        Settings.System.putInt(getContentResolver(), CURRENT_PHONE_MODE, 0);
                        disablePrivateFingerPrintUnlockScreen();
                        removePrivateFingerPrint();
                        //send broadcat that private mode if off
                        Intent intent = new Intent("android.intent.actions.CURRENT_MODE_CHANGED");
                        intent.putExtra("current_mode", 2);//0:normal mode, 1:private mode, 2:turn off private mode
                        getActivity().sendBroadcast(intent);
                        PrivacyModeSettings.this.finish();
                    }
                })
                .setNegativeButton(R.string.dlg_cancel, null)
                .show();
        return true;
    }

    private int getCurrentPasswordQuality(){
        LockPatternUtils utils = new LockPatternUtils(getActivity());
        return  utils.getKeyguardStoredPasswordQuality(UserHandle.myUserId());
    }

    private void launchResetPrivacyModePassWorld(int quality) {
        final Context context = getActivity();
        if (quality >= DevicePolicyManager.PASSWORD_QUALITY_NUMERIC) {
            int minLength = mDPM.getPasswordMinimumLength(null);
            if (minLength < MIN_PASSWORD_LENGTH) {
                minLength = MIN_PASSWORD_LENGTH;
            }
            final int maxLength = mDPM.getPasswordMaximumLength(quality);
            Intent intent = ChoosePrivacyModeLockPassword.createResetIntent(context, quality, minLength, maxLength, false, mChallenge);
            startActivityForResult(intent, RESET_PRIVACY_MODE_PASSWORD_REQUEST);
        } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_SOMETHING) {
            Intent intent = ChoosePrivacyModeLockPattern.createResetIntent(context, false, mChallenge);
            startActivityForResult(intent, RESET_PRIVACY_MODE_PASSWORD_REQUEST);
        } else if (quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED) {
            launchChooseOrConfirmScreenLock();
        }
    }

    private boolean isFingerPrintUnlockSettingEnabled(){
        try {
            return Settings.System.getInt(getContentResolver(), TCT_UNLOCK_SCREEN) == 1
                    && isEnrolledDeviceFingerprint();
        }catch (Settings.SettingNotFoundException e){
            return isEnrolledDeviceFingerprint();
        }
    }

    private void enablePrivateFingerPrintUnlockScreen(){
        Settings.System.putInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 1);
    }

    private void removePrivateFingerPrint(){
        List<Fingerprint> items = null == mFingerprintManager ? null :
                mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
        if(null != items){
            for(Fingerprint i : items){
                mFingerprintManager.remove(i, UserHandle.myUserId(), null);
            }
        }
    }

    private void disablePrivateFingerPrintUnlockScreen(){
        Settings.System.putInt(getContentResolver(), TCT_PRIVATE_FINGERPRINT_UNLOCK_SCREEN, 0);
    }

    private boolean isEnrolledPrivateFingerprint(){
        List<Fingerprint> items = null == mFingerprintManager ? null :
                mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_PRIVATE_MODE);
        return (null != items) && (items.size() > 0);
    }

    private boolean isEnrolledDeviceFingerprint(){
        List<Fingerprint> items = null == mFingerprintManager ? null :
                mTctFingerprintUtils.getTctEnrolledFingerprints(FP_TAG_COMMON);
        return (null != items) && (items.size() > 0);
    }

    private boolean isPkgInstalled(Context ctx, String packageName){
        PackageManager pm = ctx.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }


    //Begin added by dongchi.chen for XRP23159 on 2018/08/23
    private boolean changePrivateModePassWordIfAutoUnlockNeeded(){
        if(true/*getContext().getResources().getBoolean(com.jrdcom.internal.R.bool.def_keyguard_auto_unlock_with_pin_or_password)*/) {
            String normalModePassword = mTctPrivacyModeHelper.getNormalPassword();
            String privateModePassword = mTctPrivacyModeHelper.getPrivacyPassword();
            if(normalModePassword.length() != privateModePassword.length()) {
                return true;
            }
        }
        return false;
    }
    //End added by dongchi.chen for XRP23159 on 2018/08/23
}
