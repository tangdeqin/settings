/******************************************************************************/
/*                                                               Date:08/2013 */
/*                                PRESENTATION                                */
/*                                                                            */
/*       Copyright 2013 TCL Communication Technology Holdings Limited.        */
/*                                                                            */
/* This material is company confidential, cannot be reproduced in any form    */
/* without the written permission of TCL Communication Technology Holdings    */
/* Limited.                                                                   */
/*                                                                            */
/* -------------------------------------------------------------------------- */
/*  Author :  LongNa                                                          */
/*  Email  :  na.long@tcl.com                                                 */
/*  Role   :                                                                  */
/*  Reference documents :                                                     */
/* -------------------------------------------------------------------------- */
/*  Comments :                                                                */
/*  File     :                                                                */
/*  Labels   :                                                                */
/* -------------------------------------------------------------------------- */
/* ========================================================================== */
/*     Modifications on Features list / Changes Request / Problems Report     */
/* -------------------------------------------------------------------------- */
/*    date   |        author        |         Key          |     comment      */
/* ----------|----------------------|----------------------|----------------- */
/* 2013/08/24|        LongNa        |       FR715888       |Advanced NFC menu */
/* 2017/10/25|       Binjian.TU     |       5440260    |Refactor All features */
/* ----------|----------------------|----------------------|----------------- */
/******************************************************************************/

package com.tct.nfc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.nfc.cardemulation.AidGroup;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;

import com.android.internal.content.PackageMonitor;
import com.android.settings.R;
//import com.nxp.nfc.NxpConstants;
//import com.nxp.nfc.NxpNfcAdapter;
import com.tct.nfc.VNfcConstants;
import com.tct.nfc.VNfcAdapter;

import android.preference.PreferenceActivity;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import android.nfc.NfcAdapter;
//import android.nfc.cardemulation.NxpApduServiceInfo;
import com.tct.nfc.VApduServiceInfo;
import android.nfc.cardemulation.ApduServiceInfo;
//import android.nfc.cardemulation.NxpAidGroup;
import com.tct.nfc.VAidGroup;
import android.nfc.cardemulation.CardEmulation;
import android.content.ComponentName;

import android.content.SharedPreferences;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class NonPaymentUserMenu extends PreferenceActivity implements
        OnPreferenceChangeListener {
    public static final String TAG = "NonPaymentUserMenu";
    private final PackageMonitor mSettingsPackageMonitor = new SettingsPackageMonitor();
    //private NxpNfcAdapter mNxpNfcAdapter;
    private VNfcAdapter mNxpNfcAdapter;
    private CardEmulation mCardEmuManager;
    private static final int DEFAULT_ROUTE_HCE = 0x00;
    private static final int DEFAULT_ROUTE_UICC = 0x02;
    public  static final String PAYMENT_SETTINGS_PREF = "com.tct.nfc.NfcPaymentPreference";
    public  static final String PREF_NONPAYMENT_MENU_DISPLAY = "com.tct.nfc.nonPayment_menu_display";

    private Map<String, Integer> mAidCacheSize = new HashMap<String, Integer>();
    private Map<String, Boolean> mAidState = new HashMap<String, Boolean>();

    // Added by binjian.tu.hz on 2016/10/27 for Defect#3202252 BEGIN
    public  static final String PREF_AIDS_FAILTOCOMMIT_COUNT = "aids_fail_to_commit_count";
    public  static final String PREF_AIDS_FAILTOCOMMIT_PREF = "aids_fail_to_commit_index_";
    //public  static final String PREF_AIDS_PARTIALLY_SUPPORT_COUNT = "aids_partially_support_count";
    //public  static final String PREF_AIDS_PARTIALLY_SUPPORT_PREF = "aids_partially_support_index_";
    private HashSet<String> mAidsFailToCommit;
    //private HashSet<String> mPartiallySupportPkgs;
    private static final int MSG_REFRESH = 110;

    private static final String SUMMARY_KEY = "_2_summery";

    private static HashSet<String> cubeFrom(Intent intent) {
        int count = intent.getIntExtra("count", 0);
        if (count == 0) {
            return null;
        }
        HashSet<String> hashSet = new HashSet<>();
        for (int i = 0; i < count; ++i) {
            String key = intent.getStringExtra(Integer.toString(i));
            if (key != null) {
                hashSet.add(key);
            }
        }
        if (hashSet.size() == 0) {
            return null;
        }
        return hashSet;
    }

    private static void persistent(SharedPreferences preferences,
                                   HashSet<String> data,
                                   String countKey,
                                   String elementKey) {
        if (data == null) {
            Log.e(TAG, "data is null!");
            return;
        }
        int count = data.size();
        SharedPreferences.Editor e = preferences.edit();
        e.putInt(countKey, count);
        int i = 0;
        for (String d : data) {
            e.putString(elementKey + Integer.toString(i++), d);
        }
        e.apply();
    }

    private static HashSet<String> cubeFrom(SharedPreferences preferences,
                                            String countKey,
                                            String elementKey) {
        int count = preferences.getInt(countKey, 0);
        if (count == 0) {
            return null;
        }
        HashSet<String> hashSet = new HashSet<>();
        String data;
        for (int i = 0; i < count; ++i) {
            data = preferences.getString(elementKey + Integer.toString(i), "N/A");
            if ("N/A".equals(data)) {
                Log.e(TAG, "cubeFrom persistent file error.");
                return null;
            }
            hashSet.add(data);
        }
        return hashSet;
    }

    private boolean isRoutingTableFull() {
        return Settings.Global.getInt(getContentResolver(), "aid_routing_table_full", 0x00) == 0x01;
    }

    /**
     * @return
     *  -1 : totally different
     *  1  : completely identical
     *  0  : partially identical
     * */
    private int isExclusiveForAidsFailToCommit(ArrayList<VAidGroup> list) {
        boolean different = false;
        boolean identical = false;
        int count = 0;
        for (VAidGroup aidGroup : list) {
            List<String> aids = aidGroup.getAids();
            for (String aid : aids) {
                ++count;
                if (mAidsFailToCommit.contains(aid)) {
                    identical = true;
                } else {
                    different = true;
                }
                if (different && identical) {
                    return 0;
                }
            }
        }
        return count == 0 ? -1 : (different ? -1 : 1);
    }

    /**
     * @return
     *  -1 : totally different
     *  1  : completely identical
     *  0  : partially identical
     * */
    private int isExclusiveForAidsFailToCommit(VApduServiceInfo info) {
        int result;
        ArrayList<VAidGroup> list = info.getStaticVendorAidGroups();
        if (list == null || list.size() == 0) {
            list = info.getDynamicVendorAidGroups();
            result = isExclusiveForAidsFailToCommit(list);
            return result;
        }
        result = isExclusiveForAidsFailToCommit(list);
        if (result == 0) {
            return result;
        }
        list = info.getDynamicVendorAidGroups();
        if (list == null || list.size() == 0) {
            return result;
        }
        switch (isExclusiveForAidsFailToCommit(list)) {
            case 0: // -1 & 0 || 1 & 0
                return 0;
            case 1: // -1 & 1 || 1 & 1
                return result == -1 ? 0 : 1;
            case -1: // -1 & -1 || 1 & -1
                return result == -1 ? -1 : 0;
            default:
                Log.e(TAG, "error:isExclusiveForAidsFailToCommit");
                return -1;
        }
    }

    private boolean mPaused = false;
    private boolean mNeedRefresh = true;
    private IntentFilter mRoutingCommitIntent = new IntentFilter("com.tct.nfc.action.COMMIT_ROUTING");
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPaused) {
                mNeedRefresh = true;
            } else {
                mHandler.obtainMessage(MSG_REFRESH).sendToTarget();
            }
        }
    };

    private PreferenceScreen mScreen;
    private TextView mSummary;

    private Preference createPreferenceIfNeeded(PreferenceScreen screen, String key) {
        Preference preference = screen.findPreference(key);
        if (preference == null) {
            preference = new Preference(this);
            preference.setSelectable(false);
            preference.setKey(key);
            screen.addPreference(preference);
        }
        return preference;
    }

    private PreferenceScreen createPreferenceScreenIfNeeded(PreferenceManager manager) {
        if (mScreen == null) {
            mScreen = manager.createPreferenceScreen(this);
        }
        return mScreen;
    }

    private CheckBoxPreference createCheckBoxPreferenceIfNeeded(PreferenceScreen screen, String key) {
        Preference preference = screen.findPreference(key);
        if (preference != null) {
            return (CheckBoxPreference)preference;
        }
        CheckBoxPreference cbp = new CheckBoxPreference(this);
        cbp.setKey(key);
        screen.addPreference(cbp);
        return cbp;
    }

    @Override
    protected void onDestroy() {
        mSettingsPackageMonitor.unregister();
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private void createSummaryView() {
        ViewGroup content = (ViewGroup)findViewById(Window.ID_ANDROID_CONTENT);
        ViewGroup frame = (ViewGroup)content.getChildAt(0);
        mSummary = new TextView(this);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        mSummary.setPaddingRelative(20, 20, 20, 10);
        mSummary.setLayoutParams(lp);
        mSummary.setText(getResources().getString(R.string.nfc_nonpayment_explanation_title));
        mSummary.getPaint().setFakeBoldText(true);
        mSummary.setTextColor(Color.BLACK);
        LinearLayout layout = new LinearLayout(this);
        lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setLayoutParams(lp);
        View child = frame.getChildAt(0);
        frame.removeAllViews();
        layout.addView(mSummary);
        layout.addView(child);
        frame.addView(layout);
    }
    // Added by binjian.tu.hz on 2016/10/27 for Defect#3202252 END

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        Log.i(TAG, "onCreate");
        mSettingsPackageMonitor.register(this, this.getMainLooper(), false);

        final Intent intent = getIntent();
        SharedPreferences preferences = getSharedPreferences(PAYMENT_SETTINGS_PREF, Context.MODE_PRIVATE);
        // Modified by binjian.tu.hz on 2016/10/27 for Defect#3202252 BEGIN
        if (intent != null && VNfcConstants.ACTION_ROUTING_TABLE_FULL.equals(intent.getAction())) {
            mAidsFailToCommit = cubeFrom(intent);
            if (mAidsFailToCommit == null) {
                Log.e(TAG, "boot completed and Routing cache is empty? Load from persistent.");
                mAidsFailToCommit = cubeFrom(preferences,
                        PREF_AIDS_FAILTOCOMMIT_COUNT,
                        PREF_AIDS_FAILTOCOMMIT_PREF);
            } else {
                persistent(preferences,
                        mAidsFailToCommit,
                        PREF_AIDS_FAILTOCOMMIT_COUNT,
                        PREF_AIDS_FAILTOCOMMIT_PREF);
            }
        } else if (isRoutingTableFull()) {
            mAidsFailToCommit = cubeFrom(preferences,
                    PREF_AIDS_FAILTOCOMMIT_COUNT,
                    PREF_AIDS_FAILTOCOMMIT_PREF);
        }
        // Modified by binjian.tu.hz on 2016/10/27 for Defect#3202252 END

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(this);
        //if ((adapter != null) && (mNxpNfcAdapter == null)) {
        //    mNxpNfcAdapter = NxpNfcAdapter.getNxpNfcAdapter(adapter);
        //}
        mNxpNfcAdapter = VNfcAdapter.instance;
        mCardEmuManager = CardEmulation.getInstance(adapter);

        View v = inflater.inflate(R.layout.nfc_nonpayment, null);
        SharedPreferences.Editor nonPaymentPrefsEditor = preferences.edit();
        if (!preferences.getBoolean(PREF_NONPAYMENT_MENU_DISPLAY, false)) {
            nonPaymentPrefsEditor.putBoolean(PREF_NONPAYMENT_MENU_DISPLAY, true);
            nonPaymentPrefsEditor.apply();
        }
        setContentView(v);
        // Added by binjian.tu.hz on 2016/10/27 for Defect#3202252 BEGIN
        createSummaryView();
        registerReceiver(mReceiver, mRoutingCommitIntent);
        // Added by binjian.tu.hz on 2016/10/27 for Defect#3202252 END
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.i(TAG, "onNewIntent");
        // Modified by binjian.tu.hz on 2016/10/27 for Defect#3202252 BEGIN
        boolean isRoutingTableFull;
        SharedPreferences preferences = getSharedPreferences(PAYMENT_SETTINGS_PREF, Context.MODE_PRIVATE);
        if (intent != null && intent.getAction() != null &&
                intent.getAction().equals(VNfcConstants.ACTION_ROUTING_TABLE_FULL)) {
            mAidsFailToCommit = cubeFrom(intent);
            persistent(preferences,
                    mAidsFailToCommit,
                    PREF_AIDS_FAILTOCOMMIT_COUNT,
                    PREF_AIDS_FAILTOCOMMIT_PREF);
            isRoutingTableFull = true;
        } else if ((isRoutingTableFull = isRoutingTableFull())) {
            mAidsFailToCommit = cubeFrom(preferences,
                    PREF_AIDS_FAILTOCOMMIT_COUNT,
                    PREF_AIDS_FAILTOCOMMIT_PREF);
        }
        refresh(isRoutingTableFull);
        // Modified by binjian.tu.hz on 2016/10/27 for Defect#3202252 END
    }

    // Modified by binjian.tu.hz on 2017/12/12 for Defect#3202252 BEGIN
    Pair<VApduServiceInfo, Integer> getApduServiceInfo(String component, String category) {
        List<VApduServiceInfo> serviceInfos = mNxpNfcAdapter.getVendorServicesInfo(category);
        if (serviceInfos != null && serviceInfos.size() > 0) {
            if (mAidsFailToCommit == null) {
                for (VApduServiceInfo info : serviceInfos) {
                    if (info.getComponent().flattenToString().equals(component)) {
                        return new Pair<>(info, -1);
                    }
                }
                return null;
            } else {
                for (VApduServiceInfo info : serviceInfos) {
                    if (info.getComponent().flattenToString().equals(component)) {
                        int state = isExclusiveForAidsFailToCommit(info);
                        return new Pair<>(info, state);
                    }
                }
                return null;
            }
        }
        return null;
    }
    // Modified by binjian.tu.hz on 2017/12/12 for Defect#3202252 END

    private void removeInvalidEntriesByMap(PreferenceScreen screen, HashMap<String, Object> slash) {
        final int count = screen.getPreferenceCount();
        ArrayList<Preference> aboutToRemove = new ArrayList<>();
        for (int i = 0; i < count; ++i) {
            Preference p = screen.getPreference(i);
            if (null == slash.get(p.getKey())) {
                aboutToRemove.add(p);
            }
        }
        for (Preference p : aboutToRemove) {
            screen.removePreference(p);
        }
    }

    public void refresh(boolean isRoutingTableFull) {
        mAidCacheSize.clear();
        mAidState.clear();
        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = createPreferenceScreenIfNeeded(manager);
        Preference preference = screen.findPreference(SUMMARY_KEY);
        if (preference != null) screen.removePreference(preference);

        PackageManager pm = getPackageManager();
        Log.d(TAG, "aid_routing_table_full: " + isRoutingTableFull);
        mSummary.setVisibility(isRoutingTableFull ? View.VISIBLE : View.GONE);

        try {
            mAidCacheSize = mNxpNfcAdapter.getServicesAidCacheSize(UserHandle.getCallingUserId(), CardEmulation.CATEGORY_OTHER);
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "get Services AidCache size failed!");
        }
        int route = mNxpNfcAdapter.GetDefaultRouteLoc();
        final Resources res = getResources();
        final String spaceSummaryFormat = res.getString(R.string.nfc_nonpayment_service_space_summary);
        final String partiallySupported = res.getString(R.string.nfc_nonpayment_partially_supported);

        HashMap<String, Object> services = new HashMap<>();

        for (Map.Entry<String, Integer>componEntry : mAidCacheSize.entrySet()) {
            services.put(componEntry.getKey(), componEntry);
            Pair<VApduServiceInfo, Integer> nonPaymentService = getApduServiceInfo(componEntry.getKey(), CardEmulation.CATEGORY_OTHER);
            if (nonPaymentService == null) {
                continue;
            }
            VApduServiceInfo info = nonPaymentService.first;
            int state = nonPaymentService.second;
            CheckBoxPreference cbp = createCheckBoxPreferenceIfNeeded(screen, componEntry.getKey());
            cbp.setIcon(info.loadBanner(pm));
            String label = (String) info.loadLabel(pm);
            if (label == null) {
                label = componEntry.getKey();
            }

            String title = "app";
            if (info.isOnHost()) {
                title = "HCE " + title + " " + label;
            } else {
                title = "SIM " + title + " " + label;
            }
            cbp.setTitle(title);
            int serviceAidCacheSize = componEntry.getValue();
            // Modified by binjian.tu.hz on 2016/10/27 for Defect#3202252 BEGIN
            if (state != -1 && (info.isOnHost() ? route != DEFAULT_ROUTE_HCE : route == DEFAULT_ROUTE_HCE)) {
                cbp.setEnabled(false);
                info.enableService(CardEmulation.CATEGORY_OTHER, false);
                mAidState.put(componEntry.getKey(), false);
                if (state == 0) {
                    cbp.setSummary(String.format(spaceSummaryFormat, serviceAidCacheSize) +
                            "\r\n" + partiallySupported);
                    cbp.setChecked(true);
                } else {
                    cbp.setSummary(String.format(spaceSummaryFormat, serviceAidCacheSize));
                    cbp.setChecked(false);
                }
            } else {
                boolean enable = info.isServiceEnabled(CardEmulation.CATEGORY_OTHER);
                cbp.setChecked(enable);
                cbp.setEnabled(true);
                mAidState.put(componEntry.getKey(), cbp.isChecked());
                cbp.setSummary(String.format(spaceSummaryFormat, serviceAidCacheSize));
            }
            // Modified by binjian.tu.hz on 2016/10/27 for Defect#3202252 END
            cbp.setOnPreferenceChangeListener(this);
        }
        removeInvalidEntriesByMap(screen, services);
        Preference mNfcSummery = createPreferenceIfNeeded(screen, SUMMARY_KEY);
        mNfcSummery.setSelectable(false);
        Log.e(TAG,"DefaultRouteGet = " + route);
        int remainAidSpace = mNxpNfcAdapter.getRemainingAidTableSize();
        if (route == DEFAULT_ROUTE_HCE) {
            if (isRoutingTableFull) {
                mNfcSummery.setSummary(getResources().getString(R.string.nfc_nonpayment_contextual_sim_full_title));
            } else {
                mNfcSummery.setSummary(String.format(getString(R.string.nfc_nonpayment_contextual_sim_enough_title), remainAidSpace));
            }
        } else { // UICC
            if (isRoutingTableFull) {
                mNfcSummery.setSummary(getResources().getString(R.string.nfc_nonpayment_contextual_hce_full_title));
            } else {
                mNfcSummery.setSummary(String.format(getString(R.string.nfc_nonpayment_contextual_hce_enough_title), remainAidSpace));
            }
        }
        getListView().setVisibility(View.VISIBLE);
        setPreferenceScreen(screen);
    }

    private void removeAllEntries() {
        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = createPreferenceScreenIfNeeded(manager);
        if (screen != null) screen.removeAll();
    }

    private boolean mRemoveAll = false;

    @Override
    public void onResume() {
        super.onResume();
        //mSettingsPackageMonitor.register(this, this.getMainLooper(), false);
        mPaused = false;
        if (mRemoveAll) {
            mRemoveAll = false;
            removeAllEntries();
            mHandler.obtainMessage(MSG_REFRESH).sendToTarget();
            mNeedRefresh = false;
        } else if (mNeedRefresh) {
            mHandler.obtainMessage(MSG_REFRESH).sendToTarget();
            mNeedRefresh = false;
        }
    }

    @Override
    public void onPause() {
        //mSettingsPackageMonitor.unregister();
        super.onPause();
        mPaused = true;
    }

    static class H extends Handler {
        private WeakReference<NonPaymentUserMenu> mContext;

        public H(NonPaymentUserMenu context) {
            mContext = new WeakReference<>(context);
        }

        @Override
        public void dispatchMessage(Message msg) {
            NonPaymentUserMenu context = mContext.get();
            if (context == null) {
                return;
            }

            switch (msg.what) {
                case MSG_REFRESH:
                    boolean isFull = context.isRoutingTableFull();
                    if (!isFull) {
                        context.mAidsFailToCommit = null;
                    }
                    context.refresh(isFull);
                    break;
            }
        }
    }

    private final H mHandler = new H(this);

    private class SettingsPackageMonitor extends PackageMonitor {

        private void postRefreshMessage() {
            mHandler.obtainMessage(MSG_REFRESH).sendToTarget();
        }

        @Override
        public void onPackageAdded(String packageName, int uid) {
            postRefreshMessage();
        }

        @Override
        public void onPackageAppeared(String packageName, int reason) {
            postRefreshMessage();
        }

        @Override
        public void onPackageDisappeared(String packageName, int reason) {
            mRemoveAll = true;
        }

        @Override
        public void onPackageRemoved(String packageName, int uid) {
            mRemoveAll = true;
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference instanceof CheckBoxPreference) {
            CheckBoxPreference v = (CheckBoxPreference) preference;
            String component = preference.getKey();
            ComponentName comp = ComponentName.unflattenFromString(component);
            Log.d(TAG, "onPreferenceChange comp " + comp);
            boolean isOn = (boolean)newValue;
            mAidState.put(component, isOn);
            try {
                mNxpNfcAdapter.updateServiceState(mAidState);
            } catch (IOException e) {
                e.printStackTrace();
            }
            v.setChecked(isOn);
            return true;
        }
        return false;
    }
}
