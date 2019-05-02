package com.android.settings.privatemode;

import com.tct.sdk.base.privacymode.TctPrivacyModeHelper;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;

public class SetupPrivacyModeFinish extends SettingsPreferenceFragment{

    TctPrivacyModeHelper mTctPrivacyModeHelper;

    private static final String PRIVACY_MODE_ON = "privacy_mode_on";
    private static final String CURRENT_PHONE_MODE = "current_phone_mode";
    private static final String SWITCH_TO_NORMAL_MODE_TIMESTAMP = "switch_to_normal_mode_timestamp";

    public static Intent createFragmentIntent(Context context){
        return Utils.onBuildStartFragmentIntent(context, SetupPrivacyModeFinish.class.getName(),
                null, null, R.string.setup_private_mode, null, false, MetricsEvent.SECURITY);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ViewGroup contentRoot = (ViewGroup) getListView().getParent();
        View emptyView = LayoutInflater.from(getContext()).inflate(R.layout.finish_setup_privacy_mode, contentRoot, true);
        setEmptyView(emptyView);

        mTctPrivacyModeHelper = TctPrivacyModeHelper.createHelper(getContext());

        Button btn = (Button) emptyView.findViewById(R.id.enter_private_mode);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mTctPrivacyModeHelper.enterPrivacyMode(true);
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
            }
        });

        Settings.System.putInt(getContentResolver(), PRIVACY_MODE_ON, 1);
        Settings.System.putInt(getContentResolver(), CURRENT_PHONE_MODE, 0);
        Settings.System.putLong(getContentResolver(), SWITCH_TO_NORMAL_MODE_TIMESTAMP, System.currentTimeMillis());
        //Begin added by dongchi.chen for XRP23158 on 2018/08/23
        getActivity().getActionBar().setDisplayHomeAsUpEnabled(false);
        //End added by dongchi.chen for XRP23158 on 2018/08/23
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.SECURITY;
    }
}
