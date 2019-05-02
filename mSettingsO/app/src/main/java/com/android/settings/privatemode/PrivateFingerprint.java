package com.android.settings.privatemode;

import com.android.settings.fingerprint.FingerprintEnrollEnrolling;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.settings.password.ChooseLockSettingsHelper;

public class PrivateFingerprint extends Activity implements OnClickListener {
    private static final int ADD_PRIVACY_FINGERPRINT_REQUEST = 616;
    private byte[] mToken;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(com.android.settings.R.layout.private_fingerprint);

        Button skipBtn = (Button) findViewById(com.android.settings.R.id.skipBtn);
        skipBtn.setOnClickListener(this);

        Button continueBtn = (Button) findViewById(com.android.settings.R.id.continueBtn);
        continueBtn.setOnClickListener(this);

        mToken = getIntent().getByteArrayExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case com.android.settings.R.id.skipBtn:
                setResult(RESULT_CANCELED);
                finish();
                break;
            case com.android.settings.R.id.continueBtn:
                addPrivacyFingerprint();
                break;
            default:
                break;
        }
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_PRIVACY_FINGERPRINT_REQUEST && (resultCode == RESULT_FIRST_USER || resultCode == RESULT_OK)) {
            setResult(RESULT_OK);
            finish();
        }
    }

    private void addPrivacyFingerprint() {
        Intent intent = new Intent();
        intent.setClassName("com.android.settings",FingerprintEnrollEnrolling.class.getName());
        intent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, mToken);
        startActivityForResult(intent, ADD_PRIVACY_FINGERPRINT_REQUEST);
    }
}
