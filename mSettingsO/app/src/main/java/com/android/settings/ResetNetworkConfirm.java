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

package com.android.settings;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkPolicyManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import com.android.ims.ImsManager;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.internal.telephony.PhoneConstants;
import com.android.settingslib.RestrictedLockUtils;
//Begin add by shilei.zhang for fixed XR 7343365 on 2019/01/16
import android.util.Log;
import android.os.Handler;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
//End add by shilei.zhang for fixed XR 7343365 on 2019/01/16
import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import android.os.SystemClock;//Added by miaoliu for XRP24410 on 2018/9/27

//begin add by zhixiong.liu.hz  for defect 7433093  on 20190128
import android.os.SystemProperties;
//end add by zhixiong.liu.hz  for defect 7433093  on 20190128
/**
 * Confirm and execute a reset of the network settings to a clean "just out of the box"
 * state.  Multiple confirmations are required: first, a general "are you sure
 * you want to do this?" prompt, followed by a keyguard pattern trace if the user
 * has defined one, followed by a final strongly-worded "THIS WILL RESET EVERYTHING"
 * prompt.  If at any time the phone is allowed to go to sleep, is
 * locked, et cetera, then the confirmation sequence is abandoned.
 *
 * This is the confirmation screen.
 */
public class ResetNetworkConfirm extends OptionsMenuFragment {

    private View mContentView;
    private int mSubId = SubscriptionManager.INVALID_SUBSCRIPTION_ID;
    private long mLastResetTimestamp = 0;//Added by miaoliu for XRP24410 on 2018/9/27
    //Begin add by shilei.zhang for fixed XR 7343365 on 2019/01/16
    private static final int MSG_RESTORE_NETWORK_START = 1;
    private static final int MSG_RESTORE_NETWORK_COMPLETE = 2;
    private HandlerThread mNetWorkThread;
    private RestoreNetWorkHandler mRestoreNetWorkHandler;
    private RestoreCompleteHandler mRestoreCompleteHandler;
    private ProgressDialog mProgressDialog = null;
    //End add by shilei.zhang for fixed XR 7343365 on 2019/01/16
    /**
     * The user has gone through the multiple confirmation, so now we go ahead
     * and reset the network settings to its factory-default state.
     */
    private Button.OnClickListener mFinalClickListener = new Button.OnClickListener() {

        @Override
        public void onClick(View v) {
            if (Utils.isMonkeyRunning()) {
                return;
            }

            //Begin modify by shilei.zhang for fixed XR 7343365 on 2019/01/16
            //move some codes to RestoreNetWorkHandler
            //Begin added by miaoliu for XRP24410 on 2018/9/27
            final long resetInterval = 4000; //4s
            long systime = System.currentTimeMillis();
            if (systime - mLastResetTimestamp < resetInterval) {
                //forbiden reset frequently
                return;
            }
            //End added by miaoliu for XRP24410 on 2018/9/27
            Context context = getActivity();
            showProgressDialog();
            //restoreDefaultApn(context);
            restoreDefaultNetWork(context);
            mLastResetTimestamp = System.currentTimeMillis();
            //End add by shilei.zhang for fixed XR 7343365 on 2019/01/16
        }
    };

    //Begin add by shilei.zhang for fixed XR 7343365 on 2019/01/16
    private void dismissDialog(Dialog dialog) {
        if((dialog != null) && (dialog.isShowing())) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void showProgressDialog() {
        Context context = getActivity();
        String title = context.getString(R.string.reset_network_final_button_text);

        dismissDialog(mProgressDialog);
        mProgressDialog = new ProgressDialog(context);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(title);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    private class RestoreCompleteHandler extends Handler {
        private Context mContext;
        public RestoreCompleteHandler(Context context) {
            super();
            mContext = context;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_RESTORE_NETWORK_COMPLETE:
                    dismissDialog(mProgressDialog);
                    Toast.makeText(mContext, R.string.reset_network_complete_toast,
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private class RestoreNetWorkHandler extends Handler {
        private Context mContext;
        private Handler mUiHandler;

        public RestoreNetWorkHandler(Looper looper, Context context,
                                     RestoreCompleteHandler handler) {
            super(looper);
            mContext = context;
            this.mUiHandler = handler;
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_RESTORE_NETWORK_START:

                    Context context = getActivity();
                    long systime1 = System.currentTimeMillis();
                    ConnectivityManager connectivityManager = (ConnectivityManager)
                            context.getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivityManager != null) {
                        connectivityManager.factoryReset();
                    }

                    WifiManager wifiManager = (WifiManager)
                            context.getSystemService(Context.WIFI_SERVICE);
                    if (wifiManager != null) {
                        wifiManager.factoryReset();
                    }

                    TelephonyManager telephonyManager = (TelephonyManager)
                            context.getSystemService(Context.TELEPHONY_SERVICE);
                    if (telephonyManager != null) {
                        telephonyManager.factoryReset(mSubId);
                        //begin merge by zhixiong.liu.hz for defect 7101932 on 20181108
                        if (mSubId != SubscriptionManager.getDefaultDataSubscriptionId()) {
                            telephonyManager.setDataEnabled(mSubId, false);
                        }
                        //begin add by zhixiong.liu.hz for defect 7433093  on 20190128
                        else if("false".equals(SystemProperties.get("ro.com.android.mobiledata","true"))){
                            telephonyManager.setDataEnabled(mSubId, false);
                        }
                        //end add by zhixiong.liu.hz for defect 7433093 on 20190128
                        //end merge by zhixiong.liu.hz for defect 7101932 on 20190128
                    }

                    NetworkPolicyManager policyManager = (NetworkPolicyManager)
                            context.getSystemService(Context.NETWORK_POLICY_SERVICE);
                    if (policyManager != null) {
                        String subscriberId = telephonyManager.getSubscriberId(mSubId);
                        policyManager.factoryReset(subscriberId);
                    }

                    BluetoothManager btManager = (BluetoothManager)
                            context.getSystemService(Context.BLUETOOTH_SERVICE);
                    if (btManager != null) {
                        BluetoothAdapter btAdapter = btManager.getAdapter();
                        if (btAdapter != null) {
                            btAdapter.factoryReset();
                            //[TCT-ROM][BT]Begin added by chenglong.cai for XR7148139 on 18-11-29
                            btAdapter.disable();
                            //[TCT-ROM][BT]End added by chenglong.cai for XR7148139 on 18-11-29
                        }
                    }

                    ImsManager.factoryReset(context);

                    Uri uri = Uri.parse(ApnSettings.RESTORE_CARRIERS_URI);
                    if (SubscriptionManager.isUsableSubIdValue(mSubId)) {
                        uri = Uri.withAppendedPath(uri, "subId/" + String.valueOf(mSubId));
                    }
                    ContentResolver resolver = mContext.getContentResolver();
                    resolver.delete(uri, null, null);
                    mUiHandler.sendEmptyMessage(MSG_RESTORE_NETWORK_COMPLETE);
                    break;
            }
        }
    }

    private void restoreDefaultNetWork(Context context) {
        if (mNetWorkThread == null) {
            mNetWorkThread = new HandlerThread("restore default network");
            if (mNetWorkThread == null) {
                Toast.makeText(context, R.string.band_mode_failed,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            mNetWorkThread.start();
        }
        if (mRestoreCompleteHandler == null) {
            mRestoreCompleteHandler = new RestoreCompleteHandler(context);
        }
        if (mRestoreNetWorkHandler == null) {
            mRestoreNetWorkHandler = new RestoreNetWorkHandler(
                    mNetWorkThread.getLooper(), context, mRestoreCompleteHandler);
        }
        mRestoreNetWorkHandler.sendEmptyMessage(MSG_RESTORE_NETWORK_START);
    }
    //End add by shilei.zhang for fixed XR 7343365 on 2019/01/16
    /**
     * Restore APN settings to default.
     */
    private void restoreDefaultApn(Context context) {
        Uri uri = Uri.parse(ApnSettings.RESTORE_CARRIERS_URI);

        if (SubscriptionManager.isUsableSubIdValue(mSubId)) {
            uri = Uri.withAppendedPath(uri, "subId/" + String.valueOf(mSubId));
        }

        ContentResolver resolver = context.getContentResolver();
        resolver.delete(uri, null, null);
    }

    /**
     * Configure the UI for the final confirmation interaction
     */
    private void establishFinalConfirmationState() {
        mContentView.findViewById(R.id.execute_reset_network)
                .setOnClickListener(mFinalClickListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId());
        if (RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_NETWORK_RESET, UserHandle.myUserId())) {
            return inflater.inflate(R.layout.network_reset_disallowed_screen, null);
        } else if (admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(View.VISIBLE);
            return view;
        }
        mContentView = inflater.inflate(R.layout.reset_network_confirm, null);
        establishFinalConfirmationState();
        return mContentView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mSubId = args.getInt(PhoneConstants.SUBSCRIPTION_KEY,
                    SubscriptionManager.INVALID_SUBSCRIPTION_ID);
        }
    }
    //Begin add by shilei.zhang for fixed XR 7343365 on 2019/01/16
    @Override
    public void onDestroy() {
        if (mNetWorkThread != null) {
            mNetWorkThread.quit();
        }
        //Begin add by shilei.zhang for fixed XR7403128 on 2019/01/25
        dismissDialog(mProgressDialog);
        //End add by shilei.zhang for fixed XR7403128 on 2019/01/25
        super.onDestroy();
    }
    //End add by shilei.zhang for fixed XR 7343365 on 2019/01/16
    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESET_NETWORK_CONFIRM;
    }
}
