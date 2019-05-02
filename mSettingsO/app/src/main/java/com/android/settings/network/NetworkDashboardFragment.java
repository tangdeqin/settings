/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.android.settings.network;

import static com.android.settings.network.MobilePlanPreferenceController
        .MANAGE_MOBILE_PLAN_DIALOG_ID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.content.Context;
import android.provider.SearchIndexableResource;
import android.support.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.core.instrumentation.MetricsFeatureProvider;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.network.MobilePlanPreferenceController.MobilePlanPreferenceHost;
import com.android.settings.nfc.NfcPreferenceController;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.WifiMasterSwitchPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//Begin added by miaoliu for XR5237251 on 2017/9/23
import com.android.settings.SettingsActivity;
import com.android.settings.bluetooth.BluetoothMasterSwitchPreferenceController;
import com.android.settings.bluetooth.Utils;
import com.android.settings.connecteddevice.UsbModePreferenceController;
import com.android.settings.deviceinfo.UsbBackend;
//End added by miaoliu for XR5237251 on 2017/9/23

// BEGIN XR#5440260 Added by binjian.tu on 2017/12/7
import com.android.settings.nfc.NfcMasterSwitchPreferenceController;
// END XR#5440260 Added by binjian.tu on 2017/12/7
//Begin added by miaoliu for XRP10025757 on 2018/11/8
import com.android.settings.search.SearchIndexableRaw;
import com.android.settings.wfd.WifiDisplaySettings;
//End added by miaoliu for XRP10025757 on 2018/11/8
public class NetworkDashboardFragment extends DashboardFragment implements
        MobilePlanPreferenceHost {

    private static final String TAG = "NetworkDashboardFrag";

    private NetworkResetActionMenuController mNetworkResetController;

    @Override
    public int getMetricsCategory() {
        return MetricsProto.MetricsEvent.SETTINGS_NETWORK_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.network_and_internet;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mNetworkResetController = new NetworkResetActionMenuController(context);
    }

    @Override
    protected int getHelpResource() {
        return R.string.help_url_network_dashboard;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mNetworkResetController.buildMenuItem(menu);
    }

    @Override
    protected List<AbstractPreferenceController> getPreferenceControllers(Context context) {
        //Begin modified by miaoliu for XR5237251 on 2017/9/23
//        return buildPreferenceControllers(context, getLifecycle(), mMetricsFeatureProvider, this
//                /* fragment */,
//                this /* mobilePlanHost */);
        List<AbstractPreferenceController> controllers = new ArrayList<>();
        final Lifecycle lifecycle = getLifecycle();
        controllers = buildPreferenceControllers(context, lifecycle, mMetricsFeatureProvider, this, this);
        final UsbModePreferenceController mUsbPrefController = new UsbModePreferenceController(context, new UsbBackend(context));
        lifecycle.addObserver(mUsbPrefController);
        controllers.add(mUsbPrefController);
        final BluetoothMasterSwitchPreferenceController bluetoothPreferenceController =
                new BluetoothMasterSwitchPreferenceController(
                        context, Utils.getLocalBtManager(context), this,
                        (SettingsActivity) getActivity());
        lifecycle.addObserver(bluetoothPreferenceController);
        controllers.add(bluetoothPreferenceController);
        
        // BEGIN XR#5440260 Added by binjian.tu on 2017/12/7
        final NfcMasterSwitchPreferenceController nfc = 
                new NfcMasterSwitchPreferenceController(context, this, (SettingsActivity)getActivity());
        if (nfc.isAvailable()) {
            lifecycle.addObserver(nfc);
            controllers.add(nfc);
        }
        // END XR#5440260 Added by binjian.tu on 2017/12/7
        return controllers;
        //End modified by miaoliu for XR5237251 on 2017/9/23
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle, MetricsFeatureProvider metricsFeatureProvider, Fragment fragment,
            MobilePlanPreferenceHost mobilePlanHost) {
        final AirplaneModePreferenceController airplaneModePreferenceController =
                new AirplaneModePreferenceController(context, fragment);
        final MobilePlanPreferenceController mobilePlanPreferenceController =
                new MobilePlanPreferenceController(context, mobilePlanHost);
        final WifiMasterSwitchPreferenceController wifiPreferenceController =
                new WifiMasterSwitchPreferenceController(context, metricsFeatureProvider);
        final MobileNetworkPreferenceController mobileNetworkPreferenceController =
                new MobileNetworkPreferenceController(context);
        final VpnPreferenceController vpnPreferenceController =
                new VpnPreferenceController(context);

        if (lifecycle != null) {
            lifecycle.addObserver(airplaneModePreferenceController);
            lifecycle.addObserver(mobilePlanPreferenceController);
            lifecycle.addObserver(wifiPreferenceController);
            lifecycle.addObserver(mobileNetworkPreferenceController);
            lifecycle.addObserver(vpnPreferenceController);
        }

        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        controllers.add(airplaneModePreferenceController);
        controllers.add(mobileNetworkPreferenceController);
        controllers.add(new TetherPreferenceController(context, lifecycle));
        controllers.add(vpnPreferenceController);
        controllers.add(new ProxyPreferenceController(context));
        controllers.add(mobilePlanPreferenceController);
        controllers.add(wifiPreferenceController);
        return controllers;
    }

    @Override
    public void showMobilePlanMessageDialog() {
        showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Log.d(TAG, "onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                final MobilePlanPreferenceController controller =
                        getPreferenceController(MobilePlanPreferenceController.class);
                return new AlertDialog.Builder(getActivity())
                        .setMessage(controller.getMobilePlanDialogMessage())
                        .setCancelable(false)
                        .setPositiveButton(R.string.dialog_button_ok,//modify by shilei.zhang for un-use frameworks resource and fixed XR6873798 on 2018/08/24 com.android.internal.R.string.ok,
                                (dialog, id) -> controller.setMobilePlanDialogMessage(null))
                        .create();
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (MANAGE_MOBILE_PLAN_DIALOG_ID == dialogId) {
            return MetricsProto.MetricsEvent.DIALOG_MANAGE_MOBILE_PLAN;
        }
        return 0;
    }

    @VisibleForTesting
    static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;
        private final MobileNetworkPreferenceController mMobileNetworkPreferenceController;
        private final TetherPreferenceController mTetherPreferenceController;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            this(context, summaryLoader,
                    new MobileNetworkPreferenceController(context),
                    new TetherPreferenceController(context, null /* lifecycle */));
        }

        @VisibleForTesting(otherwise = VisibleForTesting.NONE)
        SummaryProvider(Context context, SummaryLoader summaryLoader,
                MobileNetworkPreferenceController mobileNetworkPreferenceController,
                TetherPreferenceController tetherPreferenceController) {
            mContext = context;
            mSummaryLoader = summaryLoader;
            mMobileNetworkPreferenceController = mobileNetworkPreferenceController;
            mTetherPreferenceController = tetherPreferenceController;
        }


        @Override
        public void setListening(boolean listening) {
            if (listening) {
                String summary = mContext.getString(R.string.wifi_settings_title);
                if (mMobileNetworkPreferenceController.isAvailable()) {
                    final String mobileSettingSummary = mContext.getString(
                            R.string.network_dashboard_summary_mobile);
                    summary = mContext.getString(R.string.join_many_items_middle, summary,
                            mobileSettingSummary);
                }
                //Begin added by miaoliu for XR5929179 on 2018/2/5
                final String bluetoothSettingsSummary = mContext.getString(
                    R.string.bluetooth_settings_title);
                summary = mContext.getString(R.string.join_many_items_middle, summary,
                        bluetoothSettingsSummary);
                //End added by miaoliu for XR5929179 on 2018/2/5
                final String dataUsageSettingSummary = mContext.getString(
                        R.string.network_dashboard_summary_data_usage);
                summary = mContext.getString(R.string.join_many_items_middle, summary,
                        dataUsageSettingSummary);
                //Begin deleted by miaoliu for XR5929179 on 2018/2/5
                // if (mTetherPreferenceController.isAvailable()) {
                //     final String hotspotSettingSummary = mContext.getString(
                //             R.string.network_dashboard_summary_hotspot);
                //     summary = mContext.getString(R.string.join_many_items_middle, summary,
                //             hotspotSettingSummary);
                // }
                //End deleted by miaoliu for XR5929179 on 2018/2/5
                mSummaryLoader.setSummary(this, summary);
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };


    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.network_and_internet;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> getPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* metricsFeatureProvider */, null /* fragment */,
                            null /* mobilePlanHost */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Remove master switch as a result
                    keys.add(WifiMasterSwitchPreferenceController.KEY_TOGGLE_WIFI);
                    return keys;
                }
                //Begin added by miaoliu for XRP10025757 on 2018/11/8
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                    if(WifiDisplaySettings.isAvailable(context)){
                        final String screenTitle = context.getResources().getString(R.string.network_and_connected_dashboard_title);
                        SearchIndexableRaw data = new SearchIndexableRaw(context);
                        data.title = context.getResources().getString(R.string.wifi_display_settings_title);;
                        data.screenTitle = screenTitle;
                        data.iconResId = R.drawable.ic_cast_24dp;
                        result.add(data);
                     }

                    return result;
                }
                //End added by miaoliu for XRP10025757 on 2018/11/8
            };

    // Begin added by weijun.pan for XR7219365 on 2018/12/13
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
    // End added by weijun.pan for XR7219365 on 2018/12/13
}
