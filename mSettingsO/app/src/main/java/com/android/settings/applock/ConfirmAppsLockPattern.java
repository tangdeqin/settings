/******************************************************************************/
/*                                                               Date:09/2016 */
/*                                PRESENTATION                                */
/*                                                                            */
/*       Copyright 2016 TCL Communication Technology Holdings Limited.        */
/*                                                                            */
/* This material is company confidential, cannot be reproduced in any form    */
/* without the written permission of TCL Communication Technology Holdings    */
/* Limited.                                                                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Author :  dongchi.chen                                                    */
/*  Email  :                                                                  */
/*  Role   :                                                                  */
/*  Reference documents :                                                     */
/* -------------------------------------------------------------------------- */
/*  Comments :                                                                */
/*  File     :                                                                */
/*  Labels   :                                                                */
/* -------------------------------------------------------------------------- */
/* ========================================================================== */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* ----------|----------------------|----------------------|----------------- */
/* 30/09/2016|     dongchi.chen     |     task 3004929     |private mode      */
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/
package com.android.settings.applock;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.animation.Interpolator;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ConfirmDeviceCredentialBaseActivity;
import com.android.settings.password.ConfirmDeviceCredentialBaseFragment;
import com.android.settings.R;
import com.android.settingslib.animation.AppearAnimationCreator;
import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import java.lang.String;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tct.sdk.base.applock.TctAppLockHelper;
import android.view.WindowManager;

import android.net.Uri;

import android.hardware.fingerprint.FingerprintManager;

import android.app.ActivityManager;
import android.app.IActivityManager;

/**
 * Launch this when you want the user to confirm their lock pattern.
 *
 * Sets an activity result of {@link Activity#RESULT_OK} when the user
 * successfully confirmed their pattern.
 */
public class ConfirmAppsLockPattern extends ConfirmDeviceCredentialBaseActivity {

    public static final String TAG = "AppsLock"; //added by dongchi.chen for XR7135877 on 20181121

    public static final String CONFIRM_SETTINGS_LOCK_PATTERN = "confirm_settings_lock_pattern";

    private static ArrayList<String> mIngnoreAddedClearTaskFlagList = new ArrayList<>();
    static {
        mIngnoreAddedClearTaskFlagList.add("com.tcl.soundrecorder");
        mIngnoreAddedClearTaskFlagList.add("com.gameloft.android.GloftANPH");
    }

    public static class InternalActivity extends ConfirmAppsLockPattern {
    }

    @Override
    public void onResume() {
        super.onResume();
        KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (keyguardManager.isKeyguardSecure()){
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        intent.putExtra(ConfirmDeviceCredentialBaseFragment.TITLE_TEXT, getString(R.string.apps_lock));

        if(null == mPowerReceiver) {
            mActivity = new WeakReference<Activity>(this);
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            filter.setPriority(10000);
            mPowerReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(final Context context, final Intent intent) {
                    final String action = intent.getAction();
                    if (action != null && action.equals(Intent.ACTION_SCREEN_OFF)) {
                        if(null != mActivity.get()){
                            //mActivity.get().finish(); //modified by dongchi.chen for XRP10024800 on 20181026
                            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                            //Begin added by dongchi.chen for XR7135877 on 20181121
                            resetFingerprintIfNeed(getApplicationContext());
                            //End added by dongchi.chen for XR7135877 on 20181121
                        }
                    }
                }
            };
            registerReceiver(mPowerReceiver, filter);
        }

        //Begin added by dongchi.chen for XRP10024562 on 20181019
        getWindow().setNavigationBarColor(Color.WHITE);
        //End added by dongchi.chen for XRP10024562 on 20181019
    }

    //Begin added by dongchi.chen for XR7135877 on 20181121
    private static void resetFingerprintIfNeed(Context context){
        try {
            if(TctAppLockHelper.createHelper(context).isFingerprintUnlockAppsLockEnable()) {
                FingerprintManager fingerprintManager = (FingerprintManager) context.getSystemService(Context.FINGERPRINT_SERVICE);
                if (null != fingerprintManager
                        && fingerprintManager.isHardwareDetected()
                        && fingerprintManager.hasEnrolledFingerprints(0)) {
                    Log.d(TAG, "resetFingerprint");
                    fingerprintManager.resetTimeout(null);
                }
            }
        }catch (Exception e){
            Log.e(TAG, "resetFingerprint error - " + e.getMessage());
        }
    }
    //End added by dongchi.chen for XR7135877 on 20181121

    private BroadcastReceiver mPowerReceiver = null;
    private WeakReference<Activity> mActivity = null;
    @Override
    protected void onDestroy() {
        if(null != mPowerReceiver){
            unregisterReceiver(mPowerReceiver);
            mPowerReceiver = null;
        }
        super.onDestroy();
    }


    @Override
    protected void onPause() {
        super.onPause();
    }

    private enum Stage {
        NeedToUnlock,
        NeedToUnlockWrong,
        LockedOut
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmAppsLockPatternFragment.class.getName());
        return modIntent;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode ==KeyEvent.KEYCODE_BACK && !getIntent().getBooleanExtra(CONFIRM_SETTINGS_LOCK_PATTERN, false)) {
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            getApplicationContext().startActivity(homeIntent);
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home && !getIntent().getBooleanExtra(CONFIRM_SETTINGS_LOCK_PATTERN, false)){
            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
            homeIntent.addCategory(Intent.CATEGORY_HOME);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            getApplicationContext().startActivity(homeIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmAppsLockPatternFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    public static class ConfirmAppsLockPatternFragment extends ConfirmDeviceCredentialBaseFragment
            implements AppearAnimationCreator<Object> {

        // how long we wait to clear a wrong pattern
        private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

        private static final String KEY_NUM_WRONG_ATTEMPTS = "num_wrong_attempts";

        private LockPatternView mLockPatternView;
        private LockPatternUtils mLockPatternUtils;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private int mNumWrongConfirmAttempts;
        private CountDownTimer mCountdownTimer;

        private TextView mHeaderTextView;
        private TextView mDetailsTextView;
        private TextView mErrorTextView;
        private View mLeftSpacerLandscape;
        private View mRightSpacerLandscape;

        // caller-supplied text for various prompts
        private CharSequence mHeaderText;
        private CharSequence mDetailsText;

        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;

        private int mEffectiveUserId;

        private TctAppLockHelper mAppLockHelper;
        // required constructor for fragments
        public ConfirmAppsLockPatternFragment() {

        }

        @Override
        protected void onShowError() {
        }

        private void StartLockedApp(){

            mAppLockHelper.setAuthedPackage(getActivity().getIntent().getStringExtra("package_name"));
            Log.d("AppsLock", "client setAuthedActivity" + getActivity().getIntent().getStringExtra("package_name"));

            Intent intent = getActivity().getIntent().getParcelableExtra("intent");
            if(null != intent){
                String action = intent.getAction();
                if(null != action && action.equalsIgnoreCase("com.tct.privacymode.action.APPS_LOCK.forIntentSender")){
                    Log.d("AppsLock", "startActivityIntentSender again");
                    Context context = getActivity().getApplicationContext();
                    IntentSender intentSender = intent.getParcelableExtra("intentSender");
                    Intent fillInIntent = intent.getParcelableExtra("fillInIntent");
                    Bundle opt = intent.getBundleExtra("opt");
                    int flagsMask = intent.getIntExtra("flagsMask", 0);
                    int flagsValues = intent.getIntExtra("flagsValues", 0);
                    try {
                        context.startIntentSender(intentSender, fillInIntent, flagsMask, flagsValues,
                                0, opt);
                    }catch (IntentSender.SendIntentException e){
                        Log.d("AppsLock", "SendIntentException", e);
                    }

                }else {
            //Begin modified by dongchi.chen for XR7115868 on 20181126
                    getActivity().startActivity(intent);
                }
            }

            boolean launch_task_from_recents = getActivity().getIntent().getBooleanExtra("launch_task_from_recents", false);
            if(launch_task_from_recents){
                Bundle bOptions = getActivity().getIntent().getBundleExtra("bOptions");
                int taskId = getActivity().getIntent().getIntExtra("taskId", -1);
                try {
                    ActivityManager.getService().startActivityFromRecents(taskId, bOptions);
                }catch (Exception e){

                }
            }
            //Begin modified by dongchi.chen for XR7115868 on 20181127
        }

        @Override
        protected int getLastTryErrorMessage(int userType) {
             switch (userType) {
                 case USER_TYPE_PRIMARY:
                     return R.string.lock_last_pattern_attempt_before_wipe_device;
                 case USER_TYPE_MANAGED_PROFILE:
                     return R.string.lock_last_pattern_attempt_before_wipe_profile;
                 case USER_TYPE_SECONDARY:
                     return R.string.lock_last_pattern_attempt_before_wipe_user;
                 default:
                     throw new IllegalArgumentException("Unrecognized user type:" + userType);
             }
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
            mAppLockHelper = TctAppLockHelper.createHelper(getContext());
            if(mAppLockHelper.isFingerprintUnlockAppsLockEnable()) {
                mApplockFpAllowed = true;
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.confirm_appslock_pattern, null);
            mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            mLeftSpacerLandscape = view.findViewById(R.id.leftSpacer);
            mRightSpacerLandscape = view.findViewById(R.id.rightSpacer);

            // make it so unhandled touch events within the unlock screen go to the
            // lock pattern view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockPatternView);

            Intent intent = getActivity().getIntent();
            if (intent != null) {
                mHeaderText = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.HEADER_TEXT);
                mDetailsText = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT);
            }

            mLockPatternView.setTactileFeedbackEnabled(
                    mLockPatternUtils.isTactileFeedbackEnabled());
            mLockPatternView.setOnPatternListener(mConfirmExistingLockPatternListener);
            updateStage(Stage.NeedToUnlock);

            if (savedInstanceState != null) {
                mNumWrongConfirmAttempts = savedInstanceState.getInt(KEY_NUM_WRONG_ATTEMPTS);
            }else{
                mNumWrongConfirmAttempts = getAppsLockLockoutTimeOut();
            }

            mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                    AppearAnimationUtils.DEFAULT_APPEAR_DURATION, 2f /* translationScale */,
                    1.3f /* delayScale */, AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.linear_out_slow_in));
            mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                    125, 4f /* translationScale */,
                    0.3f /* delayScale */, AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.fast_out_linear_in),
                    new AppearAnimationUtils.RowTranslationScaler() {
                        @Override
                        public float getRowTranslationScale(int row, int numRows) {
                            return (float)(numRows - row) / numRows;
                        }
                    });
            setAccessibilityTitle(mHeaderTextView.getText());

            return view;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mFingerprintHelper.setIsAppsLock(true);
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            // deliberately not calling super since we are managing this in full
            outState.putInt(KEY_NUM_WRONG_ATTEMPTS, mNumWrongConfirmAttempts);
        }

        @Override
        public void onPause() {
            super.onPause();

            //mAppLockHelper.dispose();
            dispose();
            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
            }
            if (mPendingLockCheck != null) {
                mPendingLockCheck.cancel(false);
                mPendingLockCheck = null;
            }
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.CONFIRM_LOCK_PATTERN;
        }

        @Override
        public void onResume() {
            super.onResume();
            String packageName = getActivity().getIntent().getStringExtra("package_name");
            if(null != packageName){
                boolean shouldLock = mAppLockHelper.shouldLockActivity(packageName);
                if(!shouldLock){
                    getActivity().finish();
                }
                Settings.System.putString(getActivity().getContentResolver(), "current_locked_app", packageName);//added by dongchi.chen for XRP10025548 on 20181114
            }

            // if the user is currently locked out, enforce it.
            long deadline = getAppsLockLockoutAttemptDeadline();//mAppLockHelper.getAppsLockLockoutAttemptDeadline();
            if (deadline != 0) {
                handleAttemptLockout(deadline);
            } else if (!mLockPatternView.isEnabled()) {
                // The deadline has passed, but the timer was cancelled. Or the pending lock
                // check was cancelled. Need to clean up.
                resetAppsLockWrongConfirmAttempts();
                //mAppLockHelper.resetAppsLockWrongConfirmAttempts();
                updateStage(Stage.NeedToUnlock);
            }
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            mHeaderTextView.setAlpha(0f);
            mCancelButton.setAlpha(0f);
            mLockPatternView.setAlpha(0f);
            mDetailsTextView.setAlpha(0f);
            mFingerprintIcon.setAlpha(1f);
        }

        private Object[][] getActiveViews() {
            ArrayList<ArrayList<Object>> result = new ArrayList<>();
            result.add(new ArrayList<Object>(Collections.singletonList(mHeaderTextView)));
            result.add(new ArrayList<Object>(Collections.singletonList(mDetailsTextView)));
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(new ArrayList<Object>(Collections.singletonList(mCancelButton)));
            }
            LockPatternView.CellState[][] cellStates = mLockPatternView.getCellStates();
            for (int i = 0; i < cellStates.length; i++) {
                ArrayList<Object> row = new ArrayList<>();
                for (int j = 0; j < cellStates[i].length; j++) {
                    row.add(cellStates[i][j]);
                }
                result.add(row);
            }
            if (mFingerprintIcon.getVisibility() == View.VISIBLE) {
                result.add(new ArrayList<Object>(Collections.singletonList(mFingerprintIcon)));
            }
            Object[][] resultArr = new Object[result.size()][cellStates[0].length];
            for (int i = 0; i < result.size(); i++) {
                ArrayList<Object> row = result.get(i);
                for (int j = 0; j < row.size(); j++) {
                    resultArr[i][j] = row.get(j);
                }
            }
            return resultArr;
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            mLockPatternView.setAlpha(1f);
            mAppearAnimationUtils.startAnimation2d(getActiveViews(), null, this);
        }

        private void updateStage(Stage stage) {
            switch (stage) {
                case NeedToUnlock:
                    if (mHeaderText != null) {
                        mHeaderTextView.setText(mHeaderText);
                    } else {
                        mHeaderTextView.setText(R.string.confirm_apps_lock_password);
                    }
                    if (mDetailsText != null) {
                        mDetailsTextView.setText(mDetailsText);
                    } else {
                        mDetailsTextView.setText(
                                R.string.confirm_apps_lock_password_generic);
                    }
                    mErrorTextView.setText("");

                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    mLockPatternView.clearPattern();
                    break;
                case NeedToUnlockWrong:
                    mErrorTextView.setText(R.string.lockpattern_need_to_unlock_wrong);

                    mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                    mLockPatternView.setEnabled(true);
                    mLockPatternView.enableInput();
                    break;
                case LockedOut:
                    mLockPatternView.clearPattern();
                    // enabled = false means: disable input, and have the
                    // appearance of being disabled.
                    mLockPatternView.setEnabled(false); // appearance of being disabled
                    break;
            }

            // Always announce the header for accessibility. This is a no-op
            // when accessibility is disabled.
            mHeaderTextView.announceForAccessibility(mHeaderTextView.getText());
        }

        private Runnable mClearPatternRunnable = new Runnable() {
            public void run() {
                mLockPatternView.clearPattern();
            }
        };

        // clear the wrong pattern unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
        }

        @Override
        protected void authenticationSucceeded() {
            startDisappearAnimation(new Intent());
        }

        private void startDisappearAnimation(final Intent intent) {
            //Begin modified by dongchi.chen for XR7115868 on 20181126
            Intent activtyIntent = getActivity().getIntent();
            if((null !=  activtyIntent) && (null != activtyIntent.getAction())
                    && (activtyIntent.getAction().equalsIgnoreCase("com.tct.privacymode.action.APPS_LOCK")
                    || activtyIntent.getAction().equalsIgnoreCase("android.intent.action.VIEW"))){
                StartLockedApp();
            } else if ((null != activtyIntent) && (null != activtyIntent.getAction())
                    && activtyIntent.getAction().equalsIgnoreCase("com.tct.fingerprint.confirm.action.APPS_LOCK")) {
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }

            if (getActivity().getThemeResId() == R.style.Theme_ConfirmDeviceCredentialsDark) {
                mLockPatternView.clearPattern();
                mDisappearAnimationUtils.startAnimation2d(getActiveViews(),
                        new Runnable() {
                            @Override
                            public void run() {
                                getActivity().setResult(RESULT_OK, intent);
                                getActivity().finish();
                                getActivity().overridePendingTransition(
                                        R.anim.confirm_credential_close_enter,
                                        R.anim.confirm_credential_close_exit);
                            }
                        }, this);
            } else {
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }
            //End modified by dongchi.chen for XR7115868 on 20181126
        }

        @Override
        public void onFingerprintIconVisibilityChanged(boolean visible) {
            if (mLeftSpacerLandscape != null && mRightSpacerLandscape != null) {

                // In landscape, adjust spacing depending on fingerprint icon visibility.
                mLeftSpacerLandscape.setVisibility(visible ? View.GONE : View.VISIBLE);
                mRightSpacerLandscape.setVisibility(visible ? View.GONE : View.VISIBLE);
            }
        }

        /**
         * The pattern listener that responds according to a user confirming
         * an existing lock pattern.
         */
        private LockPatternView.OnPatternListener mConfirmExistingLockPatternListener
                = new LockPatternView.OnPatternListener()  {

            public void onPatternStart() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCleared() {
                mLockPatternView.removeCallbacks(mClearPatternRunnable);
            }

            public void onPatternCellAdded(List<Cell> pattern) {

            }

            public void onPatternDetected(List<Cell> pattern) {
                mLockPatternView.setEnabled(false);
                if (mPendingLockCheck != null) {
                    mPendingLockCheck.cancel(false);
                }

                final boolean verifyChallenge = getActivity().getIntent().getBooleanExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
                Intent intent = new Intent();
                if (verifyChallenge) {
                    if (isInternalActivity()) {
                        startVerifyPattern(pattern, intent);
                        return;
                    }
                } else {
                    startCheckPattern(pattern, intent);
                    return;
                }

                onPatternChecked(pattern, false, intent, 0, mEffectiveUserId);
            }

            private boolean isInternalActivity() {
                return getActivity() instanceof ConfirmAppsLockPattern.InternalActivity;
            }

            private void startVerifyPattern(final List<Cell> pattern,
                                            final Intent intent) {

                final int localEffectiveUserId = mEffectiveUserId;
                String pd = LockPatternUtils.patternToString(pattern);
                boolean matched = mAppLockHelper.authenticateAppsLockPassword(pd);
                mNumWrongConfirmAttempts = matched ? 0 : mNumWrongConfirmAttempts + 1;
                onPatternChecked(pattern, matched, intent, /*mAppLockHelper.getAppsLockLockoutTimeOut()*/getAppsLockLockoutTimeOut(), localEffectiveUserId);
            }

            private void startCheckPattern(final List<Cell> pattern,
                                           final Intent intent) {
                if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                    onPatternChecked(pattern, false, intent, 0, mEffectiveUserId);
                    return;
                }

                final int localEffectiveUserId = mEffectiveUserId;
                String pd = LockPatternUtils.patternToString(pattern);
                boolean matched = mAppLockHelper.authenticateAppsLockPassword(pd);
                mNumWrongConfirmAttempts = matched ? 0 : mNumWrongConfirmAttempts + 1;
                if (matched && isInternalActivity()) {
                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_TYPE,
                            StorageManager.CRYPT_TYPE_PATTERN);
                    intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD,
                            LockPatternUtils.patternToString(pattern));
                }
                onPatternChecked(pattern, matched, intent, /*mAppLockHelper.getAppsLockLockoutTimeOut()*/getAppsLockLockoutTimeOut(), localEffectiveUserId);
            }

            private void onPatternChecked(List<Cell> pattern,
                                          boolean matched, Intent intent, int timeoutMs, int effectiveUserId) {
                mLockPatternView.setEnabled(true);
                if (matched) {
                    //Begin added by dongchi.chen for XR7135634 on 20181121
                    //Begin added by dongchi.chen for XR7135877 on 20181121
                    resetFingerprintIfNeed(getActivity());
                    //End added by dongchi.chen for XR7135877 on 20181121
                    //End added by dongchi.chen for XR7135634 on 20181121
                    startDisappearAnimation(intent);
                } else {
                    if (timeoutMs > 0) {
                        long deadline = setAppsLockLockoutAttemptDeadline(timeoutMs);//mAppLockHelper.setAppsLockLockoutAttemptDeadline(timeoutMs);
                        handleAttemptLockout(deadline);
                    } else {
                        updateStage(Stage.NeedToUnlockWrong);
                        postClearPatternRunnable();
                    }
                }
            }
        };

        //Begin modified by dongchi.chen for XRP10024561 on 20181019
        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            updateStage(Stage.LockedOut);
            mStopFingerPrintListening = true;
            refreshLockScreen();
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    if(secondsCountdown > 0) {
                        mErrorTextView.setText(getResources().getQuantityString(
                                R.plurals.lockpattern_too_many_failed_confirmation_attempts,secondsCountdown,
                                secondsCountdown));
                    }else{
                        mErrorTextView.setText("");
                    }
                }

                @Override
                public void onFinish() {
                    resetAppsLockWrongConfirmAttempts();
                    //mAppLockHelper.resetAppsLockWrongConfirmAttempts();
                    updateStage(Stage.NeedToUnlock);
                    mStopFingerPrintListening = false;
                    refreshLockScreen();
                }
            }.start();
        }
        //End modified by dongchi.chen for XRP10024561 on 20181019

        @Override
        public void createAnimation(Object obj, long delay,
                                    long duration, float translationY, final boolean appearing,
                                    Interpolator interpolator,
                                    final Runnable finishListener) {
            if (obj instanceof LockPatternView.CellState) {
                final LockPatternView.CellState animatedCell = (LockPatternView.CellState) obj;
                mLockPatternView.startCellStateAnimation(animatedCell,
                        1f, appearing ? 1f : 0f, /* alpha */
                        appearing ? translationY : 0f, /* startTranslation */
                        appearing ? 0f : translationY, /* endTranslation */
                        appearing ? 0f : 1f, 1f /* scale */,
                        delay, duration, interpolator, finishListener);
            } else {
                mAppearAnimationUtils.createAnimation((View) obj, delay, duration, translationY,
                        appearing, interpolator, finishListener);
            }
        }

        private final static int MAX_FAILED_ATTEMPTS = 5;

        public void resetAppsLockWrongConfirmAttempts(){
            mNumWrongConfirmAttempts = 0;
        }

        private void setAppsLockWrongConfirmAttempts(){
            Settings.System.putInt(getContext().getContentResolver(), "apps_lock_wr0ng_confirm_attempts", mNumWrongConfirmAttempts);
        }

        public void dispose() {
            setAppsLockWrongConfirmAttempts();
        }

        public int getAppsLockLockoutTimeOut() {
            return mNumWrongConfirmAttempts > MAX_FAILED_ATTEMPTS ? 30 * 1000 : 0;
        }

        private int getAppsLockWrongConfirmAttempts(){
            return Settings.System.getInt(getContext().getContentResolver(), "apps_lock_wr0ng_confirm_attempts", mNumWrongConfirmAttempts);
        }

        public long setAppsLockLockoutAttemptDeadline(int timeoutMs){
            final long deadline = SystemClock.elapsedRealtime() + timeoutMs;
            Settings.System.putLong(getContext().getContentResolver(), "apps_lock_lockout_attempt_deadline", deadline);
            Settings.System.putLong(getContext().getContentResolver(), "apps_lock_lockout_attempt_timeout", timeoutMs);
            return deadline;
        }

        public long getAppsLockLockoutAttemptDeadline() {
            final long deadline = Settings.System.getLong(getContext().getContentResolver(), "apps_lock_lockout_attempt_deadline", 0L);
            final long timeoutMs = Settings.System.getLong(getContext().getContentResolver(), "apps_lock_lockout_attempt_timeout", 0L);
            final long now = SystemClock.elapsedRealtime();
            if (deadline < now || deadline > (now + timeoutMs)) {
                return 0L;
            }
            return deadline;
        }

        //Begin added by dongchi.chen for XRP10024561 on 20181019
        boolean mStopFingerPrintListening = false;
        @Override
        protected void refreshLockScreen() {
            if (!mStopFingerPrintListening && mApplockFpAllowed) {
                mFingerprintHelper.startListening();
            } else {
                if (mFingerprintHelper.isListening()) {
                    mFingerprintHelper.stopListening();
                }
            }
            updateErrorMessage(mLockPatternUtils.getCurrentFailedPasswordAttempts(mEffectiveUserId));
        }
        //End added by dongchi.chen for XRP10024561 on 20181019
    }
}
