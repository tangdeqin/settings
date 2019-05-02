/*
 * Copyright (C) 2015 The Android Open Source Project
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

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.ServiceManager;
import android.telephony.RadioAccessFamily;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.graphics.Canvas;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.DisplayMetrics;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap;
import android.content.ContentResolver;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.content.res.Configuration;
import android.database.Cursor;
//import android.util.ResUtils;
import com.android.internal.telephony.ITelephony;

import android.view.inputmethod.InputMethodManager;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.Phone;
//Begin added by yuzhao.liang for XR6006692 telecomcode on 2018/04/12
import android.os.RemoteException;
//End added by yuzhao.liang for XR6006692 telecomcode on 2018/04/12
import com.android.settings.R;

public class TctSimPreferenceDialog {
    private Context mContext;
    private SubscriptionInfo mSubInfoRecord;
    private int mSlotId;
    private int[] mTintArr;
    private String[] mColorStrings;
    private int mTintSelectorPos;
    private SubscriptionManager mSubscriptionManager;
    AlertDialog.Builder mBuilder;
    View mDialogLayout;
    private final String SIM_NAME = "sim_name";
    private final String TINT_POS = "tint_pos";
    /* ADD-BEGIN by Dingyi for dualsim 2016/08/03 FR 2655692*/
    private int[] mTypeArr;
    private int[] mTypeArrAll;
    private String[] mTypeStrings;
    private int mTypeSelectorPos;
    private TypedArray mTypedArray;
    private final String TYPE_POS = "type_pos";
    private int[] mNetModeArr;
    private String[] mNetModeStrings;
    private String[] mNetModeArray;
    private int mNetModeSelectorPos;
    private final String NETMODE_POS = "netmode_pos";
    private final String SIM_NUMBER = "sim_number";
    private Phone mPhone;
    private int preferredNetworkMode = Phone.PREFERRED_NT_MODE;
    private final int NUM = 8;
    /* ADD-END by Dingyi for dualsim 2016/08/03 FR 2655692*/
    private boolean isMainCard; //Added by quan.luo for XR5503803 on 2018/01/13
    private boolean isHighLight;//Added by quan.luo for XR6179838 on 2018/04/16
    private final static String TAG = "TctSimPreferenceDialog";
    private TclDualSimManagement mTclDualSimManagement; //Added by ruihua.zhang.hz for XR6099966 on 2018/03/15

    //Begin added by yangning.hong.hz for SIMLock Data Plug-in XR4646589 on 2017/07/14
    //private IJrdSimSettingsExt mJrdSimSettingsExt;  //bai mtk
    //End added by yangning.hong.hz for SIMLock Data Plug-in XR4646589 on 2017/07/14

    public void showdialog(Context context, int slod, String[] netModeStrings, String[] netModeArray) {
        mTclDualSimManagement = TclDualSimManagement.getInstance(context); //Added by ruihua.zhang.hz for XR6099966 on 2018/03/15
        mContext = context;
        mSlotId = slod;
        mSubscriptionManager = SubscriptionManager.from(mContext);
        mSubInfoRecord = mSubscriptionManager.getActiveSubscriptionInfoForSimSlotIndex(mSlotId);

        //begin zhixiong.liu.hz for P23123  20180821
        mTintArr = mContext.getResources().getIntArray(R.array.sim_colors);

        mColorStrings = mContext.getResources().getStringArray(R.array.tcl_color_picker);

        //if(TclInterfaceAdapter.hasTclPlugin(mContext)){
        //    mTintArr = mContext.getResources().getIntArray(/*com.android.internal.*/R.array.sim_colors);
        //    mColorStrings = mContext.getResources().getStringArray(R.array.tcl_color_picker);
        //}
        //end zhixiong.liu.hz for P23123  20180821
        mTintSelectorPos = 0;

        isMainCard = true; // Added by quan.luo for XR5503803 on 2018/01/13
        isHighLight = true;// Added by quan.luo for XR6179838 on 2018/04/16

        //mTypeArrAll = mContext.getResources().getIntArray(com.android.internal.R.array.sim_icontype_index);
        mTypeArrAll = mContext.getResources().getIntArray(R.array.sim_icontype_index);

        mTypeArr = new int[NUM];
        int j = 0;
        if (mSlotId == 0) {
            for (int i = 0; i < mTypeArrAll.length; i++) {
                if (i == 1) {
                    continue;
                }
                mTypeArr[j++] = mTypeArrAll[i];
            }
        } else {
            for (int i = 0; i < mTypeArrAll.length; i++) {
                if (i == 0) {
                    continue;
                }
                mTypeArr[j++] = mTypeArrAll[i];
            }
        }

        mTypeStrings = mContext.getResources().getStringArray(R.array.icontype_picker);
        mTypeSelectorPos = 0;
        //mTypedArray = mContext.getResources().obtainTypedArray(com.android.internal.R.array.sim_icontype_drawable);
        mTypedArray = mContext.getResources().obtainTypedArray(R.array.sim_icontype_drawable);

        //old mNetModeStrings = mContext.getResources().getStringArray(R.array.preferred_network_mode_choices_custom_default);
        mNetModeSelectorPos = 0;
        //old mNetModeArray = mContext.getResources().getStringArray(R.array.preferred_network_mode_values_custom_default);
        /* ADD-END by Dingyi for dualsim 2016/08/03 FR 2655692*/
        
        /* add by quantai.zhu at 2016/10/19 for Task2963040 begin */
        mNetModeStrings = netModeStrings;
        mNetModeArray = netModeArray;
        /* add by quantai.zhu at 2016/10/19 for Task2963040 end */
        
        mBuilder = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = (LayoutInflater)mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mDialogLayout = inflater.inflate(R.layout.tcl_multi_sim_dialog, null);
        mBuilder.setView(mDialogLayout);

        createEditDialog();
    }

    /* ADD-BEGIN by Dingyi for dualsim 2016/08/03 FR 2655692*/
    //Get preferred network mode based on subId
    private int getPreferredNetworkModeForSubId() {
        int[] sId = SubscriptionManager.getSubId(mSlotId);
        final int subId = sId[0];
        int nwMode;

        nwMode = android.provider.Settings.Global.getInt(
                mContext.getContentResolver(),
                android.provider.Settings.Global.PREFERRED_NETWORK_MODE + subId,
                preferredNetworkMode);
        Log.e("TctSimPreferenceDialog","getPreferredNetworkModeForSubId: phoneNwMode = " + nwMode +
                " subId = "+ subId);
        return nwMode;
    }

    private String getOperatorName(){
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        String simCarrierName = tm.getSimOperatorName(mSubInfoRecord
                .getSubscriptionId());

        //get carrier name from sim
        if(TextUtils.isEmpty(simCarrierName)) {
            simCarrierName = TelephonyManager.getDefault().getNetworkOperatorName(
                    mSubInfoRecord.getSubscriptionId());
            if (TextUtils.isEmpty(simCarrierName)) {
                String operator = TelephonyManager.getDefault().getSimOperator(
                        mSubInfoRecord.getSubscriptionId());
                if (TclInterfaceAdapter.containsCarrier(mContext, operator)) {
                    simCarrierName = TclInterfaceAdapter.getSpn(mContext, operator);
                } else {
                    simCarrierName = mContext.getResources().getString(
                            R.string.sim_card_number_title, mSlotId + 1);
                }
            }
        }
        return simCarrierName;
    }
    /* ADD-END by Dingyi for dualsim 2016/08/03 FR 2655692*/

    private void createEditDialog() {
        final Resources res = mContext.getResources();
        EditText nameText = (EditText)mDialogLayout.findViewById(R.id.sim_name);
        nameText.setText(mSubInfoRecord.getDisplayName());
        nameText.setSelection(nameText.getText().length());//Task3017341 merge by zubai.li from Defect1461218 2016.10.11

        /* ADD-BEGIN by Dingyi for dualsim 2016/08/03 FR 2655692*/
        String simCarrierName = getOperatorName();
        nameText.setHint(simCarrierName);
        /* ADD-END by Dingyi for dualsim 2016/08/03 FR 2655692*/

        final Spinner tintSpinner = (Spinner) mDialogLayout.findViewById(R.id.spinner);
        SelectColorAdapter adapter = new SelectColorAdapter(mContext,
                R.layout.tcl_settings_color_picker_item, mColorStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tintSpinner.setAdapter(adapter);

        for (int i = 0; i < mTintArr.length; i++) {
            if (mTintArr[i] == mSubInfoRecord.getIconTint()) {
                tintSpinner.setSelection(i);
                mTintSelectorPos = i;
                break;
            }
        }

        //bai
        final Spinner typeSpinner = (Spinner) mDialogLayout.findViewById(R.id.icon_spinner);
        SelectTypeAdapter typeadapter = new SelectTypeAdapter(mContext,
                R.layout.tcl_settings_icon_type_picker_item, mTypeStrings);
        typeadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeadapter);

        tintSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id){
                tintSpinner.setSelection(pos);
                mTintSelectorPos = pos;
                typeadapter.notifyDataSetChanged();//bai

                //Begin added by shuangwu.liu for 6020373 on 2018/03/05
                //使nameText强制调用软键盘
                nameText.setFocusable(true);
                nameText.setFocusableInTouchMode(true);
                nameText.requestFocus();
                InputMethodManager inputManager = (InputMethodManager)nameText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(nameText,0);
                //End added by shuangwu.liu for 6020373 on 2018/03/05
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        /* ADD-BEGIN by Dingyi for dualsim 2016/08/03 FR 2655692*/
        /*
        //icon type
        final Spinner typeSpinner = (Spinner) mDialogLayout.findViewById(R.id.icon_spinner);
        SelectTypeAdapter typeadapter = new SelectTypeAdapter(mContext,
                R.layout.tcl_settings_icon_type_picker_item, mTypeStrings);
        typeadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeadapter);
        */

        //Begin modified by zubai.li for XR6998744 on 2018/09/21
        int defaultSimType = mSubInfoRecord.getSimSlotIndex() == 0? 0: 1;
        int simIconType = android.provider.Settings.System.getInt(mContext.getContentResolver(), mSubInfoRecord.getIccId(), defaultSimType);
        /*
        if(TclInterfaceAdapter.hasTclPlugin(mContext)) {
            simIconType = TclDualSimManagement.getInstance(mContext).getSimIconType(mSubInfoRecord.getSimSlotIndex(), mSubInfoRecord.getIccId());
        }
        */
        //End modified by zubai.li for XR6998744 on 2018/09/21

        for (int i = 0; i < mTypeArr.length; i++) {
            if (mTypeArr[i] == /*mSubInfoRecord.getSimIconType(mContext)*/simIconType) {
                typeSpinner.setSelection(i);
                mTypeSelectorPos = i;
                break;
            }
        }
        Log.w("bai","getPreferredNetworkModeForSubId: mSlotId = " + mSlotId + ", simIconType:"+ simIconType 
                + " mTypeSelectorPos:" + mTypeSelectorPos +  "\n mSubInfoRecord = "+ mSubInfoRecord);

        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id){
                typeSpinner.setSelection(pos);
                mTypeSelectorPos = pos;

                //Begin added by shuangwu.liu for 6020373 on 2018/03/05
                //使nameText强制调用软键盘
                nameText.setFocusable(true);
                nameText.setFocusableInTouchMode(true);
                nameText.requestFocus();
                InputMethodManager inputManager = (InputMethodManager)nameText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(nameText,0);
                //End added by shuangwu.liu for 6020373 on 2018/03/05
                
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //network mode
        final Spinner netModeSpinner = (Spinner) mDialogLayout.findViewById(R.id.network_mode_spinner);
        SelectNetWorkModeAdapter netmodeadapter = new SelectNetWorkModeAdapter(mContext,
                R.layout.tcl_settings_network_mode_picker_item, mNetModeStrings);
        netmodeadapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        netModeSpinner.setAdapter(netmodeadapter);

        //Begin added by quan.luo for XR5503803 on 2018/01/13
        int subId = mSubInfoRecord.getSubscriptionId();
        int phoneId = SubscriptionManager.getPhoneId(subId);
        int mainPhoneId = TclInterfaceAdapter.getMainPhoneId(mContext);
        //Begin modified by quan.luo for XR6179838 on 2018/04/16
        int currRat = TclInterfaceAdapter.getRadioAccessFamily(phoneId,mContext);
        if (mainPhoneId != phoneId) {
            isMainCard = false;
            if (((currRat & RadioAccessFamily.RAF_LTE) == RadioAccessFamily.RAF_LTE)
                    || ((currRat & RadioAccessFamily.RAF_UMTS) == RadioAccessFamily.RAF_UMTS) ) {
                netModeSpinner.setEnabled(true);
                isHighLight = true;
            } else {
                netModeSpinner.setEnabled(false);
                isHighLight = false;
                //Begin added by ruihua.zhang.hz for XR6099966 on 2018/03/14
                //Begin modified by zubai.li for XR7072293 telecomcode on 2018/10/31
                if (TclInterfaceAdapter.isSeparate3G4G(mContext)) {
                //End modified by zubai.li for XR7072293 telecomcode on 2018/10/31
                    netModeSpinner.setEnabled(true);
                    isHighLight = true;
                }
                //Begin added by ruihua.zhang.hz for XR6099966 on 2018/03/14
            }
        } else {
            isMainCard = true;
            isHighLight = true;
            netModeSpinner.setEnabled(true);
        }
        //End modified by quan.luo for XR6179838 on 2018/04/16
        //End added by quan.luo for XR5503803 on 2018/01/13
        //Begin added by zubin.chen.hz SML REQ for XR5346254 on 2017/09/19
        //for Guinea SML REQ
        if (TclInterfaceAdapter.isGuineaSimlock(mContext, mSlotId)) {
            netModeSpinner.setEnabled(false);
            TelephonyManager telephony = TelephonyManager.getDefault();
            if (Phone.NT_MODE_GSM_ONLY != getPreferredNetworkModeForSubId()) {
                Log.d(TAG, "hyn guinea SIM LOCK set mode to GSM_ONLY");
                telephony.setPreferredNetworkType(mSubInfoRecord.getSubscriptionId(), Phone.NT_MODE_GSM_ONLY);
            }
        }
        //for Orange SML REQ
        else if(TclInterfaceAdapter.isOrangeSimlock(mContext, mSlotId)){
            netModeSpinner.setEnabled(false);
            TelephonyManager telephony = TelephonyManager.getDefault();
            if (Phone.NT_MODE_GSM_ONLY != getPreferredNetworkModeForSubId()) {
                Log.d(TAG, "hyn Orange SIM LOCK set mode to GSM_ONLY");
                telephony.setPreferredNetworkType(mSubInfoRecord.getSubscriptionId(), Phone.NT_MODE_GSM_ONLY);
            }
        }
        //for kyivstar-ukraine SML REQ
        //Begin modified by zubai.li for XR7072293 telecomcode on 2018/10/31
        else if (TclInterfaceAdapter.isUkraineSimlock(mContext, mSlotId)) {
        //End modified by zubai.li for XR7072293 telecomcode on 2018/10/31
            if (mSlotId == PhoneConstants.SIM_ID_2) {
                netModeSpinner.setEnabled(false);
            }
        }
        //for Globe SML REQ
        else if (TclInterfaceAdapter.isNeedHideSimInfo(mContext, mSlotId)){
            netModeSpinner.setEnabled(false);
        }
        //End added by zubin.chen.hz SML REQ for XR5346254 on 2017/09/19
        
        int currentmode = getPreferredNetworkModeForSubId();

        //Begin added by ruihua.zhang.hz for XR6099966 on 2018/03/15
        Log.d(TAG, "[ZRH] Before currentmode=" + currentmode + ", mSlotId=" + mSlotId);
        currentmode = getRealPreferenceNetworkMode(mSlotId, currentmode);
        Log.d(TAG, "[ZRH] After currentmode=" + currentmode + ", mSlotId=" + mSlotId);
        //End added by ruihua.zhang.hz for XR6099966 on 2018/03/15

        //Begin added by yangning.hong.hz for SIMLock Data Plug-in XR4646589 on 2017/07/14
        //mJrdSimSettingsExt.setNetModeSpinnerEnabled(netModeSpinner, mSlotId, mSubInfoRecord.getSubscriptionId(), currentmode);  //bai mtk
        //End added by yangning.hong.hz for SIMLock Data Plug-in XR4646589 on 2017/07/14

        for (int i = 0; i < mNetModeArray.length; i++) {
            if (Integer.valueOf(mNetModeArray[i]).intValue()==currentmode) {
                netModeSpinner.setSelection(i);
                mNetModeSelectorPos = i;
                break;
            }
        }

        netModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id){
                netModeSpinner.setSelection(pos);
                mNetModeSelectorPos = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        //number
//        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
//                Context.TELEPHONY_SERVICE);
//        TextView numberView = (TextView)mDialogLayout.findViewById(R.id.number);
//        final String rawNumber =  tm.getLine1Number(mSubInfoRecord.getSubscriptionId());
//        if (TextUtils.isEmpty(rawNumber)) {
//            numberView.setText(res.getString(com.android.internal.R.string.unknownName));
//        } else {
//            numberView.setText(PhoneNumberUtils.formatNumber(rawNumber));
//        }
        final TelephonyManager tm = (TelephonyManager) mContext.getSystemService(
                Context.TELEPHONY_SERVICE);
        //int subId = mSubInfoRecord.getSubscriptionId();//Deleted by quan.luo for XR5503803 on 2018/01/13
        EditText numberText = (EditText)mDialogLayout.findViewById(R.id.number);
        String rawNumber =  tm.getLine1Number(mSubInfoRecord.getSubscriptionId());

        String phoneNumber = null;
        if (TextUtils.isEmpty(rawNumber)) {
            phoneNumber = getPhoneNumberForDisplay(subId);
            Log.d(TAG, "get the phone number from siminfo = " + phoneNumber);
        }
        Configuration cfg =mContext.getResources().getConfiguration();
        String localeLanguage = cfg.locale.getLanguage();
        Log.d(TAG, "localeLanguage = " + localeLanguage);
        if(localeLanguage.equals("iw") || localeLanguage.equals("ar") || localeLanguage.equals("fa")) {
            if (!TextUtils.isEmpty(rawNumber)) {
                BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                rawNumber = bidiFormatter.unicodeWrap(rawNumber,
                        TextDirectionHeuristics.LTR);
            } else if (!TextUtils.isEmpty(phoneNumber)) {
                BidiFormatter bidiFormatter = BidiFormatter.getInstance();
                phoneNumber = bidiFormatter.unicodeWrap(phoneNumber,
                        TextDirectionHeuristics.LTR);
            }
            numberText.setGravity(Gravity.RIGHT);
        }
        //begin add by zhixiong.liu.hz for  P23206 20180827
        numberText.setHint(R.string.unknown);
        //end add by zhixiong.liu.hz for  P23206 20180827
        if (!TextUtils.isEmpty(rawNumber)) {
            numberText.setText(rawNumber);
        } else if (!TextUtils.isEmpty(phoneNumber)) {
            numberText.setText(phoneNumber);
        } else {
            numberText.setText("");
        }
        //Begin modified by yuzhao.liang for XR6006692 telecomcode on 2018/04/12
        //when def_settings_MSISDN_editable is true,number can be edit permanently,
        //if false,is that when EF_MSISDN is not empty,number text can not be edited.
        boolean simnumberEditable = mContext.getResources().getBoolean(R.bool.def_settings_MSISDN_editable);
        if (!simnumberEditable && !TextUtils.isEmpty(tm.getMsisdn(subId))) {
            numberText.setEnabled(false);
        }
        
        //begin add by zhixiong.liu.hz for  P24070 20180919
        if(mContext.getResources().getBoolean(R.bool.def_settings_hide_MSISDN)){
            TextView simNumberView = (TextView)mDialogLayout.findViewById(R.id.sim_number);
            simNumberView.setVisibility(View.GONE);
            numberText.setVisibility(View.GONE);
        }
        //end add by zhixiong.liu.hz for  P24070 20180919
           
        //begin zubai for aosp without tcl plugin
        /*no permission to use setLine1NumberForDisplay to implement it.*/
        if(!TclInterfaceAdapter.hasTclPlugin(mContext)) {
            numberText.setEnabled(false);
        }
        //End zubai for aosp without tcl plugin

        //End zubai for aosp without tcl plugin
        //End modified by yuzhao.liang for XR6006692 telecomcode on 2018/04/12
        //Carrier
//        String simCarrierName = tm.getSimOperatorName(mSubInfoRecord.getSubscriptionId());
//        TextView carrierView = (TextView)mDialogLayout.findViewById(R.id.carrier);
//        carrierView.setText(!TextUtils.isEmpty(simCarrierName) ? simCarrierName :
//                mContext.getString(com.android.internal.R.string.unknownName));
        TextView carrierView = (TextView)mDialogLayout.findViewById(R.id.carrier);
        carrierView.setText(simCarrierName);
        /* ADD-END by Dingyi for dualsim 2016/08/03 FR 2655692*/
       //Begin modified by miaoliu for XR p23191 on 2018/8/24
        mBuilder.setTitle(String.format(res.getString(R.string.sim_card_number_title),
                (mSubInfoRecord.getSimSlotIndex() + 1)));
      //End modified by miaoliu for XR p23191 on 2018/8/24
        mBuilder.setPositiveButton(res.getString(R.string.okay), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                final EditText nameText = (EditText)mDialogLayout.findViewById(R.id.sim_name);

                String displayName = nameText.getText().toString();

                //Begin added by yangning.hong.hz for XR6427887 on 2018-06-15
                if (TextUtils.isEmpty(displayName)) {
                    displayName = simCarrierName;
                    Log.d(TAG, "displayName is empty, set to default name: " + displayName);
                }
                //End added by yangning.hong.hz for XR6427887 on 2018-06-15

                int subId = mSubInfoRecord.getSubscriptionId();
                Log.w(TAG, "before setDisplayName mSubInfoRecord:" + mSubInfoRecord);
                mSubInfoRecord.setDisplayName(displayName);
                mSubscriptionManager.setDisplayName(displayName, subId,
                        SubscriptionManager.NAME_SOURCE_USER_INPUT);
                Log.w(TAG, "after setDisplayName mSubInfoRecord:" + mSubInfoRecord);

                final int tintSelected = tintSpinner.getSelectedItemPosition();
                int subscriptionId = mSubInfoRecord.getSubscriptionId();
                int tint = mTintArr[tintSelected];
                mSubInfoRecord.setIconTint(tint);
                mSubscriptionManager.setIconTint(tint, subscriptionId);

                Log.w(TAG, "before setSimIconType");
                /* ADD-BEGIN by Dingyi for dualsim 2016/08/03 FR 2655692*/
                //icon type
                final int typeSelected = typeSpinner.getSelectedItemPosition();
                int mSubscriptionId = mSubInfoRecord.getSubscriptionId();
                int type = mTypeArr[typeSelected];
                //mSubInfoRecord.setSimIconType(type);
                //mSubscriptionManager.setSimIconType(type, mSubscriptionId);
                //SubscriptionController.getInstance().setSimIconType(type, mSubscriptionId);
                //Begin modified by zubai.li for XR6998744 on 2018/09/21
                /*
                if(TclInterfaceAdapter.hasTclPlugin(mContext)){
                    TclDualSimManagement.getInstance(mContext).setSimIconType(type, mSubscriptionId);
                }
                */
                android.provider.Settings.System.putInt(mContext.getContentResolver(), mSubInfoRecord.getIccId(), type);
                //End modified by zubai.li for XR6998744 on 2018/09/21
                android.util.Log.w(TAG, "after setSimIconType");

                //network mode
                int buttonNetworkMode;
                buttonNetworkMode = Integer.valueOf(mNetModeArray[mNetModeSelectorPos]).intValue();
                int settingsNetworkMode = getPreferredNetworkModeForSubId();
                boolean isNetWorkModevalid=true;
                int msubId = mSubInfoRecord.getSubscriptionId();
                Log.w(TAG, "before buttonNetworkMode, buttonNetworkMode:" + buttonNetworkMode + ", settingsNetworkMode:" + settingsNetworkMode);
                if ((buttonNetworkMode != settingsNetworkMode) || getSwitchFlag()) {//Modified by yangning.hong.hz for XR6214591 telecomcode on 2018/04/24
//                    int modemNetworkMode;
                    // if new mode is invalid ignore it
                    switch (buttonNetworkMode) {
                        case Phone.NT_MODE_WCDMA_PREF:
                        case Phone.NT_MODE_GSM_ONLY:
                        case Phone.NT_MODE_WCDMA_ONLY:
                        case Phone.NT_MODE_GSM_UMTS:
                        case Phone.NT_MODE_CDMA:
                        case Phone.NT_MODE_CDMA_NO_EVDO:
                        case Phone.NT_MODE_EVDO_NO_CDMA:
                        case Phone.NT_MODE_GLOBAL:
                        case Phone.NT_MODE_LTE_CDMA_AND_EVDO:
                        case Phone.NT_MODE_LTE_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_CDMA_EVDO_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_ONLY:
                        case Phone.NT_MODE_LTE_WCDMA:
                        case Phone.NT_MODE_TDSCDMA_ONLY:
                        case Phone.NT_MODE_TDSCDMA_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA:
                        case Phone.NT_MODE_TDSCDMA_GSM:
                        case Phone.NT_MODE_LTE_TDSCDMA_GSM:
                        case Phone.NT_MODE_TDSCDMA_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_GSM_WCDMA:
                        case Phone.NT_MODE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                        case Phone.NT_MODE_LTE_TDSCDMA_CDMA_EVDO_GSM_WCDMA:
                            // This is one of the modes we recognize
                            isNetWorkModevalid = true;
                            break;
                        default:
                            isNetWorkModevalid = false;
                    }

                    //Begin modified by yangning.hong.hz for XR6214591 telecomcode on 2018/04/24
                    //Begin added by ruihua.zhang.hz for XR6099966 on 2018/03/14
                    //Begin modified by zubai.li for XR7072293 telecomcode on 2018/10/31
                    if (!isMainCard && TclInterfaceAdapter.isSeparate3G4G(mContext)) {
                    //End modified by zubai.li for XR7072293 telecomcode on 2018/10/31
                        int phoneId = SubscriptionManager.getPhoneId(msubId);
                        int currRat = TclInterfaceAdapter.getRadioAccessFamily(phoneId, mContext);
                        if((currRat & RadioAccessFamily.RAF_LTE) == RadioAccessFamily.RAF_LTE){
                            //For L+L project, no need to switch main capability.
                            Log.d(TAG,"it is L+L do nothing on slot " + phoneId);
                        }else if ((currRat & RadioAccessFamily.RAF_UMTS) == RadioAccessFamily.RAF_UMTS) {
                            // For L+W project, it should switch main capability when click item include 4G cap.
                            if (buttonNetworkMode >= Phone.NT_MODE_LTE_CDMA_AND_EVDO) {
                                android.provider.Settings.Global.putInt(mContext.getContentResolver(),
                                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + msubId, buttonNetworkMode);
                                boolean result = TclInterfaceAdapter.setRadioCapability(mContext,msubId);
                                Log.d(TAG, "setRadioCapability L+W result:" + result);
                            }
                        } else {
                            //For L+G Project, it shouldn't switch main capability when click gsm only.
                            if (buttonNetworkMode != Phone.NT_MODE_GSM_ONLY) {
                                android.provider.Settings.Global.putInt(mContext.getContentResolver(),
                                        android.provider.Settings.Global.PREFERRED_NETWORK_MODE + msubId, buttonNetworkMode);
                                boolean result = TclInterfaceAdapter.setRadioCapability(mContext,msubId);
                                Log.d(TAG, "setRadioCapability L+G result:" + result);
                            }
                        }
                    }
                    //End added by ruihua.zhang.hz for XR6099966 on 2018/03/14
                    //End modified by yangning.hong.hz for XR6214591 telecomcode on 2018/04/24

                    // Set the modem network mode
                    Log.w(TAG, "isNetWorkModevalid:" + isNetWorkModevalid);
                    if (isNetWorkModevalid) {
                        tm.setPreferredNetworkType(msubId, buttonNetworkMode);
                    }
                }
                Log.w(TAG, "after buttonNetworkMode");
                //Begin modified by yuzhao.liang for XR6006692 telecomcode on 2018/04/12
                if (simnumberEditable || TextUtils.isEmpty(tm.getMsisdn(subId)) /*&& TelephonyUtils.isRadioOn(subId,mContext)*/) {//bai mtk
                    final EditText numberEditText = (EditText) mDialogLayout.findViewById(R.id.number);
                    String displayNumber = numberEditText.getText().toString();
                    int subsId = mSubInfoRecord.getSubscriptionId();
                    if (!TextUtils.isEmpty(displayNumber)) {
                        mSubscriptionManager.setDisplayNumber(displayNumber, subsId);
                        Log.w(TAG, "[setDisplayNumber] displayNumber:" + displayNumber);
                        /*
                        setLine1NumberForDisplay: no permission, so we need to avoid the check in PhoneInterfaceManager.java
                        * */
                        tm.setLine1NumberForDisplay(subId,null,displayNumber);
                        int resultSimcard = 0;
                        if (simnumberEditable) {
                            resultSimcard = TclInterfaceAdapter.setLine1Number(mContext, subId, mContext.getOpPackageName(), displayNumber);
                            if (resultSimcard == 0) {
                                Log.e(TAG, "Storage number in the sim card fail, may be sim card file EF_MSISDN not exist !");
                            }
                        }
                        //End modified by yuzhao.liang for XR6006692 telecomcode on 2018/04/12
                    }
                }
                /* ADD-END by Dingyi for dualsim 2016/08/03 FR 2655692*/
                dialog.dismiss();
//                finish();
            }
        });

        mBuilder.setNegativeButton(res.getString(R.string.cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
//                finish();
            }
        });

        mBuilder.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
//                finish();
            }
        });

        mBuilder.create().show();
     
    }

    //Begin added by yangning.hong.hz for XR6214591 telecomcode on 2018/04/24
    private boolean getSwitchFlag(){
        //Begin modified by zubai.li for XR7072293 telecomcode on 2018/10/31
        return TclInterfaceAdapter.isSeparate3G4G(mContext);
        //End modified by zubai.li for XR7072293 telecomcode on 2018/10/31
    }
    //End added by yangning.hong.hz for XR6214591 telecomcode on 2018/04/24

    //Begin added by ruihua.zhang.hz for XR6099966 on 2018/03/14
    private int getRealPreferenceNetworkMode(int mSlotId, int ori) {
        int realPreferenceNetworkMode = mTclDualSimManagement.getPreferenceNetworkMode(mSlotId);
        if (realPreferenceNetworkMode != -1) {
            return realPreferenceNetworkMode;
        }
        return ori;
    }
    //End added by ruihua.zhang.hz for XR6099966 on 2018/03/14

    private class SelectColorAdapter extends ArrayAdapter<CharSequence> {
        private Context mContext;
        private int mResId;

        public SelectColorAdapter(
                Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;
            Resources res = mContext.getResources();
            int iconSize = res.getDimensionPixelSize(R.dimen.color_swatch_size);
            int strokeWidth = res.getDimensionPixelSize(R.dimen.color_swatch_stroke_width);

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                ShapeDrawable drawable = new ShapeDrawable(new OvalShape());
                drawable.setIntrinsicHeight(iconSize);
                drawable.setIntrinsicWidth(iconSize);
                drawable.getPaint().setStrokeWidth(strokeWidth);
                holder.label = (TextView) rowView.findViewById(R.id.color_text);
                holder.icon = (ImageView) rowView.findViewById(R.id.color_icon);
                holder.swatch = drawable;
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            holder.label.setText(getItem(position));
            holder.swatch.getPaint().setColor(mTintArr[position]);
            holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            holder.icon.setVisibility(View.VISIBLE);
            holder.icon.setImageDrawable(holder.swatch);
            return rowView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            final ViewHolder holder = (ViewHolder) rowView.getTag();

            if (mTintSelectorPos == position) {
                holder.swatch.getPaint().setStyle(Paint.Style.FILL_AND_STROKE);
            } else {
                holder.swatch.getPaint().setStyle(Paint.Style.STROKE);
            }
            holder.icon.setVisibility(View.VISIBLE);
            return rowView;
        }

        private class ViewHolder {
            TextView label;
            ImageView icon;
            ShapeDrawable swatch;
        }
    }

    /* ADD-BEGIN by Dingyi for dualsim 2016/08/03 FR 2655692*/
    private class SelectTypeAdapter extends ArrayAdapter<CharSequence> {
        private Context mContext;
        private int mResId;

        public SelectTypeAdapter(
                Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;
            Resources res = mContext.getResources();

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.label = (TextView) rowView.findViewById(R.id.icontype_text);
                holder.icon = (ImageView) rowView.findViewById(R.id.icontype_icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }
            int resID=mTypedArray.getResourceId(mTypeArr[position], 0);
            Drawable drawable = res.getDrawable(resID);
            holder.swatch = drawable;
            holder.swatch.setTintList(null);
            holder.label.setTextColor(Color.BLACK);
            holder.label.setText(getItem(position));
            holder.icon.setVisibility(View.VISIBLE);
            //holder.icon.setImageDrawable(holder.swatch);

            holder.icon.setImageBitmap(getIconBitmap(holder.swatch, mTintArr[mTintSelectorPos]));
            return rowView;
        }

        //for ergo
        public Bitmap getIconBitmap(Drawable drawable, int iconTint){
            Bitmap iconBitmap = BitmapFactory.decodeResource(mContext.getResources(),
                    R.drawable.ic_sim_card_multi_24px_clr);

            int width = iconBitmap.getWidth();
            int height = iconBitmap.getHeight();
            DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();

            // Create a new bitmap of the same size because it will be modified.
            Bitmap workingBitmap = Bitmap.createBitmap(metrics, width, height, iconBitmap.getConfig());

            Canvas canvas = new Canvas(workingBitmap);
            Paint paint = new Paint();

            // Tint the icon with the color.
            paint.setColorFilter(new PorterDuffColorFilter(iconTint, PorterDuff.Mode.SRC_ATOP));
            canvas.drawBitmap(iconBitmap, 0, 0, paint);
            paint.setColorFilter(null);

            drawable.setBounds(0, 0, width, height);
            drawable.draw(canvas);
            return workingBitmap;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            final ViewHolder holder = (ViewHolder) rowView.getTag();
            holder.icon.setVisibility(View.VISIBLE);
            return rowView;
        }

        private class ViewHolder {
            TextView label;
            ImageView icon;
            Drawable swatch;
        }
    }

    //network mode Adapter
    private class SelectNetWorkModeAdapter extends ArrayAdapter<CharSequence> {
        private Context mContext;
        private int mResId;

        public SelectNetWorkModeAdapter(
                Context context, int resource, String[] arr) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView;
            final ViewHolder holder;
            Resources res = mContext.getResources();

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.label = (TextView) rowView.findViewById(R.id.netmode_text);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            //Begin modified by quan.luo for 6179838 on 2018/04/16
            if (isHighLight) {
                holder.label.setTextColor(Color.BLACK);
            } else {
                holder.label.setTextColor(Color.LTGRAY);
            }
            //End modified by quan.luo for 6179838 on 2018/04/16
            holder.label.setText(getItem(position));

            return rowView;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View rowView = getView(position, convertView, parent);
            final ViewHolder holder = (ViewHolder) rowView.getTag();
            return rowView;
        }

        private class ViewHolder {
            TextView label;
        }
    }
    /* ADD-END by Dingyi for dualsim 2016/08/03 FR 2655692*/

    /**
     * get the phone number from siminfo database through subId.
     * @param subId, sim card subId
     * @return return the phone number.
     */
    public String getPhoneNumberForDisplay(int subId) {
        ContentResolver resolver = mContext.getContentResolver();
        Cursor cursor = resolver.query(SubscriptionManager.CONTENT_URI,
                null, SubscriptionManager.UNIQUE_KEY_SUBSCRIPTION_ID + "=?", new String[] {String.valueOf(subId)}, null);
        String number = null;
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                number = cursor.getString(cursor.getColumnIndexOrThrow(
                        SubscriptionManager.NUMBER));
            }
        }
        Log.d(TAG, "get the phone number : " + number);
        if (number != null) {
            return number;
        } else {
            return null;
        }
    }

}
