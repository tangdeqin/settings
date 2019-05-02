/*
 * Copyright (C) 2013 The Android Open Source Project
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

package com.android.settings.nfc;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.PreferenceScreen;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.search.SearchIndexableRaw;

import com.android.settings.nfc.PaymentBackend.PaymentAppInfo;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.app.Activity;

import java.util.ArrayList;
import java.util.List;
//Begin added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
import android.util.Log;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.Intent;
//End added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23

public class PaymentSettings extends SettingsPreferenceFragment implements Indexable {
    public static final String TAG = "PaymentSettings";

    static final String PAYMENT_KEY = "payment";

    private PaymentBackend mPaymentBackend;
    // Added by binjian.tu.hz for Defect#2151901 on 2016/05/21 BEGIN
    private SharedPreferences mPreference;
    // Added by binjian.tu.hz for Defect#2151901 on 2016/05/21 END

    //Begin added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
    private IntentFilter intentFilter;
    private PaymentSettingsFreshReceiver haoReceiver;
    LocalBroadcastManager lbm = null;
    //End added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NFC_PAYMENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPaymentBackend.onDestroy();
        //Begin added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
        if (null != haoReceiver && null != lbm) {
            //getPrefContext().unregisterReceiver(haoReceiver);
            lbm.unregisterReceiver(haoReceiver);
        }
        //End added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPaymentBackend = new PaymentBackend(getActivity());
        // TS27 15.7.3.1.5 BEGIN
        mPaymentBackend.onCreate(icicle);
        // TS27 15.7.3.1.5 END        
        // Added by binjian.tu.hz for Defect#2151901 on 2016/05/21 BEGIN
        mPreference = getActivity().getSharedPreferences(
                com.tct.nfc.NonPaymentUserMenu.PAYMENT_SETTINGS_PREF, Context.MODE_PRIVATE);
        // Added by binjian.tu.hz for Defect#2151901 on 2016/05/21 END
        setHasOptionsMenu(true);
        //Begin added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
        if(null == lbm) {
            lbm = LocalBroadcastManager.getInstance(getActivity());
        }
        registerPayRefresh();
        //End added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
    }

    private void refresh() {
        PreferenceManager manager = getPreferenceManager();
        PreferenceScreen screen = manager.createPreferenceScreen(getActivity());

        List<PaymentBackend.PaymentAppInfo> appInfos = mPaymentBackend.getPaymentAppInfos();
        if (appInfos != null && appInfos.size() > 0) {
            NfcPaymentPreference preference =
                    new NfcPaymentPreference(getPrefContext(), mPaymentBackend);
            preference.setKey(PAYMENT_KEY);
            screen.addPreference(preference);
            NfcForegroundPreference foreground = new NfcForegroundPreference(getPrefContext(),
                    mPaymentBackend);
            screen.addPreference(foreground);
        }
        setPreferenceScreen(screen);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = getActivity().getLayoutInflater().inflate(
                R.layout.nfc_payment_empty, contentRoot, false);
        contentRoot.addView(emptyView);
        setEmptyView(emptyView);
        refresh();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPaymentBackend.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mPaymentBackend.onPause();
    }

    private final boolean USER_BUILD = "user".equals(android.os.Build.TYPE);
    private final boolean SHOW_MENU = "1".equals(android.os.SystemProperties.get("persist.nfc.debug.menu"));

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        MenuItem menuItem = menu.add(R.string.nfc_payment_how_it_works);
        Intent howItWorksIntent = new Intent(getActivity(), HowItWorks.class);
        menuItem.setIntent(howItWorksIntent);
        menuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        
        // Added by binjian.tu.hz for Defect#2151901 on 2016/05/21 BEGIN
        boolean isUserMenuShow = mPreference.getBoolean(
                com.tct.nfc.NonPaymentUserMenu.PREF_NONPAYMENT_MENU_DISPLAY, false);
        if (isUserMenuShow || !USER_BUILD || SHOW_MENU) {
            MenuItem nonPaymentMenuItem = menu.add(R.string.nfc_nonpayment_settings_title);
            Intent nonPaymentIntent = new Intent(getActivity(), com.tct.nfc.NonPaymentUserMenu.class);
            nonPaymentMenuItem.setIntent(nonPaymentIntent);
            nonPaymentMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        
        // TS27 15.7.3.1.5 Default Route BEGIN
        if (!USER_BUILD || SHOW_MENU) {
            /*MenuItem defaultRouteMenuItem = menu.add("Change default route");
            Intent changeDefaultRoute = new Intent(getActivity(),
                    com.tct.nfc.ChangeDefaultRouteActivity.class);
            defaultRouteMenuItem.setIntent(changeDefaultRoute);
            defaultRouteMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);*/
            MenuItem GSMAMenuItem = menu.add("GSMA");
            Intent GSMAIntent = new Intent(getActivity(),
                    com.tct.nfc.GSMASettings.class);
            GSMAMenuItem.setIntent(GSMAIntent);
            GSMAMenuItem.setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        // TS27 15.7.3.1.5 Default Route END
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                    final Resources res = context.getResources();

                    // Add fragment title
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.key = PAYMENT_KEY;
                    data.title = res.getString(R.string.nfc_payment_settings_title);
                    data.screenTitle = res.getString(R.string.nfc_payment_settings_title);
                    data.keywords = res.getString(R.string.keywords_payment_settings);
                    result.add(data);
                    return result;
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    final List<String> nonVisibleKeys = super.getNonIndexableKeys(context);
                    final PackageManager pm = context.getPackageManager();
                    if (pm.hasSystemFeature(PackageManager.FEATURE_NFC)) {
                        return nonVisibleKeys;
                    }
                    nonVisibleKeys.add(PAYMENT_KEY);
                    return nonVisibleKeys;
                }
            };

    //Begin added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
    private void registerPayRefresh() {
        intentFilter = new IntentFilter();
        intentFilter.addAction("tcl.paymment.settings.ui.fresh.action.TS157331");
        Log.d("jianhao", "haoreceiver is ok");
        haoReceiver = new PaymentSettingsFreshReceiver();
        //getPrefContext().registerReceiver(haoReceiver, intentFilter);
        if(null != lbm){
            lbm.registerReceiver(haoReceiver, intentFilter);
        }
    }

    class PaymentSettingsFreshReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("jianhao", "onReceive xixi ");
            refresh();
            Log.d("jianhao", "onReceive xixi2");
        }
    }
    //End added by jianhao/xdayi for TS27 15.7.3.3.1 and 15.7.3.3.2 on 2018/11/23
}
