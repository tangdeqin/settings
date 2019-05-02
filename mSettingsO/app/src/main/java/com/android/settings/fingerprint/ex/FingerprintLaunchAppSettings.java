package  com.android.settings.fingerprint.ex;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceScreen;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.SubSettings;
import com.android.settings.fingerprint.ex.FingerprintLaunchAppHelper.CallUserModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.hardware.fingerprint.Fingerprint;
import android.hardware.fingerprint.FingerprintManager;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.tct.sdk.base.fingerprint.FingerprintConstants;
import com.tct.sdk.base.fingerprint.TctFingerprintUtils;

/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Created by jinlong.lu on 17-10-31.
 */
//Begin added by jinlong.lu for XR6618444 on 18-7-31
public class FingerprintLaunchAppSettings extends SubSettings {
    private static final String TAG = "FingerprintLaunchAppSettings";

    @Override
    public Intent getIntent() {
        Intent modIntent = new Intent(super.getIntent());
        modIntent.putExtra(EXTRA_SHOW_FRAGMENT, FingerprintLaunchAppFragment.class.getName());
        return modIntent;
    }

    @Override
    protected boolean isValidFragment(String fragmentName) {
        if (FingerprintLaunchAppFragment.class.getName().equals(fragmentName)) return true;
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //Begin modified by jinlong.lu for XR6847910 on 18-8-18
       //Begin modified by jinlong.lu for XRP23382 on 18-8-31
        CharSequence msg = getText(R.string.fingerprint_sensor_function_title);
        setTitle(msg);
       //End modified by jinlong.lu for XRP23382 on 18-8-31
        //End modified by jinlong.lu for XR6847910 on 18-8-18
    }

    public static class FingerprintLaunchAppFragment extends SettingsPreferenceFragment implements RadioButtonPreferenceEx.OnClickListener {

        private static final String TAG = "FPLaunchApp";
        private Context mContext;
        private final String KEY_FINGERPRINT_UNLOCK_DEVICE = "unlock_device";
        private final String KEY_FINGERPRINT_QUICK_LAUNCH_FUNC = "quick_lunch_func";

        private byte[] mToken;
        private Fingerprint mFingerprint;
        private int mFingerId;
        private int mFpLaunchAppSaveIndex;
        private final int APP_CHOOSE_RESULT = 101;
        private final int APP_CHOOSE_FUNC_NAVIGATE_HOME_RESULT = 102;
        private final int APP_CHOOSE_FUNC_START_MUSIC_PLAYLIST_RESULT = 103;
        private int mFpMax;
        private static final String FUNC_APPS = "func_apps";
        private static final String FUNC_NAVIGATE_HOME = "func_navigate_home";
        private static final String FUNC_START_MUSIC_PLAYLIST = "func_start_music_playlist";
        private static final String HOME_ADDRESS = FingerprintConstants.TCT_FINGERPRINT_HOME_ADDRESS;
        private static final String FUNC_CALL_A_CONTACT = "func_call_a_contact";
        public static final int ACTION_CODE_PICK_CONTACT = 300;
        private int mTotleFuncFp = 0;
        private boolean mIsNewFpFuncNum = false;
        private RadioButtonPreferenceEx mUnlockDevice;
        private PreferenceCategory mQuickLaunchFunc;
        //Begin modified by jinlong.lu for XR6847910 on 18-8-18
        private static final String LAUNCH_APP_LIST = "fp_none;func_call_a_contact;func_recent_calls;func_selfie;func_camera;func_torch;func_start_sound_record;func_set_timer;func_calculator;func_navigate_home;fp_launch_new_message;fp_launch_new_email;func_add_contact;func_add_agenda;func_google_voice_search;func_apps";
        //End modified by jinlong.lu for XR6847910 on 18-8-18
        private static final String SQILT_A = ";";
        private ArrayList<String> mLaunchAppList;
        private FingerprintManager mFp  ;
        TctFingerprintUtils mTctFingerprintUtils;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.fingerprint_launch_app_settings);
            mContext = getActivity();
            mToken = getIntent().getByteArrayExtra(
                     ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN);
            if (mToken == null) {
                finish();
            }
            mTctFingerprintUtils =new TctFingerprintUtils(mContext);
            mFp = (FingerprintManager) mContext.getSystemService(
                    Context.FINGERPRINT_SERVICE);
            mFingerprint =  getIntent().getParcelableExtra(
                    "fingerprint");
            mFingerId = mFingerprint.getFingerId();
            //Begin modified by jinlong.lu for XR6873798 on 18-8-28
            mFpMax = mContext.getResources().getInteger(
                    R.integer.config_fingerprintMaxTemplatesPerUser);
            //End modified by jinlong.lu for XR6873798 on 18-8-28
            if (mFingerprint != null) {
                getActivity().setTitle(getString(R.string.fingerprint_function_title, mFingerprint.getName()));
            } else {
                finish();
            }

        }

        @Override
        public void onResume() {
            super.onResume();
            initPreference();
        }

        private void initPreference() {
            mFpLaunchAppSaveIndex = getFpLaunchAppSaveIndex();
            Log.d(TAG, "mFpLaunchAppSaveIndex:" + mFpLaunchAppSaveIndex);
            PreferenceScreen root = getPreferenceScreen();
            mUnlockDevice = (RadioButtonPreferenceEx) root.findPreference(KEY_FINGERPRINT_UNLOCK_DEVICE);
            mUnlockDevice.setOnClickListener(this);
            mQuickLaunchFunc = (PreferenceCategory) root.findPreference(KEY_FINGERPRINT_QUICK_LAUNCH_FUNC);
            mUnlockDevice.setChecked(true);
            mLaunchAppList = new ArrayList<>();
           //Begin modified by jinlong.lu for XR5854853 on 18-1-19
            String[] tmps = (getResources().getString(R.string.def_fp_launch_app_list)).split(SQILT_A);
           //End modified by jinlong.lu for XR5854853 on 18-1-19
            Collections.addAll(mLaunchAppList, tmps);
            if (mQuickLaunchFunc != null) {
                mQuickLaunchFunc.removeAll();
            }
            LaunchAppInfo launchAppInfo = FingerprintLaunchAppHelper.getLauncherAppInfo(mContext, mFingerId);
            for (int i = 0; i < mLaunchAppList.size(); i++) {
                String funcStr = mLaunchAppList.get(i);
                LaunchAppRadioButtonPreference funcItem = new LaunchAppRadioButtonPreference(mContext);
                funcItem.setChecked(false);
                Resources res = mContext.getResources();
                int descId = res.getIdentifier("string/" + funcStr, null, mContext.getPackageName());
                if (descId != 0) {
                    funcItem.setTitle(descId);
                }
                funcItem.setLaunchApp(funcStr);
                String launchAppType = launchAppInfo.getLaunchType();
                Log.d(TAG, "launchAppType:" + launchAppType);
                if (launchAppType != null && launchAppType.equals(funcStr)) {
                    Log.d(TAG, "getFuncById:" + launchAppType + " setChecked");
                    funcItem.setChecked(true);
                    mUnlockDevice.setChecked(false);
                }
                if (funcStr != null && funcItem.isChecked()) {
                    if (launchAppType.equals(FUNC_NAVIGATE_HOME) || launchAppType.equals(FUNC_CALL_A_CONTACT) || launchAppType.equals(FUNC_APPS)) {
                        String launchAppName =launchAppInfo.getLaunchName();
                        funcItem.setSummary(launchAppName);
                    }
                }
                mQuickLaunchFunc.addPreference(funcItem);
                funcItem.setOnClickListener(this);
            }
        }

        private int getFpLaunchAppSaveIndex() {
            int result = 0;
            for (int i = 0; i < mFpMax; i++) {
                String funcNumString = FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM + i;
                String fpInfo = Settings.System.getString(getContentResolver(),
                        funcNumString);
                Log.d(TAG, "fpInfo:   " + fpInfo);
                if (fpInfo != null) {
                    mTotleFuncFp++;
                }
                if (fpInfo == null) {
                    result = i;
                    mIsNewFpFuncNum = true;
                    continue;
                }
                //fpInfo = fpId:funcType:info1:info2
                //only compare fpId
                String[] temp = fpInfo.split(":");
                if (temp.length >= 2) {
                    if (temp[0].equals(mFingerId + "")) {
                        result = i;
                        mIsNewFpFuncNum = false;
                        break;
                    }
                }
            }
            Log.d(TAG, "getFpFuncSaveNum:" + result);
            return result;
        }

        private boolean saveLaunchApp(String launchApp, String packageName, String mainClass) {
            boolean result;
            String fpSave;
            if (launchApp == null) {
                fpSave = null;
            } else if (packageName == null) {
                fpSave = mFingerId + ":" + launchApp;
            } else {
                if (mainClass != null && !mainClass.isEmpty()) {
                    fpSave = mFingerId + ":" + launchApp + ":" + packageName + ":" + mainClass;
                } else {
                    fpSave = mFingerId + ":" + launchApp + ":" + packageName;
                }
            }
            Log.d(TAG, "fpfunc" + fpSave + " mFpLaunchAppSaveIndex:" + mFpLaunchAppSaveIndex);
            result = Settings.System.putString(getContentResolver(),
                    FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM + mFpLaunchAppSaveIndex, fpSave);
            return result;
        }

        private boolean saveLaunchApp(String launchApp, String packageName) {
            boolean result;
            String fpSave;
            if (launchApp == null) {
                fpSave = null;
            } else if (packageName == null) {
                fpSave = mFingerId + ":" + launchApp;
            } else {
                fpSave = mFingerId + ":" + launchApp + ":" + packageName;
            }
            Log.d(TAG, "fpfunc" + fpSave + " mFpLaunchAppSaveIndex:" + mFpLaunchAppSaveIndex);
            result = Settings.System.putString(getContentResolver(),
                    FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM + mFpLaunchAppSaveIndex, fpSave);
            return result;
        }

        @Override
        public int getMetricsCategory() {
            return MetricsEvent.FINGERPRINT;
        }

        @Override
        public void onRadioButtonClicked(RadioButtonPreferenceEx emiter) {
            final String launchApp;
            final List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(UserHandle.myUserId(), FingerprintConstants.FP_TAG_COMMON);
            final int fingerprintCount = items != null ? items.size() : 0;
            if (emiter == mUnlockDevice) {
                if (saveLaunchApp(null, null)) {
                    initPreference();
                }
                return;
            } else {
                LaunchAppRadioButtonPreference funcPreference = (LaunchAppRadioButtonPreference) emiter;
                launchApp = funcPreference.getLaunchApp();
                Log.d(TAG, "func clicked:" + funcPreference.getLaunchApp());
            }
            switch (launchApp) {
                case FUNC_CALL_A_CONTACT:
                    Intent intent3 = new Intent(Intent.ACTION_PICK);
                    intent3.setType(ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                    intent3.setData(ContactsContract.Contacts.CONTENT_URI);
                    startActivityForResult(intent3, ACTION_CODE_PICK_CONTACT);
                    return;
                case FUNC_NAVIGATE_HOME:
                    Intent mIntent = new Intent(
                            "android.settings.FINGERPRINT_NAVIGATE_HOME_SETTINGS");
                    startActivityForResult(mIntent, APP_CHOOSE_FUNC_NAVIGATE_HOME_RESULT);
                    return;
                case FUNC_START_MUSIC_PLAYLIST:
                    Intent mIntent1 = new Intent("com.tct.mix.action.FUNCLOCKSCREEN");
                    mIntent1.putExtra("funcSettings", true);
                    mContext.sendBroadcast(mIntent1);
                    return;
                case FUNC_APPS:
                    Log.d(TAG, FUNC_APPS);
                    Intent intent = new Intent();
                    intent.setClassName("com.android.settings",
                            "com.android.settings.fingerprint.ex.FingerprintAppListSettings");
                    startActivityForResult(intent, APP_CHOOSE_RESULT);
                    return;
                default:
                    Log.d(TAG, "fingerprintCount :" + fingerprintCount + "  mTotleFuncFp:" + mTotleFuncFp + "  mIsNewFpFuncNum" + mIsNewFpFuncNum);
                    if ((fingerprintCount == mTotleFuncFp + 1) && mIsNewFpFuncNum) {
                        ConfirmSetAllFpFuncDialog(launchApp, null);
                        return;
                    }
                    break;
            }
            if (saveLaunchApp(launchApp, null)) {
                initPreference();
            }
        }

        private void ConfirmSetAllFpFuncDialog(String Func, String PackageName) {
            final String func = Func;
            final String packageName = PackageName;
            AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.fp_all_setfunc_title)
                    .setMessage(R.string.fp_all_setfunc_message)
                    .setPositiveButton(R.string.fp_all_setfunc_positive,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (saveLaunchApp(func, packageName)) {
                                        mTotleFuncFp++;
                                        initPreference();
                                    }
                                }
                            }).setNegativeButton(R.string.fp_all_setfunc_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    initPreference();
                                    dialog.dismiss();
                                }
                            }).show();
        }

        private void ConfirmSetAllFpFuncDialog(String Func, String PackageName, String mainClass) {
            final String func = Func;
            final String packageName = PackageName;
            AlertDialog alertDialog = new AlertDialog.Builder(mContext)
                    .setTitle(R.string.fp_all_setfunc_title)
                    .setMessage(R.string.fp_all_setfunc_message)
                    .setPositiveButton(R.string.fp_all_setfunc_positive,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (saveLaunchApp(func, packageName, mainClass)) {
                                        mTotleFuncFp++;
                                        initPreference();
                                    }
                                }
                            }).setNegativeButton(R.string.fp_all_setfunc_cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    initPreference();
                                    dialog.dismiss();
                                }
                            }).show();
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            final List<Fingerprint> items = mTctFingerprintUtils.getTctEnrolledFingerprints(UserHandle.myUserId(), FingerprintConstants.FP_TAG_COMMON);
            final int fingerprintCount = items != null ? items.size() : 0;
            if (resultCode == Activity.RESULT_OK
                    && requestCode == APP_CHOOSE_RESULT) {
                Bundle bundle = data.getExtras();
                if (bundle != null) {
                    String pkgName = bundle.getString("packageName");
                    String mainClass = bundle.getString("mainclassName");
                    Log.d(TAG, "pkgName:" + pkgName);
                    if ((fingerprintCount == mTotleFuncFp + 1) && mIsNewFpFuncNum) {
                        ConfirmSetAllFpFuncDialog(FUNC_APPS, pkgName, mainClass);
                        return;
                    }
                    if (saveLaunchApp(FUNC_APPS, pkgName, mainClass)) {
                        initPreference();
                    }
                }
            } else if (resultCode == Activity.RESULT_OK && requestCode == ACTION_CODE_PICK_CONTACT) {
                String uri = data.getDataString();
                CallUserModel user = CallUserModel.getContacts(mContext, uri);
                if (user != null) {
                    if ((fingerprintCount == mTotleFuncFp + 1) && mIsNewFpFuncNum) {
                        ConfirmSetAllFpFuncDialog(FUNC_CALL_A_CONTACT, user.getUser());
                        return;
                    }
                    if (saveLaunchApp(FUNC_CALL_A_CONTACT, user.getUser())) {
                        initPreference();
                    }
                } else {
                    Toast.makeText(mContext, R.string.fingerprint_func_set_contact_error_msg, Toast.LENGTH_SHORT).show();
                    onRadioButtonClicked(mUnlockDevice);
                }
            } else if (resultCode == Activity.RESULT_CANCELED && requestCode == ACTION_CODE_PICK_CONTACT) {
                onRadioButtonClicked(mUnlockDevice);
                return;
            } else if ((resultCode == Activity.RESULT_OK || resultCode == Activity.RESULT_CANCELED)
                    && requestCode == APP_CHOOSE_FUNC_NAVIGATE_HOME_RESULT) {
                String mHomeAddress  = Settings.System.getString(mContext.getContentResolver(), HOME_ADDRESS);
                if (mHomeAddress == null || mHomeAddress.isEmpty()) {
                    onRadioButtonClicked(mUnlockDevice);
                    return;
                } else {
                    if ((fingerprintCount == mTotleFuncFp + 1) && mIsNewFpFuncNum) {
                        ConfirmSetAllFpFuncDialog(FUNC_NAVIGATE_HOME, mHomeAddress);
                        return;
                    }
                    if (saveLaunchApp(FUNC_NAVIGATE_HOME, mHomeAddress)) {
                        initPreference();
                    }
                }
            }
        }
    }


    public static class LaunchAppRadioButtonPreference extends RadioButtonPreferenceEx {

        public String getLaunchApp() {
            return mLaunchApp;
        }

        public void setLaunchApp(String mLaunchApp) {
            this.mLaunchApp = mLaunchApp;
        }

        private String mLaunchApp;

        public LaunchAppRadioButtonPreference(Context context) {
            super(context);
        }

        public LaunchAppRadioButtonPreference(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

    }

}
//End added by jinlong.lu for XR6618444 on 18-7-31

