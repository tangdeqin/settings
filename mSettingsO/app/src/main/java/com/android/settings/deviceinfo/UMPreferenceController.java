/* ----------|----------------------|----------------------|----------------- */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* -------------------------------------------------------------------------- */
/* 2017/8/30 |     zhixiong.liu.hz  |      task 5198616     |     create      */
/******************************************************************************/



package com.android.settings.deviceinfo;

import android.app.Activity;
import android.annotation.UserIdInt;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settingslib.core.AbstractPreferenceController;
import android.support.v7.preference.Preference.OnPreferenceClickListener;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.R;
import android.net.Uri;
import android.content.Intent;
import java.util.Locale;
import android.text.TextUtils;
import android.util.Log;
import android.content.pm.PackageManager;
import android.content.pm.ApplicationInfo;
import java.io.File;

public class UMPreferenceController extends AbstractPreferenceController  
             implements PreferenceControllerMixin{

    private boolean mAvailable = true;
    private Context mContext;
    private static final String KEY_USER_MANUAL = "user_manual";
    private static final String USER_MENU_FILE_PATH = "file:///system/usermanual/user_manual.html";
    public UMPreferenceController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isAvailable() {
		mAvailable = mContext.getResources().getBoolean(R.bool.def_settings_about_phone_um_display);
        if(!mAvailable){
           return false;  
        } 
        return true;
         
    }
    
    @Override
    public String getPreferenceKey() {
        return KEY_USER_MANUAL;
    }
    
    @Override
    public boolean handlePreferenceTreeClick(Preference preference){
        if(KEY_USER_MANUAL.equals(preference.getKey())){
        	
            Uri uri = Uri.parse(USER_MENU_FILE_PATH);
            File file = new File(uri.getPath());
            if (file.exists()) {
                 Intent intent = new Intent(Intent.ACTION_VIEW);
                 intent.setDataAndType(uri, "text/html");
                 PackageManager pm = mContext.getPackageManager();
                 if (!pm.queryIntentActivities(intent, 0).isEmpty()) {
                    mContext.startActivity(intent);
                    return true;
                 }
            }
            
            Intent i = new Intent();
            i.setAction(Intent.ACTION_VIEW);
            i.setData(Uri.parse(parseUMLinkAsLocale(mContext.getResources().getString(com.android.settings.R.string.UM_Link_URL))));
            mContext.startActivity(i);
            return true;
        }
        return false;
    }
    
    private String parseUMLinkAsLocale(String link) {
        final String tag = "com/support";
        String localeLan = Locale.getDefault().getLanguage();
        if (!TextUtils.isEmpty(link) && link.indexOf(tag) >= 0) {
            if (localeLan.equals("en")) {
                link = link.replace(tag, "com/uk");
            } else if(localeLan.equals("ca") || localeLan.equals("gl")|| localeLan.equals("eu")){
                link = link.replace(tag, "com/es");
            } else {
                link = link.replace(tag, "com/" + localeLan);
            }
        }
        return link;
    }
}
