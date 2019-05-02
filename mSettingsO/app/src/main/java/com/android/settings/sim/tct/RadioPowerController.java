package com.android.settings.sim.tct;

import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.ITelephony;
//Begin added by zubai.li for XR7080031 telecomcode on 2018/11/07
import com.android.internal.telephony.PhoneConstants;
//End added by zubai.li for XR7080031 telecomcode on 2018/11/07

/**
 * Radio power manager to control radio state.
 */
public class RadioPowerController {

    private static final String TAG = "RadioPowerController";
    private Context mContext;
    private static final int MODE_PHONE1_ONLY = 1;
    //bai
    //private ISimManagementExt mExt;
    //bai
    private static RadioPowerController sInstance = null;
    //Begin added by yangning.hong.hz for XR5896860 on 2018/01/25
    private static final String PROP_SWITCH_SUBID = "gsm.switch.subid";
    //End added by yangning.hong.hz for XR5896860 on 2018/01/25

   /**
    * Constructor.
    * @param context Context
    */
    private RadioPowerController(Context context) {
        mContext = context;
        //bai
        //mExt = UtilsExt.getSimManagmentExtPlugin(mContext);
        //bai
    }

    private static synchronized void createInstance(Context context) {
        if(sInstance == null) {
            sInstance = new RadioPowerController(context);
        }
    }

    public static RadioPowerController getInstance(Context context) {
        if(sInstance == null) {
            createInstance(context);
        }
        return sInstance;
    }

    //Modified by zubai.li for XR7080031 telecomcode on 2018/11/07
    // add paramter context by zubai.li
    public boolean setRadionOn(Context context, int subId, boolean turnOn) {
        Log.d(TAG, "setRadioOn, turnOn: " + turnOn + ", subId = " + subId);
        boolean isSuccessful = false;
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return isSuccessful;
        }

        //Begin added by yangning.hong.hz for XR5896860 on 2018/01/25
        //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
        /*
        int simId = SubscriptionManager.getPhoneId(subId);
        int otherSimId = simId == 0 ? 1 : 0;
        int otherSubId = TclInterfaceAdapter.getSubIdUsingPhoneId(mContext, otherSimId);
        */

        int slot1SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_1)[0];
        int slot2SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_2)[0];
        int otherSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

        if (subId == slot1SubId) {
            otherSubId = slot2SubId;
        } else if (subId == slot2SubId) {
            otherSubId = slot1SubId;
        }

        boolean isRadioOn = TclInterfaceAdapter.isRadioOn(otherSubId, mContext);
        int switchSubId = getSwitchedSubId(context);
        boolean mIsAirplaneModeOn = TclInterfaceAdapter.isAirplaneModeOn(mContext);
        Log.d(TAG, "setRadioOn, switchSubId: " + switchSubId + ", otherSubId:" + otherSubId + ", isRadioOn: " + isRadioOn);
        //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07

        if (!mIsAirplaneModeOn && (!isRadioOn) && (switchSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID)) {
            return isSuccessful;
        }
        //End added by yangning.hong.hz for XR5896860 on 2018/01/25

        ITelephony telephony = ITelephony.Stub.asInterface(ServiceManager.getService(
                Context.TELEPHONY_SERVICE));
        try {
            if (telephony != null) {
                isSuccessful = telephony.setRadioForSubscriber(subId, turnOn);
                if (isSuccessful) {
                    updateRadioMsimDb(subId, turnOn);
                    /// M: for plug-in
                    //bai mtk
                    //mExt.setRadioPowerState(subId, turnOn);
                    //bai mtk
                }
            } else {
                Log.d(TAG, "telephony is null");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "setRadionOn, isSuccessful: " + isSuccessful);
        return isSuccessful;
    }

    private void updateRadioMsimDb(int subId, boolean turnOn) {
        //bai mtk
        /*
        int priviousSimMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.MSIM_MODE_SETTING, -1);
        */
        int priviousSimMode = Settings.Global.getInt(mContext.getContentResolver(),
                "msim_mode_setting", -1);
        //bai mtk
        Log.i(TAG, "updateRadioMsimDb, The current dual sim mode is " + priviousSimMode
                + ", with subId = " + subId);
        int currentSimMode;
        boolean isPriviousRadioOn = false;
        //bai mtk
        /*
        int slot = SubscriptionManager.getSlotId(subId);
        */
        int slot = SubscriptionManager.getSlotIndex(subId);
        //bai mtk
        int modeSlot = MODE_PHONE1_ONLY << slot;
        if ((priviousSimMode & modeSlot) > 0) {
            currentSimMode = priviousSimMode & (~modeSlot);
            isPriviousRadioOn = true;
        } else {
            currentSimMode = priviousSimMode | modeSlot;
            isPriviousRadioOn = false;
        }

        Log.d(TAG, "currentSimMode=" + currentSimMode + " isPriviousRadioOn =" + isPriviousRadioOn
                + ", turnOn: " + turnOn);
        if (turnOn != isPriviousRadioOn) {
            //bai mtk
            /*
            Settings.Global.putInt(mContext.getContentResolver(),
                    Settings.Global.MSIM_MODE_SETTING, currentSimMode);
            */
            Settings.Global.putInt(mContext.getContentResolver(),
                    "msim_mode_setting", currentSimMode);
            //bai mtk
        } else {
            Log.w(TAG, "quickly click don't allow.");
        }
    }

    /**
     * whether radio switch finish on subId, according to the radio state.
     */
    public boolean isRadioSwitchComplete(final int subId) {
        if (!SubscriptionManager.isValidSubscriptionId(subId)) {
            return false;
        }
        //bai mtk
        /*
        int slotId = SubscriptionManager.getSlotId(subId);
        */
        int slotId = SubscriptionManager.getSlotIndex(subId);
        //bai mtk

        boolean isRadioOn = TclInterfaceAdapter.isRadioOn(subId, mContext);
        Log.d(TAG, "isRadioSwitchComplete: slot: " + slotId + ", isRadioOn: " + isRadioOn);
        if (!isRadioOn || (isExpectedRadioStateOn(slotId) && isRadioOn)) {
            return true;
        }
        return false;
    }

    public boolean isExpectedRadioStateOn(int slot) {
        //bai mtk
        /*
        int currentSimMode = Settings.Global.getInt(mContext.getContentResolver(),
                Settings.Global.MSIM_MODE_SETTING, -1);
         */
        int currentSimMode = Settings.Global.getInt(mContext.getContentResolver(),
                "msim_mode_setting", -1);
        //bai mtk

        boolean expectedRadioOn = (currentSimMode & (MODE_PHONE1_ONLY << slot)) != 0;
        Log.d(TAG, "isExpectedRadioStateOn: slot: " + slot + ", currentSimMode:" + currentSimMode +
                ", expectedRadioOn: " + expectedRadioOn);
        return expectedRadioOn;
    }


    //Begin added by yangning.hong.hz for XR5896860 on 2018/01/25
    public boolean isRadioOffBySimManagement(int subId) {
        boolean result = TclInterfaceAdapter.isRadioOffBySimManagement(mContext, subId);
        Log.d(TAG, "[isRadioOffBySimManagement]  subId " + subId + ", result = " + result);
        return result;
    }

    //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
    public void setSwitchedSubId(Context context, int mSubId) {
        /*
        SystemProperties.set(PROP_SWITCH_SUBID, String.valueOf(mSubId));
        */
        android.provider.Settings.System.putInt(context.getContentResolver(), PROP_SWITCH_SUBID, mSubId);
    }

    public int getSwitchedSubId(Context context) {
        /*
        return SystemProperties.getInt(PROP_SWITCH_SUBID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        */
        int subId = android.provider.Settings.System.getInt(context.getContentResolver(), PROP_SWITCH_SUBID, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        return subId;
    }
    //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07
    //End added by yangning.hong.hz for XR5896860 on 2018/01/25
}
