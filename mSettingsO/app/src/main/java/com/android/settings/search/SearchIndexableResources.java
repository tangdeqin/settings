/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.settings.search;

import android.provider.SearchIndexableResource;
import android.support.annotation.DrawableRes;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.XmlRes;
import android.text.TextUtils;
import com.android.settings.DateTimeSettings;
import com.android.settings.DeviceInfoSettings;
import com.android.settings.DisplaySettings;
import com.android.settings.EncryptionAndCredential;
import com.android.settings.LegalSettings;
import com.android.settings.R;
import com.android.settings.ScreenPinningSettings;
import com.android.settings.SecuritySettings;
import com.android.settings.accessibility.AccessibilitySettings;
import com.android.settings.accessibility.AccessibilityShortcutPreferenceFragment;
import com.android.settings.accessibility.MagnificationPreferenceFragment;
import com.android.settings.accounts.UserAndAccountDashboardFragment;
import com.android.settings.applications.AppAndNotificationDashboardFragment;
import com.android.settings.applications.DefaultAppSettings;
import com.android.settings.applications.SpecialAccessSettings;
import com.android.settings.applications.assist.ManageAssist;
import com.android.settings.backup.BackupSettingsActivity;
import com.android.settings.backup.BackupSettingsFragment;
import com.android.settings.bluetooth.BluetoothSettings;
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment;
import com.android.settings.datausage.DataUsageMeteredSettings;
import com.android.settings.datausage.DataUsageSummary;
import com.android.settings.deletionhelper.AutomaticStorageManagerSettings;
import com.android.settings.development.DevelopmentSettings;
import com.android.settings.deviceinfo.Status;
import com.android.settings.deviceinfo.StorageDashboardFragment;
import com.android.settings.deviceinfo.StorageSettings;
import com.android.settings.display.AmbientDisplaySettings;
import com.android.settings.display.ScreenZoomSettings;
import com.android.settings.dream.DreamSettings;
import com.android.settings.enterprise.EnterprisePrivacySettings;
import com.android.settings.fuelgauge.BatterySaverSettings;
import com.android.settings.fuelgauge.PowerUsageAdvanced;
import com.android.settings.fuelgauge.PowerUsageSummary;
import com.android.settings.gestures.AssistGestureSettings;
import com.android.settings.gestures.DoubleTapPowerSettings;
import com.android.settings.gestures.DoubleTapScreenSettings;
import com.android.settings.gestures.DoubleTwistGestureSettings;
import com.android.settings.gestures.GestureSettings;
import com.android.settings.gestures.PickupGestureSettings;
import com.android.settings.gestures.SwipeToNotificationSettings;
import com.android.settings.inputmethod.AvailableVirtualKeyboardFragment;
import com.android.settings.inputmethod.PhysicalKeyboardFragment;
import com.android.settings.inputmethod.VirtualKeyboardFragment;
import com.android.settings.language.LanguageAndInputSettings;
import com.android.settings.location.LocationSettings;
import com.android.settings.location.ScanningSettings;
import com.android.settings.network.NetworkDashboardFragment;
import com.android.settings.nfc.PaymentSettings;
import com.android.settings.notification.ChannelImportanceSettings;
import com.android.settings.notification.ConfigureNotificationSettings;
import com.android.settings.notification.SoundSettings;
import com.android.settings.notification.ZenModePrioritySettings;
import com.android.settings.notification.ZenModeSettings;
import com.android.settings.notification.ZenModeVisualInterruptionSettings;
import com.android.settings.print.PrintSettingsFragment;
import com.android.settings.security.LockscreenDashboardFragment;
import com.android.settings.sim.SimSettings;
import com.android.settings.support.SupportDashboardActivity;
import com.android.settings.system.ResetDashboardFragment;
import com.android.settings.system.SystemDashboardFragment;
import com.android.settings.tts.TtsEnginePreferenceFragment;
import com.android.settings.users.UserSettings;
import com.android.settings.wallpaper.WallpaperTypeSettings;
import com.android.settings.wifi.ConfigureWifiSettings;
import com.android.settings.wifi.SavedAccessPointsWifiSettings;
import com.android.settings.wifi.WifiSettings;
import java.util.Collection;
import java.util.HashMap;
import com.android.settings.AdvancedFeaturesSettings;//Added by miaoliu for XR5861646 on 2018/1/10
//Begin added by jinlong.lu for XR6618444 on 18-8-1
import com.android.settings.fingerprint.FingerprintSettings;
import com.android.settings.fingerprint.ex.FingerprintSensorSettings;
//End added by jinlong.lu for XR6618444 on 18-8-1

//[TCT-ROM][SoundEnhancement] Added by nanbing.zou for P23236 on 2018-09-05 begin
import com.tct.audioprofile.SoundEnhancement;
//[TCT-ROM][SoundEnhancement] Added by nanbing.zou for P23236 on 2018-09-05 end
import com.tct.search.SearchExt;//Added by miaoliu for XRP23522 on 2018/9/6
// [TCT-ROM] [NfcSettings] Added by jianhao.zeng for p10025718 on 2018-11-09 begin
import com.android.settings.nfc.NfcSettings;
import com.android.settings.nfc.AndroidBeam;
import android.os.SystemProperties;
// [TCT-ROM] [NfcSettings] Added by jianhao.zeng for p10025718 on 2018-11-09 end
//Begin added by junyan.wan.hz for XR 7108532 on 2018/11/16
import com.tct.wifi.hotspot.TctTetherWifiSettings;
//End added by junyan.wan.hz for XR 7108532 on 2018/11/16

public final class SearchIndexableResources {

    /**
     * Identifies subsettings which have an {@link SearchIndexableResource#intentAction} but
     * whose intents should still be treated as subsettings inside of Settings.
     */
    public static final String SUBSETTING_TARGET_PACKAGE = "subsetting_target_package";

    @XmlRes
    public static final int NO_DATA_RES_ID = 0;

    @VisibleForTesting
    static final HashMap<String, SearchIndexableResource> sResMap = new HashMap<>();

    @VisibleForTesting
    static void addIndex(Class<?> indexClass, @XmlRes int xmlResId,
            @DrawableRes int iconResId) {
        addIndex(indexClass, xmlResId, iconResId, null /* targetAction */);
    }

    @VisibleForTesting
    static void addIndex(Class<?> indexClass, @XmlRes int xmlResId,
            @DrawableRes int iconResId, String targetAction) {
        String className = indexClass.getName();
        SearchIndexableResource resource =
                new SearchIndexableResource(0, xmlResId, className, iconResId);

        if (!TextUtils.isEmpty(targetAction)) {
            resource.intentAction = targetAction;
            resource.intentTargetPackage = SUBSETTING_TARGET_PACKAGE;
        }

        sResMap.put(className, resource);
    }

    static {
        addIndex(WifiSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_wireless);
        addIndex(NetworkDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_wireless);
        addIndex(ConfigureWifiSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_wireless);
        addIndex(SavedAccessPointsWifiSettings.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_wireless);
        addIndex(BluetoothSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_bluetooth);
        addIndex(SimSettings.class, NO_DATA_RES_ID, R.drawable.ic_sim_sd);
        addIndex(DataUsageSummary.class, NO_DATA_RES_ID, R.drawable.ic_settings_data_usage);
        addIndex(DataUsageMeteredSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_data_usage);
        //addIndex(ScreenZoomSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);//Deleted by miaoliu for XRP10025479 on 2018/11/05
        addIndex(DisplaySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display,
                "android.settings.DISPLAY_SETTINGS");
        addIndex(AmbientDisplaySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);
        addIndex(WallpaperTypeSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);
        //Begin modified by miaoliu for XRP24381 on 2018/9/28
        addIndex(ConfigureNotificationSettings.class,
                NO_DATA_RES_ID, R.drawable.ic_settings_notifications);
        //End modified by miaoliu for XRP24381 on 2018/9/28
        addIndex(AppAndNotificationDashboardFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_applications);
        addIndex(SoundSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_sound,
                "android.settings.SOUND_SETTINGS");
        addIndex(ZenModeSettings.class,
                R.xml.zen_mode_settings, R.drawable.ic_settings_notifications);
        addIndex(ZenModePrioritySettings.class,
                R.xml.zen_mode_priority_settings, R.drawable.ic_settings_notifications);
        addIndex(StorageSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_storage);
        //Begin modified by miaoliu for XRP24381 on 2018/9/28
        addIndex(PowerUsageSummary.class,
                NO_DATA_RES_ID, R.drawable.ic_settings_battery);
        addIndex(PowerUsageAdvanced.class, NO_DATA_RES_ID, R.drawable.ic_settings_battery);
        addIndex(BatterySaverSettings.class,
                NO_DATA_RES_ID, R.drawable.ic_settings_battery);
        //End modified by miaoliu for XRP24381 on 2018/9/28
        addIndex(DefaultAppSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_applications);
        addIndex(ManageAssist.class, NO_DATA_RES_ID, R.drawable.ic_settings_applications);
        addIndex(SpecialAccessSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_applications);
        addIndex(UserSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_multiuser);
        addIndex(AssistGestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(PickupGestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(DoubleTapScreenSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        //addIndex(DoubleTapPowerSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);//Deleted by miaoliu for XRP23297 on 2018/9/6
        addIndex(DoubleTwistGestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(SwipeToNotificationSettings.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_gestures);
        addIndex(GestureSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_gestures);
        addIndex(LanguageAndInputSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(LocationSettings.class, R.xml.location_settings, R.drawable.ic_settings_location);
        addIndex(ScanningSettings.class, R.xml.location_scanning, R.drawable.ic_settings_location);
        addIndex(SecuritySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_security);
        addIndex(EncryptionAndCredential.class, NO_DATA_RES_ID, R.drawable.ic_settings_security);
        addIndex(ScreenPinningSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_security);
        addIndex(UserAndAccountDashboardFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_accounts);
        addIndex(VirtualKeyboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(AvailableVirtualKeyboardFragment.class,
                NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(PhysicalKeyboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(BackupSettingsActivity.class, NO_DATA_RES_ID, R.drawable.ic_settings_backup);
        addIndex(BackupSettingsFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_backup);
        addIndex(DateTimeSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_date_time);
        addIndex(AccessibilitySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_accessibility);
        addIndex(PrintSettingsFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_print);
        addIndex(DevelopmentSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_development);
        addIndex(DeviceInfoSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(Status.class, NO_DATA_RES_ID, 0 /* icon */);
        addIndex(LegalSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(ZenModeVisualInterruptionSettings.class,
                R.xml.zen_mode_visual_interruptions_settings,
                R.drawable.ic_settings_notifications);
        addIndex(SystemDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(ResetDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_restore);
        addIndex(StorageDashboardFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_storage);
        //Begin deleted by miaoliu for XR5861646 on 2018/1/10
        // addIndex(ConnectedDeviceDashboardFragment.class, NO_DATA_RES_ID,
        //         R.drawable.ic_devices_other);
        //End deleted by miaoliu for XR5861646 on 2018/1/10
        //Begin added by miaoliu for XR5861646 on 2018/1/10
        //add search index of  "Adanced Features"
        addIndex(AdvancedFeaturesSettings.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_advanced_features);
        //End added by miaoliu for XR5861646 on 2018/1/10
        addIndex(EnterprisePrivacySettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_about);
        addIndex(PaymentSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_nfc_payment);
        // [TCT-ROM] [NfcSettings] Added by jianhao.zeng for p10025718 on 2018-11-09 begin
        if( "1".equals(SystemProperties.get("persist.sys.nfc.support","0"))) {
            addIndex(NfcSettings.class, NO_DATA_RES_ID, R.drawable.ic_nfc);
            addIndex(AndroidBeam.class, NO_DATA_RES_ID, R.drawable.ic_android);
        }
        // [TCT-ROM] [NfcSettings] Added by jianhao.zeng for p10025718 on 2018-11-09 end
        addIndex(
                TtsEnginePreferenceFragment.class, NO_DATA_RES_ID, R.drawable.ic_settings_language);
        addIndex(LockscreenDashboardFragment.class, R.xml.security_lockscreen_settings,
                R.drawable.ic_settings_security);
        addIndex(MagnificationPreferenceFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_accessibility);
        addIndex(AccessibilityShortcutPreferenceFragment.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_accessibility);
        addIndex(ChannelImportanceSettings.class, NO_DATA_RES_ID,
                R.drawable.ic_settings_notifications);
        addIndex(DreamSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_display);
        addIndex(SupportDashboardActivity.class, NO_DATA_RES_ID, R.drawable.ic_help);
        //[TCT-ROM][SoundEnhancement] Added by nanbing.zou for P23236 on 2018-09-05 begin
        addIndex(SoundEnhancement.class, NO_DATA_RES_ID, R.drawable.ic_settings_sound);
        //[TCT-ROM][SoundEnhancement] Added by nanbing.zou for P23236 on 2018-09-05 end
        addIndex(
                AutomaticStorageManagerSettings.class,
                NO_DATA_RES_ID,
                R.drawable.ic_settings_storage);
        //Begin added by jinlong.lu for XR6618444 on 18-8-1
        addIndex(FingerprintSettings.FingerprintSettingsFragment.class,NO_DATA_RES_ID,R.drawable.ic_settings_fingerprint);
        addIndex(FingerprintSensorSettings.FpSettingsFragment.class,NO_DATA_RES_ID,R.drawable.ic_settings_fingerprint);
        //End added by jinlong.lu for XR6618444 on 18-8-1
        addIndex(SearchExt.class, NO_DATA_RES_ID, R.mipmap.ic_launcher_settings);//Added by miaoliu for XRP23522 on 2018/9/6
        //Begin added by junyan.wan.hz for XR 7108532 on 2018/11/16
        addIndex(TctTetherWifiSettings.class, NO_DATA_RES_ID, R.drawable.ic_settings_wireless);
        //End added by junyan.wan.hz for XR 7108532 on 2018/11/16
    }

    private SearchIndexableResources() {
    }

    public static int size() {
        return sResMap.size();
    }

    public static SearchIndexableResource getResourceByName(String className) {
        return sResMap.get(className);
    }

    public static Collection<SearchIndexableResource> values() {
        return sResMap.values();
    }
}