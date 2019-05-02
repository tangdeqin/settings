/* ----------|----------------------|----------------------|----------------- */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* -------------------------------------------------------------------------- */
/* 2017/8/30 |     zhixiong.liu.hz  |      task 5198616     |     create      */
/******************************************************************************/



package com.android.settings.display;

import android.annotation.UserIdInt;
import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.R;

public class LedPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    private final String mLedPrefKey;
    private boolean mAvailable = true;

    public LedPreferenceController(Context context,String key) {
        super(context);
        mLedPrefKey = key;
    }

    @Override
    public boolean isAvailable() {
	mAvailable = mContext.getResources().getBoolean(R.bool.def_Settings_LED_menu_enable);
        if(!mAvailable){
           return false;  
        } 
        return true;
         
    }
    @Override
    public void displayPreference(PreferenceScreen screen) {
        //super.displayPreference(screen);
        if(!isAvailable()){
            removePreference(screen, mLedPrefKey);
            return;
        }
    }
    
    @Override
    public String getPreferenceKey() {
        return mLedPrefKey;
    }
}
