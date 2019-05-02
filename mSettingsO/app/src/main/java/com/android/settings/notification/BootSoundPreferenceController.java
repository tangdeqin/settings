/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.notification;

import android.content.Context;
import android.os.SystemProperties;
import android.support.annotation.VisibleForTesting;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;

//[TCT-ROM][ power on/off ringtone]Begin added by junliang.liu for XR6703329 on 20180815
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
//[TCT-ROM][ power on/off ringtone]end added by junliang.liu for XR6703329 on 20180815


public class BootSoundPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin {

    // Boot Sounds needs to be a system property so it can be accessed during boot.
    private static final String KEY_BOOT_SOUNDS = "boot_sounds";
    @VisibleForTesting
    //[TCT-ROM][Sound]Begin modified by yang.sun for XR7101651 on 18-11-15
    static final String PROPERTY_BOOT_SOUNDS = "persist.sys.switch_on";
    static final String PROPERTY_BOOT_AVALIABLE = "ro.config.key_switch_on_off";
    //[TCT-ROM][Sound]End modified by yang.sun for XR7101651 on 18-11-15

    public BootSoundPreferenceController(Context context) {
        super(context);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            SwitchPreference preference = (SwitchPreference) screen.findPreference(KEY_BOOT_SOUNDS);
           //[TCT-ROM][ power on/off ringtone]Begin modified by junliang.liu for XR6703329 on 20180815 
            Boolean switchStatus = SystemProperties.getInt(PROPERTY_BOOT_SOUNDS, 1) == 1 ? true : false ;
            preference.setChecked(switchStatus); 
            String isChecked = switchStatus ? "1" : "0" ;
            writeFileforBootSound(isChecked);
        }
    }
    
    private void writeFileforBootSound(String isChecked ){
        File file = new File("/tctpersist/persist_sys_switch");
        try {
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream outStream = new FileOutputStream(file);
            outStream.write(isChecked.getBytes());
            outStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //[TCT-ROM][ power on/off ringtone]end modified by junliang.liu for XR6703329 on 20180815
    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (KEY_BOOT_SOUNDS.equals(preference.getKey())) {
            SwitchPreference switchPreference = (SwitchPreference) preference;
            //[TCT-ROM][ power on/off ringtone]Begin modified by junliang.liu for XR6703329 on 20180815
            String isChecked = switchPreference.isChecked() ? "1" : "0" ;
            SystemProperties.set(PROPERTY_BOOT_SOUNDS, isChecked );
            writeFileforBootSound(isChecked);
            //[TCT-ROM][ power on/off ringtone]end modified by junliang.liu for XR6703329 on 20180815
        }
        return false;
    }

    @Override
    public String getPreferenceKey() {
        return KEY_BOOT_SOUNDS;
    }

    @Override
    public boolean isAvailable() {
       //[TCT-ROM][Sound]Begin modified by yang.sun for XR7101651 on 18-11-15
        return SystemProperties.getInt(PROPERTY_BOOT_AVALIABLE,1) == 1 ? true : false;//mContext.getResources().getBoolean(com.android.settings.R.bool.has_boot_sounds);
       //[TCT-ROM][Sound]End modified by yang.sun for XR7101651 on 18-11-15
    }

}