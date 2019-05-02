package com.android.settings.notification;

import android.content.Context;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settings.R;
/**
 *  [TCT-ROM][SoundEnhancement] Created by nanbing.zou for P23236 on 2018-09-05
 */

public class SoundEnhancementController extends AdjustVolumeRestrictedPreferenceController {
    private Context mContext;
    public SoundEnhancementController(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public boolean isAvailable() {
        return mContext.getResources().getBoolean(R.bool.def_sound_enhancement_support);
    }

    @Override
    public String getPreferenceKey() {
        return "sound_enhancement";
    }
}
