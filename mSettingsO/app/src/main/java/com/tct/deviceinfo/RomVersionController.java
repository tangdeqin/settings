/* ----------|----------------------|----------------------|----------------- */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* -------------------------------------------------------------------------- */
/* 2017/8/30 |     zhixiong.liu.hz  |      task 6696757     |     create      */
/******************************************************************************/

package com.tct.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

public class RomVersionController extends AbstractPreferenceController implements
        PreferenceControllerMixin {
            
    private static final String ROM_NAME_PROPERTY = "ro.rom.name";
    private static final String ROM_VERSION_PROPERTY = "ro.rom.version";
    
    private static final String KEY_ROM_VERSION = "rom version";

    public RomVersionController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_ROM_VERSION;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        
        preference.setTitle(SystemProperties.get(ROM_NAME_PROPERTY,
                mContext.getResources().getString(R.string.rom_version)));
        preference.setSummary(SystemProperties.get(ROM_VERSION_PROPERTY,
                "v0.0.1"));

    }
}
