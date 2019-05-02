package com.android.settings.privatemode;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.UserHandle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.TextView;

import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;


public class SetupScreenLockFinish extends SettingsPreferenceFragment {
    private static final int SET_PRIVACY_PASSWORD_REQUEST = 700;
    private static final int MIN_PASSWORD_LENGTH = 4;
    private DevicePolicyManager mDPM;

    public static Intent createFragmentIntent(Context context){
        return Utils.onBuildStartFragmentIntent(context, SetupScreenLockFinish.class.getName(),
                null, null, R.string.private_mode, null, false, MetricsEvent.SECURITY);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.setup_privacy_mode, contentRoot, true);
        setEmptyView(emptyView);

        Button btn = (Button) emptyView.findViewById(R.id.continueBtn);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                launchSetPrivacyModeLock();
            }
        });

        mDPM = (DevicePolicyManager) getContext().getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SECURITY;
    }

    private int getPasswordQualityForCurrent() {
        LockPatternUtils lpu = new LockPatternUtils(getContext());
        if (lpu.isLockScreenDisabled(UserHandle.myUserId())) {
            return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
        }
        switch (lpu.getKeyguardStoredPasswordQuality(UserHandle.myUserId())) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                return DevicePolicyManager.PASSWORD_QUALITY_SOMETHING;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                return DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX;
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                return DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC;
            default:
                return DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
        }
    }

    private void launchSetPrivacyModeLock() {
        int quality = getPasswordQualityForCurrent();
        Intent intent = new Intent();
        switch (quality) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                intent = ChoosePrivacyModeLockPattern.createIntent(getContext(), false, false, 0);
                startActivityForResult(intent, SET_PRIVACY_PASSWORD_REQUEST);
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                int minLength = mDPM.getPasswordMinimumLength(null);
                if (minLength < MIN_PASSWORD_LENGTH) {
                    minLength = MIN_PASSWORD_LENGTH;
                }
                final int maxLength = mDPM.getPasswordMaximumLength(quality);
                intent = ChoosePrivacyModeLockPassword.createIntent(getContext(), quality, minLength,
                        maxLength, false, false);
                startActivityForResult(intent, SET_PRIVACY_PASSWORD_REQUEST);
                break;
            default:
                getActivity().finish();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SET_PRIVACY_PASSWORD_REQUEST) {
            if (resultCode == Activity.RESULT_OK)
                getActivity().setResult(Activity.RESULT_OK);
        }
        getActivity().finish();
    }
}

