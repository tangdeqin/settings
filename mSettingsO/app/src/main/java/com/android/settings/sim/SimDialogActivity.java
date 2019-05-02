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

package com.android.settings.sim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import android.telecom.TelecomManager;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.android.settings.R;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
//Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
import android.content.DialogInterface.OnDismissListener;
import com.android.settings.sim.tct.*;

import android.util.Log;
import android.text.TextUtils;
//End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

public class SimDialogActivity extends Activity {
    private static String TAG = "SimDialogActivity";

    public static String PREFERRED_SIM = "preferred_sim";
    public static String DIALOG_TYPE_KEY = "dialog_type";
    public static final int INVALID_PICK = -1;
    public static final int DATA_PICK = 0;
    public static final int CALLS_PICK = 1;
    public static final int SMS_PICK = 2;
    public static final int PREFERRED_PICK = 3;
    //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    public static final int SERVICE_PICK = 10;
    //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final int dialogType = getIntent().getIntExtra(DIALOG_TYPE_KEY, INVALID_PICK);

        switch (dialogType) {
            //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            case DATA_PICK:
            case CALLS_PICK:
            case SMS_PICK:
                /// M: for AlPS02113443, avoid window leak @{
                // createDialog(this, dialogType).show();
                /// M: for ALPS02463456, activity state chaos, add log to check,
                // can be removed if not happen again @{
                if (isFinishing()) {
                    Log.e(TAG, "Activity Finishing!");
                }
                /// @}

                mDialog = createDialog(this, dialogType);
                mDialog.show();
                /// @}
                break;
            case SERVICE_PICK:
                if (isFinishing()) {
                    Log.e(TAG, "Activity Finishing!");
                }
                /// @}

                mDialog = TclInterfaceAdapter.createServiceDialog(this, (Object)this, dialogType);
                mDialog.show();
                /// @}
                break;
            case PREFERRED_PICK:
                if(android.os.SystemProperties.getBoolean("ro.mmitest", false)){
                    Log.e(TAG, "It is MINI sw , ignored sim card changed dialog");
                    finish();
                    break;
                }else{
                    /// M: for ALPS02423087, hot plug timing issue, the sub list may already changed @{
                    List<SubscriptionInfo> subs = SubscriptionManager.from(this)
                            .getActiveSubscriptionInfoList();
                    if (subs == null || subs.size() != 1) {
                        Log.w(TAG, "Subscription count is not 1, skip preferred SIM dialog");
                        finish();
                        return;
                    }

                    displayPreferredDialog(getIntent().getIntExtra(PREFERRED_SIM, 0));
                    break;
                }
            //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            default:
                throw new IllegalArgumentException("Invalid dialog type " + dialogType + " sent.");
        }

    }

    private void displayPreferredDialog(final int slotId) {
        final Resources res = getResources();
        final Context context = getApplicationContext();
        final SubscriptionInfo sir = SubscriptionManager.from(context)
                .getActiveSubscriptionInfoForSimSlotIndex(slotId);

        if (sir != null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle(R.string.sim_preferred_title);
            alertDialogBuilder.setMessage(res.getString(
                        R.string.sim_preferred_message, sir.getDisplayName()));

            alertDialogBuilder.setPositiveButton(R.string.yes, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    final int subId = sir.getSubscriptionId();
                    PhoneAccountHandle phoneAccountHandle =
                            subscriptionIdToPhoneAccountHandle(subId);
                    setDefaultDataSubId(context, subId);
                    setDefaultSmsSubId(context, subId);
                    setUserSelectedOutgoingPhoneAccount(phoneAccountHandle);
                    //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                    /// M: Add dismiss dialog before finish to void screen flash.
                    dismissSimDialog();
                    finish();
                    //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                }
            });
            alertDialogBuilder.setNegativeButton(R.string.no, new
                    DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog,int id) {
                    //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                    /// M: Add dismiss dialog before finish to void screen flash.
                    dismissSimDialog();
                    finish();
                    //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                }
            });

            //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            /// M: when dialog dismissed, finish the activity as well @{
            alertDialogBuilder.setOnDismissListener(new OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                }
            });

            /// M: for ALPS02422990 avoid window leak @{
            mDialog = alertDialogBuilder.create();
            mDialog.show();
            /*old
            alertDialogBuilder.create().show();
            */
            //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        } else {
            finish();
        }
    }

    private static void setDefaultDataSubId(final Context context, final int subId) {
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);

        //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.08.01
        TelephonyManager telephonyManager = TelephonyManager.from(context);
        boolean enableBefore = telephonyManager.getDataEnabled();
        int preSubId = subscriptionManager.getDefaultDataSubscriptionId();

        //Begin modified by zubai.li for XR7080031 telecomcode on 2018/11/07
        /*
        subscriptionManager.setDefaultDataSubId(subId);
        */
        if(TclInterfaceAdapter.hasTclPlugin(context)) {
            TclInterfaceAdapter.setDefaultDataSubId(context, subId);
        }else{
            subscriptionManager.setDefaultDataSubId(subId);
        }
        //End modified by zubai.li for XR7080031 telecomcode on 2018/11/07

        // If data was enabled before and target subit not equal to before one, set before subId data not enabled when
        // enable current subid
        Log.d(TAG, "Data enabled before : " + enableBefore + ", subId : " + subId + ", preSubid :" + preSubId);
        if (enableBefore && subId != preSubId) {
            telephonyManager.setDataEnabled(preSubId, false);
        }
        telephonyManager.setDataEnabled(subId, true);
        //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.08.01
        Toast.makeText(context, R.string.data_switch_started, Toast.LENGTH_LONG).show();
    }

    private static void setDefaultSmsSubId(final Context context, final int subId) {
        Log.d(TAG, "setDefaultSmsSubId, sub = " + subId);
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        subscriptionManager.setDefaultSmsSubId(subId);
    }

    private void setUserSelectedOutgoingPhoneAccount(PhoneAccountHandle phoneAccount) {
        Log.d(TAG, "setUserSelectedOutgoingPhoneAccount phoneAccount = " + phoneAccount);
        final TelecomManager telecomManager = TelecomManager.from(this);
        telecomManager.setUserSelectedOutgoingPhoneAccount(phoneAccount);
    }

    private PhoneAccountHandle subscriptionIdToPhoneAccountHandle(final int subId) {
        final TelecomManager telecomManager = TelecomManager.from(this);
        final TelephonyManager telephonyManager = TelephonyManager.from(this);
        final Iterator<PhoneAccountHandle> phoneAccounts =
                telecomManager.getCallCapablePhoneAccounts().listIterator();

        while (phoneAccounts.hasNext()) {
            final PhoneAccountHandle phoneAccountHandle = phoneAccounts.next();
            final PhoneAccount phoneAccount = telecomManager.getPhoneAccount(phoneAccountHandle);
            if (subId == telephonyManager.getSubIdForPhoneAccount(phoneAccount)) {
                return phoneAccountHandle;
            }
        }

        return null;
    }

    public Dialog createDialog(final Context context, final int id) {
        final ArrayList<String> list = new ArrayList<String>();
        final SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
        final List<SubscriptionInfo> subInfoList =
            subscriptionManager.getActiveSubscriptionInfoList();
        final int selectableSubInfoLength = subInfoList == null ? 0 : subInfoList.size();

        final DialogInterface.OnClickListener selectionListener =
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int value) {

                        final SubscriptionInfo sir;
                        switch (id) {
                            case DATA_PICK:
                                sir = subInfoList.get(value);
                                //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.
                                /*old
                                setDefaultDataSubId(context, sir.getSubscriptionId());
                                */
                                TelephonyManager telephonyManager = TelephonyManager.getDefault();
                                int defaultDataSubId = SubscriptionManager.getDefaultDataSubscriptionId();
                                if(null == sir && !TextUtils.isEmpty(list.get(value))){
                                    if (telephonyManager.getDataEnabled(defaultDataSubId)) {
                                        telephonyManager.setDataEnabled(false);
                                    }
                                    Log.d(TAG, "disable Mobile Data, defaultDataSubId= " + defaultDataSubId);
                                    break;
                                }
                                int targetSub = (sir == null ? null : sir.getSubscriptionId());
                                setDefaultDataSubId(context, targetSub);
                                //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                                break;
                            case CALLS_PICK:
                                final TelecomManager telecomManager =
                                        TelecomManager.from(context);
                                final List<PhoneAccountHandle> phoneAccountsList =
                                        telecomManager.getCallCapablePhoneAccounts();
                                Log.d(TAG, "phoneAccountsList = " + phoneAccountsList.toString());

                                //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                                /// M: for ALPS02320816 @{
                                // phone account may changed in background
                                if (value > phoneAccountsList.size()) {
                                    Log.w(TAG, "phone account changed, do noting! value = " +
                                            value + ", phone account size = " +
                                            phoneAccountsList.size());
                                    break;
                                }
                                Log.d(TAG, "value = " + value);
                                //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

                                setUserSelectedOutgoingPhoneAccount(
                                        value < 1 ? null : phoneAccountsList.get(value - 1));
                                break;
                            case SMS_PICK:
                                //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                                /// M: for [SMS Always Ask]
                                /*old
                                sir = subInfoList.get(value);
                                setDefaultSmsSubId(context, sir.getSubscriptionId());
                                */
                                int subId = getPickSmsDefaultSub(subInfoList, value);
                                setDefaultSmsSubId(context, subId);
                                //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                                break;
                            default:
                                throw new IllegalArgumentException("Invalid dialog type "
                                        + id + " in SIM dialog.");
                        }
                        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                        /// M: Add dismiss dialog before finish to void screen flash.
                        dismissSimDialog();
                        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                        finish();
                    }
                };

        Dialog.OnKeyListener keyListener = new Dialog.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface arg0, int keyCode,
                    KeyEvent event) {
                    if (keyCode == KeyEvent.KEYCODE_BACK) {
                        finish();
                    }
                    return true;
                }
            };

        ArrayList<SubscriptionInfo> callsSubInfoList = new ArrayList<SubscriptionInfo>();
        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        /// M: for [SMS Always Ask] @{
        ArrayList<SubscriptionInfo> smsSubInfoList = new ArrayList<SubscriptionInfo>();
        //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        /// @}
        if (id == CALLS_PICK) {
            final TelecomManager telecomManager = TelecomManager.from(context);
            final TelephonyManager telephonyManager = TelephonyManager.from(context);
            final Iterator<PhoneAccountHandle> phoneAccounts =
                    telecomManager.getCallCapablePhoneAccounts().listIterator();

            //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            /*old
            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            callsSubInfoList.add(null);
            */

            /// M: only for multiple accounts
            int accountSize = telecomManager.getCallCapablePhoneAccounts().size();
            Log.d(TAG, "phoneAccounts size = " + accountSize);
            if (accountSize > 1) {
                list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
                callsSubInfoList.add(null);
            }
            //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

            while (phoneAccounts.hasNext()) {
                final PhoneAccount phoneAccount =
                        telecomManager.getPhoneAccount(phoneAccounts.next());
                //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                /// M: for ALPS02362894, seldom happened that phone account
                // unregistered in the background @{
                if (phoneAccount == null) {
                    Log.d(TAG, "phoneAccount is null");
                    continue;
                }
                //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

                list.add((String)phoneAccount.getLabel());
                int subId = telephonyManager.getSubIdForPhoneAccount(phoneAccount);
                Log.d(TAG, "phoneAccount label = " + phoneAccount.getLabel()
                        + ", subId = " + subId);
                if (subId != SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
                    final SubscriptionInfo sir = SubscriptionManager.from(context)
                            .getActiveSubscriptionInfo(subId);
                    callsSubInfoList.add(sir);
                } else {
                    callsSubInfoList.add(null);
                }
            }
            Log.d(TAG, "callsSubInfoList = " + callsSubInfoList + ", list = " + list);
        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        /// M: for [SMS Always Ask] @{
        } else if (id == SMS_PICK) {
            setupSmsSubInfoList(list, subInfoList, selectableSubInfoLength, smsSubInfoList);
        //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        } else {
            for (int i = 0; i < selectableSubInfoLength; ++i) {
                final SubscriptionInfo sir = subInfoList.get(i);
                CharSequence displayName = sir.getDisplayName();
                if (displayName == null) {
                    displayName = "";
                }
                list.add(displayName.toString());
            }
        }
        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        TclDualSimManagement.getInstance(SimDialogActivity.this).customItemsForPick(subInfoList, list, selectableSubInfoLength, id,  "SimSelectService".equals(getIntent().getExtra("from")));
        //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

        String[] arr = list.toArray(new String[0]);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ListAdapter adapter = new SelectAccountListAdapter(
                //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                /// M: for [SMS Always Ask]
                /*old
                id == CALLS_PICK ? callsSubInfoList : subInfoList,
                */
                getAdapterData(id, subInfoList, callsSubInfoList, smsSubInfoList),
                //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                builder.getContext(),
                R.layout.select_account_list_item,
                arr, id);

        switch (id) {
            case DATA_PICK:
                builder.setTitle(R.string.select_sim_for_data);
                break;
            case CALLS_PICK:
                builder.setTitle(R.string.select_sim_for_calls);
                break;
            case SMS_PICK:
                builder.setTitle(R.string.sim_card_select_title);
                break;
            default:
                throw new IllegalArgumentException("Invalid dialog type "
                        + id + " in SIM dialog.");
        }

        Dialog dialog = builder.setAdapter(adapter, selectionListener).create();
        dialog.setOnKeyListener(keyListener);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                finish();
            }
        });

        return dialog;

    }

    private class SelectAccountListAdapter extends ArrayAdapter<String> {
        private Context mContext;
        private int mResId;
        private int mDialogId;
        private final float OPACITY = 0.54f;
        private List<SubscriptionInfo> mSubInfoList;

        public SelectAccountListAdapter(List<SubscriptionInfo> subInfoList,
                Context context, int resource, String[] arr, int dialogId) {
            super(context, resource, arr);
            mContext = context;
            mResId = resource;
            mDialogId = dialogId;
            mSubInfoList = subInfoList;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)
                    mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView;
            final ViewHolder holder;

            //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            rowView = TclDualSimManagement.getInstance(SimDialogActivity.this).customItemView(
                    convertView, position, mSubInfoList, mDialogId, getItem(position));
            if(rowView != null){
                return rowView;
            }
            //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

            if (convertView == null) {
                // Cache views for faster scrolling
                rowView = inflater.inflate(mResId, null);
                holder = new ViewHolder();
                holder.title = (TextView) rowView.findViewById(R.id.title);
                holder.summary = (TextView) rowView.findViewById(R.id.summary);
                holder.icon = (ImageView) rowView.findViewById(R.id.icon);
                rowView.setTag(holder);
            } else {
                rowView = convertView;
                holder = (ViewHolder) rowView.getTag();
            }

            final SubscriptionInfo sir = mSubInfoList.get(position);
            if (sir == null) {
                holder.title.setText(getItem(position));
                holder.summary.setText("");
                //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                /// M: display icon for non-sub accounts
                /*old
                holder.icon.setImageDrawable(getResources()
                        .getDrawable(R.drawable.ic_live_help));
                */
                if (mDialogId == CALLS_PICK) {
                    setPhoneAccountIcon(holder, position);
                } else {
                    holder.icon.setImageDrawable(getResources()
                            .getDrawable(R.drawable.ic_live_help));
                }
                //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                holder.icon.setAlpha(OPACITY);
            } else {
                holder.title.setText(sir.getDisplayName());
                holder.summary.setText(sir.getNumber());
                holder.icon.setImageBitmap(sir.createIconBitmap(mContext));
                //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
                /// M: when item numbers is over the screen, should set alpha 1.0f.
                holder.icon.setAlpha(1.0f);
                //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            }
            return rowView;
        }

        private class ViewHolder {
            TextView title;
            TextView summary;
            ImageView icon;
        }

        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        private void setPhoneAccountIcon(ViewHolder holder, int location) {
            Log.d(TAG, "setSipAccountBitmap()... location: " + location);
            String askFirst = getResources().getString(R.string.sim_calls_ask_first_prefs_title);
            String lableString = getItem(location);
            final TelecomManager telecomManager = TelecomManager.from(mContext);
            List<PhoneAccountHandle> phoneAccountHandles =
                    telecomManager.getCallCapablePhoneAccounts();
            if (!askFirst.equals(lableString)) {
                if (phoneAccountHandles.size() > 1) {
                    location = location - 1;
                }
                PhoneAccount phoneAccount = null;
                if (location >= 0 && location < phoneAccountHandles.size()) {
                    phoneAccount =
                            telecomManager.getPhoneAccount(phoneAccountHandles.get(location));
                }
                Log.d(TAG, "setSipAccountBitmap()... position: " + location
                        + " account: "  + phoneAccount);
                if (phoneAccount != null) {
                    holder.icon.setImageDrawable(phoneAccount.getIcon().loadDrawable(mContext));
                }
            } else {
                holder.icon.setImageDrawable(getResources().getDrawable(R.drawable.ic_live_help));
            }
        }
    }

    private Dialog mDialog;

    private int getPickSmsDefaultSub(final List<SubscriptionInfo> subInfoList,
            int value) {
        int subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
        if (value < 1) {
            int length = subInfoList == null ? 0 : subInfoList.size();
            if (length == 1) {
                subId = subInfoList.get(value).getSubscriptionId();
            } else {
                subId = -2;//ASK_USER_SUB_ID
            }
        } else if (value >= 1 && value < subInfoList.size() + 1) {
            subId = subInfoList.get(value - 1).getSubscriptionId();
        }
        Log.d(TAG, "getPickSmsDefaultSub, value: " + value + ", subId: " + subId);
        return subId;
    }

    private void setupSmsSubInfoList(final ArrayList<String> list,
            final List<SubscriptionInfo> subInfoList, final int selectableSubInfoLength,
            ArrayList<SubscriptionInfo> smsSubInfoList) {

        if (selectableSubInfoLength > 1) {
            list.add(getResources().getString(R.string.sim_calls_ask_first_prefs_title));
            smsSubInfoList.add(null);
        }
        for (int i = 0; i < selectableSubInfoLength; ++i) {
            final SubscriptionInfo sir = subInfoList.get(i);
            smsSubInfoList.add(sir);
            CharSequence displayName = sir.getDisplayName();
            if (displayName == null) {
                displayName = "";
            }
            list.add(displayName.toString());
        }
    }

    private List<SubscriptionInfo> getAdapterData(final int id,
            final List<SubscriptionInfo> subInfoList, ArrayList<SubscriptionInfo> callsSubInfoList,
            ArrayList<SubscriptionInfo> smsSubInfoList) {
        List<SubscriptionInfo> listForAdpter = null;
        switch (id) {
            case DATA_PICK:
                listForAdpter = subInfoList;
                break;
            case CALLS_PICK:
                listForAdpter = callsSubInfoList;
                break;
            case SMS_PICK:
                listForAdpter = smsSubInfoList;
                break;
            default:
                listForAdpter = null;
                throw new IllegalArgumentException("Invalid dialog type "
                        + id + " in SIM dialog.");
        }
        return listForAdpter;
    }
    private void dismissSimDialog() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
    }
    //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    //Begin added by miaoliu for XR7107006 on 2019/1/15
     @Override
    protected void onDestroy() {
        // avoid window leak.
        dismissSimDialog();
        super.onDestroy();
    }
    //End added by miaoliu for XR7107006 on 2019/1/15
}
