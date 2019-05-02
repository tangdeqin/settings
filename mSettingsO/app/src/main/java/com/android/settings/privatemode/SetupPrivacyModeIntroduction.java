package com.android.settings.privatemode;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.android.internal.widget.LockPatternUtils;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

import com.android.settings.R;

import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;
import android.os.UserManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class SetupPrivacyModeIntroduction extends SettingsPreferenceFragment {
    private SwitchBar mSwitchBar;
    protected ToggleSwitch mToggleSwitch;
    private static final String APK_FILEMANAGER = "com.jrdcom.filemanager";
    private static final String APK_GALLERY = "com.tcl.gallery";
    private static final String PRIVACY_MODE_ON = "privacy_mode_on";

    //Begin added by dongchi.chen for XRP23629 on 20180915
    private static final String APK_MESSAGE = "com.android.mms";
    private static final String APK_CONTACTS = "com.tct.contacts";
    //End added by dongchi.chen for XRP23629 on 20180915

    private ContentObserver mPrivacyModeObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (Settings.System.getInt(getContentResolver(), PRIVACY_MODE_ON, 0) == 1) {
                getActivity().finish();
            }
        }
    };

    public static Intent createFragmentIntent(Context context){
        return Utils.onBuildStartFragmentIntent(context, SetupPrivacyModeIntroduction.class.getName(),
                null, null, R.string.private_mode, null, false, MetricsEvent.SECURITY);
    }

    // required constructor for fragments
    public SetupPrivacyModeIntroduction() {
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mToggleSwitch = mSwitchBar.getSwitch();
        mSwitchBar.setChecked(false);
        mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                mSwitchBar.setCheckedInternal(false);
                startActivity(PrivacyModeSettings.createFragmentIntent(getContext()));
                return true;
            }
        });
        mSwitchBar.show();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.privacy_mode_help, contentRoot, true);
        setEmptyView(emptyView);
        resetPrivateModeIntroIfNoFingerPrintHardware(emptyView);
        if(!isPkgInstalled(getContext(), APK_FILEMANAGER)){
            ImageView v = (ImageView)emptyView.findViewById(R.id.filemanager_icon);
            if(null != v){
                v.setVisibility(View.GONE);
            }
        }
        if(!isPkgInstalled(getContext(), APK_GALLERY)){
            ImageView v = (ImageView)emptyView.findViewById(R.id.gallery_icon);
            if(null != v){
                v.setVisibility(View.GONE);
            }
        }
        //Begin added by dongchi.chen for XRP23629 on 20180915
        if(!isPkgInstalled(getContext(), APK_MESSAGE)
                || !isPkgInstalled(getContext(), APK_CONTACTS)){
            ImageView v = (ImageView)emptyView.findViewById(R.id.call_icon);
            if(null != v){
                v.setVisibility(View.GONE);
            }
            v = (ImageView)emptyView.findViewById(R.id.contacts_icon);
            if(null != v){
                v.setVisibility(View.GONE);
            }
        }
        //End added by dongchi.chen for XRP23629 on 20180915
        this.getContentResolver().registerContentObserver(Settings.System.getUriFor(
                PRIVACY_MODE_ON), true, mPrivacyModeObserver);
    }

    private void resetPrivateModeIntroIfNoFingerPrintHardware(View parent){
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        if (null == fingerprintManager ? true : !fingerprintManager.isHardwareDetected()) { //modified by dongchi.chen for XR5987286
            TextView privateModeIntro = (TextView)parent.findViewById(R.id.private_mode_intro);
            privateModeIntro.setText(R.string.private_mode_intro_nofingerprint);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mToggleSwitch.setOnBeforeCheckedChangeListener(null);
        mSwitchBar.hide();
        try {
            this.getContentResolver().unregisterContentObserver(mPrivacyModeObserver);
        } catch(Exception e) {
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SECURITY;
    }


    public static final String MY_PREF_FILE = "MY_PERFS";
    public static final String KEY_SWITCH_TO_PRIVATE_MODE_SHOW_AGAIN = "SwitchToPrivateModeShowAgain";
    public static void enterPrivateModeSettings(final Context context) {
        TctPrivacyModeHelper tctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(context);
        if (!tctPrivacyModeHelper.isPrivacyModeEnable()) {
            context.startActivity(SetupPrivacyModeIntroduction.createFragmentIntent(context));
        } else if(isPasswordQualityUnspecified(context)){
            showSetUpScreenLockDialog(context);
        } else if (tctPrivacyModeHelper.isInPrivacyMode()) {
            context.startActivity(PrivacyModeSettings.createFragmentIntent(context));
        } else {
            final SharedPreferences sharedpreferences = context.getSharedPreferences(MY_PREF_FILE, Context.MODE_PRIVATE);
            boolean showAgain = sharedpreferences.getBoolean(KEY_SWITCH_TO_PRIVATE_MODE_SHOW_AGAIN, true);
            if (showAgain) {
                LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                View showAgainView = inflater.inflate(R.layout.not_show_again, null);
                final CheckBox notShowAgainCheckbox = (CheckBox)showAgainView.findViewById(R.id.check);
                AlertDialog alertDialog = new AlertDialog.Builder(context)
                        .setCancelable(false)
                        .setView(showAgainView)
                        .setTitle(R.string.switch_to_private_mode)
                        .setMessage(R.string.switch_to_private_msg)
                        .setPositiveButton(R.string.dlg_switch, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putBoolean(KEY_SWITCH_TO_PRIVATE_MODE_SHOW_AGAIN,
                                        !notShowAgainCheckbox.isChecked());
                                editor.commit();
                                context.startActivity(PrivacyModeSettings.createFragmentIntent(context));
                            }
                        })
                        .setNegativeButton(R.string.dlg_cancel, null)
                        .show();
                ((TextView)alertDialog.findViewById(android.R.id.message)).setTextColor(0x8a000000);
            } else {
                context.startActivity(PrivacyModeSettings.createFragmentIntent(context));
            }
        }
    }

    private static boolean isPasswordQualityUnspecified(Context context){
        LockPatternUtils utils = new LockPatternUtils(context);
        int quality = utils.getKeyguardStoredPasswordQuality(UserHandle.myUserId());
        return quality == DevicePolicyManager.PASSWORD_QUALITY_UNSPECIFIED;
    }

    private static void showSetUpScreenLockDialog(final Context context){
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(R.string.set_up_screen_lock_method)
                .setMessage(R.string.set_up_screen_lock_method_msg)
                .setPositiveButton(R.string.dlg_ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        context.startActivity(PrivacyModeSettings.createFragmentIntent(context));
                    }
                })
                .setNegativeButton(R.string.dlg_cancel, null)
                .show();
        ((TextView)alertDialog.findViewById(android.R.id.message)).setTextColor(0x8a000000);
    }

    private boolean isPkgInstalled(Context ctx, String packageName){
        PackageManager pm = ctx.getPackageManager();
        try {
            pm.getPackageInfo(packageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        return true;
    }
}



