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

package com.android.settings.sim.tct;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.SystemProperties;
import android.provider.SearchIndexableResource;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.TelephonyProperties;

import java.util.ArrayList;
import java.util.List;

import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import com.android.internal.telephony.TelephonyIntents;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.support.v7.preference.PreferenceCategory;
import android.support.v7.preference.PreferenceViewHolder;

import com.android.internal.telephony.Phone;
import com.android.internal.telephony.uicc.SpnOverride;

import com.android.internal.telephony.PhoneConstants;

import android.telecom.PhoneAccount;

//import android.util.ResUtils;
import android.support.v14.preference.SwitchPreference;
import android.widget.Switch;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.CompoundButton;
import android.view.View;
import android.provider.Settings;
import android.telephony.SubscriptionInfo;
import com.android.internal.telephony.SubscriptionController;
//Added by zubai.li.hz for Task 4584435 on 2017/05/03 begin
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.widget.TextView;
import android.content.res.Configuration;
//End added by jiayi.wang for for XR5503715 on 2017/12/28
//Begin added by zubai.li for XR7101841 telecomcode on 2018/11/11
import android.os.Handler;
//End added by zubai.li for XR7101841 telecomcode on 2018/11/11
//Begin added by zubin.chen.hz for XR5858324 on 2018/01/26
import com.android.settings.R;
//Begin added by zubai.li for XR7108238 telecomcode on 2018/11/26
import android.os.AsyncTask;
//End added by zubai.li for XR7108238 telecomcode on 2018/11/26
//Begin added by zubai.li for XR7226036 telecomcode on 2018/12/21
import android.os.UserHandle;
//End added by zubai.li for XR7226036 telecomcode on 2018/12/21


/// M: for [SIM Radio On/Off]
// private class SimPreference extends Preference {
public class TclSimPreference extends SwitchPreference{
    private static final String TAG = "TclSimPreference";
    private SubscriptionInfo mSubInfoRecord;
    public int mSlotId;
    Context mContext;
    //Added by zubai.li for dualsim 2016.09.22 Task2963040 start
    private String[] mNetModeStrings;
    private String[] mNetModeArray;
    //Added by zubai.li for dualsim 2016.09.22 Task2963040 end

    /* add by quantai.zhu 10/19/16 for Task2963040 begin */
    private int mPreferenceNetworkMode = -1;
    /* add by quantai.zhu 10/19/16 for Task2963040 end   */
    private RadioPowerController mRadioController;

    //Added by zubai.li.hz for Task 4584435 on 2017/05/03 begin
    private AlertDialog mDialog = null;
    private static final String SIM_DISABLED = "sim_disabled";
    //Added by zubai.li.hz for Task 4584435 on 2017/05/03 end

    //Begin added by zubai.li for XR7101841 telecomcode on 2018/11/11
    private final Handler mUiHandler;
    //End added by zubai.li for XR7101841 telecomcode on 2018/11/11

    public TclSimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
        super(context);
        mContext = context;
        //Begin added by zubai.li for XR7101841 telecomcode on 2018/11/11
        mUiHandler = new Handler();
        //End added by zubai.li for XR7101841 telecomcode on 2018/11/11
        mSubInfoRecord = subInfoRecord;
        mSlotId = slotId;
        setKey("sim" + mSlotId);
        //Begin modified by wensen.luo for XR4646589 on 2017/08/01
        TclDualSimManagement.getInstance(mContext).resetPreferenceNetworkMode(mSlotId);
        TclDualSimManagement.getInstance(mContext).initModeStringsValues(mSlotId);
        //End modified by wensen.luo for XR4646589 on 2017/08/01

        /// M: for radio switch control
        mRadioController = RadioPowerController.getInstance(getContext());
        /// @}
        update();
    }

    public void update() {
        Log.d(TAG,"tclUpdate()++");
        final Resources res = mContext.getResources();
        setTitle(String.format(mContext.getResources()
                .getString(R.string.sim_editor_title), (mSlotId + 1)));
        /// M: for Plug-in
        customizeTclPreferenceTitle();

        //Begin modified by zubai.li.hz for dualsim ergo Plug-in XR4646589 on 2017/07/27

        String networkstring = res.getString(com.android.internal.R.string.unknownName);
        //String networkstring = ResUtils.getString(mContext, "unknownName", "");

        if (mSubInfoRecord != null) {
            int netmode = TclDualSimManagement.getInstance(mContext).getPreferredNetworkModeForSubId(mSlotId);

            //Begin added by wensen.luo for XR4646589 on 2017/07/31
            mPreferenceNetworkMode = TclDualSimManagement.getInstance(mContext).getPreferenceNetworkMode(mSlotId);
            Log.d(TAG,"mPreferenceNetworkMode = " + mPreferenceNetworkMode + " -mSlotId = " + mSlotId + "netmode = " + netmode);
            //End added by wensen.luo for XR4646589 on 2017/07/31

            if(mPreferenceNetworkMode != -1){
                netmode = mPreferenceNetworkMode;
            }
            //Begin added by wensen.luo for XR4646589 on 2017/07/31
            mNetModeArray = TclDualSimManagement.getInstance(mContext).getNetModeArray(mSlotId);
            mNetModeStrings = TclDualSimManagement.getInstance(mContext).getNetModeStrings(mSlotId);
            //End added by wensen.luo for XR4646589 on 2017/07/31

            for(int i=0;i<mNetModeArray.length;i++){
                if (Integer.valueOf(mNetModeArray[i]).intValue()==netmode) {
                    networkstring=mNetModeStrings[i];
                    break;
                }
            }
        }
        //End modified by zubai.li.hz for dualsim ergo Plug-in XR4646589 on 2017/07/27
        if (mSubInfoRecord != null) {
            //Begin modified by zubai.li.hz for dualsim ergo Plug-in XR4646589 on 2017/07/27
            final TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
            String operatorName = getOperatorName(tm, mSubInfoRecord);
            if (TextUtils.isEmpty(getPhoneNumber(mSubInfoRecord))) {
                //old:setSummary(mSubInfoRecord.getDisplayName());
                setSummary(getCustomSummary(operatorName, getPhoneNumber(mSubInfoRecord), networkstring));
            } else {
                    /*old:
                    setSummary(mSubInfoRecord.getDisplayName() + " - " +
                            PhoneNumberUtils.createTtsSpannable(getPhoneNumber(mSubInfoRecord)));
                    */
                setSummary(getCustomSummary(operatorName, getPhoneNumber(mSubInfoRecord), networkstring));

                setEnabled(true);
            }
            setIcon(new BitmapDrawable(res, (mSubInfoRecord.createIconBitmap(mContext))));
            //End modified by zubai.li.hz for dualsim ergo Plug-in XR4646589 on 2017/07/27
            /// M: add for radio on/off @{
            int subId = mSubInfoRecord.getSubscriptionId();
            boolean mIsAirplaneModeOn = TclInterfaceAdapter.isAirplaneModeOn(mContext);
            setTclRadioEnabled(!mIsAirplaneModeOn
                    && mRadioController.isRadioSwitchComplete(subId));
            if (mRadioController.isRadioSwitchComplete(subId)) {
                setTclRadionOn(TclInterfaceAdapter.isRadioOn(subId, getContext()));
            }
            //Begin added by zubin.chen.hz for XR5858324 on 2018/01/26
            //Begin modified by zubai.li for XR7072293 telecomcode on 2018/10/31
            if(TclInterfaceAdapter.isCellcomGuineaSimlock(mContext, mSlotId)){
            //End modified by zubai.li for XR7072293 telecomcode on 2018/10/31
            	if(mSlotId < 0){
            		Log.d(TAG, "cellcom guinea simlock isGuineaCompetitor mSlotId = " + mSlotId);
            		return;
            	}  
                Log.d(TAG, "cellcom guinea simlock isGuineaCompetitor mSlotId = " + mSlotId);
                if(TclInterfaceAdapter.isGuineaCompetitor(mContext,mSlotId)){
                    setEnabled(false);
                    setTclRadioEnabled(false);
                    setTclRadionOn(false);
                    if(mSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID){
                        //Begin added by zubai.li for XR7108238 telecomcode on 2018/11/26
                        //For QCOM
                        if(TclInterfaceAdapter.isQcomPlatform(mContext)){
                                SubscriptionManager subscriptionManager=SubscriptionManager.from(getContext());
                            if (subscriptionManager == null)  return;
                            int slot1SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_1)[0];
                            int slot2SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_2)[0];
                            int otherSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

                            if (mSubId == slot1SubId) {
                                otherSubId = slot2SubId;
                            } else if (mSubId == slot2SubId) {
                                otherSubId = slot1SubId;
                            }
                            int otherSimId = SubscriptionManager.getSlotIndex(otherSubId);
                            mIsChecked = false;
                            mOhterSimId = otherSimId;
                            handleUserRequest();
                            return;
                        }
                        //End added by zubai.li for XR7108238 telecomcode on 2018/11/26

                        //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
                        mRadioController.setRadionOn(getContext(), mSubId, false);
                        //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07
                        setCheckedFlag(mSubId, "disable");
                        setToChangeDefaultCallAndSms(false, mSubId);
                        //setToChangeDefaultData(false,mSubId);
                    }
                    Log.d(TAG, "cellcom guinea simlock isGuineaCompetitor mPowerState = " + mPowerState + " subid = " + mSubId);
                    return;
                }
            }
            //End added by zubin.chen.hz for XR5858324 on 2018/01/26  
            /// @}
        } else {
            //bai
            /*
            setSummary(R.string.sim_slot_empty);
            */
            setSummary(mContext.getResources().getString(R.string.sim_slot_empty));
            //bai
            setFragment(null);
            setEnabled(false);
        }
        Log.d(TAG,"tclUpdate() --");
    }

    public int getSlotId() {
        return mSlotId;
    }

    /**
     * only for plug-in, change "SIM" to "UIM/SIM".
     */
    private void customizeTclPreferenceTitle() {
        Log.d(TAG,"TclSimPreference customizeTclPreferenceTitle()");
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (mSubInfoRecord != null) {
            subId = mSubInfoRecord.getSubscriptionId();
        }
        setTitle(String.format(mContext.getResources().getString(R.string.sim_editor_title), (mSlotId + 1)));

        //Begin added by zubai.li.hz for dualsim ergo Plug-in XR4646589 on 2017/07/27
        if (mSubInfoRecord != null) {
            String title = customizeSimDisplayString(
                    mContext.getResources().getString(R.string.sim_card_number_title),
                    mSlotId, mSubInfoRecord);
            if(!TextUtils.isEmpty(title)){
                setTitle(title);
            }
        }
        //End added by zubai.li.hz for dualsim ergo Plug-in XR4646589 on 2017/07/27
    }

    // Returns the line1Number. Line1number should always be read from TelephonyManager since it can
    // be overridden for display purposes.
    private String getPhoneNumber(SubscriptionInfo info) {
        final TelephonyManager tm =
            (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        /* modify by quantai.zhu for Task2963040 at 2016/10/24 begin */
        //return tm.getLine1Number(info.getSubscriptionId());
        String phoneNumber=tm.getLine1Number(info.getSubscriptionId());
        Log.d(TAG,"phoneNumber: "+phoneNumber);
        if(TextUtils.isEmpty(phoneNumber)){
        	return info.getNumber();
        }else{
        	return phoneNumber;
        }
        /* modify by quantai.zhu for Task2963040 at 2016/10/24 end  */
    }



    private boolean mPowerState;
    private boolean mPowerEnabled = true;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private Switch mRadioSwith = null;

    /**
     * Set the radio switch state.
     * @param state On/off.
     */
    public void setTclRadionOn(boolean state) {
        Log.d(TAG, "setTclRadionOn " + state + " subId = " + mSubId);
        //Begin added by yangning.hong.hz for XR5896860 on 2018/01/25
        //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
        /*
        Log.d(TAG, "isTclRadioOffBySimManagement = " + mRadioController.isRadioOffBySimManagement(mSubId));
        if (!state && (!mRadioController.isRadioOffBySimManagement(mSubId))) {
            int switchSubId = mRadioController.getSwitchedSubId();
        */
        if (!state) {
            int switchSubId = mRadioController.getSwitchedSubId(getContext());
        //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07

            Log.d(TAG, "currentSudId=" + mSubId  + ", switchSubId =" + switchSubId);
            boolean mIsAirplaneModeOn = TclInterfaceAdapter.isAirplaneModeOn(getContext());
            if (!mIsAirplaneModeOn && switchSubId != SubscriptionManager.INVALID_SUBSCRIPTION_ID
                    && mSubId == switchSubId) {
                Log.d(TAG, "data capability switch");
                state =  true;
            }
        }
        //End added by yangning.hong.hz for XR5896860 on 2018/01/25
        mPowerState = state;
        if (mRadioSwith != null) {
            mRadioSwith.setChecked(state);
        }
    }

    /**
     * Set the radio switch enable state.
     * @param enable Enable.
     */
    public void setTclRadioEnabled(boolean enable) {
        //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
        Log.d(TAG,"setTclRadioEnabled() mRadioSwith:" + mRadioSwith + ", mPowerEnabled:" + mPowerEnabled + ", enable:" + enable);
        //mPowerEnabled = enable;
        if (mRadioSwith != null) {
            mPowerEnabled = enable;
        //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07
            mRadioSwith.setEnabled(enable);
            Log.d(TAG,"setTclRadioEnabled() setEnabled:" + enable);
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        mPowerEnabled = enabled;
        super.setEnabled(enabled);
    }

    /**
     * Bind the preference with corresponding property.
     * @param subId sub id for this preference
     * @param radioSwitchComplete radio switch complete or not
     */
    public void bindRadioPowerState(final int subId, boolean radioSwitchComplete) {
        mSubId = subId;
        if (radioSwitchComplete) {
            setTclRadionOn(TclInterfaceAdapter.isRadioOn(subId, getContext()));
            setTclRadioEnabled(SubscriptionManager.isValidSubscriptionId(subId));
        } else {
            setTclRadioEnabled(false);
            //bai
            /*
            setTclRadionOn(mController.isTclExpectedRadioStateOn(SubscriptionManager.getSlotId(subId)));
            */
            setTclRadionOn(mRadioController.isExpectedRadioStateOn(SubscriptionManager.getSlotIndex(subId)));
            //
        }
    }


    @Override
    protected void onClick() {
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        //Begin added by jiayi.wang for XR5503715 on 2017/12/28
        TextView summaryView = (TextView) view.findViewById(android.R.id.summary);
        Configuration cfg =mContext.getResources().getConfiguration();
        String localeLanguage = cfg.locale.getLanguage();
        if(localeLanguage.equals("ar") || localeLanguage.equals("fa")) {
            summaryView.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }
        //End added by jiayi.wang for XR5503715 on 2017/12/28

        //mRadioSwith = (Switch) view.findViewById(R.id.radio_state);
        mRadioSwith = (Switch) view.findViewById(com.android.internal.R.id.switch_widget);

        if (mRadioSwith != null) {
            Log.d(TAG, "onBindViewHolder mRadioSwith != null");
            //bai
            /*
            if (FeatureOption.MTK_A1_FEATURE) {
                mRadioSwith.setVisibility(View.GONE);
            }
            */
            //begin zubai for aosp without tcl plugin
            if(!TclInterfaceAdapter.hasTclPlugin(mContext)) {
                mRadioSwith.setVisibility(View.GONE);
            }
            //End zubai for aosp without tcl plugin

            mRadioSwith.setClickable(true);
            //Begin modified by zubai.li for XR7108238 telecomcode on 2018/11/26
            boolean isAirplaneModeOn = TclInterfaceAdapter.isAirplaneModeOn(mContext);
            mRadioSwith.setEnabled(mPowerEnabled && !isAirplaneModeOn);
            //End modified by zubai.li for XR7108238 telecomcode on 2018/11/26
            mRadioSwith.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    Log.d(TAG, "onCheckedChanged, mPowerState = " + mPowerState
                            + ", isChecked = " + isChecked + ", subId = " + mSubId);

                    //Added by zubai.li.hz for Task 4584435 on 2017/05/03 begin
                    SubscriptionManager subscriptionManager=SubscriptionManager.from(getContext());
                    if (subscriptionManager == null)  return;

                    //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
                    /*
                    int simId = subscriptionManager.getPhoneId(mSubId);
                    int otherSimId = simId == 0 ? 1 : 0;
                    int otherSubId = TclInterfaceAdapter.getSubIdUsingPhoneId(mContext, otherSimId);
                    */

                    int slot1SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_1)[0];
                    int slot2SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_2)[0];
                    int otherSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;

                    if (mSubId == slot1SubId) {
                        otherSubId = slot2SubId;
                    } else if (mSubId == slot2SubId) {
                        otherSubId = slot1SubId;
                    }
                    int otherSimId = SubscriptionManager.getSlotIndex(otherSubId);

                    String mSimDisabled = Settings.Global.getString(getContext().getContentResolver(),
                            /*Settings.Global.*/SIM_DISABLED +otherSubId);

                    boolean otherSubIsEnable = TclInterfaceAdapter.isRadioOn(otherSubId, getContext());
                    Log.d(TAG, "onCheckedChanged mSimDisabled = " + mSimDisabled + ", otherSubIsEnable:" + otherSubIsEnable
                            + ", otherSubId:" + otherSubId + ", otherSimId:" + otherSimId);

                    boolean mIsAirplaneModeOn = TclInterfaceAdapter.isAirplaneModeOn(getContext());
                    int otherSimState =  SubscriptionManager.getSimStateForSlotIndex(otherSimId);
                    Log.d(TAG, "onCheckedChanged otherSubId: " + otherSubId + ", SlotIsDisabled: " + mSimDisabled
                            + ", otherSimState: " + otherSimState);
                    if (mPowerState != isChecked && !isChecked  && (!mIsAirplaneModeOn)
                            && ("disable".equalsIgnoreCase(mSimDisabled) || otherSimState == TelephonyManager.SIM_STATE_ABSENT || !otherSubIsEnable)){
                        Log.d(TAG, "onCheckedChanged: can not disable all the SIMs");
                        mRadioSwith.setChecked(true);
                        popDialog();
                        return;
                    }
                    //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07
                    //Added by zubai.li.hz for Task 4584435 on 2017/05/03 end

                    if (mPowerState != isChecked) {
                        // Begin added by guoqi.zou for XR6319238 on 2018/05/16
                        toSaveCurrentState(isChecked);
                        // End added by guoqi.zou for XR6319238 on 2018/05/16

                        //Begin added by zubai.li for XR7108238 telecomcode on 2018/11/26
                        //For Qcom
                        if(TclInterfaceAdapter.isQcomPlatform(mContext)){
                            Log.d(TAG, "onCheckedChanged Qcom Platform Action");
                            mIsChecked = isChecked;
                            mOhterSimId = otherSimId;
                            handleUserRequest();
                            return;
                        }
                        //End added by zubai.li for XR7108238 telecomcode on 2018/11/26

                        //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
                        if (mRadioController.setRadionOn(getContext(), mSubId, isChecked)) {
                        //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07
                            // disable radio switch to prevent continuous click
                            Log.d(TAG, "onCheckedChanged mPowerState = " + isChecked);
                            mPowerState = isChecked;
                            setTclRadioEnabled(false);
                            //Begin added by yangning.hong.hz for XR5896860 on 2018/01/25
                            mRadioController.setSwitchedSubId(getContext(), SubscriptionManager.INVALID_SUBSCRIPTION_ID);//Modified by zubai.li for XR7080031 telecomcode on 2018/11/07
                            //End added by yangning.hong.hz for XR5896860 on 2018/01/25
                            //Added by zubin.chen.hz for Task4375025 on 2017/03/17 begin
                            Log.d(TAG, "onCheckedChanged: isChecked : " + isChecked
                                    + ", subId:" + mSubId);
                            if (!isChecked) {
                                //Added by zubai.li.hz for Task 4584435 on 2017/05/03 begin
                                setCheckedFlag(mSubId, "disable");
                                //Added by zubai.li.hz for Task 4584435 on 2017/05/03 end
                                setToChangeDefaultCallAndSms(isChecked, mSubId);
                                setToChangeDefaultData(isChecked,mSubId);
                            } else {
                                //Modified by zubai.li.hz for Task 4584435 on 2017/05/03 begin
                                setCheckedFlag(mSubId, "enable");
                                /*
                                SubscriptionManager subscriptionManager=SubscriptionManager.from(getContext());
                                int simId = subscriptionManager.getPhoneId(mSubId);
                                int otherSimId = simId == 0 ? 1 : 0;
                                */
                                //Modified by zubai.li.hz for Task 4584435 on 2017/05/03 end

                                Log.d(TAG,"otherSimId isRadioOn = " + TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(otherSimId)[0], getContext()));
                                if (!TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(otherSimId)[0], getContext())){
                                    setToChangeDefaultCallAndSms(isChecked, mSubId);
                                    setToChangeDefaultData(isChecked,mSubId);
                                // Begin added by guoqi.zou for XR6319238 on 2018/05/16
                                } else {
                                    autoChangeDefaultCallAndSmsToBoth(isChecked);
                                // End added by guoqi.zou for XR6319238 on 2018/05/16
                                }
                            }
                            //Added by zubin.chen.hz for Task4375025 on 2017/03/17 end
                            //Begin added by zubai.li for XR7101841 telecomcode on 2018/11/11
                            updateAfterClick();
                            //End added by zubai.li for XR7101841 telecomcode on 2018/11/11
                        } else {
                            // if set radio fail, revert button status.
                            Log.w(TAG, "set radio power FAIL!");
                            setTclRadionOn(!isChecked);
                        }
                    }
                }
            });
            // ensure setOnCheckedChangeListener before setChecked state, or the
            // expired OnCheckedChangeListener will be called, due to the view is RecyclerView
            //Begin added by zubai.li for XR7108238 telecomcode on 2018/11/26
            //For Qcom
            if(TclInterfaceAdapter.isQcomPlatform(mContext)){
                boolean state = TclInterfaceAdapter.isRadioOn(mSubId, getContext());
                Log.d(TAG, "onBindViewHolder mPowerState = " + mPowerState + " subid = " + mSubId + ", state:" + state);
                mRadioSwith.setChecked(/*mPowerState*/state);
                mPowerState = state;
                return;
            }
            //End added by zubai.li for XR7108238 telecomcode on 2018/11/26
            Log.d(TAG, "onBindViewHolder mPowerState = " + mPowerState + " subid = " + mSubId);
            mRadioSwith.setChecked(mPowerState);
        }
    }

    //Begin added by zubai.li for XR7101841 telecomcode on 2018/11/11
    public void updateAfterClick(){
        mUiHandler.postDelayed(mUpdateRunnable, 3000);
    }

    int count = 0;
    private final Runnable mUpdateRunnable = new Runnable() {
        public void run() {
            String mSimDisabled = Settings.Global.getString(getContext().getContentResolver(),
                    SIM_DISABLED +mSubId);
            boolean mSubIsEnable = TclInterfaceAdapter.isRadioOn(mSubId, getContext());
            Log.d(TAG, "mUpdateRunnable mSimDisabled = " + mSimDisabled + ", mSubIsEnable:" + mSubIsEnable
                    + ", mSubId:" + mSubId);
            //click disable, but the radio is still not off/ click enable, but the radio is still not on,try to update 10 time only
            if(("disable".equalsIgnoreCase(mSimDisabled) && mSubIsEnable && count < 10)
                || (("enable".equalsIgnoreCase(mSimDisabled) && !mSubIsEnable && count < 10))){
                updateAfterClick();
                count ++;
                Log.d(TAG, "mUpdateRunnable to update the UI count:" + count);
            }else{
                count = 0;
                Log.d(TAG, "mUpdateRunnable to update the UI update, count reset");
            }
            update();
        }
    };

    public void pause() {
        Log.d(TAG, "pause() to remove");
        mUiHandler.removeCallbacks(mUpdateRunnable);
    }
    //End added by zubai.li for XR7101841 telecomcode on 2018/11/11

    //Added by zubin.chen.hz for Task4375025 on 2017/03/17 begin
    /**
     * add to charge wether to change data service to other simslot when turn off/on the 
     * simslot from SIM card Management UI; switch data service , 3/4G capability , and data enable
     * state together
     */
    public void setToChangeDefaultData(boolean isChecked, int mSubId) {
        SubscriptionManager sm = SubscriptionManager.from(getContext());
        TelephonyManager tm = TelephonyManager.from(getContext());
        if (sm == null || tm == null) {
            return;
        }
        int currentSubId = mSubId;
        int defaultDatasubId = sm.getDefaultDataSubscriptionId();
        Log.d(TAG, " setToChangeDefaultData: currentSubId = " + currentSubId
                + " defaultDatasubId : "
                + defaultDatasubId);
        //bai
        /*
        int slot1SubId = sm.getSubIdUsingPhoneId(PhoneConstants.SIM_ID_1);
        int slot2SubId = sm.getSubIdUsingPhoneId(PhoneConstants.SIM_ID_2);
        */
        int slot1SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_1)[0];
        int slot2SubId = SubscriptionManager.getSubId(PhoneConstants.SIM_ID_2)[0];
        //bai
        int otherSubId = sm.INVALID_SUBSCRIPTION_ID;
        boolean beforeDataEnable = Settings.Global.getInt(getContext()
                .getContentResolver(), Settings.Global.MOBILE_DATA + defaultDatasubId, -1) == 1;
        if (currentSubId == slot1SubId) {
            otherSubId = slot2SubId;
        } else if (currentSubId == slot2SubId) {
            otherSubId = slot1SubId;
        }

        //Begin modified by yangning.hong.hz for XR6516404 on 2018/07/06
        //Begin modified by zubai.li for XR7072293 telecomcode on 2018/10/31
        boolean disabledData = TclInterfaceAdapter.isDisabledData(mContext);
        //End modified by zubai.li for XR7072293 telecomcode on 2018/10/31
        Log.d(TAG, " setToChangeDefaultData: otherSubId = " + otherSubId + " beforeDataEnable:"
                + beforeDataEnable + ", isChecked =" + isChecked + ", disabledData = " + disabledData);
        //End modified by yangning.hong.hz for XR6516404 on 2018/07/06

        if (!isChecked && (currentSubId == defaultDatasubId)
                && TclInterfaceAdapter.isRadioOn(otherSubId, getContext())) {
            Log.d(TAG, " setToChangeDefaultData: change defaut data sub when disable the default data slot with the other slot on ");
            sm.setDefaultDataSubId(otherSubId);
            //Begin added by yangning.hong.hz for XR5896860 on 2018/01/25
            mRadioController.setSwitchedSubId(getContext(), otherSubId);//Modified by zubai.li for XR7080031 telecomcode on 2018/11/07
            //End added by yangning.hong.hz for XR5896860 on 2018/01/25
            if (beforeDataEnable) {
                tm.setDataEnabled(currentSubId, false);
                //Begin modified by yangning.hong.hz for XR6516404 on 2018/07/06
                if (!disabledData) {
                    tm.setDataEnabled(otherSubId, true);
                }
                //End modified by yangning.hong.hz for XR6516404 on 2018/07/06
            }
        } else if (isChecked && (currentSubId != defaultDatasubId)) {
            Log.d(TAG, " setToChangeDefaultData: change defaut data sub when enable the un-default data slot when both slot off ");
            sm.setDefaultDataSubId(currentSubId);
            //Begin added by yangning.hong.hz for XR5896860 on 2018/01/25
            mRadioController.setSwitchedSubId(getContext(), currentSubId);//Modified by zubai.li for XR7080031 telecomcode on 2018/11/07
            //End added by yangning.hong.hz for XR5896860 on 2018/01/25
            if (beforeDataEnable) {
                tm.setDataEnabled(otherSubId, false);
                //Begin modified by yangning.hong.hz for XR6516404 on 2018/07/06
                if (!disabledData) {
                    tm.setDataEnabled(currentSubId, true);
                }
                //End modified by yangning.hong.hz for XR6516404 on 2018/07/06
            }
        }

    }

    private void setToChangeDefaultCallAndSms(boolean isChecked, int subId) {
        TelecomManager telecomManager = TelecomManager.from(getContext());
        SubscriptionManager subscriptionManager=SubscriptionManager.from(getContext());
        boolean radioSwitchOn = isChecked;
        if (telecomManager == null || telecomManager == null) {
            return;
        }
        PhoneAccountHandle phoneAccount = telecomManager.getUserSelectedOutgoingPhoneAccount();
        final List<PhoneAccountHandle> allPhoneAccounts = telecomManager
                    .getCallCapablePhoneAccounts();
            SubscriptionInfo sir = SubscriptionManager.from(getContext()).getDefaultSmsSubscriptionInfo();
            if (allPhoneAccounts != null && allPhoneAccounts.size() > 1) {
                //for (PhoneAccountHandle phoneAccountHandle : allPhoneAccounts) {
                    int simId = subscriptionManager.getPhoneId(subId);
                    int otherSimId = simId == 0 ? 1 : 0;
                    Log.d(TAG,"setToChangeDefaultCallAndSms subInfos size > 1, set to : SubId = " + SubscriptionManager.getSubId(otherSimId)[0]
                            + ", radioSwitchOn:" + radioSwitchOn);
                    if (!radioSwitchOn && TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(otherSimId)[0], getContext())){
                        Log.d(TAG,"setToChangeDefaultCallAndSms setupAccounts And default sms simId = " + otherSimId);
                        telecomManager.setUserSelectedOutgoingPhoneAccount(allPhoneAccounts.get(otherSimId));
                        subscriptionManager.setDefaultSmsSubId(SubscriptionManager.getSubId(otherSimId)[0]);  
                    } 
                    else if (radioSwitchOn) {
                    	telecomManager.setUserSelectedOutgoingPhoneAccount(allPhoneAccounts.get(simId));
                    	subscriptionManager.setDefaultSmsSubId(SubscriptionManager.getSubId(simId)[0]);  
                    }
                //}
           }
        //Begin added by zubai.li for XR7226036 telecomcode on 2018/12/21
        if (allPhoneAccounts != null && allPhoneAccounts.size() == 1 && !radioSwitchOn) {
            int simId = subscriptionManager.getPhoneId(subId);
            int otherSimId = simId == 0 ? 1 : 0;
            int otherSubId = SubscriptionManager.getSubId(otherSimId)[0];
            Log.d(TAG,"setToChangeDefaultCallAndSms subInfos size == 1, set to : otherSubId = " + otherSubId
                    + ", radioSwitchOn:" + radioSwitchOn);
            if (TclInterfaceAdapter.isRadioOn(otherSubId, getContext())){
                Log.d(TAG,"setToChangeDefaultCallAndSms set default sms simId = " + otherSimId);
                //set sms only, not need to set call as only have one call capable account.
                subscriptionManager.setDefaultSmsSubId(otherSubId);
            }
        }
        //End added by zubai.li for XR7226036 telecomcode on 2018/12/21
    }
    //Added by zubin.chen.hz for Task4375025 on 2017/03/17 end

    //Added by zubai.li.hz for Task 4584435 on 2017/05/03 begin
    public void popDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
        alertDialogBuilder.setTitle(mContext.getString(R.string.error));
        alertDialogBuilder.setMessage(mContext.getString(R.string.sim_slot_checked));
        alertDialogBuilder.setNegativeButton(mContext.getString(com.android.internal.R.string.ok),new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                mDialog.dismiss();
            }
        });

        mDialog = alertDialogBuilder.create();
        mDialog.show();
    }

    public void setCheckedFlag(int mSubId, String flag) {
        Settings.Global.putString(getContext().getContentResolver(), /*Settings.Global.*/SIM_DISABLED + mSubId, flag);
    }
    //Added by zubai.li.hz for Task 4584435 on 2017/05/03 end
    

    public static String customizeSimDisplayString (String title, int slotId, SubscriptionInfo subInfoRecord){
        if (subInfoRecord == null) {
            return "";
        }
        return String.format(title, (slotId + 1)) + " (" + subInfoRecord.getDisplayName() + ")";//Modified by miaoliu for XR7139980 on 2018/12/03
    }

    public String getOperatorName (TelephonyManager telephonyManager, SubscriptionInfo sir){
        //Begin added by ruihua.zhang.hz for XR6425227 on 2018/06/26
        String simIMSI = telephonyManager.getSubscriberId(sir.getSubscriptionId());
        Log.d(TAG, "getOperatorName, simIMSI=" + simIMSI);
        if (simIMSI != null && simIMSI.startsWith("206018014")) {
            return "UPC AT";
        }
        //End added by ruihua.zhang.hz for XR6425227 on 2018/06/26

        String operatorName = telephonyManager.getSimOperatorName(sir.getSubscriptionId());
        Log.d(TAG,"getOperatorName, spn = " + operatorName);

        if (TextUtils.isEmpty(operatorName)) {
            operatorName = TelephonyManager.getDefault().getNetworkOperatorName(sir.getSubscriptionId());
            int mSlotId = sir.getSimSlotIndex();
            if (TextUtils.isEmpty(operatorName)) {
                String operator = TelephonyManager.getDefault().getSimOperator(sir.getSubscriptionId());
                //SpnOverride mSpnOverride = new SpnOverride();
                //MtkSpnOverride mSpnOverride = MtkSpnOverride.getInstance();
                String CarrierName = operator;
                /*
                if (mSpnOverride.containsCarrier(CarrierName)) {
                    operatorName = mSpnOverride.getSpn(CarrierName);
                } else {
                    operatorName = "";
                }
                */
                if (TclInterfaceAdapter.containsCarrier(mContext, operator)) {
                    operatorName = TclInterfaceAdapter.getSpn(mContext, operator);
                } else {
                    operatorName = "";
                }
            }
        }
        return operatorName;
    }


    public String getCustomSummary (String operatorName, String number, String networkstring){

        //Begin added by yangning.hong.hz for XR6335032 on 2018/05/22
        if (mContext.getResources().getBoolean(R.bool.def_settings_hide_MSISDN)) {
            number = "";
        }
        //End added by yangning.hong.hz for XR6335032 on 2018/05/22

        if(TextUtils.isEmpty(number)){
            if(TextUtils.isEmpty(operatorName)){
                return networkstring;
            }else{
                return operatorName + "\n" + networkstring;
            }
        }else{
            if(TextUtils.isEmpty(operatorName)){
                if(TextUtils.isEmpty(networkstring)){
                    return number;
                }else{
                    return number + "\n" + networkstring;
                }
            }else{
                return operatorName + " - " + number + "\n" + networkstring;
            }
    	}
    }

    // Begin added by guoqi.zou for XR6319238 on 2018/05/16
    private static String SELETED_STATE = "selected_state";
    private static String KEY_SMS_STATE = "key_sms_state";
    private static String KEY_CALL_STATE = "key_call_state";
    public static final int ASK_USER_SUB_ID = -2; //copy from SimDialogActivity
    private void toSaveCurrentState(boolean isChecked){
        if(isChecked){
            return;
        }

        int smsSubId = SubscriptionManager.getDefaultSmsSubscriptionId();
        TelecomManager telecomManager = TelecomManager.from(getContext());
        TelephonyManager telephonyManager = TelephonyManager.from(getContext());

        if (telecomManager == null || telephonyManager == null) {
            return;
        }
        PhoneAccountHandle phoneAccount = telecomManager.getUserSelectedOutgoingPhoneAccount();
        int callSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if(phoneAccount != null){
            callSubId = telephonyManager.getSubIdForPhoneAccount(telecomManager.getPhoneAccount(phoneAccount));
        }

        Log.d(TAG,"toSaveCurrentState, smsId = " + smsSubId + ", callId=" + callSubId);
        SharedPreferences sharedPreferences = getContext().getSharedPreferences(SELETED_STATE, Context.MODE_MULTI_PROCESS);
        Editor edit = sharedPreferences.edit();
        edit.putInt(KEY_SMS_STATE, smsSubId);
        edit.commit();
        edit.putInt(KEY_CALL_STATE, callSubId);
        edit.commit();
    }

    private void autoChangeDefaultCallAndSmsToBoth(boolean isChecked) {
        TelecomManager telecomManager = TelecomManager.from(getContext());
        SubscriptionManager subscriptionManager = SubscriptionManager.from(getContext());

        if (telecomManager == null || subscriptionManager == null || !isChecked) {
            return;
        }

        final List<PhoneAccountHandle> allPhoneAccounts = telecomManager
                    .getCallCapablePhoneAccounts();
        if (allPhoneAccounts != null && allPhoneAccounts.size() > 1) {
            SharedPreferences sharedPreferences = getContext().getSharedPreferences(SELETED_STATE, Context.MODE_MULTI_PROCESS);
            int smsSubId = sharedPreferences.getInt(KEY_SMS_STATE, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            int callSubId = sharedPreferences.getInt(KEY_CALL_STATE, SubscriptionManager.INVALID_SUBSCRIPTION_ID);
            Log.d(TAG,"autoChangeDefaultCallAndSmsToBoth subInfos size > 1, smsId = " + smsSubId + ", callId=" + callSubId);
            if(SubscriptionManager.INVALID_SUBSCRIPTION_ID == callSubId){
                telecomManager.setUserSelectedOutgoingPhoneAccount(null);
            }
            if(ASK_USER_SUB_ID == smsSubId || SubscriptionManager.INVALID_SUBSCRIPTION_ID == smsSubId){
                subscriptionManager.setDefaultSmsSubId(smsSubId);
            }
        }
    }
    // End added by guoqi.zou for XR6319238 on 2018/05/16

    //Begin added by zubai.li for XR7108238 telecomcode on 2018/11/26
    //For Qcom mRadioSwith
    private boolean mIsChecked;
    private int mOhterSimId = -1;
    // These are the list of  possible values that
    private static final int PROVISIONED = 1;
    private static final int NOT_PROVISIONED = 0;

    // This internal method called when user changes preference from UI
    // 1. For activation/deactivation request from User, if device in APM mode
    //    OR if voice call active on any SIM it dispay error dialog and returns.
    // 2. For deactivation request it returns error dialog if only one SUB in
    //    active state.
    // 3. In other cases it sends user request to framework.
    synchronized private void handleUserRequest() {
        Log.d(TAG, "handleUserRequest");
        sendUiccProvisioningRequest();
    }

    private void sendUiccProvisioningRequest() {
        if (!mRadioSwith.isEnabled()) {
            return;
        }
        new SimEnablerDisabler().execute();
    }

    private static final int REQUEST_SUCCESS = 0;
    private class SimEnablerDisabler extends AsyncTask<Void, Void, Integer> {

        int newProvisionedState = NOT_PROVISIONED;
        String mOldSimDisabled = "";

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //setEnabled(false);
            // disable radio switch to prevent continuous click
            mOldSimDisabled = Settings.Global.getString(getContext().getContentResolver(),
                    /*Settings.Global.*/SIM_DISABLED +mSubId);
            Log.d(TAG, "onCheckedChanged mPowerState = " + mIsChecked + ", mOldSimDisabled:" + mOldSimDisabled);
            mPowerState = mIsChecked;
            setTclRadioEnabled(false);
            if (!mIsChecked) {
                Log.w(TAG, "onPreExecute disabled");
                setCheckedFlag(mSubId, "disable");
            } else {
                Log.w(TAG, "onPreExecute enabled");
                setCheckedFlag(mSubId, "enable");
            }
        }

        @Override
        protected Integer doInBackground(Void... params) {
            int result = -1;
            newProvisionedState = NOT_PROVISIONED;
            try {
                if (mIsChecked) {
                    result = TclInterfaceAdapter.activateUiccCard(mContext, mSubInfoRecord.getSimSlotIndex());
                    newProvisionedState = PROVISIONED;
                } else {
                    result = TclInterfaceAdapter.deactivateUiccCard(mContext, mSubInfoRecord.getSimSlotIndex());
                }
            } catch (Exception ex) {
                Log.d(TAG,"Activate  sub failed " + result + " phoneId " + mSubInfoRecord.getSimSlotIndex());
            }
            Log.w(TAG, "doInBackground mIsChecked:" + mIsChecked + ", result:" + result);
            return result;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if(result != REQUEST_SUCCESS){
                Log.w(TAG, "set radio power FAIL! mOldSimDisabled:" + mOldSimDisabled);
                setCheckedFlag(mSubId, mOldSimDisabled);
                setTclRadionOn(!mIsChecked);
                update();
                return;
            }
            if (!mIsChecked) {
                Log.w(TAG, "onPostExecute disabled");
                setToChangeDefaultCallAndSms(mIsChecked, mSubId);
                setToChangeDefaultData(mIsChecked,mSubId);
            } else {
                Log.d(TAG,"onPostExecute enabled, otherSimId isRadioOn = " + TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(mOhterSimId)[0], getContext()));
                if (!TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(mOhterSimId)[0], getContext())){
                    setToChangeDefaultCallAndSms(mIsChecked, mSubId);
                    setToChangeDefaultData(mIsChecked,mSubId);
                } else {
                    autoChangeDefaultCallAndSmsToBoth(mIsChecked);
                }
            }
            update();
            //Begin added by zubai.li for XR7226036 telecomcode on 2018/12/21
            Intent intent = new Intent("com.tct.intent.action.RADIO_STATE_CHANGED");
            intent.putExtra("radioState", mIsChecked);
            mContext.sendBroadcastAsUser(intent, UserHandle.ALL);
            //End added by zubai.li for XR7226036 telecomcode on 2018/12/21

        }
    }
    //End added by zubai.li for XR7108238 telecomcode on 2018/11/26
}
