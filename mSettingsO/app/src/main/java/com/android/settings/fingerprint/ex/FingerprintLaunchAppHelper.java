package com.android.settings.fingerprint.ex;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;

import com.android.settings.R;
import com.tct.sdk.base.fingerprint.FingerprintConstants;


/**
 * Copyright (C) 2017 Tcl Corporation Limited
 * Created by jinlong.lu 2018-03-23
 */
public class FingerprintLaunchAppHelper {
    public static final String FUNC_CALL_A_CONTACT = "func_call_a_contact";
    public static final String FUNC_RECENT_CALLS = "func_recent_calls";
    public static final String FUNC_SELFIE = "func_selfie";
    public static final String FUNC_CAMERA = "func_camera";
    public static final String FUNC_TORCH = "func_torch";
    public static final String FUNC_START_SOUND_RECORD = "func_start_sound_record";
    public static final String FUNC_SET_TIMER = "func_set_timer";
    public static final String FUNC_CALCULATOR = "func_calculator";
    public static final String FUNC_NAVIGATE_HOME = "func_navigate_home";
    public static final String FUNC_START_MUSIC_PLAYLIST = "func_start_music_playlist";
    //Begin modified by jinlong.lu for XR6847910 on 18-8-18
    public static final String FUNC_NEW_MESSAGE = "fp_launch_new_message";
    public static final String FUNC_NEW_EMAIL = "fp_launch_new_email";
    //End modified by jinlong.lu for XR6847910 on 18-8-18
    public static final String FUNC_ADD_CONTACT = "func_add_contact";
    public static final String FUNC_ADD_AGENDA = "func_add_agenda";
    public static final String FUNC_GOOGLE_VOICE_SEARCH = "func_google_voice_search";
    public static final String FUNC_APPS = "func_apps";
    private static final String FINGERPRINT_FUNC_NUM = FingerprintConstants.TCT_FINGERPRINT_LAUNCH_NUM;
    private static final String TCT_FP_QUICK_LAUNCH_FUNC = FingerprintConstants.TCT_FINGERPRINT_QUICK_LAUNCH_FUNC;
    public static final int NAVIGATE_TYPE_CAR = 1;
    public static final int NAVIGATE_TYPE_TRANSIT = 2;
    public static final int NAVIGATE_TYPE_WALK = 3;
    public static final int NAVIGATE_TYPE_BIKING = 4;
    private static final String SPLIT_A = ":";
    private static final int INFO_MIN_LENGHT = 2;
    private static final String TAG = FingerprintLaunchAppHelper.class.getSimpleName();

    private static String getFingerprintLaunchInfo(Context context, int fpId) {
        String fpInfo = Settings.System.getString(context.getContentResolver(), FINGERPRINT_FUNC_NUM + getFpFuncSaveNum(context, fpId));
        Log.d(TAG, "getFingerprintLaunchInfo:" + fpInfo);
        return fpInfo;
    }

    //Begin added by jinlong.lu for XR6179735 on 18-4-11
    public static void resetLaunchAppData(Context context, int fpId) {
        Log.d(TAG, "reset launch app settings");
        Settings.System.putString(context.getContentResolver(), FINGERPRINT_FUNC_NUM + getFpFuncSaveNum(context, fpId), null);
    }
    //End added by jinlong.lu for XR6179735 on 18-4-11
    /**********
     * get FP launch app save index
     * @param context
     * @param fpid
     * @return save in list index
     */
    private static int getFpFuncSaveNum(Context context, int fpid) {
        int result = 0;
        //Begin modified by jinlong.lu for XR6873798 on 18-8-28
        final int max = context.getResources().getInteger(
                R.integer.config_fingerprintMaxTemplatesPerUser);
        //End modified by jinlong.lu for XR6873798 on 18-8-28
        for (int i = 0; i < max; i++) {
            String funcNumString = FINGERPRINT_FUNC_NUM + i;
            String fpInfo = Settings.System.getString(context.getContentResolver(),
                    funcNumString);
            //new fp settings
            if (fpInfo == null) {
                result = i;
                continue;
            }
            //old fp settings
            //fpInfo = fpId:funcType:info1:info2
            //only compare fpId
            String[] temp = fpInfo.split(":");
            if (temp.length >= 2) {
                if (temp[0].equals(fpid + "")) {
                    result = i;
                    break;
                }
            }
        }
        return result;
    }
    public static LaunchAppInfo getLauncherAppInfo(Context context, int fpId) {
        LaunchAppInfo launchAppInfo =new LaunchAppInfo();
        String result = null;
        String appName = null;
        String fpInfo = getFingerprintLaunchInfo(context, fpId);
        if (fpInfo == null) {
            String type =context.getString(R.string.fingerprint_functions_unlock_screen);
            launchAppInfo.setLaunchName(type);
            launchAppInfo.setLaunchType(type);
            return launchAppInfo;
        }
        String[] fpInfos = fpInfo.split(SPLIT_A);
        if (fpInfos.length >= INFO_MIN_LENGHT) {
            String mLaunchType = fpInfos[1];
            launchAppInfo.setLaunchType(mLaunchType);
            Resources res = context.getResources();
            //get launch app name id
            int descId = res.getIdentifier("string/" + mLaunchType, null, context.getPackageName());
            if (descId != 0) {
                //get launch app name
                appName = context.getString(descId);
            }
            switch (mLaunchType) {
                case FUNC_APPS:
                    if (fpInfos.length == 3) {
                        PackageManager pm = context.getPackageManager();
                        try {
                            appName = pm.getApplicationLabel(
                                    pm.getApplicationInfo(fpInfos[2],
                                            PackageManager.GET_META_DATA)).toString();
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                    } else if (fpInfos.length == 4) {
                        appName = getAppShortcutsName(context, fpInfos[2], fpInfos[3]);
                    }
                    //Begin added by jinlong.lu for XR6179735 on 18-4-11
                    if(android.text.TextUtils.isEmpty(appName)){
                        //app had disable or uninstall
                        //we need to clear this settings
                        Log.w(TAG,"app had disable or uninstall,app name is null");
                        resetLaunchAppData(context,fpId);
                        String type =context.getString(R.string.fingerprint_functions_unlock_screen);
                        launchAppInfo.setLaunchName(type);
                        launchAppInfo.setLaunchType(type);
                        return launchAppInfo;
                    }
                    //End added by jinlong.lu for XR6179735 on 18-4-11
                    result = context.getString(R.string.func_apps_summary, appName);
                    break;
                case FUNC_CALL_A_CONTACT:
                    if (fpInfos.length > 2) {
                        appName = fpInfos[2];
                    }
                    result = context.getString(R.string.call_a_contact_summary, appName);
                    break;
                case FUNC_NAVIGATE_HOME:
                    if (fpInfos.length > 2) {
                        appName = fpInfos[2];
                    }
                    result = context.getString(R.string.func_navigate_home_summary, appName);
                    break;
                default:
                    result = appName;
                    break;
            }
        }
        launchAppInfo.setLaunchName(result);
        return launchAppInfo;
    }
    private static String getAppShortcutsName(Context context,
                                              String packagename, String mainClassName) {
        String title = "";
        try {
            ActivityInfo info = context.getPackageManager().getActivityInfo(new ComponentName(packagename, mainClassName),
                    PackageManager.GET_META_DATA);
            title = info.loadLabel(context.getPackageManager()).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return title;
    }
    public static class CallUserModel {
        private static final String[] PHONES_PROJECTION = new String[]{
                ContactsContract.CommonDataKinds.Phone._ID};

        static final String[] CONTACTS_SUMMARY_PROJECTION = new String[]{
                ContactsContract.Contacts.DISPLAY_NAME,
                //[TCT-ROM][Fingerprint]Begin modified by jinlong.lu for XRP24356 on 18-9-29
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                //[TCT-ROM][Fingerprint]End modified by jinlong.lu for XRP24356 on 18-9-29
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        private String phone;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
        //Begin modified by jinlong.lu for XRP24356 on 18-9-28
        private long id;
        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getUser() {
            return this.getName()+":"+this.getPhone()+":"+this.getId();
        }
        //End modified by jinlong.lu for XRP24356 on 18-9-28

        public static CallUserModel getContacts(Context context, String uri) {
            ContentResolver mContentResolver = context.getContentResolver();
            CallUserModel result = null;
            final Uri contactUri = Uri.parse(uri);
            Cursor contactCursor = mContentResolver.query(contactUri, PHONES_PROJECTION, null, null, null);
            if (contactCursor != null) {
                while (contactCursor.moveToNext()) {
                    long contact_id = contactCursor.getLong(0);//get id
                    Cursor phoneCursor = mContentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, CONTACTS_SUMMARY_PROJECTION,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + contact_id, null, null);
                    if (phoneCursor != null) {
                        while (phoneCursor.moveToNext()) {
                            result = new CallUserModel();
                            String name = phoneCursor.getString(0);
                            String phone = phoneCursor.getString(2);
                           //Begin modified by jinlong.lu for XRP24356 on 18-9-28
                            long id =phoneCursor.getLong(1);
                            result.setName(name);
                            result.setPhone(phone);
                            result.setId(id);
                            Log.d("UserModel", "name:" + name + "\nphone:" + phone+"\nid: "+id);
                           //End modified by jinlong.lu for XRP24356 on 18-9-28
                        }
                        phoneCursor.close();
                    }

                }
                contactCursor.close();
            }
            return result;
        }
    }
}