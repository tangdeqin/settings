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
import android.view.View;
import android.widget.Button;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
//Begin added by jinlong.lu for XR6618444 on 18-7-31
import android.provider.Settings;

import com.android.settings.fingerprint.ex.FingerprintUtils;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.tct.sdk.base.fingerprint.FingerprintConstants;
import com.tct.sdk.base.fingerprint.TctFingerprintUtils;
//End added by jinlong.lu for XR6618444 on 18-7-31

/**
 * Activity which concludes fingerprint enrollment.
 */
public class FingerprintEnrollFinish extends FingerprintEnrollBase {

    private static final int REQUEST_ADD_ANOTHER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fingerprint_enroll_finish);
        setHeaderText(R.string.security_settings_fingerprint_enroll_finish_title);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Button addButton = (Button) findViewById(R.id.add_another_button);
        //Begin modified by jinlong.lu for XR6618444 on 18-7-31
        final TctFingerprintUtils fpm =FingerprintUtils.getTctFingerprintUtils(this);
        boolean hideAddAnother = false;
        if (fpm != null) {

            // old int enrolled = fpm.getEnrolledFingerprints(mUserId).size();
            int enrolled = fpm.getTctEnrolledFingerprints(mUserId, FingerprintConstants.FP_TAG_COMMON).size();
            if (enrolled == 1) {
                Settings.System.putInt(getContentResolver(),
                        FingerprintConstants.TCT_FINGERPRINT_UNLOCK_SCREEN, 1);
            }
            //Begin modified by jinlong.lu for XR6873798 on 18-8-28
            int max = getResources().getInteger(
                    R.integer.config_fingerprintMaxTemplatesPerUser);
            //End modified by jinlong.lu for XR6873798 on 18-8-28
            hideAddAnother = enrolled >= max;
            //Begin added by dongchi.chen for XR5904853
            if(0 != getIntent().getIntExtra(
                  FingerprintUtils.EXTRA_KEY_ENROLL_FINGERPRINT_TAG, 0)){
                hideAddAnother = true;
            }
            //End added by dongchi.chen for XR5904853
        }
        if (hideAddAnother) {
            // Don't show "Add" button if too many fingerprints already added
            addButton.setVisibility(View.INVISIBLE);
        } else {
            addButton.setOnClickListener(this);
        }
        //End modified by jinlong.lu for XR6618444 on 18-7-31
    }

    @Override
    protected void onNextButtonClick() {
        //Begin added by jinlong.lu for XR6618444 on 18-7-31
        //setResult(RESULT_FINISHED);
        Intent result = new Intent();
        result.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        setResult(RESULT_FINISHED,result);
        //End added by jinlong.lu for XR6618444 on 18-7-31
        finish();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.add_another_button) {
            startActivityForResult(getEnrollingIntent(), REQUEST_ADD_ANOTHER);
        }
        super.onClick(v);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ADD_ANOTHER && resultCode != RESULT_CANCELED) {
            setResult(resultCode, data);
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.FINGERPRINT_ENROLL_FINISH;
    }
}
