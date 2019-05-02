package com.android.settings.applock;

import android.annotation.Nullable;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.SwitchPreference;
import android.view.MenuItem;
import com.android.settings.R;
import com.tct.sdk.base.applock.TctAppLockHelper;

import android.hardware.fingerprint.FingerprintManager;
import android.content.Context;


public class AppsLockSettingsActivity extends PreferenceActivity {
    private static final String RESET_PASSWORLD = "reset_password";
    private static final String FINGERPRINT_UNLOCK_ENABLE = "fingerprint_unlock_enable";

    private static final int CHOOSE_APPS_LOCK_REQUEST = 100;
    private TctAppLockHelper mAppLockHelper;

    private boolean isDisplayFingerPrint(){
        boolean displayFingerPrintFlag = false;
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        displayFingerPrintFlag = fingerprintManager != null && fingerprintManager.isHardwareDetected(); //modified by dongchi.chen for XRP10025056 on 20181030
        return displayFingerPrintFlag;
     }
	
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAppLockHelper = TctAppLockHelper.createHelper(this);
        this.addPreferencesFromResource(R.xml.apps_lock_settings);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        Preference resetPassword= (Preference) findPreference(RESET_PASSWORLD);
        if(null != resetPassword){
            resetPassword.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = ChooseAppsLockPattern.createResetIntent(getApplicationContext(), false);
                    startActivityForResult(intent, CHOOSE_APPS_LOCK_REQUEST);
                    return true;
                }
            });
        }
	
        SwitchPreference fingerprintUnlockEnable = (SwitchPreference) findPreference(FINGERPRINT_UNLOCK_ENABLE);
        boolean isDisplayFingerPrint = isDisplayFingerPrint();
        if(true == isDisplayFingerPrint){
            fingerprintUnlockEnable.setChecked(mAppLockHelper.isFingerprintUnlockAppsLockEnable());
            fingerprintUnlockEnable.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    mAppLockHelper.setFingerprintUnlockAppsLockEnable((Boolean) newValue);
                    return true;
                }
            });
            }
        else{
            getPreferenceScreen().removePreference(fingerprintUnlockEnable);
        }

        mHomeNRecentKeyListener = new HomeNRecentKeyListener().register(this); //added by dongchi.chen for XRP10025585 on 20181106
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case CHOOSE_APPS_LOCK_REQUEST:
                if(Activity.RESULT_OK == resultCode){
                    finish();
                }
                break;
            default:
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
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
    protected void onResume() {
        super.onResume();
        //Begin modified by dongchi.chen for XRP10025056 on 20181030
        if(isDisplayFingerPrint()){
            FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
            if(null != fingerprintManager && !fingerprintManager.hasEnrolledFingerprints()){
                SwitchPreference fingerprintUnlockEnable = (SwitchPreference) findPreference(FINGERPRINT_UNLOCK_ENABLE);
                fingerprintUnlockEnable.setEnabled(false);
            }
        }
        //End modified by dongchi.chen for XRP10025056 on 20181030

    }

    //Begin added by dongchi.chen for XRP10025585 on 20181106
    private HomeNRecentKeyListener mHomeNRecentKeyListener;
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mHomeNRecentKeyListener) {
            mHomeNRecentKeyListener.unregister();
        }
    }
    //End added by dongchi.chen for XRP10025585 on 20181106
}
