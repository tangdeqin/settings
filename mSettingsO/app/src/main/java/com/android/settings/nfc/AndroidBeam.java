/*
 * Copyright (C) 2011 The Android Open Source Project
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

import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settingslib.HelpUtils;
import com.android.settings.core.InstrumentedPreferenceFragment;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.ShowAdminSupportDetailsDialog;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.RestrictedLockUtils;

import static com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
// added by longxing.pan for Task4644689 on 2017/04/25 begin
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.widget.Toast;
import android.app.ActionBar;
import android.content.Context;
// added by longxing.pan for Task4644689 on 2017/04/25 end

//Begin added by jianhao.zeng for defect6000282 on 2018/02/25
import com.android.settings.search.Indexable;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.SearchIndexableRaw;
import android.content.res.Resources;
import java.util.ArrayList;
import java.util.List;
//End added by jianhao.zeng for defect6000282 on 2018/02/25

public class AndroidBeam extends InstrumentedPreferenceFragment
        implements SwitchBar.OnSwitchChangeListener, Indexable {
    private View mView;
    private NfcAdapter mNfcAdapter;
    private SwitchBar mSwitchBar;
    private CharSequence mOldActivityTitle;
    private boolean mBeamDisallowedByBase;
    private boolean mBeamDisallowedByOnlyAdmin;
    //Modified by longxing.pan for Task4733346 on 2017/05/08 begin
    private boolean isStop = false;
    private IntentFilter mIntentFilter = null;
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            int extra = intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, NfcAdapter.STATE_OFF);
            if (NfcAdapter.ACTION_ADAPTER_STATE_CHANGED.equals(action)) {
                if (!isStop && (extra == NfcAdapter.STATE_OFF || extra == NfcAdapter.STATE_TURNING_OFF)) {
                        Toast.makeText(getActivity(), R.string.nfc_off_beam_exit_toast, Toast.LENGTH_SHORT).show();
                        getActivity().onBackPressed();
                }
            }
        }
    };
    //Modified by longxing.pan for Task4733346 on 2017/05/08 end

    //Added by longxing.pan for Task4733346 on 2017/05/08 begin
    @Override
    public void onStart() {
        super.onStart();
        isStop = false;
        if (!mNfcAdapter.isEnabled()) {
            Toast.makeText(getActivity(), R.string.nfc_off_beam_exit_toast, Toast.LENGTH_SHORT).show();
            //Begin modified by jianhao.zeng for defect6000282 on 2018/02/25
            getActivity().finish();
            //End modified by jianhao.zeng for defect6000282 on 2018/02/25
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        isStop = true;
    }
    //Added by longxing.pan for Task4733346 on 2017/05/08 end

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        HelpUtils.prepareHelpMenuItem(getActivity(), menu, R.string.help_uri_beam,
                getClass().getName());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final EnforcedAdmin admin = RestrictedLockUtils.checkIfRestrictionEnforced(
                getActivity(), UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        final UserManager um = UserManager.get(getActivity());
        mBeamDisallowedByBase = RestrictedLockUtils.hasBaseUserRestriction(getActivity(),
                UserManager.DISALLOW_OUTGOING_BEAM, UserHandle.myUserId());
        if (!mBeamDisallowedByBase && admin != null) {
            View view = inflater.inflate(R.layout.admin_support_details_empty_view, null);
            ShowAdminSupportDetailsDialog.setAdminSupportDetails(getActivity(), view, admin, false);
            view.setVisibility(View.VISIBLE);
            mBeamDisallowedByOnlyAdmin = true;
            return view;
        }
        mView = inflater.inflate(R.layout.android_beam, container, false);
    // added by longxing.pan for Task4644689 on 2017/04/25 begin
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        getActivity().registerReceiver(mReceiver, mIntentFilter);
    // added by longxing.pan for Task4644689 on 2017/04/25 end
        return mView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();

        mOldActivityTitle = activity.getActionBar().getTitle();
        mSwitchBar = activity.getSwitchBar();
        if (mBeamDisallowedByOnlyAdmin) {
            mSwitchBar.hide();
        } else {
            mSwitchBar.setChecked(!mBeamDisallowedByBase && mNfcAdapter.isNdefPushEnabled());
            mSwitchBar.addOnSwitchChangeListener(this);
            mSwitchBar.setEnabled(!mBeamDisallowedByBase);
            mSwitchBar.show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    // added by longxing.pan for Task4644689 on 2017/04/25 begin
        if (mReceiver != null) {
            getActivity().unregisterReceiver(mReceiver);
        }
    // added by longxing.pan for Task4644689 on 2017/04/25 end
        if (mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(mOldActivityTitle);
        }
        if (!mBeamDisallowedByOnlyAdmin) {
            mSwitchBar.removeOnSwitchChangeListener(this);
            mSwitchBar.hide();
        }
    }

    @Override
    public void onSwitchChanged(Switch switchView, boolean desiredState) {
        boolean success = false;
        mSwitchBar.setEnabled(false);
        if (desiredState) {
            success = mNfcAdapter.enableNdefPush();
        } else {
            success = mNfcAdapter.disableNdefPush();
        }
        if (success) {
            mSwitchBar.setChecked(desiredState);
        }
        mSwitchBar.setEnabled(true);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.NFC_BEAM;
    }

    //Begin added by jianhao.zeng for defect6000282 on 2018/02/25
    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context, boolean enabled) {
                    final List<SearchIndexableRaw> result = new ArrayList<SearchIndexableRaw>();
                    final Resources res = context.getResources();
                    SearchIndexableRaw data = new SearchIndexableRaw(context);
                    data.title = res.getString(R.string.android_beam_settings_title);
                    data.screenTitle = res.getString(R.string.android_beam_settings_title);
                    data.iconResId = R.drawable.ic_android;
                    result.add(data);
                    return result;
                }
            };
    //End added by jianhao.zeng for defect6000282 on 2018/02/25
}
