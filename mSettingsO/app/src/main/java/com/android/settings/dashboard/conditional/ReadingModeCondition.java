package com.android.settings.dashboard.conditional;

import android.graphics.drawable.Icon;
import android.provider.Settings;
import android.util.Log;

import com.android.internal.app.NightDisplayController;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.display.ReadingModeSettings;

public final class ReadingModeCondition extends Condition
        implements NightDisplayController.Callback {

    private NightDisplayController mController;
    private boolean isTurnOff = false;
    ReadingModeCondition(ConditionManager manager) {
        super(manager);
        mController = new NightDisplayController(manager.getContext());
        mController.setListener(this);
    }

    @Override
    public int getMetricsConstant() {
        return MetricsEvent.SETTINGS_CONDITION_NIGHT_DISPLAY;//MetricsEvent.SETTINGS_CONDITION_READING_MODE;
    }

    @Override
    public Icon getIcon() {
        return Icon.createWithResource(mManager.getContext(), R.drawable.ic_settings_reading_mode);
    }

    @Override
    public CharSequence getTitle() {
        return mManager.getContext().getString(R.string.condition_reading_mode_title);
    }

    @Override
    public CharSequence getSummary() {
        return mManager.getContext().getString(R.string.condition_reading_mode_summary);
    }

    @Override
    public CharSequence[] getActions() {
        return new CharSequence[] { mManager.getContext().getString(R.string.condition_turn_off) };
    }

    @Override
    public void onPrimaryClick() {
        Utils.startWithFragment(mManager.getContext(), ReadingModeSettings.class.getName(), null,
                null, 0, R.string.reading_mode, null, MetricsEvent.DASHBOARD_SUMMARY);
    }

    @Override
    public void onActionClick(int index) {
        if (index == 0) {
            //mController.setActivated(false);
            setReadingModeActivated(false);
            isTurnOff = true;
            refreshState();
        } else {
            throw new IllegalArgumentException("Unexpected index " + index);
        }
    }

    private void setReadingModeActivated(boolean activated) {
        Settings.System.putInt(mManager.getContext().getContentResolver(),"reading_mode_always_enable", activated ? 1:0);
        if(activated  && mController.isActivated()) {
            mController.setActivated(false);
            if(Settings.Secure.getInt(mManager.getContext().getContentResolver(),"night_theme_enabled", 0) == 1){
                //Begin modify by shilei.zhang for fixed XR7065062 on 2018/10/25
                //Settings.Secure.putInt(mManager.getContext().getContentResolver(), Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED, 0);
                Settings.Secure.putInt(mManager.getContext().getContentResolver(), "night_theme_display_screen", 0);
                //End modify by shilei.zhang for fixed XR7065062 on 2018/10/25
            }
        }
    }

    @Override
    public void refreshState() {
        //setActive(mController.isActivated());
        if(isTurnOff){
            setActive(false);
            isTurnOff = false;
        }else {
            setActive(Settings.Secure.getInt(mManager.getContext().getContentResolver(), "reading_mode_be_on", 0) == 1);
        }
    }
}
