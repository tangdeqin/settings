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
 * limitations under the License
 */

package com.android.settings.fingerprint;

import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settingslib.HelpUtils;
import com.android.settingslib.RestrictedLockUtils;
import com.android.setupwizardlib.span.LinkSpan;
//Begin added by jinlong.lu for XR6618444 on 18-8-1
import android.content.Context;
import com.tct.sdk.base.fingerprint.FingerprintConstants;
import com.tct.sdk.base.fingerprint.TctFingerprintUtils;
import com.android.settings.fingerprint.ex.FingerprintUtils;
//End added by jinlong.lu for XR6618444 on 18-8-1

/**
 * Onboarding activity for fingerprint enrollment.
 */
public class FingerprintEnrollIntroduction extends FingerprintEnrollBase
        implements View.OnClickListener, LinkSpan.OnClickListener {

    private static final String TAG = "FingerprintIntro";

    protected static final int CHOOSE_LOCK_GENERIC_REQUEST = 1;
    protected static final int FINGERPRINT_FIND_SENSOR_REQUEST = 2;
    protected static final int LEARN_MORE_REQUEST = 3;

    private UserManager mUserManager;
    private boolean mHasPassword;
    private boolean mFingerprintUnlockDisabledByAdmin;
    private TextView mErrorText;
    //Begin added by jinlong.lu for XR6618444 on 18-7-31
    private static final int FINGERPRINT_ENROLL_INTRODUCTION_ERGO_REQUEST = 103;
    private static final int CONFIRM_REQUEST = 101;
    //End added by jinlong.lu for XR6618444 on 18-7-31

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFingerprintUnlockDisabledByAdmin = RestrictedLockUtils.checkIfKeyguardFeaturesDisabled(
                this, DevicePolicyManager.KEYGUARD_DISABLE_FINGERPRINT, mUserId) != null;

        setContentView(R.layout.fingerprint_enroll_introduction);
        if (mFingerprintUnlockDisabledByAdmin) {
            setHeaderText(R.string
                    .security_settings_fingerprint_enroll_introduction_title_unlock_disabled);
        } else {
            setHeaderText(R.string.security_settings_fingerprint_enroll_introduction_title);
        }

        Button cancelButton = (Button) findViewById(R.id.fingerprint_cancel_button);
        cancelButton.setOnClickListener(this);

        mErrorText = (TextView) findViewById(R.id.error_text);

        mUserManager = UserManager.get(this);
        updatePasswordQuality();
    }

    @Override
    protected void onResume() {
        super.onResume();

        final FingerprintManager fingerprintManager = Utils.getFingerprintManagerOrNull(this);
        int errorMsg = 0;
        if (fingerprintManager != null) {
            //Begin modified by jinlong.lu for XR6873798 on 18-8-28
            final int max = getResources().getInteger(
                    R.integer.config_fingerprintMaxTemplatesPerUser);
            //End modified by jinlong.lu for XR6873798 on 18-8-28
            final int numEnrolledFingerprints =
                    fingerprintManager.getEnrolledFingerprints(mUserId).size();
            if (numEnrolledFingerprints >= max) {
                errorMsg = R.string.fingerprint_intro_error_max;
            }
        } else {
            errorMsg = R.string.fingerprint_intro_error_unknown;
        }
        if (errorMsg == 0) {
            mErrorText.setText(null);
            getNextButton().setVisibility(View.VISIBLE);
        } else {
            mErrorText.setText(errorMsg);
            getNextButton().setVisibility(View.GONE);
        }
    }

    private void updatePasswordQuality() {
        final int passwordQuality = new ChooseLockSettingsHelper(this).utils()
                .getActivePasswordQuality(mUserManager.getCredentialOwnerProfile(mUserId));
        mHasPassword = passwordQuality != DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    @Override
    protected Button getNextButton() {
        return (Button) findViewById(R.id.fingerprint_next_button);
    }

    @Override
    protected void onNextButtonClick() {
        if (!mHasPassword) {
            // No fingerprints registered, launch into enrollment wizard.
            launchChooseLock();
        } else {
            // Lock thingy is already set up, launch directly into find sensor step from wizard.
            //launchFindSensor(null);
            //Begin modified by dongchi.chen for XR6140582
            //long challenge = getSystemService(FingerprintManager.class).preEnroll();
            long challenge = 0;
            boolean fromPrivateMode = getIntent().getBooleanExtra(FingerprintUtils.EXTRA_KEY_FROM_PRIVATE_MODE_SETTINGS, false);
            if (fromPrivateMode) {
                challenge = getIntent().getLongExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, 0);
            }else {
                challenge = getSystemService(FingerprintManager.class).preEnroll();
            }
            //Begin modified by dongchi.chen for XR6140582
            ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
            helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge, mUserId);
        }
    }
   //Begin added by jinlong.lu for XR6618444 on 18-7-31
    private void launchFingerprintEnroll(byte[] token) {
        TctFingerprintUtils fpm =FingerprintUtils.getTctFingerprintUtils(this);
        if (fpm != null) {
            int enrolled = fpm.getTctEnrolledFingerprints(mUserId, FingerprintConstants.FP_TAG_COMMON).size();
            //Begin modified by jinlong.lu for XR6873798 on 18-8-28
            int max = getResources().getInteger(
                    R.integer.config_fingerprintMaxTemplatesPerUser);
            //End modified by jinlong.lu for XR6873798 on 18-8-28
            if (enrolled >= max) {
                Intent intent =new Intent();
                intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
                setResult(RESULT_FINISHED,intent);
                finish();
                return;
            }
        }
        Intent intent = getEnrollingIntent();
        if (token != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        }
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivityForResult(intent, FINGERPRINT_FIND_SENSOR_REQUEST);
    }
   //End added by jinlong.lu for XR6618444 on 18-7-31
    private void launchChooseLock() {
        Intent intent = getChooseLockIntent();
        long challenge = Utils.getFingerprintManagerOrNull(this).preEnroll();
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.MINIMUM_QUALITY_KEY,
                DevicePolicyManager.PASSWORD_QUALITY_SOMETHING);
        intent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_DISABLED_PREFS, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_HAS_CHALLENGE, true);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE, challenge);
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_FOR_FINGERPRINT, true);
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivityForResult(intent, CHOOSE_LOCK_GENERIC_REQUEST);
    }

    private void launchFindSensor(byte[] token) {
        Intent intent = getFindSensorIntent();
        if (token != null) {
            intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, token);
        }
        if (mUserId != UserHandle.USER_NULL) {
            intent.putExtra(Intent.EXTRA_USER_ID, mUserId);
        }
        startActivityForResult(intent, FINGERPRINT_FIND_SENSOR_REQUEST);
    }

    protected Intent getChooseLockIntent() {
        return new Intent(this, ChooseLockGeneric.class);
    }

    protected Intent getFindSensorIntent() {
        return new Intent(this, FingerprintEnrollFindSensor.class);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final boolean isResultFinished = resultCode == RESULT_FINISHED;
        if (requestCode == FINGERPRINT_FIND_SENSOR_REQUEST) {
            if (isResultFinished || resultCode == RESULT_SKIP) {
                final int result = isResultFinished ? RESULT_OK : RESULT_SKIP;
                setResult(result, data);
                finish();
                return;
            }
            //Begin modified by jinlong.lu for XR6618444 on 18-7-31
        } else if (requestCode == CHOOSE_LOCK_GENERIC_REQUEST) {
            if (isResultFinished) {
                updatePasswordQuality();
                byte[] token = data.getByteArrayExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                //launchFindSensor(token);
                launchFingerprintEnroll(token);
                //End modified by jinlong.lu for XR6618444 on 18-7-31
                return;
            }
        } else if (requestCode == LEARN_MORE_REQUEST) {
            overridePendingTransition(R.anim.suw_slide_back_in, R.anim.suw_slide_back_out);
        }
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        else  if (requestCode == FINGERPRINT_ENROLL_INTRODUCTION_ERGO_REQUEST) {
            if (isResultFinished) {
                updatePasswordQuality();
                byte[] token = data.getByteArrayExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                launchFingerprintEnroll(token);
            }
        }else if(requestCode == CONFIRM_REQUEST){
            //add if
            if (resultCode == RESULT_OK){
                updatePasswordQuality();
                byte[] token = data.getByteArrayExtra(
                        ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                launchFingerprintEnroll(token);
                return;
            }

        }
        //End added by jinlong.lu for XR6618444 on 18-7-31
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.fingerprint_cancel_button) {
            onCancelButtonClick();
        } else {
            super.onClick(v);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_INTRO;
    }

    protected void onCancelButtonClick() {
        finish();
    }

    @Override
    protected void initViews() {
        super.initViews();

        TextView description = (TextView) findViewById(R.id.description_text);
        if (mFingerprintUnlockDisabledByAdmin) {
            description.setText(R.string
                    .security_settings_fingerprint_enroll_introduction_message_unlock_disabled);
        }
    }

    @Override
    public void onClick(LinkSpan span) {
        if ("url".equals(span.getId())) {
            String url = getString(R.string.help_url_fingerprint);
            Intent intent = HelpUtils.getHelpIntent(this, url, getClass().getName());
            if (intent == null) {
                Log.w(TAG, "Null help intent.");
                return;
            }
            try {
                // This needs to be startActivityForResult even though we do not care about the
                // actual result because the help app needs to know about who invoked it.
                startActivityForResult(intent, LEARN_MORE_REQUEST);
            } catch (ActivityNotFoundException e) {
                Log.w(TAG, "Activity was not found for intent, " + e);
            }
        }
    }
}
