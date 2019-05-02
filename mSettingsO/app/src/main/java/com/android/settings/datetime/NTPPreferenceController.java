/* ----------|----------------------|----------------------|----------------- */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* -------------------------------------------------------------------------- */
/* 2017/8/30 |     zhixiong.liu.hz  |      task 5198636     |     create      */
/******************************************************************************/


package com.android.settings.datetime;

import android.content.Context;
import android.provider.Settings;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;

import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settings.R;
import android.os.SystemProperties;
import android.util.Log;

public class NTPPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, Preference.OnPreferenceChangeListener{

    ListPreference mNtpServer;
    private static final String KEY_NTP_SERVER_LIST ="ntp_server_list";
    private static final String KEY_UPDATE_TIME ="auto_update_time_settings";
    private final Context mContext;
    private boolean mAvailable = false;
    
    public NTPPreferenceController(Context context, UpdateTimeAndDateCallback callback) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isAvailable() {
		mAvailable = mContext.getResources().getBoolean(R.bool.def_settings_hide_ntp_server);
        if(mAvailable){
           return false;
        }
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_NTP_SERVER_LIST;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen); 
        if(!isAvailable()){
            removePreference(screen, KEY_UPDATE_TIME);
            removePreference(screen, KEY_NTP_SERVER_LIST);
            return;
        }
        mNtpServer = (ListPreference) screen.findPreference(KEY_NTP_SERVER_LIST);
        
        String[] ntpServer = mContext.getResources().getStringArray(
                R.array.ntp_server_list);
        String[] ntpServerCust = new String[ntpServer.length + 1];
        String currentServer = Settings.Global.getString(mContext.getContentResolver(),Settings.Global.NTP_SERVER);

        String customServer = SystemProperties.get("ro.system.ntp.server", "pool.ntp.org");
        if (currentServer == null) {
                currentServer = "";
        }

        boolean existFlag = false;
        for (int j = 0; j < ntpServer.length; j++) {
            if (ntpServer[j].equals(customServer)) {
                existFlag = true;
                Log.d("NTP", "custom NTP server exists in NTP setting list");
                break;
            }
        }
        if (existFlag &&(currentServer.equals("")))
        {
        	currentServer = customServer;
        }
        if (!existFlag) {
            for (int k = 0; k < ntpServer.length; k++) {
                ntpServerCust[k] = ntpServer[k];
            }
            ntpServerCust[ntpServer.length] = customServer;
            ntpServer = ntpServerCust;
            Log.d("NTP",
                    "custom NTP server does not exist in NTP setting list, customServer is "
                            + customServer);
        }
        mNtpServer.setEntries(ntpServer);
        mNtpServer.setEntryValues(ntpServer);
        mNtpServer.setValue(currentServer);
        mNtpServer.setSummary(currentServer);
    }


    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String server = (String) newValue;

        boolean flag = Settings.Global.putString(mContext.getContentResolver(),
                        Settings.Global.NTP_SERVER, server);
            
        mNtpServer.setSummary(server);
        return true;
    }
}
