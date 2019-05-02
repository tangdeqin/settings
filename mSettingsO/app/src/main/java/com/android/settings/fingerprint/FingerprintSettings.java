/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.android.settings.fingerprint;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.UserHandle;
import android.os.UserManager;
import android.support.annotation.VisibleForTesting;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.support.v7.preference.PreferenceViewHolder;
import android.text.Annotation;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.Utils;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.TwoTargetPreference;
import com.android.settingslib.widget.FooterPreference;

import java.util.HashMap;
import java.util.List;

//Begin added by jinlong.lu for XR6618444 on 18-7-31
import android.support.v14.preference.SwitchPreference;
import android.provider.Settings;
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import android.content.res.Resources;
import java.util.ArrayList;
import com.android.settings.fingerprint.ex.FingerprintUtils;
import android.text.Editable;
import android.text.TextWatcher;
import com.android.settings.fingerprint.ex.FingerprintLaunchAppHelper;
import com.android.settings.fingerprint.ex.LaunchAppInfo;
import com.tct.sdk.base.fingerprint.FingerprintConstants;
import com.tct.sdk.base.fingerprint.TctFingerprintUtils;
import android.provider.SearchIndexableResource;
import java.util.Arrays;
//End added by jinlong.lu for XR6618444 on 18-7-31
//Begin added by jinlong.lu for XRP23382 on 18-8-30
import android.view.LayoutInflater;
//End added by jinlong.lu for XRP23382 on 18-8-30
//Begin added by jinlong.lu for XR7074057 on 18-11-3
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
//End added by jinlong.lu for XR7074057 on 18-11-3

/**
 * Settings screen for fingerprints
 */
public class FingerprintSettings extends SubSettings {

    private static final String TAG = "FingerprintSettings";

    /**
     * Used by the choose fingerprint wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    protected static final int RESULT_FINISHED = RESULT_FIRST_USER;

    /**
     * Used by the enrolling screen during setup wizard to skip over setting up fingerprint, which
     * will be useful if the user accidentally entered this flow.
     */
    protected static final int RESULT_SKIP = RESULT_FIRST_USER + 1;

    /**
     * Like {@link #RESULT_FINISHED} except this one indicates enrollment failed because the
     * device was left idle. This is used to clear the credential token to require the user to
     * re-enter their pin/pattern/password before continuing.
     */
    protected static final int RESULT_TIMEOUT = RESULT_FIRST_USER + 2;

    private static final long LOCKOUT_DURATION = 30000; // time we have to wait for fp to reset, ms

    public static final String KEY_FINGERPRINT_SETTINGS = "fingerprint_settings";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintSettingsFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintSettingsFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Begin modified by jinlong.lu for XR6847910 on 18-8-18
        CharSequence msg = getText(R.string.fingerprint_manage_category_title);
        //End modified by jinlong.lu for XR6847910 on 18-8-18
        setTitle(msg);
    }
    //Begin added by jinlong.lu for XR6618444 on 18-7-31
    public static class FingerprintSettingsFragment extends SettingsPreferenceFragment
            implements OnPreferenceChangeListener, FingerprintPreference.OnDeleteClickListener, FingerprintPreference.OnGearClickListener,Indexable {
    //End added by jinlong.lu for XR6618444 on 18-7-31
        private static final int RESET_HIGHLIGHT_DELAY_MS = 500;

        private static final String TAG = "FingerprintSettings";
        private static final String KEY_FINGERPRINT_ITEM_PREFIX = "key_fingerprint_item";
        private static final String KEY_FINGERPRINT_ADD = "key_fingerprint_add";
        private static final String KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE =
                "fingerprint_enable_keyguard_toggle";
        private static final String KEY_LAUNCHED_CONFIRM = "launched_confirm";

        private static final int MSG_REFRESH_FINGERPRINT_TEMPLATES = 1000;
        private static final int MSG_FINGER_AUTH_SUCCESS = 1001;
        private static final int MSG_FINGER_AUTH_FAIL = 1002;
        private static final int MSG_FINGER_AUTH_ERROR = 1003;
        private static final int MSG_FINGER_AUTH_HELP = 1004;

        private static final int CONFIRM_REQUEST = 101;
        private static final int CHOOSE_LOCK_GENERIC_REQUEST = 102;

        private static final int ADD_FINGERPRINT_REQUEST = 10;

        protected static final boolean DEBUG = true;

        private FingerprintManager mFingerprintManager;
        private CancellationSignal mFingerprintCancel;
        private boolean mInFingerprintLockout;
        private byte[] mToken;
        private boolean mLaunchedConfirm;
        private Drawable mHighlightDrawable;
        private int mUserId;

        private static final String TAG_REMOVAL_SIDECAR = "removal_sidecar";
        private FingerprintRemoveSidecar mRemovalSidecar;
        private HashMap<Integer, String> mFingerprintsRenaming;
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        private final String PRE_FINGERPRINTS_KEY = "fingerprints_list";
        private final String KEY_FINGERPRINT_QUICK_LAUNCH_FUNC = "fingerprint_quick_launch_func";
        public static final String EXTRA_FINGERPRINT = "fingerprint";
        private static final String KEY_FINGERPRINT_UNLOCK_APP_ENABLE =
                "fingerprint_unlock_app_enable";
        private static final String KEY_CONFIRM_PASSWORD = "key_confirm_password";
        private SwitchPreference mQuickLaunchFunc;
        private SwitchPreference mUnlockScreen;
        private SwitchPreference mFingerprintUnlockApp;
        private PreferenceGroup mFingerprintsCategory;
        private boolean mHasPassword;
        private boolean isConfirmPassword;
        private static final int FINGERPRINT_ENROLL_INTRODUCTION_ERGO_REQUEST = 103;
        //End added by jinlong.lu for XR6618444 on 18-7-31
        //Begin added by jinlong.lu for XR6312208 on 18-5-9
        private static final String FINGERPRINT_APP_LOCK_ACTION = "com.tct.fingerprint.action.APPS_LOCK";
        private static final String FINGERPRINT_CONFIRM_APP_LOCK_ACTION = "com.tct.fingerprint.confirm.action.APPS_LOCK";
        private static final int FINGERPRINT_APP_LOCK_REQUEST = 104;
        private static final int FINGERPRINT_CONFIRM_APP_LOCK_REQUEST = 105;
        private TctFingerprintUtils mTctFingerprintUtils;
        //End added by jinlong.lu for XR6312208 on 18-5-9
        //Begin added by jinlong.lu for XR7074057 on 18-11-3
        HomeAndRecentKeyListener mHomeAndRecentKeyListener;
        //End added by jinlong.lu for XR7074057 on 18-11-3

        private AuthenticationCallback mAuthCallback = new AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                int fingerId = result.getFingerprint().getFingerId();
                mHandler.obtainMessage(MSG_FINGER_AUTH_SUCCESS, fingerId, 0).sendToTarget();
            }

            @Override
            public void onAuthenticationFailed() {
                mHandler.obtainMessage(MSG_FINGER_AUTH_FAIL).sendToTarget();
            }

            @Override
            public void onAuthenticationError(int errMsgId, CharSequence errString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_ERROR, errMsgId, 0, errString)
                        .sendToTarget();
            }

            @Override
            public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
                mHandler.obtainMessage(MSG_FINGER_AUTH_HELP, helpMsgId, 0, helpString)
                        .sendToTarget();
            }
        };

        FingerprintRemoveSidecar.Listener mRemovalListener =
                new FingerprintRemoveSidecar.Listener() {
            public void onRemovalSucceeded(Fingerprint fingerprint) {
                mHandler.obtainMessage(MSG_REFRESH_FINGERPRINT_TEMPLATES,
                        fingerprint.getFingerId(), 0).sendToTarget();
                updateDialog();
                //Begin added by jinlong.lu for XR5823817 on 17-12-29
                final Activity activity = getActivity();
                if (activity != null) {
                    resetFingerprintFuncById(fingerprint.getFingerId(),activity);
                }
                //End added by jinlong.lu for XR5823817 on 17-12-29

            }

            public void onRemovalError(Fingerprint fp, int errMsgId, CharSequence errString) {
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT);
                }
                updateDialog();
            }

            private void updateDialog() {
                RenameDialog renameDialog = (RenameDialog) getFragmentManager().
                        findFragmentByTag(RenameDialog.class.getName());
                if (renameDialog != null) {
                    renameDialog.enableDelete();
                }
            }
        };

        private final Handler mHandler = new Handler() {
            @Override
            public void handleMessage(android.os.Message msg) {
                switch (msg.what) {
                    case MSG_REFRESH_FINGERPRINT_TEMPLATES:
                        removeFingerprintPreference(msg.arg1);
                        updateAddPreference();
                        retryFingerprint();
                    break;
                    case MSG_FINGER_AUTH_SUCCESS:
                        mFingerprintCancel = null;
                        highlightFingerprintItem(msg.arg1);
                        retryFingerprint();
                    break;
                    case MSG_FINGER_AUTH_FAIL:
                        // No action required... fingerprint will allow up to 5 of these
                    break;
                    case MSG_FINGER_AUTH_ERROR:
                        handleError(msg.arg1 /* errMsgId */, (CharSequence) msg.obj /* errStr */ );
                    break;
                    case MSG_FINGER_AUTH_HELP: {
                        // Not used
                    }
                    break;
                }
            }
        };

        private void stopFingerprint() {
            if (mFingerprintCancel != null && !mFingerprintCancel.isCanceled()) {
                mFingerprintCancel.cancel();
            }
            mFingerprintCancel = null;
        }

        /**
         * @param errMsgId
         */
        protected void handleError(int errMsgId, CharSequence msg) {
            mFingerprintCancel = null;
            switch (errMsgId) {
                case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                    return; // Only happens if we get preempted by another activity. Ignored.
                case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                    mInFingerprintLockout = true;
                    // We've been locked out.  Reset after 30s.
                    if (!mHandler.hasCallbacks(mFingerprintLockoutReset)) {
                        mHandler.postDelayed(mFingerprintLockoutReset,
                                LOCKOUT_DURATION);
                    }
                    break;
                case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT_PERMANENT:
                    mInFingerprintLockout = true;
                    break;
            }

            if (mInFingerprintLockout) {
                // Activity can be null on a screen rotation.
                final Activity activity = getActivity();
                if (activity != null) {
                    Toast.makeText(activity, msg , Toast.LENGTH_SHORT).show();
                }
            }
            retryFingerprint(); // start again
        }

        private void retryFingerprint() {
            if (mRemovalSidecar.inProgress()) {
                return;
            }
            // Don't start authentication if ChooseLockGeneric is showing, otherwise if the user
            // is in FP lockout, a toast will show on top
            if (mLaunchedConfirm) {
                return;
            }
            if (!mInFingerprintLockout) {
                mFingerprintCancel = new CancellationSignal();
                mFingerprintManager.authenticate(null, mFingerprintCancel, 0 /* flags */,
                        mAuthCallback, null, mUserId);
            }
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.FINGERPRINT;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Activity activity = getActivity();
            mFingerprintManager = Utils.getFingerprintManagerOrNull(activity);
            //Begin modified by jinlong.lu for XR7025241 on 18-9-21
            //Begin added by jinlong.lu for XR6618444 on 18-7-31
            mTctFingerprintUtils =new TctFingerprintUtils(activity,true);
            //End added by jinlong.lu for XR6618444 on 18-7-31
            //End modified by jinlong.lu for XR7025241 on 18-9-21
            mRemovalSidecar = (FingerprintRemoveSidecar)
                    getFragmentManager().findFragmentByTag(TAG_REMOVAL_SIDECAR);
            if (mRemovalSidecar == null) {
                mRemovalSidecar = new FingerprintRemoveSidecar();
                getFragmentManager().beginTransaction()
                        .add(mRemovalSidecar, TAG_REMOVAL_SIDECAR).commit();
            }
            mRemovalSidecar.setFingerprintManager(mFingerprintManager);
            mRemovalSidecar.setListener(mRemovalListener);

            RenameDialog renameDialog = (RenameDialog) getFragmentManager().
                    findFragmentByTag(RenameDialog.class.getName());
            if (renameDialog != null) {
                renameDialog.setDeleteInProgress(mRemovalSidecar.inProgress());
            }

            mFingerprintsRenaming = new HashMap<Integer, String>();

            if (savedInstanceState != null) {
                mFingerprintsRenaming = (HashMap<Integer, String>)
                        savedInstanceState.getSerializable("mFingerprintsRenaming");
                mToken = savedInstanceState.getByteArray(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                mLaunchedConfirm = savedInstanceState.getBoolean(
                        KEY_LAUNCHED_CONFIRM, false);
                //Begin added by jinlong.lu for XR6618444 on 18-7-31
                isConfirmPassword = savedInstanceState.getBoolean(
                        KEY_CONFIRM_PASSWORD, false);
                //End added by jinlong.lu for XR6618444 on 18-7-31
            }
            mUserId = getActivity().getIntent().getIntExtra(
                    Intent.EXTRA_USER_ID, UserHandle.myUserId());
           //Begin added by jinlong.lu for XR6618444 on 18-7-31
            updatePasswordQuality();
            final List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(mUserId, FingerprintConstants.FP_TAG_COMMON);
            final int fingerprintCount = items != null ? items.size() : 0;
            final String clazz;
            if (fingerprintCount <= 0) {
                clazz = FingerprintEnrollIntroduction.class.getName();
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", clazz);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivityForResult(intent, FINGERPRINT_ENROLL_INTRODUCTION_ERGO_REQUEST);
            } else if (mHasPassword && mLaunchedConfirm == false&&!isConfirmPassword) {
                mLaunchedConfirm = true;
                launchChooseOrConfirmLock();
            }
           //End added by jinlong.lu for XR6618444 on 18-7-31

            // Need to authenticate a session token if none
            /*if (mToken == null && mLaunchedConfirm == false) {
                mLaunchedConfirm = true;
                launchChooseOrConfirmLock();
            }*/
            //Begin added by jinlong.lu for XR7074057 on 18-11-3
            mHomeAndRecentKeyListener = new HomeAndRecentKeyListener().register(this);
            //End added by jinlong.lu for XR7074057 on 18-11-3
            final FooterPreference pref = mFooterPreferenceMixin.createFooterPreference();
            final EnforcedAdmin admin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                    activity, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId);
            pref.setTitle(LearnMoreSpan.linkify(getText(admin != null
                            ? R.string.security_settings_fingerprint_enroll_disclaimer_lockscreen_disabled
                            : R.string.security_settings_fingerprint_enroll_disclaimer),
                    getString(getHelpResource()), admin));
        }
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        private void updatePasswordQuality() {
            Activity activity =getActivity();
            final int passwordQuality = new ChooseLockSettingsHelper(activity).utils()
                    .getActivePasswordQuality(UserManager.get(activity).getCredentialOwnerProfile(mUserId));
            mHasPassword = passwordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
        }
        //End added by jinlong.lu for XR6618444 on 18-7-31
        protected void removeFingerprintPreference(int fingerprintId) {
            String name = genKey(fingerprintId);
            Preference prefToRemove = findPreference(name);
            if (prefToRemove != null) {
               //Begin modified by jinlong.lu for XR6618444 on 18-7-31
                /* old if (!getPreferenceScreen().removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }*/
                PreferenceGroup securityCategory = (PreferenceGroup)
                        getPreferenceScreen().findPreference(PRE_FINGERPRINTS_KEY);
                //if (!getPreferenceScreen().removePreference(prefToRemove)) {
                if (!securityCategory.removePreference(prefToRemove)) {
                    Log.w(TAG, "Failed to remove preference with key " + name);
                }
                //Begin added by jinlong.lu for XR5823818 on 17-12-28
                if (mFingerprintsRenaming.containsKey(fingerprintId)){
                    mFingerprintsRenaming.remove(fingerprintId);
                    Log.w(TAG, "remove fingerprint from FingerprintsRenaming" + name);
                }
                //End added by jinlong.lu for XR5823818 on 17-12-28
                updateFingerprintTitlePreference();
               //End modified by jinlong.lu for XR6618444 on 18-7-31
            } else {
                Log.w(TAG, "Can't find preference to remove: " + name);
            }
        }
       //Begin added by jinlong.lu for XR6618444 on 18-7-31
       void updateFingerprintTitlePreference(){
           final List<Fingerprint> items =  mTctFingerprintUtils.getTctEnrolledFingerprints(mUserId,FingerprintConstants.FP_TAG_COMMON);
           final int fpnum = items.size();
           //Begin modified by jinlong.lu for XR6873798 on 18-8-28
           final int max = getContext().getResources().getInteger(
                   R.integer.config_fingerprintMaxTemplatesPerUser);
           //End modified by jinlong.lu for XR6873798 on 18-8-28
           String title = getResources().getString(
                   R.string.fingerprints_list_title)
                   + "(" + fpnum + "/" + max + ")";
           if(mFingerprintsCategory!=null) {
               mFingerprintsCategory.setTitle(title);
           }
       }
       //End added by jinlong.lu for XR6618444 on 18-7-31
        /**
         * Important!
         *
         * Don't forget to update the SecuritySearchIndexProvider if you are doing any change in the
         * logic or adding/removing preferences here.
         */
        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            //Begin modified by jinlong.lu for XR6618444 on 18-7-31
            addPreferencesFromResource(R.xml.security_settings_fingerprint_manager);
            root = getPreferenceScreen();
            mFingerprintsCategory = (PreferenceGroup)
                    root.findPreference(PRE_FINGERPRINTS_KEY);
            //Begin modified by jinlong.lu for XR6873798 on 18-8-28
            final int max = getContext().getResources().getInteger(R.integer.config_fingerprintMaxTemplatesPerUser);
            //End modified by jinlong.lu for XR6873798 on 18-8-28
            final List<Fingerprint> items =  mTctFingerprintUtils.getTctEnrolledFingerprints(mUserId,FingerprintConstants.FP_TAG_COMMON);
            final int fpnum = items.size();
            String title = getResources().getString(
                    R.string.fingerprints_list_title)
                    + "(" + fpnum + "/" + max + ")";
            mFingerprintsCategory.setTitle(title);
            mUnlockScreen = (SwitchPreference) findPreference(KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE);
            mUnlockScreen.setOnPreferenceChangeListener(this);
            mUnlockScreen.setChecked(Settings.System.getInt(getContentResolver(),
                    FingerprintConstants.TCT_FINGERPRINT_UNLOCK_SCREEN, 0) == 1);
            mFingerprintUnlockApp = (SwitchPreference) findPreference(KEY_FINGERPRINT_UNLOCK_APP_ENABLE);
            mFingerprintUnlockApp.setOnPreferenceChangeListener(this);
            //Begin modified by jinlong.lu for XR7099324 on 18-11-06
            //Begin modified by jinlong.lu for XR7109895 on 18-11-14
            //switch on when applock switch on
            mFingerprintUnlockApp.setChecked(Settings.System.getInt(getContentResolver(),
                    FingerprintUtils.APPS_LOCK_FINGERPRINT_UNLOCK, 0) == 1 && Settings.System.getInt(getContentResolver(),
                    FingerprintUtils.APPS_LOCK_ON, 0) == 1);
            //End modified by jinlong.lu for XR7109895 on 18-11-14
            //End modified by jinlong.lu for XR6312208 on 18-5-9

            mQuickLaunchFunc = (SwitchPreference) findPreference(KEY_FINGERPRINT_QUICK_LAUNCH_FUNC);
            mQuickLaunchFunc.setOnPreferenceChangeListener(this);
            mQuickLaunchFunc
                    .setChecked(Settings.System.getInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_QUICK_LAUNCH_FUNC, 0) == 1);
            addFingerprintItemPreferences(mFingerprintsCategory);
            //End modified by jinlong.lu for XR6618444 on 18-7-31
            setPreferenceScreen(root);
            return root;
        }

        private void addFingerprintItemPreferences(PreferenceGroup root) {
            root.removeAll();
            //Begin modified by jinlong.lu for XR6618444 on 18-7-31
            final List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(mUserId,FingerprintConstants.FP_TAG_COMMON);
            //End modified by jinlong.lu for XR6618444 on 18-7-31
            final int fingerprintCount = items.size();
            for (int i = 0; i < fingerprintCount; i++) {
                final Fingerprint item = items.get(i);
              //Begin modified by jinlong.lu for XR6618444 on 18-7-31
                FingerprintPreference pref = new FingerprintPreference(root.getContext(),
                        this /* onDeleteClickListener */,this);
              //End modified by jinlong.lu for XR6618444 on 18-7-31
                pref.setKey(genKey(item.getFingerId()));
                pref.setTitle(item.getName());
                pref.setFingerprint(item);
                pref.setPersistent(false);
                pref.setIcon(R.drawable.ic_fingerprint_24dp);
                if (mRemovalSidecar.isRemovingFingerprint(item.getFingerId())) {
                    pref.setEnabled(false);
                }
                if (mFingerprintsRenaming.containsKey(item.getFingerId())) {
                    pref.setTitle(mFingerprintsRenaming.get(item.getFingerId()));
                }
                //Begin added by jinlong.lu for XR6618444 on 18-7-31
                boolean allowQuickLaunch = Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_QUICK_LAUNCH_FUNC, 0) == 1;
                boolean allowUnlock = Settings.System.getInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_UNLOCK_SCREEN, 0) == 1;
                if (allowQuickLaunch) {
                    pref.setGearEnabled(true);
                } else {
                    pref.setGearEnabled(false);
                }
                if (allowUnlock) {
                    LaunchAppInfo appInfo =FingerprintLaunchAppHelper.getLauncherAppInfo(getActivity(), item.getFingerId());
                    String launcherName = appInfo != null ? appInfo.getLaunchName() : "";
                    pref.setSummary(launcherName);
                } else {
                    pref.setSummary(R.string.fp_none);
                }
                //End added by jinlong.lu for XR6618444 on 18-7-31
                root.addPreference(pref);
                pref.setOnPreferenceChangeListener(this);
            }
            Preference addPreference = new Preference(root.getContext());
            addPreference.setKey(KEY_FINGERPRINT_ADD);
            addPreference.setTitle(R.string.fingerprint_add_title);
            addPreference.setIcon(R.drawable.ic_menu_add);
            root.addPreference(addPreference);
            addPreference.setOnPreferenceChangeListener(this);
            updateAddPreference();
        }

        private void updateAddPreference() {
            if (getActivity() == null) return; // Activity went away

            /* Disable preference if too many fingerprints added */
           //Begin modified by jinlong.lu for XR6873798 on 18-8-28
            final int max = getContext().getResources().getInteger(
                    R.integer.config_fingerprintMaxTemplatesPerUser);
           //End modified by jinlong.lu for XR6873798 on 18-8-28
            //Begin modified by jinlong.lu for XR6618444 on 18-7-31
            final List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(mUserId, FingerprintConstants.FP_TAG_COMMON);
            final int fingerprintCount = items != null ? items.size() : 0;
            boolean tooMany = fingerprintCount >= max;
            if (fingerprintCount == 0) {
                Settings.System.putInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_UNLOCK_SCREEN, 0);
                if(mUnlockScreen!=null) {
                    mUnlockScreen.setChecked(false);
                }
                Settings.System.putInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_QUICK_LAUNCH_FUNC, 0);
                if(mQuickLaunchFunc!=null) {
                    mQuickLaunchFunc.setChecked(false);
                    mQuickLaunchFunc.setEnabled(false);
                }
                Settings.System.putInt(getContentResolver(),
                        FingerprintUtils.APPS_LOCK_FINGERPRINT_UNLOCK, 0);
                if(mFingerprintUnlockApp!=null) {
                    mFingerprintUnlockApp.setChecked(false);
                    mFingerprintUnlockApp.setEnabled(false);
                }
            }
            //End modified by jinlong.lu for XR6618444 on 18-7-31
            // retryFingerprint() will be called when remove finishes
            // need to disable enroll or have a way to determine if enroll is in progress
            final boolean removalInProgress = mRemovalSidecar.inProgress();
            CharSequence maxSummary = tooMany ?
                    getContext().getString(R.string.fingerprint_add_max, max) : "";
            Preference addPreference = findPreference(KEY_FINGERPRINT_ADD);
            addPreference.setSummary(maxSummary);
            addPreference.setEnabled(!tooMany && !removalInProgress);
        }

        private static String genKey(int id) {
            return KEY_FINGERPRINT_ITEM_PREFIX + "_" + id;
        }

        @Override
        public void onResume() {
            super.onResume();
            mInFingerprintLockout = false;
            // Make sure we reload the preference hierarchy since fingerprints may be added,
            // deleted or renamed.
            updatePreferences();
            //[TCT-ROM][Fingerprint]Begin added by jinlong.lu for XR6618444 on 18-8-1
            retryFingerprint();
            //[TCT-ROM][Fingerprint]End added by jinlong.lu for XR6618444 on 18-8-1
            if (mRemovalSidecar != null) {
                mRemovalSidecar.setListener(mRemovalListener);
            }
        }

        private void updatePreferences() {
            createPreferenceHierarchy();
            //Begin deleted by jinlong.lu for XR6618444 on 18-7-31
            //retryFingerprint();
            //End deleted by jinlong.lu for XR6618444 on 18-7-31
        }

        @Override
        public void onPause() {
            super.onPause();
            stopFingerprint();
            if (mRemovalSidecar != null) {
                mRemovalSidecar.setListener(null);
            }
        }

        @Override
        public void onSaveInstanceState(final Bundle outState) {
            outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                    mToken);
            outState.putBoolean(KEY_LAUNCHED_CONFIRM, mLaunchedConfirm);
            outState.putSerializable("mFingerprintsRenaming", mFingerprintsRenaming);
            //Begin added by jinlong.lu for XR6618444 on 18-7-31
            outState.putBoolean(KEY_CONFIRM_PASSWORD, isConfirmPassword);
            //End added by jinlong.lu for XR6618444 on 18-7-31
        }

        @Override
        public boolean onPreferenceTreeClick(Preference pref) {
            final String key = pref.getKey();
            if (KEY_FINGERPRINT_ADD.equals(key)) {
                Intent intent = new Intent();
                intent.setClassName("com.android.settings",
                        FingerprintEnrollEnrolling.class.getName());
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
            } else if (pref instanceof FingerprintPreference) {
                FingerprintPreference fpref = (FingerprintPreference) pref;
                final Fingerprint fp = fpref.getFingerprint();
                //Begin modified by jinlong.lu for XR7074057 on 18-11-3
                //Begin modified by jinlong.lu for XR6618444 on 18-7-31
                showRenameDialog(fp);
                //showRenameDeleteDialog(fp);
                //End modified by jinlong.lu for XR6618444 on 18-7-31
                //End modified by jinlong.lu for XR7074057 on 18-11-3
            }
            return super.onPreferenceTreeClick(pref);
        }

        @Override
        public void onDeleteClick(FingerprintPreference p) {
            final boolean hasMultipleFingerprint =
                    mFingerprintManager.getEnrolledFingerprints(mUserId).size() > 1;
            final Fingerprint fp = p.getFingerprint();

            if (hasMultipleFingerprint) {
                if (mRemovalSidecar.inProgress()) {
                    Log.d(TAG, "Fingerprint delete in progress, skipping");
                    return;
                }
                DeleteFingerprintDialog.newInstance(fp, this /* target */)
                        .show(getFragmentManager(), DeleteFingerprintDialog.class.getName());
            } else {
                ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                final boolean isProfileChallengeUser =
                        UserManager.get(getContext()).isManagedProfile(mUserId);
                final Bundle args = new Bundle();
                args.putParcelable("fingerprint", fp);
                args.putBoolean("isProfileChallengeUser", isProfileChallengeUser);
                lastDeleteDialog.setArguments(args);
                lastDeleteDialog.setTargetFragment(this, 0);
                lastDeleteDialog.show(getFragmentManager(),
                        ConfirmLastDeleteDialog.class.getName());
            }
        }
        //Begin added by jinlong.lu for XR5823817 on 17-12-29
        private void resetFingerprintFuncById(int id, Context context) {
          //Begin modified by jinlong.lu for XR6873798 on 18-8-28
          int mMaxfp = context.getResources().getInteger(
                    R.integer.config_fingerprintMaxTemplatesPerUser);
          //End modified by jinlong.lu for XR6873798 on 18-8-28
            for (int i = 0; i < mMaxfp; i++) {
                String funcNumString = FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM + i;
                String fpInfo = Settings.System.getString(context.getContentResolver(),
                        funcNumString);
                if (fpInfo == null) {
                    continue;
                }
                String[] temp = fpInfo.split(":");
                if (temp.length >= 2) {
                    if (temp[0].equals("" + id)) {
                        Log.d(TAG, "fpInfo:   " + fpInfo);
                        Settings.System.putString(context.getContentResolver(), funcNumString,
                                null);
                        break;
                    }
                }
            }
        }
        //End added by jinlong.lu for XR5823817 on 17-12-29
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        @Override
        public void onGearClick(FingerprintPreference fpref) {
            Log.d(TAG, "onGearClick");
            FingerprintPreference mFpPreference = (FingerprintPreference)fpref;
            final Fingerprint fp =mFpPreference.getFingerprint();
            Intent intent = new Intent();
            intent.putExtra(EXTRA_FINGERPRINT, fp);
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
            intent.setClassName("com.android.settings",
                    com.android.settings.fingerprint.ex.FingerprintLaunchAppSettings.class.getName());
            intent.putExtra(EXTRA_SHOW_FRAGMENT_AS_SUBSETTING, true);
            startActivity(intent);

        }
        //End added by jinlong.lu for XR6618444 on 18-7-31
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        private void showRenameDeleteDialog(final Fingerprint fp) {
            RenameDeleteDialog renameDeleteDialog = new RenameDeleteDialog();
            Bundle args = new Bundle();
            args.putParcelable("fingerprint", fp);
            renameDeleteDialog.setArguments(args);
            renameDeleteDialog.setTargetFragment(this, 0);
            renameDeleteDialog.show(getFragmentManager(), RenameDeleteDialog.class.getName());
        }
        public static class RenameDeleteDialog extends InstrumentedDialogFragment {

            private Fingerprint mFp;
            private EditText mDialogTextField;
            private String mFingerName;
            private Boolean mTextHadFocus;
            private int mTextSelectionStart;
            private int mTextSelectionEnd;
            @Override
            public int getMetricsCategory() {
                return MetricsEvent.DIALOG_FINGERPINT_EDIT;
            }
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
                        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String newName =
                                                mDialogTextField.getText().toString();
                                        final CharSequence name = mFp.getName();
                                        if (!newName.equals(name)) {
                                            if (DEBUG) {
                                                Log.v(TAG, "rename " + name + " to " + newName);
                                            }
                                            FingerprintSettingsFragment parent
                                                    = (FingerprintSettingsFragment)
                                                    getTargetFragment();
                                            parent.renameFingerPrint(mFp.getFingerId(),
                                                    newName);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        onDeleteClick(dialog);
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
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                if (s == null) {
                                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                    return;
                                }
                                if (s.toString().trim().equals("")) {
                                    alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                    Log.d(TAG, "Fingerprint name is empty,disable ok button");
                                }else {
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

            private void onDeleteClick(DialogInterface dialog) {
                if (DEBUG) Log.v(TAG, "Removing fpId=" + mFp.getFingerId());
                FingerprintSettingsFragment parent
                        = (FingerprintSettingsFragment) getTargetFragment();
                final boolean isProfileChallengeUser =
                        UserManager.get(getContext()).isManagedProfile(parent.mUserId);
                if (parent.mTctFingerprintUtils.getTctEnrolledFingerprints(parent.mUserId, FingerprintConstants.FP_TAG_COMMON).size() > 1) {
                    parent.deleteFingerPrint(mFp);
                } else {
                    ConfirmLastDeleteDialog lastDeleteDialog = new ConfirmLastDeleteDialog();
                    Bundle args = new Bundle();
                    args.putParcelable("fingerprint", mFp);
                    args.putBoolean("isProfileChallengeUser", isProfileChallengeUser);
                    lastDeleteDialog.setArguments(args);
                    lastDeleteDialog.setTargetFragment(getTargetFragment(), 0);
                    lastDeleteDialog.show(getFragmentManager(),
                            ConfirmLastDeleteDialog.class.getName());
                }
                dialog.dismiss();
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

        //End added by jinlong.lu for XR6618444 on 18-7-31
        private void showRenameDialog(final Fingerprint fp) {
            RenameDialog renameDialog = new RenameDialog();
            Bundle args = new Bundle();
            if (mFingerprintsRenaming.containsKey(fp.getFingerId())) {
                final Fingerprint f = new Fingerprint(mFingerprintsRenaming.get(fp.getFingerId()),
                        fp.getGroupId(), fp.getFingerId(), fp.getDeviceId());
                args.putParcelable("fingerprint", f);
            } else {
                args.putParcelable("fingerprint", fp);
            }
            renameDialog.setDeleteInProgress(mRemovalSidecar.inProgress());
            renameDialog.setArguments(args);
            renameDialog.setTargetFragment(this, 0);
            renameDialog.show(getFragmentManager(), RenameDialog.class.getName());
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            boolean result = true;
            final String key = preference.getKey();
            //Begin added by jinlong.lu for XR6618444 on 18-7-31
            final List<Fingerprint> items =mTctFingerprintUtils.getTctEnrolledFingerprints(mUserId,FingerprintConstants.FP_TAG_COMMON);
            int fingerprintCount = items.size();
            if (KEY_FINGERPRINT_ENABLE_KEYGUARD_TOGGLE.equals(key)) {
                if ((Boolean) (value)) {
                    if (fingerprintCount < 1) {
                        Intent intent = new Intent();
                        intent.setClassName("com.android.settings",
                                FingerprintEnrollEnrolling.class.getName());
                        intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                        intent.putExtra(FingerprintUtils.EXTRA_KEY_ENROLL_FINGERPRINT_TAG, FingerprintConstants.FP_TAG_COMMON);
                        startActivityForResult(intent, ADD_FINGERPRINT_REQUEST);
                    } else {
                        Settings.System.putInt(getContentResolver(),
                                FingerprintConstants.TCT_FINGERPRINT_UNLOCK_SCREEN, 1);
                    }
                } else {
                    showRemoveUnlockDeviceDialog();
                    return false;
                }
                updatePreferences();
            }
            else if (KEY_FINGERPRINT_QUICK_LAUNCH_FUNC.equals(key)) {
                if ((Boolean) (value)) {
                    ////if the quick launch func is on ,the unlock device must be turned on
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_UNLOCK_SCREEN, 1);
                    Settings.System.putInt(getContentResolver(),
                            FingerprintConstants.TCT_FINGERPRINT_QUICK_LAUNCH_FUNC, 1);
                    updatePreferences();
                } else {
                    showRemoveFuncDialog();
                    return false;
                }
            } else if (KEY_FINGERPRINT_UNLOCK_APP_ENABLE.equals(key)) {
                //Begin added by jinlong.lu for XR6618444 on 18-7-31
                if (fingerprintCount <= 0){
                    return false;//Do not update preference
                }
                //End added by jinlong.lu for XR6618444 on 18-7-31
                //Begin modified by jinlong.lu for XR7099324 on 18-11-6
                //Begin added by jinlong.lu for XR6312208 on 18-5-9
                //Begin modified by jinlong.lu for XR7109895 on 18-11-14
                //String hasPassword = Settings.System.getString(getContentResolver(), FingerprintUtils.APPS_LOCK_PASSWORD);
                Intent intent = new Intent();
                boolean switchOn =(boolean) value;
                boolean isAppsLockOn = Settings.System.getInt(getContentResolver(), FingerprintUtils.APPS_LOCK_ON, 0)==1;
                if (isAppsLockOn) {
                //End modified by jinlong.lu for XR7109895 on 18-11-14
                    //applock switch on
                    if (switchOn) {//fingerprint unlock app switch on,confirm app lock password
                        try {
                            intent.setAction(FINGERPRINT_CONFIRM_APP_LOCK_ACTION);
                            intent.putExtra(FingerprintUtils.CONFIRM_SETTINGS_LOCK_PATTERN, true);
                            startActivityForResult(intent, FINGERPRINT_CONFIRM_APP_LOCK_REQUEST);
                            return false;
                        } catch (ActivityNotFoundException e) {
                            Log.e(TAG, "apps lock activity not found");
                        }
                    } else {//close fingerprint unlock app
                        Settings.System.putInt(getContentResolver(),
                                FingerprintUtils.APPS_LOCK_FINGERPRINT_UNLOCK, 0);
                    }
                } else {
                    //set up app lock password
                    try {
                        intent.setAction(FINGERPRINT_APP_LOCK_ACTION);
                        startActivityForResult(intent, FINGERPRINT_APP_LOCK_REQUEST);
                        return false;
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "apps lock activity not found");
                    }
                }
                //End modified by jinlong.lu for XR7099324 on 18-11-6
                //End added by jinlong.lu for XR6312208 on 18-5-9
            }
            //End added by jinlong.lu for XR6618444 on 18-7-31
            else {
                Log.v(TAG, "Unknown key:" + key);
            }
            return result;
        }
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        private void showRemoveUnlockDeviceDialog() {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    //Begin modified by jinlong.lu for XRP23382 on 18-8-30
                    .setCustomTitle(createDialogTitleView())
                    .setView(R.layout.dialog_remove_fingerprint_functions_info)
                    //End modified by jinlong.lu for XRP23382 on 18-8-30
                    .setPositiveButton(R.string.fp_reset_unlock_device_positive,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Settings.System.putInt(getContentResolver(),
                                            FingerprintConstants.TCT_FINGERPRINT_UNLOCK_SCREEN, 0);
                                    //if the unlock device is off ,the quick launch func must be turned off
                                    Settings.System.putInt(getContentResolver(),
                                            FingerprintConstants.TCT_FINGERPRINT_QUICK_LAUNCH_FUNC, 0);//turned off QUICK_LAUNCH_FUNC
                                    resetFingerprintFunc();//reset func
                                    updatePreferences();
                                }
                            }).setNegativeButton(R.string.fp_reset_unlock_device_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (mUnlockScreen != null) {
                                        mUnlockScreen.setChecked(true);
                                    }
                                    dialog.dismiss();
                                }
                            }).show();
        }
        private void showRemoveFuncDialog() {
            AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.fp_reset_func_title)
                    .setMessage(R.string.fp_reset_func_message)
                    .setPositiveButton(R.string.fp_reset_func_positive,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Settings.System.putInt(getContentResolver(),
                                            FingerprintConstants.TCT_FINGERPRINT_QUICK_LAUNCH_FUNC, 0);
                                    resetFingerprintFunc();
                                    updatePreferences();
                                }
                            }).setNegativeButton(R.string.fp_reset_func_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    mQuickLaunchFunc.setChecked(true);
                                    dialog.dismiss();
                                }
                            }).show();
        }

        //Begin added by jinlong.lu for XRP23382 on 18-8-30
        private View createDialogTitleView() {
            final LayoutInflater layoutInflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View view = layoutInflater.inflate(R.layout.dialog_remove_fingerprint_functions_title, null);
            return view;
        }
        //End added by jinlong.lu for XRP23382 on 18-8-30
        private void resetFingerprintFunc(){
            //Begin modified by jinlong.lu for XR6873798 on 18-8-28
            int mMaxfp=getActivity().getResources().getInteger(
                    R.integer.config_fingerprintMaxTemplatesPerUser);
            //End modified by jinlong.lu for XR6873798 on 18-8-28
            for (int i = 0; i < mMaxfp; i++) {
                Settings.System.putString(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM + i, null);
            }
        }
        //End added by jinlong.lu for XR6618444 on 18-7-31
        @Override
        protected int getHelpResource() {
            return R.string.help_url_fingerprint;
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST
                    || requestCode == CONFIRM_REQUEST) {
                mLaunchedConfirm = false;
                if (resultCode == RESULT_FINISHED || resultCode == RESULT_OK) {
                    // The lock pin/pattern/password was set. Start enrolling!
                    if (data != null) {
                        mToken = data.getByteArrayExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                    }
                    //Begin added by jinlong.lu for XR6618444 on 18-7-31
                    isConfirmPassword = true;
                    //End added by jinlong.lu for XR6618444 on 18-7-31
                }
            } else if (requestCode == ADD_FINGERPRINT_REQUEST) {
                if (resultCode == RESULT_TIMEOUT) {
                    Activity activity = getActivity();
                    activity.setResult(RESULT_TIMEOUT);
                    activity.finish();
                }
            }
            //Begin added by jinlong.lu for XR6618444 on 18-7-31
            else if(requestCode ==FINGERPRINT_ENROLL_INTRODUCTION_ERGO_REQUEST){
                mLaunchedConfirm = false;
                if (data != null) {
                    mToken = data.getByteArrayExtra(
                            ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                    if (mToken != null){
                        isConfirmPassword = true;
                    }
                }
            //Begin modified by jinlong.lu for XR7099324 on 18-11-6
            } else if (requestCode == FINGERPRINT_APP_LOCK_REQUEST) {
                if (mFingerprintUnlockApp != null) {
                  //Begin modified by jinlong.lu for XR7109895 on 18-11-14
                  boolean isAppsLockOn =  Settings.System.getInt(getContentResolver(), FingerprintUtils.APPS_LOCK_ON, 0)==1;
                    if (isAppsLockOn) {
                  //End modified by jinlong.lu for XR7109895 on 18-11-14
                        Settings.System.putInt(getContentResolver(),
                                FingerprintUtils.APPS_LOCK_FINGERPRINT_UNLOCK, 1);
                        mFingerprintUnlockApp.setChecked(true);
                        mFingerprintUnlockApp.setEnabled(true);
                    } else {
                        mFingerprintUnlockApp.setChecked(false);
                        Settings.System.putInt(getContentResolver(),
                                FingerprintUtils.APPS_LOCK_FINGERPRINT_UNLOCK, 0);
                    }
                }
            }else if(requestCode ==FINGERPRINT_CONFIRM_APP_LOCK_REQUEST){
            //End modified by jinlong.lu for XR7099324 on 18-11-6
                if(resultCode ==RESULT_OK) {
                    if (mFingerprintUnlockApp != null) {
                        boolean isFingerprintAppsLockOn = Settings.System.getInt(getContentResolver(), FingerprintUtils.APPS_LOCK_FINGERPRINT_UNLOCK, 0) == 1;
                        mFingerprintUnlockApp.setChecked(!isFingerprintAppsLockOn);
                        Settings.System.putInt(getContentResolver(),
                                FingerprintUtils.APPS_LOCK_FINGERPRINT_UNLOCK, isFingerprintAppsLockOn ? 0 : 1);
                    }
                }
            }
            //End added by jinlong.lu for XR6618444 on 18-7-31

            if (mToken == null) {
                // Didn't get an authentication, finishing
                getActivity().finish();
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (getActivity().isFinishing()) {
                int result = mFingerprintManager.postEnroll();
                if (result < 0) {
                    Log.w(TAG, "postEnroll failed: result = " + result);
                }
            }
            //Begin added by jinlong.lu for XR7025241 on 18-9-21
            if (mTctFingerprintUtils != null) {
                mTctFingerprintUtils.unregisterDataObserver();
            }
            //End added by jinlong.lu for XR7025241 on 18-9-21
            //Begin added by jinlong.lu for XR7074057 on 18-11-3
            if (null != mHomeAndRecentKeyListener) {
                try {
                    mHomeAndRecentKeyListener.unregister();
                } catch (Exception e) {
                    Log.d(TAG, "unregister error" + e.getMessage());
                }
            }
            //End added by jinlong.lu for XR7074057 on 18-11-3
        }

        private Drawable getHighlightDrawable() {
            if (mHighlightDrawable == null) {
                final Activity activity = getActivity();
                if (activity != null) {
                    mHighlightDrawable = activity.getDrawable(R.drawable.preference_highlight);
                }
            }
            return mHighlightDrawable;
        }

        private void highlightFingerprintItem(int fpId) {
            String prefName = genKey(fpId);
            FingerprintPreference fpref = (FingerprintPreference) findPreference(prefName);
          //Begin modified by jinlong.lu for XR6618444 on 18-3-23
            final Drawable highlight = getHighlightDrawable();
            //private mode fingerprint auth success,fpref =null
            if (highlight != null && fpref != null) {
                final View view = fpref.getView();
                if(view!=null) {
                    final int centerX = view.getWidth() / 2;
                    final int centerY = view.getHeight() / 2;
                    highlight.setHotspot(centerX, centerY);
                    view.setBackground(highlight);
                    view.setPressed(true);
                    view.setPressed(false);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            view.setBackground(null);
                        }
                    }, RESET_HIGHLIGHT_DELAY_MS);
                }
            }
          //End modified by jinlong.lu for XR6618444 on 18-3-23
        }

        private void launchChooseOrConfirmLock() {
            Intent intent = new Intent();
            long challenge = mFingerprintManager.preEnroll();
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(getActivity(), this);
            if (!helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge, mUserId)) {
                intent.setClassName("com.android.settings", ChooseLockGeneric.class.getName());
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS,
                        true);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
                //Begin added by jinlong.lu for XR6618444 on 18-7-31
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);
                //End added by jinlong.lu for XR6618444 on 18-7-31
                intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
                startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
            }
        }

        @VisibleForTesting
        void deleteFingerPrint(Fingerprint fingerPrint) {
            mRemovalSidecar.startRemove(fingerPrint, mUserId);
            String name = genKey(fingerPrint.getFingerId());
            Preference prefToRemove = findPreference(name);
            prefToRemove.setEnabled(false);
            updateAddPreference();
        }

        private void renameFingerPrint(int fingerId, String newName) {
            mFingerprintManager.rename(fingerId, mUserId, newName);
            if (!TextUtils.isEmpty(newName)) {
                mFingerprintsRenaming.put(fingerId, newName);
            }
            updatePreferences();
        }

        private final Runnable mFingerprintLockoutReset = new Runnable() {
            @Override
            public void run() {
                mInFingerprintLockout = false;
                retryFingerprint();
            }
        };
        //Begin added by jinlong.lu for XR7074057 on 18-11-3
        /**
         * It listen the Home and recent menu keys, and finish current activity
         * when these two buttons are pressed.
         */
        static class HomeAndRecentKeyListener extends BroadcastReceiver {
            Activity mHost;
            private static final String SYSTEM_REASON = "reason";
            private static final String SYSTEM_HOME_KEY = "homekey";
            private static final String SYSTEM_RECENT_APPS = "recentapps";
            private FingerprintSettingsFragment mFragment;

            public HomeAndRecentKeyListener register(FingerprintSettingsFragment fragment) {
                mHost = fragment.getActivity();
                mFragment =fragment;
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
                //Begin added by jinlong.lu for XRP10025530 on 18-11-5
                intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
                intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
                //End added by jinlong.lu for XRP10025530 on 18-11-5
                if (mHost != null) {
                    mHost.registerReceiver(this, intentFilter);
                }
                return this;
            }

            public void unregister() {
                if (null != mHost) {
                    mHost.unregisterReceiver(this);
                }
            }

            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                //Begin modified by jinlong.lu for XR7102040 on 18-11-9
                if (action != null && action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
                    String reason = intent.getStringExtra(SYSTEM_REASON);
                    if (SYSTEM_HOME_KEY.equals(reason) ||SYSTEM_RECENT_APPS.equals(reason)) {
                        if (mFragment != null && mFragment.isConfirmPassword) {
                            Log.d(TAG,"Home or recent key call");
                            //only finish when password was confirm
                            if(mHost!=null) {
                                mHost.finish();
                            }
                        }
                    }
                //Begin modified by jinlong.lu for XRP10025530 on 18-11-5
                } else if (action != null && action.equals(Intent.ACTION_SCREEN_OFF)) {
                    //only finish when password was confirm
                    if (mFragment != null && mFragment.isConfirmPassword) {
                        Log.d(TAG, "screen off,finish");
                        //only finish when password was confirm
                        if(mHost!=null) {
                            mHost.finish();
                        }
                    }
                }
                //End modified by jinlong.lu for XR7102040 on 18-11-9
                //End modified by jinlong.lu for XRP10025530 on 18-11-5
            }
        }
        //End added by jinlong.lu for XR7074057 on 18-11-3
        public static class DeleteFingerprintDialog extends InstrumentedDialogFragment
                implements DialogInterface.OnClickListener {

            private static final String KEY_FINGERPRINT = "fingerprint";
            private Fingerprint mFp;
            private AlertDialog mAlertDialog;

            public static DeleteFingerprintDialog newInstance(Fingerprint fp,
                    FingerprintSettingsFragment target) {
                final DeleteFingerprintDialog dialog = new DeleteFingerprintDialog();
                final Bundle bundle = new Bundle();
                bundle.putParcelable(KEY_FINGERPRINT, fp);
                dialog.setArguments(bundle);
                dialog.setTargetFragment(target, 0 /* requestCode */);
                return dialog;
            }

            @Override
            public int getMetricsCategory() {
                return MetricsEvent.DIALOG_FINGERPINT_EDIT;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable(KEY_FINGERPRINT);
                final String title = getString(R.string.fingerprint_delete_title, mFp.getName());

                mAlertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(title)
                        .setMessage(R.string.fingerprint_delete_message)
                        .setPositiveButton(
                                R.string.security_settings_fingerprint_enroll_dialog_delete,
                                this /* onClickListener */)
                        .setNegativeButton(R.string.cancel, null /* onClickListener */)
                        .create();
                return mAlertDialog;
            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == DialogInterface.BUTTON_POSITIVE) {
                    final int fingerprintId = mFp.getFingerId();
                    Log.v(TAG, "Removing fpId=" + fingerprintId);
                    mMetricsFeatureProvider.action(getContext(),
                            MetricsEvent.ACTION_FINGERPRINT_DELETE,
                            fingerprintId);
                    FingerprintSettingsFragment parent
                            = (FingerprintSettingsFragment) getTargetFragment();
                    parent.deleteFingerPrint(mFp);
                }
            }
        }

        public static class RenameDialog extends InstrumentedDialogFragment {

            private Fingerprint mFp;
            private EditText mDialogTextField;
            private String mFingerName;
            private Boolean mTextHadFocus;
            private int mTextSelectionStart;
            private int mTextSelectionEnd;
            private AlertDialog mAlertDialog;
            private boolean mDeleteInProgress;

            public void setDeleteInProgress(boolean deleteInProgress) {
                mDeleteInProgress = deleteInProgress;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                if (savedInstanceState != null) {
                    mFingerName = savedInstanceState.getString("fingerName");
                    mTextHadFocus = savedInstanceState.getBoolean("textHadFocus");
                    mTextSelectionStart = savedInstanceState.getInt("startSelection");
                    mTextSelectionEnd = savedInstanceState.getInt("endSelection");
                }
                mAlertDialog = new AlertDialog.Builder(getActivity())
                        .setView(R.layout.fingerprint_rename_dialog)
                        .setPositiveButton(R.string.security_settings_fingerprint_enroll_dialog_ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        final String newName =
                                                mDialogTextField.getText().toString();
                                        final CharSequence name = mFp.getName();
                                        if (!TextUtils.equals(newName, name)) {
                                            Log.d(TAG, "rename " + name + " to " + newName);
                                            mMetricsFeatureProvider.action(getContext(),
                                                    MetricsEvent.ACTION_FINGERPRINT_RENAME,
                                                    mFp.getFingerId());
                                            FingerprintSettingsFragment parent
                                                    = (FingerprintSettingsFragment)
                                                    getTargetFragment();
                                            parent.renameFingerPrint(mFp.getFingerId(),
                                                    newName);
                                        }
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                mAlertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                    @Override
                    public void onShow(DialogInterface dialog) {
                        mDialogTextField = (EditText) mAlertDialog.findViewById(
                                R.id.fingerprint_rename_field);
                        CharSequence name = mFingerName == null ? mFp.getName() : mFingerName;
                        mDialogTextField.setText(name);
                        if (mTextHadFocus == null) {
                            mDialogTextField.selectAll();
                        } else {
                            mDialogTextField.setSelection(mTextSelectionStart, mTextSelectionEnd);
                        }
                        if (mDeleteInProgress) {
                            mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                        }

                        //Begin added by jinlong.lu for XR6618444 on 18-7-31
                        mDialogTextField.addTextChangedListener(new TextWatcher() {
                            @Override
                            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                            }

                            @Override
                            public void onTextChanged(CharSequence s, int start, int before, int count) {

                            }

                            @Override
                            public void afterTextChanged(Editable s) {
                                if (s == null) {
                                    mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                    return;
                                }
                                if (s.toString().trim().equals("")) {
                                    mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                                } else {
                                    mAlertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                                }
                            }
                        });
                        //End added by jinlong.lu for XR6618444 on 18-7-31

                    }
                });
                if (mTextHadFocus == null || mTextHadFocus) {
                    // Request the IME
                    mAlertDialog.getWindow().setSoftInputMode(
                            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
                return mAlertDialog;
            }

            public void enableDelete() {
                mDeleteInProgress = false;
                if (mAlertDialog != null) {
                    mAlertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(true);
                }
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

            @Override
            public int getMetricsCategory() {
                return MetricsEvent.DIALOG_FINGERPINT_EDIT;
            }
        }

        public static class ConfirmLastDeleteDialog extends InstrumentedDialogFragment {

            private Fingerprint mFp;

            @Override
            public int getMetricsCategory() {
                return MetricsEvent.DIALOG_FINGERPINT_DELETE_LAST;
            }

            @Override
            public Dialog onCreateDialog(Bundle savedInstanceState) {
                mFp = getArguments().getParcelable("fingerprint");
                final boolean isProfileChallengeUser =
                        getArguments().getBoolean("isProfileChallengeUser");
                final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                        .setTitle(R.string.fingerprint_last_delete_title)
                        .setMessage((isProfileChallengeUser)
                                ? R.string.fingerprint_last_delete_message_profile_challenge
                                : R.string.fingerprint_last_delete_message)
                        .setPositiveButton(R.string.fingerprint_last_delete_confirm,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        FingerprintSettingsFragment parent
                                                = (FingerprintSettingsFragment) getTargetFragment();
                                        parent.deleteFingerPrint(mFp);
                                        dialog.dismiss();
                                    }
                                })
                        .setNegativeButton(
                                R.string.cancel,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                })
                        .create();
                return alertDialog;
            }
        }
        //Begin added by jinlong.lu for XR6618444 on 18-8-1
        public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
                new FingerprintSearchIndexProvider();

        private static class FingerprintSearchIndexProvider extends BaseSearchIndexProvider {
            private static final String KEY_FINGERPRINTS_LIST = "fingerprints_list";
            private static final String KEY_FINGERPRINTS_FUNCTION = "features";
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                                                                        boolean enabled) {
                final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(context);
                if (fpm != null && fpm.isHardwareDetected()) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.security_settings_fingerprint_manager;
                    return Arrays.asList(sir);
                }else {
                    return null;
                }

            }
            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                keys.add(KEY_FINGERPRINTS_LIST);
                keys.add(KEY_FINGERPRINTS_FUNCTION);
                return keys;
            }
        }
        //End added by jinlong.lu for XR6618444 on 18-8-1
    }

    public static class FingerprintPreference extends TwoTargetPreference {

        private final OnDeleteClickListener mOnDeleteClickListener;

        private Fingerprint mFingerprint;
        private View mView;
        private View mDeleteView;
        //Begin modified by jinlong.lu for XR6618444 on 18-7-31
        private View mGearView;
        private boolean isEnabled = true;
        private final OnGearClickListener mOnGearClickListener;
        public boolean isGearEnabled() {
            return isEnabled;
        }
        public void setGearEnabled(boolean enabled) {
            isEnabled = enabled;
        }

        public interface OnDeleteClickListener {
            void onDeleteClick(FingerprintPreference p);
        }
        public interface OnGearClickListener {
            void onGearClick(FingerprintPreference p);
        }
        public FingerprintPreference(Context context, OnGearClickListener onGearClickListener,OnDeleteClickListener onDeleteClickListener) {
            super(context);
            mOnDeleteClickListener = onDeleteClickListener;
            mOnGearClickListener =onGearClickListener;
        }
        //End modified by jinlong.lu for XR6618444 on 18-7-31
        public View getView() {
            return mView;
        }

        public void setFingerprint(Fingerprint item) {
            mFingerprint = item;
        }

        public Fingerprint getFingerprint() {
            return mFingerprint;
        }

        @Override
        protected int getSecondTargetResId() {
            //Begin modified by jinlong.lu for XR6618444 on 18-7-31
            return R.layout.preference_widget_delete_gear;
            //End modified by jinlong.lu for XR6618444 on 18-7-31
        }

        @Override
        public void onBindViewHolder(PreferenceViewHolder view) {
            super.onBindViewHolder(view);
            mView = view.itemView;
            mDeleteView = view.itemView.findViewById(R.id.delete_button);
            mDeleteView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnDeleteClickListener != null) {
                        mOnDeleteClickListener.onDeleteClick(FingerprintPreference.this);
                    }
                }
            });
            //Begin added by jinlong.lu for XR6618444 on 18-7-31
            mGearView = view.itemView.findViewById(R.id.settings_button);
            mGearView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mOnGearClickListener != null) {
                        mOnGearClickListener.onGearClick(FingerprintPreference.this);
                    }
                }
            });
            mGearView.setEnabled(isEnabled);
            //End added by jinlong.lu for XR6618444 on 18-7-31
        }
    }

    private static class LearnMoreSpan extends URLSpan {

        private static final Typeface TYPEFACE_MEDIUM =
                Typeface.create("sans-serif-medium", Typeface.NORMAL);

        private static final String ANNOTATION_URL = "url";
        private static final String ANNOTATION_ADMIN_DETAILS = "admin_details";

        private EnforcedAdmin mEnforcedAdmin = null;

        private LearnMoreSpan(String url) {
            super(url);
        }

        private LearnMoreSpan(EnforcedAdmin admin) {
            super((String) null);
            mEnforcedAdmin = admin;
        }

        @Override
        public void onClick(View widget) {
            Context ctx = widget.getContext();
            if (mEnforcedAdmin != null) {
                RestrictedLockUtils.sendShowAdminSupportDetailsIntent(ctx, mEnforcedAdmin);
            } else {
                Intent intent = HelpUtils.getHelpIntent(ctx, getURL(), ctx.getClass().getName());
                try {
                    widget.startActivityForResult(intent, 0);
                } catch (ActivityNotFoundException e) {
                    Log.w(FingerprintSettingsFragment.TAG,
                            "Actvity was not found for intent, " + intent.toString());
                }
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setUnderlineText(false);
            ds.setTypeface(TYPEFACE_MEDIUM);
        }

        public static CharSequence linkify(CharSequence rawText, String uri, EnforcedAdmin admin) {
            SpannableString msg = new SpannableString(rawText);
            Annotation[] spans = msg.getSpans(0, msg.length(), Annotation.class);
            /// M: ALPS02884741 If uri is empty
            if (TextUtils.isEmpty(uri)) {
                CharSequence ret = rawText;
                for (Annotation annotation : spans) {
                    int start = msg.getSpanStart(annotation);
                    int end = msg.getSpanEnd(annotation);
                    ret = TextUtils.concat(ret.subSequence(0, (start > ret.length() ? ret.length()
                            : start)), msg.subSequence(end, msg.length()));
                }
                return ret;
            } else {
                SpannableStringBuilder builder = new SpannableStringBuilder(msg);
                for (Annotation annotation : spans) {
                    final String key = annotation.getValue();
                    int start = msg.getSpanStart(annotation);
                    int end = msg.getSpanEnd(annotation);
                    LearnMoreSpan link = null;
                    if (ANNOTATION_URL.equals(key)) {
                        link = new LearnMoreSpan(uri);
                    } else if (ANNOTATION_ADMIN_DETAILS.equals(key)) {
                        link = new LearnMoreSpan(admin);
                    }
                    if (link != null) {
                        builder.setSpan(link, start, end, msg.getSpanFlags(link));
                    }
                }
                return builder;
            }
        }
    }

    public static Preference getFingerprintPreferenceForUser(Context context, final int userId) {
        final FingerprintManager fpm = Utils.getFingerprintManagerOrNull(context);
        if (fpm == null || !fpm.isHardwareDetected()) {
            Log.v(TAG, "No fingerprint hardware detected!!");
            return null;
        }
        Preference fingerprintPreference = new Preference(context);
        fingerprintPreference.setKey(KEY_FINGERPRINT_SETTINGS);
        fingerprintPreference.setTitle(R.string.security_settings_fingerprint_preference_title);
        TctFingerprintUtils tctFingerprintUtils = new TctFingerprintUtils(context);
        //Begin modified by jinlong.lu for XR6618444 on 18-7-31
        final List<Fingerprint> items = tctFingerprintUtils.getTctEnrolledFingerprints(userId, FingerprintConstants.FP_TAG_COMMON);
        //End modified by jinlong.lu for XR6618444 on 18-7-31
        final int fingerprintCount = items != null ? items.size() : 0;
        final String clazz;
        if (fingerprintCount > 0) {
            fingerprintPreference.setSummary(context.getResources().getQuantityString(
                    R.plurals.security_settings_fingerprint_preference_summary,
                    fingerprintCount, fingerprintCount));
            clazz = FingerprintSettings.class.getName();
        } else {
            fingerprintPreference.setSummary(
                    R.string.security_settings_fingerprint_preference_summary_none);
            clazz = FingerprintEnrollIntroduction.class.getName();
        }
        fingerprintPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                final Context context = preference.getContext();
                final UserManager userManager = UserManager.get(context);
                if (Utils.startQuietModeDialogIfNecessary(context, userManager,
                        userId)) {
                    return false;
                }
                Intent intent = new Intent();
                intent.setClassName("com.android.settings", clazz);
                intent.putExtra(Intent.EXTRA_USER_ID, userId);
                context.startActivity(intent);
                return true;
            }
        });
        return fingerprintPreference;
    }
}
