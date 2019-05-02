/*
 * Copyright (C) 2008 The Android Open Source Project
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

package com.android.settings;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PersistableBundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Telephony;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceGroup;
import android.support.v7.preference.PreferenceScreen;
import android.telephony.CarrierConfigManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.dataconnection.ApnSetting;
import com.android.internal.telephony.uicc.IccRecords;
import com.android.internal.telephony.uicc.UiccController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;

import java.util.ArrayList;
//Begin added by keyong.yu for APN REQ on 2018/07/18
import android.os.SystemProperties;
import android.preference.PreferenceFrameLayout;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.support.v7.widget.RecyclerView;
import java.util.List;
import android.app.AlertDialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import com.tcl.sdk.TclPluginManager; //Modify by chenli.gao.hz for XR6998744 on 2018/09/12
import com.android.settings.util.ResUtils;
//End added by keyong.yu for APN REQ on 2018/07/18

import android.provider.Settings;  //added by shengjiao.liu.hz for XR7031603 on 2018/11/23
//Begin added by miaoliu for XR7226032 on 2018/12/19
import com.android.settings.sim.tct.SimHotSwapHandler;
import com.android.settings.sim.tct.SimHotSwapHandler.OnSimHotSwapListener;
//End added by miaoliu for XR7226032 on 2018/12/19
public class ApnSettings extends RestrictedSettingsFragment implements
        Preference.OnPreferenceChangeListener {
    static final String TAG = "ApnSettings";

    public static final String EXTRA_POSITION = "position";
    public static final String RESTORE_CARRIERS_URI =
        "content://telephony/carriers/restore";
    public static final String PREFERRED_APN_URI =
        "content://telephony/carriers/preferapn";

    public static final String APN_ID = "apn_id";
    public static final String SUB_ID = "sub_id";
    public static final String MVNO_TYPE = "mvno_type";
    public static final String MVNO_MATCH_DATA = "mvno_match_data";

    private static final int ID_INDEX = 0;
    private static final int NAME_INDEX = 1;
    private static final int APN_INDEX = 2;
    private static final int TYPES_INDEX = 3;
    private static final int MVNO_TYPE_INDEX = 4;
    private static final int MVNO_MATCH_DATA_INDEX = 5;

    private static final int MENU_NEW = Menu.FIRST;
    private static final int MENU_RESTORE = Menu.FIRST + 1;

    private static final int EVENT_RESTORE_DEFAULTAPN_START = 1;
    private static final int EVENT_RESTORE_DEFAULTAPN_COMPLETE = 2;

    private static final int DIALOG_RESTORE_DEFAULTAPN = 1001;

    private static final Uri DEFAULTAPN_URI = Uri.parse(RESTORE_CARRIERS_URI);
    private static final Uri PREFERAPN_URI = Uri.parse(PREFERRED_APN_URI);

    private static boolean mRestoreDefaultApnMode;

    private UserManager mUserManager;
    private RestoreApnUiHandler mRestoreApnUiHandler;
    private RestoreApnProcessHandler mRestoreApnProcessHandler;
    private HandlerThread mRestoreDefaultApnThread;
    private SubscriptionInfo mSubscriptionInfo;
    private UiccController mUiccController;
    private String mMvnoType;
    private String mMvnoMatchData;

    private String mSelectedKey;

    private IntentFilter mMobileStateFilter;

    private boolean mUnavailable;

    private boolean mHideImsApn;
    private boolean mAllowAddingApns;

    public ApnSettings() {
        super(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS);
    }

    //Begin added by keyong.yu for APN REQ on 2018/07/18
    private Boolean isStartByPowerOn = false;
    private final String SOURCE_TYPE = "sourcetype";
    private static final int SOURCE_TYPE_INDEX = 6;
    private static final int CSDNUM_INDEX = 7;
    private static final String APN_HIDE = "1";
    private static final String APN_UNEDITABLE = "2";
    private static final int SOURCE_TYPE_DEFAULT = 0;
    private int mApnCount = 0;
    private Toast mToast;
    private static final String TAG_RESET_DEFAULT_APN = "confirmResetDefaultApn";
    //End added by keyong.yu for APN REQ on 2018/07/18
    //Begin added by miaoliu for XR7226032 on 2018/12/19
    private SimHotSwapHandler mSimHotSwapHandler;
    //End added by miaoliu for XR7226032 on 2018/12/19
    private final BroadcastReceiver mMobileStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(
                    TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED)) {
                PhoneConstants.DataState state = getMobileDataState(intent);
                switch (state) {
                case CONNECTED:
                    if (!mRestoreDefaultApnMode) {
                        fillList();
                    } else {
                        showDialog(DIALOG_RESTORE_DEFAULTAPN);
                    }
                    break;
                }
           //Begin added by miaoliu for XR7226032 on 2018/12/19
            }else if (Intent.ACTION_AIRPLANE_MODE_CHANGED.equals(intent.getAction())) {
                //Update the screen enable status, if airplane mode change
                updateScreenEnableState(context);
            //End added by miaoliu for XR7226032 on 2018/12/19
            }
        }
    };

    private static PhoneConstants.DataState getMobileDataState(Intent intent) {
        String str = intent.getStringExtra(PhoneConstants.STATE_KEY);
        if (str != null) {
            return Enum.valueOf(PhoneConstants.DataState.class, str);
        } else {
            return PhoneConstants.DataState.DISCONNECTED;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.APN;
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        final Activity activity = getActivity();
        final int subId = activity.getIntent().getIntExtra(SUB_ID,
                SubscriptionManager.INVALID_SUBSCRIPTION_ID);

        mMobileStateFilter = new IntentFilter(
                TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);

        setIfOnlyAvailableForAdmins(true);

        mSubscriptionInfo = SubscriptionManager.from(activity).getActiveSubscriptionInfo(subId);
        mUiccController = UiccController.getInstance();

        //Begin added by miaoliu for XR7226032 on 2018/12/19
         mMobileStateFilter.addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED);

        if (mSubscriptionInfo == null) {
            Log.d(TAG, "onCreate()... Invalid subId: " + subId);
            getActivity().finish();
        }

        // for [SIM Hot Swap] 
        mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                Log.d(TAG, "onSimHotSwap, finish activity");
                if (getActivity() != null) {
                    getActivity().finish();
                }
            }
        });
       
        //End added by miaoliu for XR7226032 on 2018/12/19

        CarrierConfigManager configManager = (CarrierConfigManager)
                getSystemService(Context.CARRIER_CONFIG_SERVICE);
        PersistableBundle b = configManager.getConfig();
        mHideImsApn = b.getBoolean(CarrierConfigManager.KEY_HIDE_IMS_APN_BOOL);
        mAllowAddingApns = b.getBoolean(CarrierConfigManager.KEY_ALLOW_ADDING_APNS_BOOL);
        
        //Begin deleted by shengjiao.liu.hz for XR7139481 on 2018/11/21
        // Use TCL customization about readonly or not
        /*if (mAllowAddingApns) {
            String[] readOnlyApnTypes = b.getStringArray(
                    CarrierConfigManager.KEY_READ_ONLY_APN_TYPES_STRING_ARRAY);
            // if no apn type can be edited, do not allow adding APNs
            if (ApnEditor.hasAllApns(readOnlyApnTypes)) {
                Log.d(TAG, "not allowing adding APN because all APN types are read only");
                mAllowAddingApns = false;
            }
        }*/
        //End deleted by shengjiao.liu.hz for XR7139481 on 2018/11/21
        
        mUserManager = UserManager.get(activity);
        //Begin added by keyong.yu for APN REQ on 2018/07/18
        mToast = Toast.makeText(getActivity(), R.string.apn_amount_toplimit, Toast.LENGTH_LONG);
        //End added by keyong.yu for APN REQ on 2018/07/18

        setFirstPNN(activity); //added by shengjiao.liu.hz for XR7031603 on 2018/11/23
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getEmptyTextView().setText(R.string.apn_settings_not_available);
        mUnavailable = isUiRestricted();
        setHasOptionsMenu(!mUnavailable);
        if (mUnavailable) {
            addPreferencesFromResource(R.xml.placeholder_prefs);
            return;
        }

        //Begin modified by keyong.yu for APN REQ on 2018/07/18
        // addPreferencesFromResource(R.xml.apn_settings);
        isStartByPowerOn = false;
        Bundle tempBundle = getActivity().getIntent().getExtras();
        if (tempBundle != null) {
            isStartByPowerOn = tempBundle.getBoolean("startbypoweron", false);
            addPreferencesFromResource(R.xml.apn_settings);
            if (isStartByPowerOn == true) {
                Log.d(TAG, "onCreate, isStartByPowerOn is true.");
                getActivity().getActionBar().setTitle(R.string.apn_settings_for_poweron);
                setView();
            } else {
                Log.d(TAG, "onCreate, isStartByPowerOn is false.");
            }
        }else{
            Log.d(TAG, "onCreate, tempBundle is null.");
        }
        //End modified by keyong.yu for APN REQ on 2018/07/18
    }

    //Begin added by keyong.yu for APN REQ on 2018/07/18
    /**
     * This method draw the UI,make the UI more pretty
     */
    private void setView() {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                PreferenceFrameLayout.LayoutParams.MATCH_PARENT, PreferenceFrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
        View v =  LayoutInflater.from(getActivity()).inflate(R.layout.apn_setting, null);
        Button aButton = (Button) v.findViewById(R.id.next);
        aButton.setText(getResources().getText(R.string.dlg_ok));
        aButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        getActivity().addContentView(v, lp);

        ViewTreeObserver vto = getListView().getViewTreeObserver();
        vto.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                RecyclerView lv = getListView();
                lv.getViewTreeObserver().removeOnPreDrawListener(this);

                Button b = (Button) getActivity().getWindow().findViewById(R.id.next);
                lv.setPadding(0, 0, 0, b.getHeight() + 5);
                return true;
            }
        });
    }

    /**
     * Current APN show to user or not.
     * if return true. APN will hide, but it's no affect to use it.
     */
    private boolean shouldSkipApn(String csdnum) {
        if ((csdnum != null && APN_HIDE.equals(csdnum))) {
            return true;
        }
        return false;
    }

    /**
     * Current APN is editable or not.
     * if return false, user can only see the info, no way to change it.
     */
    private boolean isApnEditable(int sourcetype, String csdnum) {
        boolean editable = getResources().getBoolean(R.bool.def_apn_edit_exit_flg);
        return ((sourcetype != 0) || editable) && !(csdnum != null && APN_UNEDITABLE.equals(csdnum));
    }
    private boolean isSelectable(String type) {
        String unSelectType = getResources().getString(R.string.def_settings_unSelectable_for_Apn_list);
        boolean selectable = ((type == null) || (!type.equals("mms")
                 && !type.equals("emergency")));

        Log.d(TAG, "unSelectable APN type: " + type);
        String[] unSelectableAPNArray = unSelectType.split(",");
        List<String> unSelectableAPNList = new ArrayList<String>();
        for (String unSelectableAPN : unSelectableAPNArray) {
            unSelectableAPNList.add(unSelectableAPN);
        }

        String[] apnTypeList = type.split(",");
        boolean apnUnSelect = true;
        if (apnTypeList != null) {
            for (String apnType : apnTypeList) {
                if(!unSelectableAPNList.contains(apnType)) {
                    apnUnSelect = false;
                    break;
                }
            }
        }
        selectable &= !apnUnSelect;
        return selectable;
    }

    private void setPreferApnChecked(ArrayList<ApnPreference> apnList) {
        if (apnList == null || apnList.isEmpty()) {
            return;
        }

        String selectedKey = null;
        if (mSelectedKey != null) {
            for (ApnPreference pref : apnList) {
                if (mSelectedKey.equals(pref.getKey())) {
                    pref.setChecked();
                    selectedKey = mSelectedKey;
                }
            }
        }

        // can't find prefer APN in the list, reset to the first one
        if (selectedKey == null && apnList.get(0) != null) {
            apnList.get(0).setChecked();
            selectedKey = apnList.get(0).getKey();
        }

        // save the new APN
        if (selectedKey != null && selectedKey != mSelectedKey) {
            setSelectedApnKey(selectedKey);
            mSelectedKey = selectedKey;
        }

        Log.d(TAG, "setPreferApnChecked, APN = " + mSelectedKey);
    }

    public static class ConfirmResetDefaultApnFragment extends DialogFragment {

        public static void show(ApnSettings parent) {
            if (!parent.isAdded())
                return;
            final ConfirmResetDefaultApnFragment dialog = new ConfirmResetDefaultApnFragment();
            dialog.setTargetFragment(parent, 0);
            dialog.show(parent.getFragmentManager(), TAG_RESET_DEFAULT_APN);
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Context context = getActivity();
            final AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(context.getResources().getString(
                    R.string.reset_default_apn_warning_msg));
            builder.setPositiveButton(context.getResources().getString(
                    R.string.reset_default_apn), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final ApnSettings taget = (ApnSettings) getTargetFragment();
                    taget.restoreDefaultApn();
                }
            });
            builder.setNegativeButton(context.getResources().getString(
                    R.string.cancel_reset_default_apn), null);
            return builder.create();
        }
    }
    //End added by keyong.yu for APN REQ on 2018/07/18

    @Override
    public void onResume() {
        super.onResume();

        if (mUnavailable) {
            return;
        }

        getActivity().registerReceiver(mMobileStateReceiver, mMobileStateFilter);

        if (!mRestoreDefaultApnMode) {
            fillList();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUnavailable) {
            return;
        }

        getActivity().unregisterReceiver(mMobileStateReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mRestoreDefaultApnThread != null) {
            mRestoreDefaultApnThread.quit();
        }
        //Begin added by keyong.yu for APN REQ on 2018/07/18
        if (mToast != null) {
            mToast.cancel();
        }
        //End added by keyong.yu for APN REQ on 2018/07/18
        //Begin added by miaoliu for XR7226032 on 2018/12/19
        if (mSimHotSwapHandler != null) {
            mSimHotSwapHandler.unregisterOnSimHotSwap();
        }
        //End added by miaoliu for XR7226032 on 2018/12/19
    }

    @Override
    public EnforcedAdmin getRestrictionEnforcedAdmin() {
        final UserHandle user = UserHandle.of(mUserManager.getUserHandle());
        if (mUserManager.hasUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS, user)
                && !mUserManager.hasBaseUserRestriction(UserManager.DISALLOW_CONFIG_MOBILE_NETWORKS,
                        user)) {
            return EnforcedAdmin.MULTIPLE_ENFORCED_ADMIN;
        }
        return null;
    }

    private void fillList() {
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        final int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        final String mccmnc = mSubscriptionInfo == null ? "" : tm.getSimOperator(subId);
        Log.d(TAG, "mccmnc = " + mccmnc);
        StringBuilder where = new StringBuilder("numeric=\"" + mccmnc +
                "\" AND NOT (type='ia' AND (apn=\"\" OR apn IS NULL)) AND user_visible!=0");
        //Begin modified by keyong.yu for APN REQ on 2018/07/18
        /*if (mHideImsApn) {
            where.append(" AND NOT (type='ims')");
        }*/
        where = TclPluginManager.getTclSdkAdapter(getActivity()).getFillListQuery(where, subId); //Modify by chenli.gao.hz for XR6998744 on 2018/09/12
        Log.d(TAG, "fillList where: " + where);
        
        //use frameworks resource since it is used by others
        //Begin modified by miaoliu for XR7107006 on 2018/11/15
        //String order = getResources().getString(getResources().getIdentifier("def_settings_order_for_apnlist", "string", "com.tct")); 
        int id = getResources().getIdentifier("def_settings_order_for_apnlist", "string", "com.tct");
        String order = id > 0 ? getResources().getString(id) : null; 
        //End modified by miaoliu for XR7107006 on 2018/11/15
        if (order == null || "".equals(order)) {
            order = null;
        }
        Log.d(TAG, "order = " + order);
        /*Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
                "_id", "name", "apn", "type", "mvno_type", "mvno_match_data"}, where.toString(),
                null, Telephony.Carriers.DEFAULT_SORT_ORDER);*/
        //NOTE:Make sure it has added sourcetype&csdnum when creating database.
        Cursor cursor = getContentResolver().query(Telephony.Carriers.CONTENT_URI, new String[] {
              "_id", "name", "apn", "type", "mvno_type", "mvno_match_data",
              "sourcetype", "csdnum"}, where.toString(), null, order);
        //End modified by keyong.yu for APN REQ on 2018/07/18
        if (cursor != null) {
            IccRecords r = null;
            if (mUiccController != null && mSubscriptionInfo != null) {
                r = mUiccController.getIccRecords(
                        SubscriptionManager.getPhoneId(subId), UiccController.APP_FAM_3GPP);
            }
            PreferenceGroup apnList = (PreferenceGroup) findPreference("apn_list");
            apnList.removeAll();

            ArrayList<ApnPreference> mnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mnoMmsApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> mvnoMmsApnList = new ArrayList<ApnPreference>();
            //Begin added by shengjiao.liu.hz for XR7099319 on 2018/11/06
            ArrayList<ApnPreference> userApnList = new ArrayList<ApnPreference>();
            ArrayList<ApnPreference> userMmsApnList = new ArrayList<ApnPreference>();
            //End added by shengjiao.liu.hz for XR7099319 on 2018/11/06
            mSelectedKey = getSelectedApnKey();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                String name = cursor.getString(NAME_INDEX);
                String apn = cursor.getString(APN_INDEX);
                String key = cursor.getString(ID_INDEX);
                String type = cursor.getString(TYPES_INDEX);
                String mvnoType = cursor.getString(MVNO_TYPE_INDEX);
                String mvnoMatchData = cursor.getString(MVNO_MATCH_DATA_INDEX);
                //Begin added by keyong.yu for APN REQ on 2018/07/18
                int sourcetype = cursor.getInt(SOURCE_TYPE_INDEX);
                String csdnum = cursor.getString(CSDNUM_INDEX);

                if (shouldSkipApn(csdnum)) {
                    cursor.moveToNext();
                    continue;
                }
                //End added by keyong.yu for APN REQ on 2018/07/18
                ApnPreference pref = new ApnPreference(getPrefContext());

                pref.setKey(key);
                pref.setTitle(name);
                pref.setSummary(apn);
                pref.setPersistent(false);
                pref.setOnPreferenceChangeListener(this);
                pref.setSubId(subId);
                //Begin modified by keyong.yu for APN REQ on 2018/07/18
                pref.setApnEditable(isApnEditable(sourcetype, csdnum));
                //boolean selectable = ((type == null) || !type.equals("mms"));
                boolean selectable = isSelectable(type);
                pref.setSelectable(selectable);
                if (selectable) {
                    /*if ((mSelectedKey != null) && mSelectedKey.equals(key)) {
                        pref.setChecked();
                    }*/
                    //Begin modified by shengjiao.liu.hz for XR7099319 on 2018/11/06
                    if (sourcetype == SOURCE_TYPE_DEFAULT) {
                        addApnToList(pref, mnoApnList, mvnoApnList, r, mvnoType, mvnoMatchData);
                    } else {
                        userApnList.add(pref);
                    }
                } else {
                    if (sourcetype == SOURCE_TYPE_DEFAULT) {
                        addApnToList(pref, mnoMmsApnList, mvnoMmsApnList, r, mvnoType, mvnoMatchData);
                    } else {
                        userMmsApnList.add(pref);
                    }
                    //End modified by shengjiao.liu.hz for XR7099319 on 2018/11/06
                }
                //End modified by keyong.yu for APN REQ on 2018/07/18
                cursor.moveToNext();
            }
            cursor.close();
            //Begin modified by keyong.yu for APN REQ on 2018/07/18
            //if (!mvnoApnList.isEmpty()) {
            if (!mvnoApnList.isEmpty() || !mvnoMmsApnList.isEmpty()) {
            //End modified by keyong.yu for APN REQ on 2018/07/18
                mnoApnList = mvnoApnList;
                mnoMmsApnList = mvnoMmsApnList;
                // Also save the mvno info
            }
            //Begin added by shengjiao.liu.hz for XR7099319 on 2018/11/06
            mnoApnList.addAll(userApnList);
            mnoMmsApnList.addAll(userMmsApnList);
            //End added by shengjiao.liu.hz for XR7099319 on 2018/11/06

            for (Preference preference : mnoApnList) {
                apnList.addPreference(preference);
            }
            for (Preference preference : mnoMmsApnList) {
                apnList.addPreference(preference);
            }

            //Begin added by keyong.yu for APN REQ on 2018/07/18
            mApnCount = mnoApnList.size() + mnoMmsApnList.size();
            mSelectedKey = getSelectedApnKey();
            setPreferApnChecked(mnoApnList);
            //End added by keyong.yu for APN REQ on 2018/07/18
             updateScreenEnableState(getActivity());//Added by miaoliu for XR7226032 on 2018/12/19
        }
    }

    private void addApnToList(ApnPreference pref, ArrayList<ApnPreference> mnoList,
                              ArrayList<ApnPreference> mvnoList, IccRecords r, String mvnoType,
                              String mvnoMatchData) {
        //Begin modified by keyong.yu for APN REQ on 2018/07/18
        if (r != null && !TextUtils.isEmpty(mvnoType) /*&& !TextUtils.isEmpty(mvnoMatchData)*/) {
            final int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                    : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            //Begin modify by chenli.gao.hz for XR6998744 on 2018/09/12
            int filterResult = TclPluginManager.getTclSdkAdapter(getActivity())
                    .filterAPN(r, mvnoType, mvnoMatchData, getActivity(), subId);
            //End modify by chenli.gao.hz for XR6998744 on 2018/09/12
            Log.d(TAG, "filterResult = " + filterResult);

            if(filterResult == -1) {  //not handled by plugin
                if (ApnSetting.mvnoMatches(r, mvnoType, mvnoMatchData)) {
                    mvnoList.add(pref);
                    // Since adding to mvno list, save mvno info
                    mMvnoType = mvnoType;
                    mMvnoMatchData = mvnoMatchData;
                    Log.d(TAG, "mvnoMatches...");
                }
            } else if (filterResult == 1) {
                mvnoList.add(pref);
                // Since adding to mvno list, save mvno info
                mMvnoType = mvnoType;
                mMvnoMatchData = mvnoMatchData;
            } else if (filterResult == 2) {
                mnoList.add(pref);
            }
        }
        //End modified by keyong.yu for APN REQ on 2018/07/18
        else {
            mnoList.add(pref);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!mUnavailable) {
            if (mAllowAddingApns) {
                menu.add(0, MENU_NEW, 0,
                        getResources().getString(R.string.menu_new))
                        .setIcon(R.drawable.ic_menu_add_white)
                        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
            menu.add(0, MENU_RESTORE, 0,
                    getResources().getString(R.string.menu_restore))
                    .setIcon(android.R.drawable.ic_menu_upload);
        }

        //super.onCreateOptionsMenu(menu, inflater);//Deleted by miaoliu for XR6107743 on 2018/4/10
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_NEW:
            //Begin modified by keyong.yu for APN REQ on 2018/07/18
            //addNewApn();
            if (mApnCount > 30) {
                if (mToast != null) {
                    mToast.show();
                }
            } else {
                addNewApn();
            }
            //End modified by keyong.yu for APN REQ on 2018/07/18
            return true;

        case MENU_RESTORE:
            //Begin modified by keyong.yu for APN REQ on 2018/07/18
            //restoreDefaultApn();
            final int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                    : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
            TelephonyManager tm = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
            String numeric = tm.getSimOperator(subId);
            if (TclPluginManager.getTclSdkAdapter(getActivity()).isOpenMarketSim(numeric)) { //Modify by chenli.gao.hz for XR6998744 on 2018/09/12
                ConfirmResetDefaultApnFragment.show(ApnSettings.this);
            } else {
                restoreDefaultApn();
            }
            //End modified by keyong.yu for APN REQ on 2018/07/18
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addNewApn() {
        Intent intent = new Intent(Intent.ACTION_INSERT, Telephony.Carriers.CONTENT_URI);
        int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        intent.putExtra(SUB_ID, subId);
        if (!TextUtils.isEmpty(mMvnoType) && !TextUtils.isEmpty(mMvnoMatchData)) {
            intent.putExtra(MVNO_TYPE, mMvnoType);
            intent.putExtra(MVNO_MATCH_DATA, mMvnoMatchData);
        }
        startActivity(intent);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        int pos = Integer.parseInt(preference.getKey());
        Uri url = ContentUris.withAppendedId(Telephony.Carriers.CONTENT_URI, pos);
        //Begin modified by shengjiao.liu.hz for XR7214914 on 2018/12/06
        //startActivity(new Intent(Intent.ACTION_EDIT, url));
        Intent intent = new Intent(Intent.ACTION_EDIT, url);
        intent.putExtra("readOnly", !((ApnPreference) preference).getApnEditable());
        startActivity(intent);
        //End modified by shengjiao.liu.hz for XR7214914 on 2018/12/06
        return true;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        Log.d(TAG, "onPreferenceChange(): Preference - " + preference
                + ", newValue - " + newValue + ", newValue type - "
                + newValue.getClass());
        if (newValue instanceof String) {
            setSelectedApnKey((String) newValue);
        }

        return true;
    }

    private void setSelectedApnKey(String key) {
        mSelectedKey = key;
        ContentResolver resolver = getContentResolver();

        ContentValues values = new ContentValues();
        values.put(APN_ID, mSelectedKey);
        resolver.update(getUriForCurrSubId(PREFERAPN_URI), values, null, null);
    }

    private String getSelectedApnKey() {
        String key = null;

        Cursor cursor = getContentResolver().query(getUriForCurrSubId(PREFERAPN_URI),
                new String[] {"_id"}, null, null, Telephony.Carriers.DEFAULT_SORT_ORDER);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            key = cursor.getString(ID_INDEX);
        }
        cursor.close();
        return key;
    }

    private boolean restoreDefaultApn() {
        showDialog(DIALOG_RESTORE_DEFAULTAPN);
        mRestoreDefaultApnMode = true;

        if (mRestoreApnUiHandler == null) {
            mRestoreApnUiHandler = new RestoreApnUiHandler();
        }

        if (mRestoreApnProcessHandler == null ||
            mRestoreDefaultApnThread == null) {
            mRestoreDefaultApnThread = new HandlerThread(
                    "Restore default APN Handler: Process Thread");
            mRestoreDefaultApnThread.start();
            mRestoreApnProcessHandler = new RestoreApnProcessHandler(
                    mRestoreDefaultApnThread.getLooper(), mRestoreApnUiHandler);
        }

        mRestoreApnProcessHandler
                .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_START);
        return true;
    }

    // Append subId to the Uri
    private Uri getUriForCurrSubId(Uri uri) {
        int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (SubscriptionManager.isValidSubscriptionId(subId)) {
            return Uri.withAppendedPath(uri, "subId/" + String.valueOf(subId));
        } else {
            return uri;
        }
    }

    private class RestoreApnUiHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_COMPLETE:
                    Activity activity = getActivity();
                    if (activity == null) {
                        mRestoreDefaultApnMode = false;
                        return;
                    }
                    fillList();
                    getPreferenceScreen().setEnabled(true);
                    mRestoreDefaultApnMode = false;
                    removeDialog(DIALOG_RESTORE_DEFAULTAPN);
                    Toast.makeText(
                        activity,
                        getResources().getString(
                                R.string.restore_default_apn_completed),
                        Toast.LENGTH_LONG).show();
                    //Begin added by keyong.yu for APN REQ on 2018/07/18     
                    int subId = mSubscriptionInfo != null ? mSubscriptionInfo.getSubscriptionId()
                            : SubscriptionManager.INVALID_SUBSCRIPTION_ID;
                    TclPluginManager.getTclSdkAdapter(getActivity()).checkShowSimProvider(subId); //Modify by chenli.gao.hz for XR6998744 on 2018/09/12
                    //End added by keyong.yu for APN REQ on 2018/07/18
                    break;
            }
        }
    }

    private class RestoreApnProcessHandler extends Handler {
        private Handler mRestoreApnUiHandler;

        public RestoreApnProcessHandler(Looper looper, Handler restoreApnUiHandler) {
            super(looper);
            this.mRestoreApnUiHandler = restoreApnUiHandler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case EVENT_RESTORE_DEFAULTAPN_START:
                    ContentResolver resolver = getContentResolver();
                    resolver.delete(getUriForCurrSubId(DEFAULTAPN_URI), null, null);
                    mRestoreApnUiHandler
                        .sendEmptyMessage(EVENT_RESTORE_DEFAULTAPN_COMPLETE);
                    break;
            }
        }
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == DIALOG_RESTORE_DEFAULTAPN) {
            ProgressDialog dialog = new ProgressDialog(getActivity()) {
                public boolean onTouchEvent(MotionEvent event) {
                    return true;
                }
            };
            dialog.setMessage(getResources().getString(R.string.restore_default_apn));
            //Begin modified by miaoliu for XRP10028391 on 2018/12/24
            //dialog.setCancelable(false);
            setCancelable(false);
            //Begin modified by miaoliu for XRP10028391 on 2018/12/24
            return dialog;
        }
        return null;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (dialogId == DIALOG_RESTORE_DEFAULTAPN) {
            return MetricsEvent.DIALOG_APN_RESTORE_DEFAULT;
        }
        return 0;
    }

    //Begin added by shengjiao.liu.hz for XR7031603 on 2018/11/23
    public void setFirstPNN(Context context) {
        Settings.Global.putString(context.getContentResolver(), "firstPnn", "");
        if (mSubscriptionInfo != null) {
            int phoneId = SubscriptionManager.getPhoneId(mSubscriptionInfo.getSubscriptionId());
            String pnn = TclPluginManager.getTclSdkAdapter(context).getPnnHomeName(phoneId);
            if (!TextUtils.isEmpty(pnn)) {
                Settings.Global.putString(context.getContentResolver(), "firstPnn", pnn);
            }
        }
    }
    //End added by shengjiao.liu.hz for XR7031603 on 2018/11/23
    //Begin added by miaoliu for XR7226032 on 2018/12/19
     private void updateScreenEnableState(Context context) {
        //int subId = mSubscriptionInfo.getSubscriptionId();
        // boolean simReady = TelephonyManager.SIM_STATE_READY == TelephonyManager.getDefault()
        //         .getSimState(SubscriptionManager.getSlotIndex(subId));
        boolean airplaneModeEnabled = android.provider.Settings.System.getInt(context
                .getContentResolver(), android.provider.Settings.System.AIRPLANE_MODE_ON, -1) == 1;
        boolean enable = !airplaneModeEnabled;
          getPreferenceScreen().setEnabled(enable);
    }
    //End added by miaoliu for XR7226032 on 2018/12/19
  
}
