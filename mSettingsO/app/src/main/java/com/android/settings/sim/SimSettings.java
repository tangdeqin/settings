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
import com.android.settings.R;
import com.android.settings.RestrictedSettingsFragment;
import com.android.settings.Utils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;

import java.util.ArrayList;
import java.util.List;
//Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
import com.android.settings.sim.tct.*;
import android.provider.Settings;
import com.android.internal.telephony.Phone;
import android.content.IntentFilter;
import com.android.internal.telephony.TelephonyIntents;
import android.content.BroadcastReceiver;
import android.support.v7.preference.PreferenceCategory;
import android.telecom.PhoneAccount;
import android.telecom.PhoneAccountHandle;
import com.android.settings.sim.tct.SimHotSwapHandler.OnSimHotSwapListener;
//End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

public class SimSettings extends RestrictedSettingsFragment implements Indexable {
    private static final String TAG = "SimSettings";
    private static final boolean DBG = false;

    private static final String DISALLOW_CONFIG_SIM = "no_config_sim";
    private static final String SIM_CARD_CATEGORY = "sim_cards";
    private static final String KEY_CELLULAR_DATA = "sim_cellular_data";
    private static final String KEY_CALLS = "sim_calls";
    private static final String KEY_SMS = "sim_sms";
    public static final String EXTRA_SLOT_ID = "slot_id";

    //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    private static final String KEY_SIM_ACTIVITIES = "sim_activities";
    private static final String KEY_34G_service = "sim_34G_service";
    public static final int SERVICE_PICK = 10;
    public static final String _3G4GSERVICE = "_3G4G_service_sim_setting";
    private static final String PROPERTY_3G_SIM = "persist.radio.simswitch";
    private static final int s34gServiceOFF = 2;
    //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

    /**
     * By UX design we use only one Subscription Information(SubInfo) record per SIM slot.
     * mAvalableSubInfos is the list of SubInfos we present to the user.
     * mSubInfoList is the list of all SubInfos.
     * mSelectableSubInfos is the list of SubInfos that a user can select for data, calls, and SMS.
     */
    private List<SubscriptionInfo> mAvailableSubInfos = null;
    private List<SubscriptionInfo> mSubInfoList = null;
    private List<SubscriptionInfo> mSelectableSubInfos = null;
    private PreferenceScreen mSimCards = null;
    private SubscriptionManager mSubscriptionManager;
    private int mNumSlots;
    private Context mContext;

    private int mPhoneCount = TelephonyManager.getDefault().getPhoneCount();
    private int[] mCallState = new int[mPhoneCount];
    private PhoneStateListener[] mPhoneStateListener = new PhoneStateListener[mPhoneCount];

    public SimSettings() {
        super(DISALLOW_CONFIG_SIM);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SIM;
    }

    @Override
    public void onCreate(final Bundle bundle) {
        super.onCreate(bundle);
        mContext = getActivity();

        mSubscriptionManager = SubscriptionManager.from(getActivity());
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        addPreferencesFromResource(R.xml.sim_settings);

        mNumSlots = tm.getSimCount();
        mSimCards = (PreferenceScreen)findPreference(SIM_CARD_CATEGORY);
        mAvailableSubInfos = new ArrayList<SubscriptionInfo>(mNumSlots);
        mSelectableSubInfos = new ArrayList<SubscriptionInfo>();
        SimSelectNotification.cancelNotification(getActivity());
        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        /// M: for [SIM Hot Swap], [SIM Radio On/Off] etc.
        initForSimStateChange();

        /// M: for radio switch control
        mRadioController = RadioPowerController.getInstance(getContext());

        addServicePreference();
        //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    }

    private final SubscriptionManager.OnSubscriptionsChangedListener mOnSubscriptionsChangeListener
            = new SubscriptionManager.OnSubscriptionsChangedListener() {
        @Override
        public void onSubscriptionsChanged() {
            if (DBG) log("onSubscriptionsChanged:");
            updateSubscriptions();
        }
    };

    private void updateSubscriptions() {
        mSubInfoList = mSubscriptionManager.getActiveSubscriptionInfoList();
        for (int i = 0; i < mNumSlots; ++i) {
            Preference pref = mSimCards.findPreference("sim" + i);
            if (pref instanceof SimPreference) {
                mSimCards.removePreference(pref);
            }
            //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            if (pref instanceof TclSimPreference) {
                mSimCards.removePreference(pref);
            }
            //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        }
        mAvailableSubInfos.clear();
        mSelectableSubInfos.clear();

        for (int i = 0; i < mNumSlots; ++i) {
            final SubscriptionInfo sir = mSubscriptionManager
                    .getActiveSubscriptionInfoForSimSlotIndex(i);
            //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            /*
            SimPreference simPreference = new SimPreference(getPrefContext(), sir, i);
            */
            TclSimPreference simPreference = new TclSimPreference(getPrefContext(), sir, i);
            simPreference.setOrder(i-mNumSlots);

            /// M: for [SIM Radio On/Off]
            if (sir != null) {
                int subId = sir.getSubscriptionId();
                simPreference.bindRadioPowerState(subId,
                        !mIsAirplaneModeOn && mRadioController.isRadioSwitchComplete(subId));
            } else {
                simPreference.bindRadioPowerState(SubscriptionManager.INVALID_SUBSCRIPTION_ID,
                        !mIsAirplaneModeOn && mRadioController.isRadioSwitchComplete(
                                SubscriptionManager.INVALID_SUBSCRIPTION_ID));
            }

            logInEng("addPreference slot " + i);
            //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            mSimCards.addPreference(simPreference);
            mAvailableSubInfos.add(sir);
            if (sir != null) {
                mSelectableSubInfos.add(sir);
            }
        }
        updateAllOptions();
    }

    private void updateAllOptions() {
        updateSimSlotValues();
        updateActivitesCategory();
    }

    private void updateSimSlotValues() {
        final int prefSize = mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof SimPreference) {
                ((SimPreference)pref).update();
            }
            //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
            if (pref instanceof TclSimPreference) {
                Log.d(TAG, "updateSimSlotValues TclSimPreference");
                ((TclSimPreference)pref).update();
            }
            //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        }
    }

    private void updateActivitesCategory() {
        updateCellularDataValues();
        updateCallValues();
        updateSmsValues();
        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        update34GServiceValues();
        //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    }

    private void updateSmsValues() {
        final Preference simPref = findPreference(KEY_SMS);
        final SubscriptionInfo sir = mSubscriptionManager.getDefaultSmsSubscriptionInfo();
        simPref.setTitle(R.string.sms_messages_title);
        if (DBG) log("[updateSmsValues] mSubInfoList=" + mSubInfoList);

        //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        /*
        if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
            simPref.setEnabled(mSelectableSubInfos.size() > 1);
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
            simPref.setEnabled(mSelectableSubInfos.size() >= 1);
        }
        */
        if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
        } else if (sir == null) {
            simPref.setSummary(mContext.getResources().getString(R.string.sim_sms_both_sim_cards_prefs_title));
        }
        boolean enabled = sir == null ? mSelectableSubInfos.size() >= 1
                : mSelectableSubInfos.size() > 1;

        boolean isRadioOn1 = TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(0)[0], mContext);
        boolean isRadioOn2 = TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(1)[0], mContext);
        Log.d(TAG, "updateSmsValues isRadioOn1:" + isRadioOn1 + ", isRadioOn2:" + isRadioOn2 + ", sir:" + sir);
        Log.d(TAG, "updateSmsValues subid 1:" + SubscriptionManager.getSubId(0)[0] + ", 2:" + SubscriptionManager.getSubId(1)[0]);

        enabled = enabled && isRadioOn1 && isRadioOn2;
        simPref.setEnabled(enabled);
        //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    }

    private void updateCellularDataValues() {
        final Preference simPref = findPreference(KEY_CELLULAR_DATA);
        final SubscriptionInfo sir = mSubscriptionManager.getDefaultDataSubscriptionInfo();
        simPref.setTitle(R.string.cellular_data_title);
        if (DBG) log("[updateCellularDataValues] mSubInfoList=" + mSubInfoList);


        boolean callStateIdle = isCallStateIdle();

        final boolean ecbMode = SystemProperties.getBoolean(
                TelephonyProperties.PROPERTY_INECM_MODE, false);
        //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        /*
        if (sir != null) {
            simPref.setSummary(sir.getDisplayName());
            // Enable data preference in msim mode and call state idle
            simPref.setEnabled((mSelectableSubInfos.size() > 1) && callStateIdle && !ecbMode);
        } else if (sir == null) {
            simPref.setSummary(R.string.sim_selection_required_pref);
            // Enable data preference in msim mode and call state idle
            simPref.setEnabled((mSelectableSubInfos.size() >= 1) && callStateIdle && !ecbMode);
        }
        */
        boolean capSwitching = TclInterfaceAdapter.isCapabilitySwitching(mContext);
        boolean inCall = TelecomManager.from(mContext).isInCall();

        final TelephonyManager tm =
                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        final int mDDSubId = SubscriptionManager.getDefaultDataSubscriptionId();
        if (sir != null) {
            if (!tm.getDataEnabled(mDDSubId)) {
                simPref.setSummary(mContext.getResources().getString(R.string.sim_calls_cellular_data_disabled_new));//Modified by miaoliu for XR7349158 on 2019/1/18
            } else {
                simPref.setSummary(sir.getDisplayName());
            }
        } else if (sir == null) {
            simPref.setSummary(mContext.getResources().getString(R.string.sim_selection_required_pref));
        }

        boolean isRadioOn1 = TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(0)[0], mContext);
        boolean isRadioOn2 = TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(1)[0], mContext);
        boolean enabled = mSelectableSubInfos.size() >= 1 && callStateIdle /*&& capSwitching*/ && !ecbMode
                && (isRadioOn1 || isRadioOn2);//Modified by miaoliu for XRP10026499 on 2018/11/21
        //Modified && to || by yeqing.lv for 7442130 on 2019/2/14

        enabled = enabled && !(TclInterfaceAdapter.isNeedDisableSimPref(mContext, mSubInfoList));
        Log.d(TAG, "updateCellularDataValues enabled:" + enabled + ", mSelectableSubInfos.size() >= 1:" + (mSelectableSubInfos.size() >= 1)
                + ", callStateIdle:" + callStateIdle + ", capSwitching:" + capSwitching + ", !ecbMode:" + !ecbMode);
        simPref.setEnabled(enabled);
        //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    }

    private void updateCallValues() {
        final Preference simPref = findPreference(KEY_CALLS);
        final TelecomManager telecomManager = TelecomManager.from(mContext);
        final PhoneAccountHandle phoneAccount =
            telecomManager.getUserSelectedOutgoingPhoneAccount();
        final List<PhoneAccountHandle> allPhoneAccounts =
            telecomManager.getCallCapablePhoneAccounts();

        simPref.setTitle(R.string.calls_title);
        //Begin modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        /*
        simPref.setSummary(phoneAccount == null
                ? mContext.getResources().getString(R.string.sim_calls_ask_first_prefs_title)
                : (String)telecomManager.getPhoneAccount(phoneAccount).getLabel());
        simPref.setEnabled(allPhoneAccounts.size() > 1);
        */

        String summary = mContext.getResources().getString(R.string.sim_calls_both_sim_cards_prefs_title);
        if(phoneAccount != null){
            PhoneAccount account = telecomManager.getPhoneAccount(phoneAccount);
            if(account != null){
                summary = (String)account.getLabel();
            }
        }
        simPref.setSummary(summary);

        boolean isRadioOn1 = TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(0)[0], mContext);
        boolean isRadioOn2 = TclInterfaceAdapter.isRadioOn(SubscriptionManager.getSubId(1)[0], mContext);

        Log.d(TAG, "updateCallValues summary:" + summary + ", all phone accounts size:" + allPhoneAccounts.size());
        Log.d(TAG, "updateCallValues isRadioOn1:" + isRadioOn1 + ", isRadioOn2:" + isRadioOn2);

        simPref.setEnabled(allPhoneAccounts.size() > 1 && isRadioOn1 && isRadioOn2);
        //End modified by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    }

    @Override
    public void onResume() {
        super.onResume();
        mSubscriptionManager.addOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        updateSubscriptions();
        final TelephonyManager tm =
                (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        if (mSelectableSubInfos.size() > 1) {
            Log.d(TAG, "Register for call state change");
            for (int i = 0; i < mPhoneCount; i++) {
                int subId = mSelectableSubInfos.get(i).getSubscriptionId();
                tm.listen(getPhoneStateListener(i, subId),
                        PhoneStateListener.LISTEN_CALL_STATE);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        mSubscriptionManager.removeOnSubscriptionsChangedListener(mOnSubscriptionsChangeListener);
        final TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        for (int i = 0; i < mPhoneCount; i++) {
            if (mPhoneStateListener[i] != null) {
                tm.listen(mPhoneStateListener[i], PhoneStateListener.LISTEN_NONE);
                mPhoneStateListener[i] = null;
            }
        }

        //Begin added by zubai.li for XR7101841 telecomcode on 2018/11/11
        final int prefSize = mSimCards.getPreferenceCount();
        for (int i = 0; i < prefSize; ++i) {
            Preference pref = mSimCards.getPreference(i);
            if (pref instanceof TclSimPreference) {
                Log.d(TAG, "onPause()");
                ((TclSimPreference)pref).pause();
            }
        }
        //End added by zubai.li for XR7101841 telecomcode on 2018/11/11
    }

    private PhoneStateListener getPhoneStateListener(int phoneId, int subId) {
        // Disable Sim selection for Data when voice call is going on as changing the default data
        // sim causes a modem reset currently and call gets disconnected
        // ToDo : Add subtext on disabled preference to let user know that default data sim cannot
        // be changed while call is going on
        final int i = phoneId;
        mPhoneStateListener[phoneId]  = new PhoneStateListener(subId) {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (DBG) log("PhoneStateListener.onCallStateChanged: state=" + state);
                mCallState[i] = state;
                updateCellularDataValues();
            }
        };
        return mPhoneStateListener[phoneId];
    }

    @Override
    public boolean onPreferenceTreeClick(final Preference preference) {
        final Context context = mContext;
        Intent intent = new Intent(context, SimDialogActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
        if (preference instanceof TclSimPreference) {
            TctSimPreferenceDialog mTctSimPreference = new TctSimPreferenceDialog();
            int slotId = ((TclSimPreference)preference).getSlotId();
            mTctSimPreference.showdialog(mContext, slotId,
                    TclDualSimManagement.getInstance(mContext).getNetModeStrings(slotId),
                    TclDualSimManagement.getInstance(mContext).getNetModeArray(slotId));
            return true;
        }
        if (findPreference(KEY_34G_service) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SERVICE_PICK);
            context.startActivity(intent);
            return true;
        }
        //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23

        if (preference instanceof SimPreference) {
            Intent newIntent = new Intent(context, SimPreferenceDialog.class);
            newIntent.putExtra(EXTRA_SLOT_ID, ((SimPreference)preference).getSlotId());
            startActivity(newIntent);
        } else if (findPreference(KEY_CELLULAR_DATA) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.DATA_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_CALLS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.CALLS_PICK);
            context.startActivity(intent);
        } else if (findPreference(KEY_SMS) == preference) {
            intent.putExtra(SimDialogActivity.DIALOG_TYPE_KEY, SimDialogActivity.SMS_PICK);
            context.startActivity(intent);
        }

        return true;
    }

    private class SimPreference extends Preference {
        private SubscriptionInfo mSubInfoRecord;
        private int mSlotId;
        Context mContext;

        public SimPreference(Context context, SubscriptionInfo subInfoRecord, int slotId) {
            super(context);

            mContext = context;
            mSubInfoRecord = subInfoRecord;
            mSlotId = slotId;
            setKey("sim" + mSlotId);
            update();
        }

        public void update() {
            final Resources res = mContext.getResources();

            setTitle(String.format(mContext.getResources()
                    .getString(R.string.sim_editor_title), (mSlotId + 1)));
            if (mSubInfoRecord != null) {
                if (TextUtils.isEmpty(getPhoneNumber(mSubInfoRecord))) {
                    setSummary(mSubInfoRecord.getDisplayName());
                } else {
                    setSummary(mSubInfoRecord.getDisplayName() + " - " +
                            PhoneNumberUtils.createTtsSpannable(getPhoneNumber(mSubInfoRecord)));
                    setEnabled(true);
                }
                setIcon(new BitmapDrawable(res, (mSubInfoRecord.createIconBitmap(mContext))));
            } else {
                setSummary(R.string.sim_slot_empty);
                setFragment(null);
                setEnabled(false);
            }
        }

        private int getSlotId() {
            return mSlotId;
        }
    }

    // Returns the line1Number. Line1number should always be read from TelephonyManager since it can
    // be overridden for display purposes.
    private String getPhoneNumber(SubscriptionInfo info) {
        final TelephonyManager tm =
            (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return tm.getLine1Number(info.getSubscriptionId());
    }

    private void log(String s) {
        Log.d(TAG, s);
    }

    /**
     * For search
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(Context context,
                        boolean enabled) {
                    ArrayList<SearchIndexableResource> result =
                            new ArrayList<SearchIndexableResource>();

                    if (Utils.showSimCardTile(context)) {
                        SearchIndexableResource sir = new SearchIndexableResource(context);
                        sir.xmlResId = R.xml.sim_settings;
                        result.add(sir);
                    }

                    return result;
                }
            };

    private boolean isCallStateIdle() {
        boolean callStateIdle = true;
        for (int i = 0; i < mCallState.length; i++) {
            if (TelephonyManager.CALL_STATE_IDLE != mCallState[i]) {
                callStateIdle = false;
            }
        }
        Log.d(TAG, "isCallStateIdle " + callStateIdle);
        return callStateIdle;
    }


    //Begin added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
    private void addServicePreference() {
        if (TclInterfaceAdapter.is34GServiceOn(mContext)) {
            PreferenceCategory mPreferenceCategoryActivities = (PreferenceCategory)findPreference(KEY_SIM_ACTIVITIES);
            Preference servicePreference = new Preference(mContext);
            servicePreference.setKey(KEY_34G_service);
            servicePreference.setTitle(mContext.getResources().getString(R.string._34G_service_title));
            mPreferenceCategoryActivities.addPreference(servicePreference);
        }
    }

    public void update34GServiceValues(){
        //change SimSettings to PreferenceScreen
        final Preference simPref = findPreference(KEY_34G_service);
        if (simPref != null) {
            CharSequence[] m34gSeviceSummary = mContext.getResources().getTextArray(R.array.service_sim_choices);

            String curr3GSim = SystemProperties.get(PROPERTY_3G_SIM, "");
            Log.d(TAG, "current 3G Sim = " + curr3GSim);
            if (curr3GSim != null && !curr3GSim.equals("")) {
                int curr3GSIMSlot = Integer.parseInt(curr3GSim) - 1;
                int curr3GSubId = SubscriptionManager.getSubId(curr3GSIMSlot)[0];
                int settingsPreferNetworkMode = Settings.Global.getInt(
                        mContext.getContentResolver(),
                        Settings.Global.PREFERRED_NETWORK_MODE + curr3GSubId,
                        Phone.PREFERRED_NT_MODE);
                int settings34gService = Settings.Global.getInt(
                        mContext.getContentResolver(),
                        "_3G4G_service_sim_setting", s34gServiceOFF);
                Log.d(TAG,"settingsPreferNetworkMode = " + settingsPreferNetworkMode
                        + "settings34gService = " + settings34gService
                        + " phoneSubId = " + curr3GSubId);
                // User changed the Network mode before.
                if (settingsPreferNetworkMode == Phone.NT_MODE_GSM_ONLY
                        && settings34gService == s34gServiceOFF) {
                    simPref.setSummary(m34gSeviceSummary[s34gServiceOFF]);
                } else {
                    SubscriptionInfo sir = SubscriptionManager.from(mContext).getActiveSubscriptionInfo(curr3GSubId);
                    if (sir != null) {
                        simPref.setSummary(sir.getDisplayName());
                    } else {
                        simPref.setSummary(m34gSeviceSummary[curr3GSIMSlot]);
                    }
                }
            }
        }

    }
     ///----------------------------------------MTK-----------------------------------------------

    //private static final String KEY_SIM_ACTIVITIES = "sim_activities";
    private static final boolean ENG_LOAD = SystemProperties.get("ro.build.type").equals("eng") ?
            true : false || Log.isLoggable(TAG, Log.DEBUG);

    private SimHotSwapHandler mSimHotSwapHandler;
    private boolean mIsAirplaneModeOn = false;

    private RadioPowerController mRadioController;

    // Receiver to handle different actions
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "mReceiver action = " + action);
            if (action.equals(Intent.ACTION_AIRPLANE_MODE_CHANGED)) {
                handleAirplaneModeChange(intent);
            } else if (action.equals(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED)) {
                updateCellularDataValues();
            } else if (action.equals(TelecomManager.ACTION_PHONE_ACCOUNT_REGISTERED)
                    || action.equals(TelecomManager.ACTION_PHONE_ACCOUNT_UNREGISTERED)) {
                updateCallValues();
            } else if (action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE)
                    || action.equals(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED)) {
                updateActivitesCategory();
            } else if (action.equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
                updateActivitesCategory();
            // listen to radio state chang
            /*
            } else if (action.equals(MtkTelephonyIntents.ACTION_RADIO_STATE_CHANGED)) {
                int subId = intent.getIntExtra("subId", -1);
                if (mRadioController.isRadioSwitchComplete(subId)) {
                    handleRadioPowerSwitchComplete();
                }
            */
            } //Migration change
            //Begin added by zubai.li for XR7080031 telecomcode on 2018/11/07
            else if (action.equals("com.tct.intent.action.RADIO_STATE_CHANGED")) {
                handleRadioPowerSwitchComplete();
            }
            //End added by zubai.li for XR7080031 telecomcode on 2018/11/07

        }
    };

    /**
     * init for sim state change, including SIM hot swap, airplane mode, etc.
     */
    private void initForSimStateChange() {
        /// M: for [SIM Hot Swap] @{
        mSimHotSwapHandler = new SimHotSwapHandler(getActivity().getApplicationContext());
        mSimHotSwapHandler.registerOnSimHotSwap(new OnSimHotSwapListener() {
            @Override
            public void onSimHotSwap() {
                if (getActivity() != null) {
                    log("onSimHotSwap, finish Activity~~");
                    getActivity().finish();
                }
            }
        });
        /// @}

        mIsAirplaneModeOn = TclInterfaceAdapter.isAirplaneModeOn(getActivity().getApplicationContext());
        logInEng("init()... airplane mode is: " + mIsAirplaneModeOn);

        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_AIRPLANE_MODE_CHANGED);
        intentFilter.addAction(TelephonyIntents.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED);

        // For radio on/off
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_DONE);
        intentFilter.addAction(TelephonyIntents.ACTION_SET_RADIO_CAPABILITY_FAILED);
        // listen to radio state
        //intentFilter.addAction(MtkTelephonyIntents.ACTION_RADIO_STATE_CHANGED);
        //Begin added by zubai.li for XR7080031 telecomcode on 2018/11/07
        intentFilter.addAction("com.tct.intent.action.RADIO_STATE_CHANGED");
        //End added by zubai.li for XR7080031 telecomcode on 2018/11/07

        // listen to PHONE_STATE_CHANGE
        intentFilter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        //listen to Telecom Manager event
        intentFilter.addAction(TelecomManager.ACTION_PHONE_ACCOUNT_REGISTERED);
        intentFilter.addAction(TelecomManager.ACTION_PHONE_ACCOUNT_UNREGISTERED);

        //Begin added by zubai.li for XR6168008 telecomcode on 2018.04.04
        intentFilter.addAction(TelephonyIntents.ACTION_ANY_DATA_CONNECTION_STATE_CHANGED);
        //End added by zubai.li for tclplugin XR6168008 telecomcode on 2018.04.04

        getActivity().registerReceiver(mReceiver, intentFilter);
    }

    /**
     * update SIM values after radio switch
     */
    private void handleRadioPowerSwitchComplete() {
        if (isResumed()) {
            updateSimSlotValues();
        }
        logInEng("handleRadioPowerSwitchComplete isResumed =  " + isResumed());
        updateActivitesCategory();
    }

    /**
     * When airplane mode is on, some parts need to be disabled for prevent some telephony issues
     * when airplane on.
     * Default data is not able to switch as may cause modem switch
     * SIM radio power switch need to disable, also this action need operate modem
     * @param airplaneOn airplane mode state true on, false off
     */
    private void handleAirplaneModeChange(Intent intent) {
        mIsAirplaneModeOn = intent.getBooleanExtra("state", false);
        Log.d(TAG, "airplane mode is = " + mIsAirplaneModeOn);
        updateSimSlotValues();
        updateActivitesCategory();
    }

    @Override
    public void onDestroy() {
        logInEng("onDestroy()");
        getActivity().unregisterReceiver(mReceiver);
        mSimHotSwapHandler.unregisterOnSimHotSwap();
        super.onDestroy();
    }

    private boolean shouldEnableSimPref(boolean defaultState) {
        String ecbMode = SystemProperties.get(TelephonyProperties.PROPERTY_INECM_MODE, "false");
        boolean isInEcbMode = false;
        if (ecbMode != null && ecbMode.contains("true")) {
            isInEcbMode = true;
        }
        boolean capSwitching = TclInterfaceAdapter.isCapabilitySwitching(mContext);
        boolean inCall = TelecomManager.from(mContext).isInCall();

        log("defaultState :" + defaultState + ", capSwitching :"
                + capSwitching + ", airplaneModeOn :" + mIsAirplaneModeOn + ", inCall :"
                + inCall + ", ecbMode: " + ecbMode);
        return defaultState && !capSwitching && !mIsAirplaneModeOn && !inCall && !isInEcbMode;
    }

    private void logInEng(String s) {
        if (ENG_LOAD) {
            Log.d(TAG, s);
        }
    }
    //End added by zubo.li for XR6628870 telecomcode sim-settings on 2018.07.23
}
