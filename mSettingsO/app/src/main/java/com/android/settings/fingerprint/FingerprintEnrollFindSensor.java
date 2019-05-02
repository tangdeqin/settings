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

import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fingerprint.FingerprintEnrollSidecar.Listener;
import com.android.settings.password.ChooseLockSettingsHelper;
//Begin added by jinlong.lu for XR6618444 on 18-7-31
import android.content.SharedPreferences;
import android.content.Context;
//End added by jinlong.lu for XR6618444 on 18-7-31

/**
 * Activity explaining the fingerprint sensor location for fingerprint enrollment.
 */
public class FingerprintEnrollFindSensor extends FingerprintEnrollBase {

    private static final int CONFIRM_REQUEST = 1;
    private static final int ENROLLING = 2;
    public static final String EXTRA_KEY_LAUNCHED_CONFIRM = "launched_confirm_lock";

    @Nullable
    private FingerprintFindSensorAnimation mAnimation;
    private boolean mLaunchedConfirmLock;
    private FingerprintEnrollSidecar mSidecar;
    private boolean mNextClicked;
    //Begin added by jinlong.lu for XR6618444 on 18-7-31
    private SharedPreferences sp;
    public static final String FINGERPRINT_FIND_SENSOR ="is_fingerprint_find_sensor";
    //End added by jinlong.lu for XR6618444 on 18-7-31
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentView());
        Button skipButton = findViewById(R.id.skip_button);
        skipButton.setOnClickListener(this);

        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        Button gotItButton =findViewById(R.id.got_it_button);
        gotItButton.setOnClickListener(this);
        //End added by jinlong.lu for XR6618444 on 18-7-31

        setHeaderText(R.string.security_settings_fingerprint_enroll_find_sensor_title);
        if (savedInstanceState != null) {
            mLaunchedConfirmLock = savedInstanceState.getBoolean(EXTRA_KEY_LAUNCHED_CONFIRM);
            mToken = savedInstanceState.getByteArray(
                    ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
        }
        if (mToken == null && !mLaunchedConfirmLock) {
            launchConfirmLock();
        } else if (mToken != null) {
            startLookingForFingerprint(); // already confirmed, so start looking for fingerprint
        }
        View animationView = findViewById(R.id.fingerprint_sensor_location_animation);
        if (animationView instanceof FingerprintFindSensorAnimation) {
            mAnimation = (FingerprintFindSensorAnimation) animationView;
        } else {
            mAnimation = null;
        }
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        sp = getSharedPreferences(FINGERPRINT_FIND_SENSOR, Context.MODE_PRIVATE);
        if(sp!=null){
            SharedPreferences.Editor ed = sp.edit();
            ed.putInt(FINGERPRINT_FIND_SENSOR, 1);
            ed.apply();
        }
        //End added by jinlong.lu for XR6618444 on 18-7-31
    }

    protected int getContentView() {
        return R.layout.fingerprint_enroll_find_sensor;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mAnimation != null) {
            mAnimation.startAnimation();
        }
    }

    private void startLookingForFingerprint() {
        mSidecar = (FingerprintEnrollSidecar) getFragmentManager().findFragmentByTag(
                FingerprintEnrollEnrolling.TAG_SIDECAR);
        if (mSidecar == null) {
            mSidecar = new FingerprintEnrollSidecar();
            getFragmentManager().beginTransaction()
                    .add(mSidecar, FingerprintEnrollEnrolling.TAG_SIDECAR).commit();
        }
        mSidecar.setListener(new Listener() {
            @Override
            public void onEnrollmentProgressChange(int steps, int remaining) {
                mNextClicked = true;
                proceedToEnrolling(true /* cancelEnrollment */);
            }

            @Override
            public void onEnrollmentHelp(CharSequence helpString) {
            }

            @Override
            public void onEnrollmentError(int errMsgId, CharSequence errString) {
                if (mNextClicked && errMsgId == FingerprintManager.FINGERPRINT_ERROR_CANCELED) {
                    mNextClicked = false;
                    proceedToEnrolling(false /* cancelEnrollment */);
                }
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAnimation != null) {
            mAnimation.pauseAnimation();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAnimation != null) {
            mAnimation.stopAnimation();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(EXTRA_KEY_LAUNCHED_CONFIRM, mLaunchedConfirmLock);
        outState.putByteArray(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.skip_button:
                onSkipButtonClick();
                break;
            //Begin added by jinlong.lu for XR6618444 on 18-7-31
            case R.id.got_it_button:
                proceedToEnrolling(false);
                break;
            //End added by jinlong.lu for XR6618444 on 18-7-31
            default:
                super.onClick(v);
        }
    }

    protected void onSkipButtonClick() {
        setResult(RESULT_SKIP);
        finish();
    }

    private void proceedToEnrolling(boolean cancelEnrollment) {
        if (mSidecar != null) {
            if (cancelEnrollment) {
                if (mSidecar.cancelEnrollment()) {
                    // Enrollment cancel requested. When the cancellation is successful,
                    // onEnrollmentError will be called with FINGERPRINT_ERROR_CANCELED, calling
                    // this again.
                    return;
                }
            }
            getFragmentManager().beginTransaction().remove(mSidecar).commitAllowingStateLoss();
            mSidecar = null;
            startActivityForResult(getEnrollingIntent(), ENROLLING);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONFIRM_REQUEST) {
            if (resultCode == RESULT_OK) {
                mToken = data.getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
                overridePendingTransition(R.anim.suw_slide_next_in, R.anim.suw_slide_next_out);
                getIntent().putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
                startLookingForFingerprint();
            } else {
                finish();
            }
        } else if (requestCode == ENROLLING) {
            if (resultCode == RESULT_FINISHED) {
                //Begin modified by jinlong.lu for XR6618444 on 18-7-31
                setResult(RESULT_FINISHED,data);
                //End modified by jinlong.lu for XR6618444 on 18-7-31
                finish();
            } else if (resultCode == RESULT_SKIP) {
                setResult(RESULT_SKIP);
                finish();
            } else if (resultCode == RESULT_TIMEOUT) {
                setResult(RESULT_TIMEOUT);
                finish();
            } else {
                FingerprintManager fpm = Utils.getFingerprintManagerOrNull(this);
                int enrolled = fpm.getEnrolledFingerprints().size();
                //Begin modified by jinlong.lu for XR6873798 on 18-8-28
                int max = getResources().getInteger(
                        R.integer.config_fingerprintMaxTemplatesPerUser);
                //End modified by jinlong.lu for XR6873798 on 18-8-28
                if (enrolled >= max) {
                    finish();
                } else {
                    // We came back from enrolling but it wasn't completed, start again.
                    startLookingForFingerprint();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void launchConfirmLock() {
        long challenge = Utils.getFingerprintManagerOrNull(this).preEnroll();
        ChooseLockSettingsHelper helper = new ChooseLockSettingsHelper(this);
        boolean launchedConfirmationActivity = false;
        if (mUserId == UserHandle.USER_NULL) {
            launchedConfirmationActivity = helper.launchConfirmationActivity(CONFIRM_REQUEST,
                getString(R.string.security_settings_fingerprint_preference_title),
                null, null, challenge);
        } else {
            launchedConfirmationActivity = helper.launchConfirmationActivity(CONFIRM_REQUEST,
                    getString(R.string.security_settings_fingerprint_preference_title),
                    null, null, challenge, mUserId);
        }
        if (!launchedConfirmationActivity) {
            // This shouldn't happen, as we should only end up at this step if a lock thingy is
            // already set.
            finish();
        } else {
            mLaunchedConfirmLock = true;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_FIND_SENSOR;
    }
}
