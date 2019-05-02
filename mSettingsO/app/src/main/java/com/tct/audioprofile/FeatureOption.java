/*
* [TCT-ROM][SoundEnhancement] Added by nanbing.zou for P23236 on 2018-09-05
* */

package com.tct.audioprofile;

import android.os.SystemProperties;

public class FeatureOption {
    public static final boolean MTK_NFC_ADDON_SUPPORT = getValue("ro.mtk_nfc_addon_support");
    public static final boolean MTK_DRM_APP = getValue("ro.mtk_oma_drm_support");
    public static final boolean MTK_CLEARMOTION_SUPPORT = getValue("ro.mtk_clearmotion_support");
    public static final boolean MTK_AGPS_APP = getValue("ro.mtk_agps_app");
    public static final boolean MTK_OMACP_SUPPORT = getValue("ro.mtk_omacp_support");
    public static final boolean MTK_GPS_SUPPORT = getValue("ro.mtk_gps_support");
    public static final boolean MTK_BG_POWER_SAVING_SUPPORT =
            getValue("ro.mtk_bg_power_saving_support");
    public static final boolean MTK_BG_POWER_SAVING_UI_SUPPORT =
            getValue("ro.mtk_bg_power_saving_ui");
    public static final boolean MTK_MIRAVISION_SETTING_SUPPORT =
            getValue("ro.mtk_miravision_support");
    public static final boolean MTK_PRODUCT_IS_TABLET =
            SystemProperties.get("ro.build.characteristics").equals("tablet");
    public static final boolean MTK_GMO_RAM_OPTIMIZE = getValue("ro.mtk_gmo_ram_optimize");
    public static final boolean MTK_C2K_SUPPORT = getValue("ro.boot.opt_c2k_support");
    public static final boolean MTK_A1_FEATURE = getValue("ro.mtk_a1_feature");
    public static final boolean MTK_SYSTEM_UPDATE_SUPPORT =
            getValue("ro.mtk_system_update_support");
    public static final boolean MTK_FOTA_ENTRY = getValue("ro.mtk_fota_entry");
    public static final boolean MTK_SCOMO_ENTRY = getValue("ro.mtk_scomo_entry");
    public static final boolean MTK_MDM_SCOMO = getValue("ro.mtk_mdm_scomo");
    public static final boolean MTK_MDM_FUMO = getValue("ro.mtk_mdm_fumo");
    public static final boolean MTK_AOD_SUPPORT = getValue("ro.mtk_aod_support");
    public static final boolean MTK_BESLOUDNESS_SUPPORT = getValue("ro.mtk_besloudness_support");
    public static final boolean MTK_BESSURROUND_SUPPORT = getValue("ro.mtk_bessurround_support");
    public static final boolean MTK_ANC_SUPPORT = getValue("ro.mtk_active_noise_cancel");
    public static final boolean MTK_HIFI_AUDIO_SUPPORT = getValue("ro.mtk_hifiaudio_support");
    public static final boolean MTK_WFD_SUPPORT = getValue("ro.mtk_wfd_support");
    public static final boolean MTK_EMMC_SUPPORT = getValue("ro.mtk_emmc_support");
    public static final boolean MTK_CACHE_MERGE_SUPPORT = getValue("ro.mtk_cache_merge_support");
    public static final boolean MTK_NAND_FTL_SUPPORT = getValue("ro.mtk_nand_ftl_support");
    public static final boolean MTK_UFS_BOOTING = getValue("ro.mtk_ufs_booting");
    public static final boolean MTK_MNTL_SUPPORT = getValue("ro.mntl_support");
    public static final boolean MTK_VOLTE_SUPPORT = getValue("persist.mtk_volte_support");
    public static final boolean MTK_WAPI_SUPPORT = getValue("ro.mtk_wapi_support");
    public static final boolean MTK_DEFAULT_WRITE_DISK = getValue("ro.mtk_default_write_disk");
    public static final boolean MTK_ST_NFC_GSMA_SUPPORT = getValue("persist.st_nfc_gsma_support");

    // Important!!!  the SystemProperties key's length must less than 31 , or will have JE
    /* get the key's value*/
    private static boolean getValue(String key) {
        return SystemProperties.get(key).equals("1");
    }
}
