package com.android.settings.notification;

import android.content.Context;
import android.content.Intent;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.TelephonyManager;//[TCT-ROM][Sound]Added by yingying.qiao.hz for task7305591 on 2018/12/29

import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.core.PreferenceControllerMixin; //add by zhixiong.liu.hz for xr5642278 search FC 20171124
/**
 * Created for Task 5571741 by yonglin.liao 2017/11/08.
 */

public class DualSoundSettingsController extends AbstractPreferenceController implements PreferenceControllerMixin{//add by zhixiong.liu.hz for xr 5642278 search fc 20171124

    private static final String TAG = "DualSoundSettings";
    private static final String KEY_DUAL_SOUND_SETTINGS = "dual_sound_settings";

    private Context mContext;
    private Preference mPreference = null;
    private boolean dualSupport = false;


    public DualSoundSettingsController(Context context) {
        super(context);
        mContext = context;
        //[TCT-ROM][Sound]Begin added by yingying.qiao.hz for task7305591 on 2018/12/29
        dualSupport = TelephonyManager.getDefault().getPhoneCount() == 2 ? true:false;//SystemProperties.getBoolean("dualsim.ui.support", false);
        //[TCT-ROM][Sound]End added by yingying.qiao.hz for task7305591 on 2018/12/29
    }

    @Override
    public boolean isAvailable() {
        if (dualSupport) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getPreferenceKey() {
        return KEY_DUAL_SOUND_SETTINGS;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        mPreference = screen.findPreference(KEY_DUAL_SOUND_SETTINGS);
        if (mPreference != null) {
            mPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mContext.startActivity(new Intent(mContext, SoundDualSettings.class));
                    return false;
                }
            });
        }
    }
}
