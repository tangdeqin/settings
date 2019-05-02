package com.android.settings.privatemode;

import android.text.TextUtils;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.TextViewInputDisabler;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.password.ConfirmDeviceCredentialBaseActivity;
import com.android.settings.password.ConfirmDeviceCredentialBaseFragment;

import com.android.settingslib.animation.AppearAnimationUtils;
import com.android.settingslib.animation.DisappearAnimationUtils;

import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Intent;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.os.storage.StorageManager;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import android.provider.Settings;
import java.util.ArrayList;

import com.android.settings.R;
import com.android.settings.password.ConfirmDeviceCredentialBaseFragment;
import com.android.settings.password.ConfirmDeviceCredentialBaseActivity;
import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;

public class ConfirmPrivacyModeLockPassword extends ConfirmDeviceCredentialBaseActivity {
    final static String TAG = "ConfirmPrivacyModeLockPassword";
    public static class InternalActivity extends ConfirmPrivacyModeLockPassword {
    }

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, ConfirmPrivacyModeLockPasswordFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ConfirmPrivacyModeLockPasswordFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        Fragment fragment = getFragmentManager().findFragmentById(R.id.main_content);
        if (fragment != null && fragment instanceof ConfirmPrivacyModeLockPasswordFragment) {
            ((ConfirmPrivacyModeLockPasswordFragment)fragment).onWindowFocusChanged(hasFocus);
        }
    }

    public static class ConfirmPrivacyModeLockPasswordFragment extends ConfirmDeviceCredentialBaseFragment
            implements OnClickListener, OnEditorActionListener {
        private static final String KEY_NUM_WRONG_CONFIRM_ATTEMPTS
                = "confirm_lock_password_fragment.key_num_wrong_confirm_attempts";

        private static final long ERROR_MESSAGE_TIMEOUT = 3000;
        private TextView mPasswordEntry;
        private TextViewInputDisabler mPasswordEntryInputDisabler;
        private LockPatternUtils mLockPatternUtils;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private TextView mHeaderTextView;
        private TextView mDetailsTextView;
        private int mNumWrongConfirmAttempts;
        private CountDownTimer mCountdownTimer;
        private boolean mIsAlpha;
        private InputMethodManager mImm;
        private boolean mUsingFingerprint = false;
        private AppearAnimationUtils mAppearAnimationUtils;
        private DisappearAnimationUtils mDisappearAnimationUtils;
        private boolean mBlockImm;

        private String mNormalModePassword;
        private String mPrivacyModePassword;
        private TctPrivacyModeHelper mTctPrivacyModeHelper;

        // required constructor for fragments
        public ConfirmPrivacyModeLockPasswordFragment() {

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mLockPatternUtils = new LockPatternUtils(getActivity());
            mTctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(getContext());

            if (savedInstanceState != null) {
                mNumWrongConfirmAttempts = savedInstanceState.getInt(
                        KEY_NUM_WRONG_CONFIRM_ATTEMPTS, 0);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(
                    mEffectiveUserId);
            View view = inflater.inflate(R.layout.confirm_lock_password, null);

            mPasswordEntry = (TextView) view.findViewById(R.id.password_entry);
            mPasswordEntry.setOnEditorActionListener(this);
            mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

            mHeaderTextView = (TextView) view.findViewById(R.id.headerText);
            if (mHeaderTextView == null) {
                mHeaderTextView = view.findViewById(R.id.suw_layout_title);
            }
            mDetailsTextView = (TextView) view.findViewById(R.id.detailsText);
            mErrorTextView = (TextView) view.findViewById(R.id.errorText);
            mIsAlpha = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                    || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality;

            mImm = (InputMethodManager) getActivity().getSystemService(
                    Context.INPUT_METHOD_SERVICE);

            mNormalModePassword = mTctPrivacyModeHelper.getNormalPassword();
            mPrivacyModePassword = mTctPrivacyModeHelper.getPrivacyPassword();
            
            Intent intent = getActivity().getIntent();
            if (intent != null) {
                CharSequence headerMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.HEADER_TEXT);
                CharSequence detailsMessage = intent.getCharSequenceExtra(
                        ConfirmDeviceCredentialBaseFragment.DETAILS_TEXT);
                if (TextUtils.isEmpty(headerMessage)) {
                    headerMessage = getString(getDefaultHeader());
                }
                if (TextUtils.isEmpty(detailsMessage)) {
                    detailsMessage = getString(getDefaultDetails());
                }
                mHeaderTextView.setText(headerMessage);
                mDetailsTextView.setText(detailsMessage);
            }
            int currentType = mPasswordEntry.getInputType();
            mPasswordEntry.setInputType(mIsAlpha ? currentType
                    : (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD));
            mAppearAnimationUtils = new AppearAnimationUtils(getContext(),
                    220, 2f /* translationScale */, 1f /* delayScale*/,
                    AnimationUtils.loadInterpolator(getContext(),
                            android.R.interpolator.linear_out_slow_in));
            mDisappearAnimationUtils = new DisappearAnimationUtils(getContext(),
                    110, 1f /* translationScale */,
                    0.5f /* delayScale */, AnimationUtils.loadInterpolator(
                    getContext(), android.R.interpolator.fast_out_linear_in));
            setAccessibilityTitle(mHeaderTextView.getText());
            return view;
        }

        private int getDefaultHeader() {
            return mIsAlpha ? R.string.confirm_private_password
                    : R.string.confirm_private_password;
        }

        private int getDefaultDetails() {
            return mIsAlpha ? R.string.confirm_private_password_generic
                    : R.string.confirm_private_password_generic;
        }

        private int getErrorMessage() {
            return mIsAlpha ? R.string.lockpassword_invalid_password
                    : R.string.lockpassword_invalid_pin;
        }

        @Override
        public void prepareEnterAnimation() {
            super.prepareEnterAnimation();
            mHeaderTextView.setAlpha(0f);
            mDetailsTextView.setAlpha(0f);
            mCancelButton.setAlpha(0f);
            mPasswordEntry.setAlpha(0f);
            mFingerprintIcon.setAlpha(0f);
            mBlockImm = true;
        }

        private View[] getActiveViews() {
            ArrayList<View> result = new ArrayList<>();
            result.add(mHeaderTextView);
            result.add(mDetailsTextView);
            if (mCancelButton.getVisibility() == View.VISIBLE) {
                result.add(mCancelButton);
            }
            result.add(mPasswordEntry);
            if (mFingerprintIcon.getVisibility() == View.VISIBLE) {
                result.add(mFingerprintIcon);
            }
            return result.toArray(new View[] {});
        }

        @Override
        public void startEnterAnimation() {
            super.startEnterAnimation();
            mAppearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                @Override
                public void run() {
                    mBlockImm = false;
                    long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
                    if (deadline == 0) {
                        resetState();
                    }
                }
            });
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mCountdownTimer != null) {
                mCountdownTimer.cancel();
                mCountdownTimer = null;
            }
            if (mPendingLockCheck != null) {
                mPendingLockCheck.cancel(false);
                mPendingLockCheck = null;
            }
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.CONFIRM_LOCK_PASSWORD;
        }

        @Override
        public void onResume() {
            super.onResume();
            long deadline = mLockPatternUtils.getLockoutAttemptDeadline(mEffectiveUserId);
            if (deadline != 0) {
                handleAttemptLockout(deadline);
            } else {
                resetState();
                mErrorTextView.setText("");
                mNumWrongConfirmAttempts = 0;
            }
        }

        @Override
        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);
            outState.putInt(KEY_NUM_WRONG_CONFIRM_ATTEMPTS, mNumWrongConfirmAttempts);
        }

        @Override
        protected void authenticationSucceeded() {
            startDisappearAnimation(new Intent());
        }

        @Override
        public void onFingerprintIconVisibilityChanged(boolean visible) {
            mUsingFingerprint = visible;
        }

        private void resetState() {
            if (mBlockImm) return;
            mPasswordEntry.setEnabled(true);
            mPasswordEntryInputDisabler.setInputEnabled(true);
            if (shouldAutoShowSoftKeyboard()) {
                mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
            }
        }

        private boolean shouldAutoShowSoftKeyboard() {
            return mPasswordEntry.isEnabled() && !mUsingFingerprint;
        }

        public void onWindowFocusChanged(boolean hasFocus) {
            if (!hasFocus || mBlockImm) {
                return;
            }
            // Post to let window focus logic to finish to allow soft input show/hide properly.
            mPasswordEntry.post(new Runnable() {
                @Override
                public void run() {
                    if (shouldAutoShowSoftKeyboard()) {
                        resetState();
                        return;
                    }

                    mImm.hideSoftInputFromWindow(mPasswordEntry.getWindowToken(),
                            InputMethodManager.HIDE_IMPLICIT_ONLY);
                }
            });
        }

        private void handleNext() {
            /// M: ALPS01849630 {@
            if (getActivity() == null) {
                Log.e(TAG, "error,getActivity() is null");
                return;
            }
            /// @}
            mPasswordEntryInputDisabler.setInputEnabled(false);
            if (mPendingLockCheck != null) {
                mPendingLockCheck.cancel(false);
            }

            final String pin = mPasswordEntry.getText().toString();
            final boolean verifyChallenge = getActivity().getIntent().getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            Intent intent = new Intent();
            if (verifyChallenge)  {
                if (isInternalActivity()) {
                    startVerifyPassword(pin, intent);
                    return;
                }
            } else {
                startCheckPassword(pin, intent);
                return;
            }

            onPasswordChecked(false, intent, 0, mEffectiveUserId);
        }

        private boolean isInternalActivity() {
            return getActivity() instanceof ConfirmPrivacyModeLockPassword.InternalActivity;
        }

        private void startVerifyPassword(final String pin, final Intent intent) {

            if (pin.equals(mPrivacyModePassword)) {
                startVerifyCorrectNormalModePassword(intent);
            }else{
                startVerifyWrongNormalModePassword(intent);
            }
        }

        private void startVerifyCorrectNormalModePassword(final Intent intent){
            startVerifyNormalModePassword(intent, mNormalModePassword);
        }

        private void startVerifyWrongNormalModePassword(final Intent intent){
            startVerifyNormalModePassword(intent, mNormalModePassword+"wrong");
        }

        private void startVerifyNormalModePassword(final Intent intent, final String password){
            long challenge = getActivity().getIntent().getLongExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            final int localEffectiveUserId = mEffectiveUserId;
            mPendingLockCheck = LockPatternChecker.verifyPassword(
                    mLockPatternUtils,
                    password,
                    challenge,
                    localEffectiveUserId,
                    new LockPatternChecker.OnVerifyCallback() {
                        @Override
                        public void onVerified(byte[] token, int timeoutMs) {
                            mPendingLockCheck = null;
                            boolean matched = false;
                            if (token != null) {
                                matched = true;
                                intent.putExtra(
                                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN,
                                        token);
                            }
                            onPasswordChecked(matched, intent, timeoutMs, localEffectiveUserId);
                        }
                    });
        }

        private void startCheckPassword(final String pin, final Intent intent) {
            final int localEffectiveUserId = mEffectiveUserId;
            boolean matched = pin.equals(mPrivacyModePassword);
            if (matched && isInternalActivity()) {
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_TYPE,
                        mIsAlpha ? StorageManager.CRYPT_TYPE_PASSWORD
                                : StorageManager.CRYPT_TYPE_PIN);
                intent.putExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, pin);
            }
            onPasswordChecked(matched, intent, 0, localEffectiveUserId);
        }

        private void startDisappearAnimation(final Intent intent) {
            if (getActivity() == null) {
                Log.e(TAG, "error,getActivity() is null");
                return;
            }

            if (getActivity().getThemeResId() == R.style.Theme_ConfirmDeviceCredentialsDark) {
                mDisappearAnimationUtils.startAnimation(getActiveViews(), new Runnable() {
                    @Override
                    public void run() {
                        getActivity().setResult(RESULT_OK, intent);
                        getActivity().finish();
                        getActivity().overridePendingTransition(
                                R.anim.confirm_credential_close_enter,
                                R.anim.confirm_credential_close_exit);
                    }
                });
            } else {
                getActivity().setResult(RESULT_OK, intent);
                getActivity().finish();
            }
        }

        private void onPasswordChecked(boolean matched, Intent intent, int timeoutMs,
                                       int effectiveUserId) {
            mPasswordEntryInputDisabler.setInputEnabled(true);
            if (matched) {
                startDisappearAnimation(intent);
            } else {
                if (timeoutMs > 0) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(
                            effectiveUserId, timeoutMs);
                    handleAttemptLockout(deadline);
                } else {
                    showError(getErrorMessage(), ERROR_MESSAGE_TIMEOUT);
                }
            }
        }

        @Override
        protected int getLastTryErrorMessage(int userType) {
            switch (userType) {
                case USER_TYPE_PRIMARY:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_device
                            : R.string.lock_last_pin_attempt_before_wipe_device;
                case USER_TYPE_MANAGED_PROFILE:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_profile
                            : R.string.lock_last_pin_attempt_before_wipe_profile;
                case USER_TYPE_SECONDARY:
                    return mIsAlpha ? R.string.lock_last_password_attempt_before_wipe_user
                            : R.string.lock_last_pin_attempt_before_wipe_user;
                default:
                    throw new IllegalArgumentException("Unrecognized user type:" + userType);
            }
        }

        @Override
        protected void onShowError() {
            mPasswordEntry.setText(null);
        }

        private void handleAttemptLockout(long elapsedRealtimeDeadline) {
            long elapsedRealtime = SystemClock.elapsedRealtime();
            mPasswordEntry.setEnabled(false);
            mCountdownTimer = new CountDownTimer(
                    elapsedRealtimeDeadline - elapsedRealtime,
                    LockPatternUtils.FAILED_ATTEMPT_COUNTDOWN_INTERVAL_MS) {

                @Override
                public void onTick(long millisUntilFinished) {
                    if (!isAdded()) return;
                    final int secondsCountdown = (int) (millisUntilFinished / 1000);
                    if(secondsCountdown > 0) {
                        showError(getResources().getQuantityString(
                                R.plurals.lockpattern_too_many_failed_confirmation_attempts,secondsCountdown,
                                secondsCountdown), 0);
                    }else{
                        showError("", 0);
                    }
                }

                @Override
                public void onFinish() {
                    resetState();
                    mErrorTextView.setText("");
                    mNumWrongConfirmAttempts = 0;
                }
            }.start();
        }

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.next_button:
                    handleNext();
                    break;

                case R.id.cancel_button:
                    getActivity().setResult(RESULT_CANCELED);
                    getActivity().finish();
                    break;
            }
        }

        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                handleNext();
                return true;
            }
            return false;
        }
    }
}
