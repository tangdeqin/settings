package com.android.settings.sim.tct;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.util.Log;

import com.android.internal.telephony.TelephonyIntents;

import java.util.Arrays;
import java.util.List;

public class SimHotSwapHandler {

    private static final String TAG = "SimHotSwapHandler";
    private SubscriptionManager mSubscriptionManager;
    private Context mContext;
    private List<SubscriptionInfo> mSubscriptionInfoList;
    private OnSimHotSwapListener mListener;
    private BroadcastReceiver mSubReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            handleHotSwap();
        }
    };

    public SimHotSwapHandler(Context context) {
        mContext = context;
        mSubscriptionManager = SubscriptionManager.from(context);
        mSubscriptionInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        Log.d(TAG, "Cache list: " + mSubscriptionInfoList);
    }

    public void registerOnSimHotSwap(OnSimHotSwapListener listener) {
        if (mContext != null) {
            mContext.registerReceiver(mSubReceiver, new IntentFilter(
                    TelephonyIntents.ACTION_SUBINFO_RECORD_UPDATED));
            mListener = listener;
        }
    }

    public void unregisterOnSimHotSwap() {
        if (mContext != null) {
            mContext.unregisterReceiver(mSubReceiver);
        }
        mListener = null;
    }

    private void handleHotSwap() {
        List<SubscriptionInfo> subscriptionInfoListCurrent =
                mSubscriptionManager.getActiveSubscriptionInfoList();
        Log.d(TAG, "handleHotSwap, current subId list: " + subscriptionInfoListCurrent);
        if (hasHotSwapHappened(mSubscriptionInfoList, subscriptionInfoListCurrent) &&
                mListener != null) {
            mListener.onSimHotSwap();
        }
    }

    public interface OnSimHotSwapListener {
        void onSimHotSwap();
    }

    /**
     * Return whether the phone is hot swap or not.
     * @return If hot swap, return true, else return false
     */
    private boolean hasHotSwapHappened(List<SubscriptionInfo> originalList,
            List<SubscriptionInfo> currentList) {
        boolean result = false;
        int oriCount = (originalList == null ? 0 : originalList.size());
        int curCount = (currentList == null ? 0 : currentList.size());

        if (oriCount == 0 && curCount == 0) {
            return false;
        }
        if (oriCount == 0 || curCount == 0 ||
                originalList.size() != currentList.size()) {
            Log.d(TAG, "hasHotSwapHappened, SIM count is different"
                    + ", oriCount=" + oriCount + ", curCount=" + curCount);
            return true;
        }
        for (int i = 0; i < currentList.size(); i++) {
            SubscriptionInfo currentSubInfo = currentList.get(i);
            SubscriptionInfo originalSubInfo = originalList.get(i);
            if (!(currentSubInfo.getIccId()).equals(originalSubInfo.getIccId())) {
                result = true;
                break;
            }
        }

        Log.d(TAG, "hasHotSwapHappened, result=" + result);
        return result;
    }
}
