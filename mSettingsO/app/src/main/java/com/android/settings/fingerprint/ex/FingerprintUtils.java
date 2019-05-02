package com.android.settings.fingerprint.ex;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;

import com.tct.sdk.base.fingerprint.TctFingerprintUtils;

/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Created by jinlong.lu on 18-3-22.
 */

public class FingerprintUtils {

    public static final String APPS_LOCK_ON = "apps_lock_on";
    public static final String PRIVACY_MODE_ON = "privacy_mode_on";
    /**
     * Whether fingerprint unlock apps lock
     *
     */
    public static final String APPS_LOCK_FINGERPRINT_UNLOCK = "apps_lock_fingerprint_unlock";
    /**Fingerprint enroll tag*/
    public static final String EXTRA_KEY_ENROLL_FINGERPRINT_TAG = "enroll_fingerprint_tag";
    /**Control fingerprint page show skip button.*/
    public static final String EXTRA_KEY_SHOW_SKIP_BUTTON = "show_skip_button";
    /**Is it from private mode settings*/
    public static final String EXTRA_KEY_FROM_PRIVATE_MODE_SETTINGS = "from_private_mode_settings";
    /* fingerprint unlock app data string*/
    public static final String CONFIRM_SETTINGS_LOCK_PATTERN = "confirm_settings_lock_pattern";
    //Begin added by jinlong.lu for XR7099324 on 18-11-6
    public static final String APPS_LOCK_PASSWORD = "apps_lock_password";
    //End added by jinlong.lu for XR7099324 on 18-11-6
    public static TctFingerprintUtils getTctFingerprintUtils(Context context) {

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return new TctFingerprintUtils(context);
        } else {
            return null;
        }
    }

}
