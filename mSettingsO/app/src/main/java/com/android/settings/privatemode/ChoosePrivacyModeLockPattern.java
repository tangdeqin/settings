package com.android.settings.privatemode;

import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.widget.LinearLayoutWithDefaultTouchRecepient;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.LockPatternView.Cell;
import com.android.internal.widget.LockPatternView.DisplayMode;

import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.EncryptionInterstitial;
import com.android.settings.SettingsActivity;
import com.android.settings.notification.RedactionInterstitial;
import com.google.android.collect.Lists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;

import com.android.settings.R;
import com.android.settings.core.InstrumentedPreferenceFragment;

/**
 * If the user has a lock pattern set already, makes them confirm the existing one.
 *
 * Then, prompts the user to choose a lock pattern:
 * - prompts for initial pattern
 * - asks for confirmation / restart
 * - saves chosen password when confirmed
 */
public class ChoosePrivacyModeLockPattern extends SettingsActivity {
    /**
     * Used by the choose lock pattern wizard to indicate the wizard is
     * finished, and each activity in the wizard should finish.
     * <p>
     * Previously, each activity in the wizard would finish itself after
     * starting the next activity. However, this leads to broken 'Back'
     * behavior. So, now an activity does not finish itself until it gets this
     * result.
     */
    static final int RESULT_FINISHED = RESULT_FIRST_USER;

    private static final String TAG = "ChoosePrivacyModeLockPattern";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, getFragmentClass().getName());
        return modIntent;
    }

    public static Intent createIntent(Context context,
                                      boolean requirePassword, boolean confirmCredentials) {
        Intent intent = new Intent(context, ChoosePrivacyModeLockPattern.class);
        intent.putExtra("key_lock_method", "pattern");
        intent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, confirmCredentials);
        intent.putExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, requirePassword);
        return intent;
    }

    public static Intent createIntent(Context context,
                                      boolean requirePassword, long challenge) {
        Intent intent = createIntent(context, requirePassword, false);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        return intent;
    }

    public static Intent createResetIntent(Context context,
                                      boolean requirePassword, long challenge) {
        Intent intent = createIntent(context, requirePassword, false);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);

        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_RESET_PRIVACY_PASSWORD, true);
        return intent;
    }

    public static Intent createIntent(Context context,
                                      boolean requirePassword, String pattern) {
        Intent intent = createIntent(context, requirePassword, false);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, pattern);
        return intent;
    }

    public static Intent createIntent(Context context,
                                      boolean requirePassword, boolean confirmCredentials, int userId) {
        Intent intent = new Intent(context, ChoosePrivacyModeLockPattern.class);
        intent.putExtra("key_lock_method", "pattern");
        intent.putExtra(ChooseLockGeneric.CONFIRM_CREDENTIALS, confirmCredentials);
        intent.putExtra(EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, requirePassword);
        return intent;
    }

    public static Intent createIntent(Context context,
                                      boolean requirePassword, String pattern, int userId) {
        Intent intent = createIntent(context, requirePassword, false, userId);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD, pattern);
        return intent;
    }

    public static Intent createIntent(Context context,
                                      boolean requirePassword, long challenge, int userId) {
        Intent intent = createIntent(context, requirePassword, false, userId);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        return intent;
    }

    public static Intent createIntent(Context context,
                                      boolean requirePassword, boolean hasChallenge, long challenge, int userId, boolean modifyNormalPrivacyPassword) {
        Intent intent = createIntent(context, requirePassword, false, userId);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, hasChallenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHANGE_NORMAL_AND_PRIVACY_PASSWORD, modifyNormalPrivacyPassword);
        return intent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (ChoosePrivacyModeLockPatternFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    /* package */ Class<? extends Fragment> getFragmentClass() {
        return ChoosePrivacyModeLockPatternFragment.class;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CharSequence msg;
        if(getIntent().getBooleanExtra(ChooseLockSettingsHelper.EXTRA_KEY_RESET_PRIVACY_PASSWORD, false)){
            msg = getText(R.string.reset_private_password_title);
        }else{
            msg = getText(R.string.setup_private_mode);
        }
        setTitle(msg);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Return true, so we get notified when items in the menu are clicked.
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // *** TODO ***
        // ChoosePrivacyModeLockPatternFragment.onKeyDown(keyCode, event);
        return super.onKeyDown(keyCode, event);
    }

    public static class ChoosePrivacyModeLockPatternFragment extends InstrumentedPreferenceFragment
            implements View.OnClickListener {

        public static final int CONFIRM_EXISTING_REQUEST = 55;

        // how long after a confirmation message is shown before moving on
        static final int INFORMATION_MSG_TIMEOUT_MS = 3000;

        // how long we wait to clear a wrong pattern
        private static final int WRONG_PATTERN_CLEAR_TIMEOUT_MS = 2000;

        private static final int ID_EMPTY_MESSAGE = -1;

        private static final String FRAGMENT_TAG_SAVE_AND_FINISH = "save_and_finish_worker";

        private String mNormalModePassword;
        private String mCurrentNormalModePassword;
        private String mCurrentPattern;
        private boolean mHasChallenge;
        private long mChallenge;
        protected TextView mHeaderText;
        protected LockPatternView mLockPatternView;
        protected TextView mFooterText;
        private TextView mFooterLeftButton;
        private TextView mFooterRightButton;
        protected List<LockPatternView.Cell> mChosenPattern = null;
        private boolean mHideDrawer = false;
        private boolean mIsChangeNormalAndPrivacyPassword;
        private boolean mDone = false;
        private AsyncTask<?, ?, ?> mPendingLockCheck;
        private TctPrivacyModeHelper mTctPrivacyModeHelper;
        /**
         * The patten used during the help screen to show how to draw a pattern.
         */
        private final List<LockPatternView.Cell> mAnimatePattern =
                Collections.unmodifiableList(Lists.newArrayList(
                        LockPatternView.Cell.of(0, 0),
                        LockPatternView.Cell.of(0, 1),
                        LockPatternView.Cell.of(1, 1),
                        LockPatternView.Cell.of(2, 1)
                ));

        @Override
        public void onActivityResult(int requestCode, int resultCode,
                                     Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            switch (requestCode) {
                case CONFIRM_EXISTING_REQUEST:
                    if (resultCode != Activity.RESULT_OK) {
                        getActivity().setResult(RESULT_FINISHED);
                        getActivity().finish();
                    } else {
                        mCurrentPattern = data.getStringExtra(
                                ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
                    }

                    updateStage(Stage.Introduction);
                    break;
            }
        }

        protected void setRightButtonEnabled(boolean enabled) {
            mFooterRightButton.setEnabled(enabled);
        }

        protected void setRightButtonText(int text) {
            mFooterRightButton.setText(text);
        }

        /**
         * The pattern listener that responds according to a user choosing a new
         * lock pattern.
         */
        protected LockPatternView.OnPatternListener mChooseNewLockPatternListener =
                new LockPatternView.OnPatternListener() {

                    public void onPatternStart() {
                        mLockPatternView.removeCallbacks(mClearPatternRunnable);
                        patternInProgress();
                    }

                    public void onPatternCleared() {
                        mLockPatternView.removeCallbacks(mClearPatternRunnable);
                    }

                    public void onPatternDetected(List<LockPatternView.Cell> pattern) {
                        if (mUiStage == Stage.NeedToConfirm || mUiStage == Stage.ConfirmWrong) {
                            if (mChosenPattern == null) throw new IllegalStateException(
                                    "null chosen pattern in stage 'need to confirm");
                            if (mChosenPattern.equals(pattern)) {
                                updateStage(Stage.ChoiceConfirmed);
                            } else {
                                updateStage(Stage.ConfirmWrong);
                            }
                        } else if (mUiStage == Stage.Introduction || mUiStage == Stage.ChoiceTooShort
                                || mUiStage == Stage.ChoiceNormalPrivacyPasswordSame){
                            if (pattern.size() < LockPatternUtils.MIN_LOCK_PATTERN_SIZE) {
                                updateStage(Stage.ChoiceTooShort);
                            } else {
                                mChosenPattern = new ArrayList<LockPatternView.Cell>(pattern);
                                String p = LockPatternUtils.patternToString(mChosenPattern);
                                if (p.equals(mNormalModePassword)) {
                                    updateStage(Stage.ChoiceNormalPrivacyPasswordSame);
                                } else {
                                    updateStage(Stage.FirstChoiceValid);
                                }
                            }
                        } else {
                            throw new IllegalStateException("Unexpected stage " + mUiStage + " when "
                                    + "entering the pattern.");
                        }
                    }

                    public void onPatternCellAdded(List<Cell> pattern) {

                    }

                private void patternInProgress() {
                    mHeaderText.setText(R.string.lockpattern_recording_inprogress);
                    mFooterText.setText("");
                    mFooterLeftButton.setEnabled(false);
                    mFooterRightButton.setEnabled(false);
                }
         };

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.CHOOSE_LOCK_PATTERN;
        }


        /**
         * The states of the left footer button.
         */
        enum LeftButtonMode {
            Cancel(R.string.cancel, true),
            CancelDisabled(R.string.cancel, false),
            Retry(R.string.lockpattern_retry_button_text, true),
            RetryDisabled(R.string.lockpattern_retry_button_text, false),
            Gone(ID_EMPTY_MESSAGE, false);


            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            LeftButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * The states of the right button.
         */
        enum RightButtonMode {
            Continue(R.string.lockpattern_continue_button_text, true),
            ContinueDisabled(R.string.lockpattern_continue_button_text, false),
            Confirm(R.string.lockpattern_confirm_button_text, true),
            ConfirmDisabled(R.string.lockpattern_confirm_button_text, false),
            Ok(android.R.string.ok, true);

            /**
             * @param text The displayed text for this mode.
             * @param enabled Whether the button should be enabled.
             */
            RightButtonMode(int text, boolean enabled) {
                this.text = text;
                this.enabled = enabled;
            }

            final int text;
            final boolean enabled;
        }

        /**
         * Keep track internally of where the user is in choosing a pattern.
         */
        protected enum Stage {

            Introduction(
                    R.string.lockpattern_recording_intro_header,
                    LeftButtonMode.Cancel, RightButtonMode.ContinueDisabled,
                    ID_EMPTY_MESSAGE, true),
            HelpScreen(
                    R.string.lockpattern_settings_help_how_to_record,
                    LeftButtonMode.Gone, RightButtonMode.Ok, ID_EMPTY_MESSAGE, false),
            ChoiceTooShort(
                    R.string.lockpattern_recording_incorrect_too_short,
                    LeftButtonMode.Retry, RightButtonMode.ContinueDisabled,
                    ID_EMPTY_MESSAGE, true),
            ChoiceNormalPrivacyPasswordSame(
                    R.string.lockpassword_confirm_privacy_mode_match_admin,
                    LeftButtonMode.Retry, RightButtonMode.ContinueDisabled,
                    ID_EMPTY_MESSAGE, true),
            FirstChoiceValid(
                    R.string.lockpattern_pattern_entered_header,
                    LeftButtonMode.Retry, RightButtonMode.Continue, ID_EMPTY_MESSAGE, false),
            NeedToConfirm(
                    R.string.lockpattern_need_to_confirm,
                    LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled,
                    ID_EMPTY_MESSAGE, true),
            ConfirmWrong(
                    R.string.lockpattern_need_to_unlock_wrong,
                    LeftButtonMode.Cancel, RightButtonMode.ConfirmDisabled,
                    ID_EMPTY_MESSAGE, true),
            ChoiceConfirmed(
                    R.string.lockpattern_pattern_confirmed_header,
                    LeftButtonMode.Cancel, RightButtonMode.Confirm, ID_EMPTY_MESSAGE, false);


            /**
             * @param headerMessage The message displayed at the top.
             * @param leftMode The mode of the left button.
             * @param rightMode The mode of the right button.
             * @param footerMessage The footer message.
             * @param patternEnabled Whether the pattern widget is enabled.
             */
            Stage(int headerMessage,
                  LeftButtonMode leftMode,
                  RightButtonMode rightMode,
                  int footerMessage, boolean patternEnabled) {
                this.headerMessage = headerMessage;
                this.leftMode = leftMode;
                this.rightMode = rightMode;
                this.footerMessage = footerMessage;
                this.patternEnabled = patternEnabled;
            }

            final int headerMessage;
            final LeftButtonMode leftMode;
            final RightButtonMode rightMode;
            final int footerMessage;
            final boolean patternEnabled;
        }

        private Stage mUiStage = Stage.Introduction;

        private Runnable mClearPatternRunnable = new Runnable() {
            public void run() {
                mLockPatternView.clearPattern();
            }
        };

        private ChooseLockSettingsHelper mChooseLockSettingsHelper;
        private int mUserId;

        private static final String KEY_UI_STAGE = "uiStage";
        private static final String KEY_PATTERN_CHOICE = "chosenPattern";
        private static final String KEY_CURRENT_PATTERN = "currentPattern";

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            mTctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(getContext());
            mChooseLockSettingsHelper = new ChooseLockSettingsHelper(getActivity());
            if (!(getActivity() instanceof ChoosePrivacyModeLockPattern)) {
                throw new SecurityException("Fragment contained in wrong activity");
            }
            Intent intent = getActivity().getIntent();
            mNormalModePassword = getActivity().getIntent().getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_NORMAL_PASSWORD);
            if (TextUtils.isEmpty(mNormalModePassword)) {
                mNormalModePassword = mTctPrivacyModeHelper.getNormalPassword();
            }
            mCurrentNormalModePassword = getActivity().getIntent().getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_CURRENT_NORMAL_PASSWORD);
            mIsChangeNormalAndPrivacyPassword = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHANGE_NORMAL_AND_PRIVACY_PASSWORD, false);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.choose_privacy_mode_lock_pattern, container, false);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            mHeaderText = (TextView) view.findViewById(R.id.headerText);
            mLockPatternView = (LockPatternView) view.findViewById(R.id.lockPattern);
            mLockPatternView.setOnPatternListener(mChooseNewLockPatternListener);
            mLockPatternView.setTactileFeedbackEnabled(
                    mChooseLockSettingsHelper.utils().isTactileFeedbackEnabled());

            mFooterText = (TextView) view.findViewById(R.id.footerText);

            mFooterLeftButton = (TextView) view.findViewById(R.id.footerLeftButton);
            mFooterRightButton = (TextView) view.findViewById(R.id.footerRightButton);

            mFooterLeftButton.setOnClickListener(this);
            mFooterRightButton.setOnClickListener(this);

            // make it so unhandled touch events within the unlock screen go to the
            // lock pattern view.
            final LinearLayoutWithDefaultTouchRecepient topLayout
                    = (LinearLayoutWithDefaultTouchRecepient) view.findViewById(
                    R.id.topLayout);
            topLayout.setDefaultTouchRecepient(mLockPatternView);

            final boolean confirmCredentials = getActivity().getIntent()
                    .getBooleanExtra("confirm_credentials", true);
            Intent intent = getActivity().getIntent();
            mCurrentPattern = intent.getStringExtra(ChooseLockSettingsHelper.EXTRA_KEY_PASSWORD);
            mHasChallenge = intent.getBooleanExtra(
                    ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, false);
            mChallenge = intent.getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);

            if(intent.getBooleanExtra(mChooseLockSettingsHelper.EXTRA_KEY_RESET_PRIVACY_PASSWORD, false)){
                TextView title = (TextView) view.findViewById(com.android.settings.R.id.subTitleText);
                title.setVisibility(View.VISIBLE);
                title.setText(getString(R.string.reset_private_pattern));
                TextView prompt = (TextView) view.findViewById(com.android.settings.R.id.detailsText);
                prompt.setVisibility(View.VISIBLE);
                prompt.setText(getString(R.string.reset_private_pattern_prompt));
            }
            if (savedInstanceState == null) {
                if (confirmCredentials) {
                    // first launch. As a security measure, we're in NeedToConfirm mode until we
                    // know there isn't an existing password or the user confirms their password.
                    updateStage(Stage.NeedToConfirm);
                    boolean launchedConfirmationActivity =
                        mChooseLockSettingsHelper.launchConfirmationActivity(
                                CONFIRM_EXISTING_REQUEST,
                                getString(R.string.unlock_set_unlock_launch_picker_title), true,
                                mUserId);
                    if (!launchedConfirmationActivity) {
                        updateStage(Stage.Introduction);
                    }
                } else {
                    updateStage(Stage.Introduction);
                }
            } else {
                // restore from previous state
                final String patternString = savedInstanceState.getString(KEY_PATTERN_CHOICE);
                if (patternString != null) {
                    mChosenPattern = LockPatternUtils.stringToPattern(patternString);
                }

                if (mCurrentPattern == null) {
                    mCurrentPattern = savedInstanceState.getString(KEY_CURRENT_PATTERN);
                }
                updateStage(Stage.values()[savedInstanceState.getInt(KEY_UI_STAGE)]);
            }
            mDone = false;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateStage(mUiStage);
        }

        @Override
        public void onPause() {
            super.onPause();
            if (mPendingLockCheck != null) {
                mPendingLockCheck.cancel(false);
                mPendingLockCheck = null;
            }
        }

        protected Intent getRedactionInterstitialIntent(Context context) {
            return RedactionInterstitial.createStartIntent(context, mUserId);
        }

        public void handleLeftButton() {
            if (mUiStage.leftMode == LeftButtonMode.Retry) {
                mChosenPattern = null;
                mLockPatternView.clearPattern();
                updateStage(Stage.Introduction);
            } else if (mUiStage.leftMode == LeftButtonMode.Cancel) {
                getActivity().finish();
            } else {
                throw new IllegalStateException("left footer button pressed, but stage of " +
                        mUiStage + " doesn't make sense");
            }
        }

        public void handleRightButton() {
            if (mUiStage.rightMode == RightButtonMode.Continue) {
                if (mUiStage != Stage.FirstChoiceValid) {
                    throw new IllegalStateException("expected ui stage "
                            + Stage.FirstChoiceValid + " when button is "
                            + RightButtonMode.Continue);
                }
                updateStage(Stage.NeedToConfirm);
            } else if (mUiStage.rightMode == RightButtonMode.Confirm) {
                if (mUiStage != Stage.ChoiceConfirmed) {
                    throw new IllegalStateException("expected ui stage " + Stage.ChoiceConfirmed
                            + " when button is " + RightButtonMode.Confirm);
                }
                startSaveAndFinish();
            } else if (mUiStage.rightMode == RightButtonMode.Ok) {
                if (mUiStage != Stage.HelpScreen) {
                    throw new IllegalStateException("Help screen is only mode with ok button, "
                            + "but stage is " + mUiStage);
                }
                mLockPatternView.clearPattern();
                mLockPatternView.setDisplayMode(DisplayMode.Correct);
                updateStage(Stage.Introduction);
            }
        }

        public void onClick(View v) {
            if (v == mFooterLeftButton) {
                handleLeftButton();
            } else if (v == mFooterRightButton) {
                handleRightButton();
            }
        }

        public boolean onKeyDown(int keyCode, KeyEvent event) {
            if (keyCode == KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0) {
                if (mUiStage == Stage.HelpScreen) {
                    updateStage(Stage.Introduction);
                    return true;
                }
            }
            if (keyCode == KeyEvent.KEYCODE_MENU && mUiStage == Stage.Introduction) {
                updateStage(Stage.HelpScreen);
                return true;
            }
            return false;
        }

        public void onSaveInstanceState(Bundle outState) {
            super.onSaveInstanceState(outState);

            outState.putInt(KEY_UI_STAGE, mUiStage.ordinal());
            if (mChosenPattern != null) {
                outState.putString(KEY_PATTERN_CHOICE,
                        LockPatternUtils.patternToString(mChosenPattern));
            }

            if (mCurrentPattern != null) {
                outState.putString(KEY_CURRENT_PATTERN,
                        mCurrentPattern);
            }
        }

        /**
         * Updates the messages and buttons appropriate to what stage the user
         * is at in choosing a view.  This doesn't handle clearing out the pattern;
         * the pattern is expected to be in the right state.
         * @param stage
         */
        protected void updateStage(Stage stage) {
            final Stage previousStage = mUiStage;

            mUiStage = stage;

            // header text, footer text, visibility and
            // enabled state all known from the stage
            if (stage == Stage.ChoiceTooShort) {
                mHeaderText.setText(
                        getResources().getString(
                                stage.headerMessage,
                                LockPatternUtils.MIN_LOCK_PATTERN_SIZE));
            } else {
                mHeaderText.setText(stage.headerMessage);
            }
            if (stage.footerMessage == ID_EMPTY_MESSAGE) {
                mFooterText.setText("");
            } else {
                mFooterText.setText(stage.footerMessage);
            }

            if (stage.leftMode == LeftButtonMode.Gone) {
                mFooterLeftButton.setVisibility(View.GONE);
            } else {
                mFooterLeftButton.setVisibility(View.VISIBLE);
                mFooterLeftButton.setText(stage.leftMode.text);
                mFooterLeftButton.setEnabled(stage.leftMode.enabled);
            }

            setRightButtonText(stage.rightMode.text);
            setRightButtonEnabled(stage.rightMode.enabled);

            // same for whether the pattern is enabled
            if (stage.patternEnabled) {
                mLockPatternView.enableInput();
            } else {
                mLockPatternView.disableInput();
            }

            // the rest of the stuff varies enough that it is easier just to handle
            // on a case by case basis.
            mLockPatternView.setDisplayMode(DisplayMode.Correct);
            boolean announceAlways = false;

            switch (mUiStage) {
                case Introduction:
                    mLockPatternView.clearPattern();
                    break;
                case HelpScreen:
                    mLockPatternView.setPattern(DisplayMode.Animate, mAnimatePattern);
                    break;
                case ChoiceTooShort:
                case ChoiceNormalPrivacyPasswordSame:
                    mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    postClearPatternRunnable();
                    announceAlways = true;
                    break;
                case FirstChoiceValid:
                    break;
                case NeedToConfirm:
                    mLockPatternView.clearPattern();
                    break;
                case ConfirmWrong:
                    mLockPatternView.setDisplayMode(DisplayMode.Wrong);
                    postClearPatternRunnable();
                    announceAlways = true;
                    break;
                case ChoiceConfirmed:
                    break;
            }

            // If the stage changed, announce the header for accessibility. This
            // is a no-op when accessibility is disabled.
            if (previousStage != stage || announceAlways) {
                mHeaderText.announceForAccessibility(mHeaderText.getText());
            }
        }

        // clear the wrong pattern unless they have started a new one
        // already
        private void postClearPatternRunnable() {
            mLockPatternView.removeCallbacks(mClearPatternRunnable);
            mLockPatternView.postDelayed(mClearPatternRunnable, WRONG_PATTERN_CLEAR_TIMEOUT_MS);
        }

        private void startVerifyPattern(LockPatternUtils utils, final boolean wasSecureBefore) {
            mLockPatternView.disableInput();
            if (mPendingLockCheck != null) {
                mPendingLockCheck.cancel(false);
            }

            mPendingLockCheck = LockPatternChecker.verifyPattern(
                    utils,
                    mChosenPattern,
                    mChallenge,
                    UserHandle.myUserId(),
                    new LockPatternChecker.OnVerifyCallback() {
                        @Override
                        public void onVerified(byte[] token, int timeoutMs) {
                            if (token == null) {
                                Log.e(TAG, "critical: no token returned for known good pattern");
                            }

                            mLockPatternView.enableInput();
                            mPendingLockCheck = null;

                            if (!wasSecureBefore) {
                                Intent intent = getRedactionInterstitialIntent(getActivity());
                                if (intent != null) {
                                    startActivity(intent);
                                }
                            }

                            Intent intent = new Intent();
                            intent.putExtra(
                                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
                            getActivity().setResult(RESULT_FINISHED, intent);
                            doFinish();
                        }
                    });
        }

        private void doFinish() {
            getActivity().finish();
            mDone = true;
        }

        private void startSaveAndFinish() {
            if (mDone) return;
            LockPatternUtils utils = mChooseLockSettingsHelper.utils();
            boolean wasSecureBefore = utils.isSecure(UserHandle.myUserId());

            if (mIsChangeNormalAndPrivacyPassword) {
                final boolean lockVirgin = !utils.isPatternEverChosen(UserHandle.myUserId());
                final boolean required = getActivity().getIntent().getBooleanExtra(
                        EncryptionInterstitial.EXTRA_REQUIRE_PASSWORD, true);

                utils.setCredentialRequiredToDecrypt(required);
                utils.saveLockPattern(LockPatternUtils.stringToPattern(mNormalModePassword), mCurrentNormalModePassword, UserHandle.myUserId());

                if (lockVirgin) {
                    utils.setVisiblePatternEnabled(true, UserHandle.myUserId());
                }
                mTctPrivacyModeHelper.saveNormalPassword(mNormalModePassword);
                mTctPrivacyModeHelper.setPrivacyPassword(LockPatternUtils.patternToString(mChosenPattern),
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);

                if (mHasChallenge) {
                    mChosenPattern = LockPatternUtils.stringToPattern(mNormalModePassword);
                    startVerifyPattern(utils, wasSecureBefore);
                } else {
                    if (!wasSecureBefore) {
                        Intent intent = getRedactionInterstitialIntent(getActivity());
                        if (intent != null) {
                            startActivity(intent);
                        }
                    }
                    getActivity().setResult(RESULT_FINISHED);
                    doFinish();
                }

            }else {
                mTctPrivacyModeHelper.setPrivacyPassword(LockPatternUtils.patternToString(mChosenPattern),
                        DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
                if (mHasChallenge) {
                    mChosenPattern = LockPatternUtils.stringToPattern(mNormalModePassword);
                    startVerifyPattern(utils, wasSecureBefore);
                } else {
                    getActivity().setResult(RESULT_OK);
                    getActivity().finish();
                }

            }
        }
    }
}
