package com.android.settings.sim.tct;

import android.content.Context;
import android.telephony.SubscriptionManager;
import android.database.Cursor;

public class TclDualSim {
    private final String TAG = "TclDualSim";

    //Begin added by zubai.li for dualsim Task5245955 on 2017.09.02
    /**
     * TelephonyProvider column name for the simtype of a SIM
     */
    public static final String SIM_ICON_TYPE = "sim_icon_type";

    public static final int SIM_ICON_TYPE_1 = 0;

    public static final int SIM_ICON_TYPE_2 = 1;

    public static final int SIM_ICON_TYPE_3 = 2;

    public static final int SIM_ICON_TYPE_4 = 3;

    public static final int SIM_ICON_TYPE_5 = 4;

    public static final int SIM_ICON_TYPE_6 = 5;

    public static final int SIM_ICON_TYPE_7 = 6;

    public static final int SIM_ICON_TYPE_8 = 7;

    public static final int SIM_ICON_TYPE_9 = 8;

    public static final int SIM_ICON_TYPE_DEFAULT = SIM_ICON_TYPE_1;

    public static final int SIM_ICON_TYPE_DEFAULT_SIM2 = SIM_ICON_TYPE_2;

    //End added by zubai.li for dualsim Task5245955 on 2017.09.02

    public static int getSimIconType(Context context, int slotIndex, String iccId){
        Cursor cursor = context.getContentResolver().query(SubscriptionManager.CONTENT_URI,
                null, SubscriptionManager.ICC_ID + "=?", new String[] {iccId}, null);
        try {
            if (cursor == null || !cursor.moveToFirst()) {
                if(slotIndex==0){
                    return SIM_ICON_TYPE_DEFAULT;
                } else {
                    return SIM_ICON_TYPE_DEFAULT_SIM2;
                }
            } else {
                int simType = cursor.getInt(cursor.getColumnIndexOrThrow(
                        SIM_ICON_TYPE));
                if(simType==SIM_ICON_TYPE_1){
                    if(slotIndex ==1){
                        simType = SIM_ICON_TYPE_2;
                    }
                } else if (simType==SIM_ICON_TYPE_2){
                    if(slotIndex ==0){
                        simType =  SIM_ICON_TYPE_1;
                    }
                }
                return simType;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
